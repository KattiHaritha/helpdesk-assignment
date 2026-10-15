package com.haritha.helpdesk.service;

import com.haritha.helpdesk.dto.Dtos.*;
import com.haritha.helpdesk.entity.*;
import com.haritha.helpdesk.exception.BadRequestException;
import com.haritha.helpdesk.exception.ResourceNotFoundException;
import com.haritha.helpdesk.repository.CommentRepository;
import com.haritha.helpdesk.repository.HistoryRepository;
import com.haritha.helpdesk.repository.TicketRepository;
import com.haritha.helpdesk.repository.UserRepository;
import com.haritha.helpdesk.security.AuthPrincipal;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Business logic for tickets, comments and history.
 * Tenant isolation: the organization id ALWAYS comes from the logged-in user's JWT (AuthPrincipal),
 * never from request data. Tickets of other organizations are reported as 404.
 */
@Service
public class TicketService {
    private final TicketRepository ticketRepository;
    private final UserRepository userRepository;
    private final CommentRepository commentRepository;
    private final HistoryRepository historyRepository;

    public TicketService(TicketRepository ticketRepository, UserRepository userRepository,
                         CommentRepository commentRepository, HistoryRepository historyRepository) {
        this.ticketRepository = ticketRepository;
        this.userRepository = userRepository;
        this.commentRepository = commentRepository;
        this.historyRepository = historyRepository;
    }

    // ------------------------------------------------------------------ tickets

    @Transactional
    public TicketResponse create(CreateTicketRequest req, AuthPrincipal p) {
        User requester = loadUser(p);
        Ticket t = new Ticket();
        t.setSubject(req.subject().trim());
        t.setDescription(req.description().trim());
        t.setPriority(req.priority() != null ? req.priority() : Priority.MEDIUM);
        t.setStatus(Status.OPEN);
        t.setRequester(requester);
        t.setOrganization(requester.getOrganization());
        return toResponse(ticketRepository.save(t));
    }

    @Transactional(readOnly = true)
    public TicketResponse get(Long id, AuthPrincipal p) {
        return toResponse(findVisible(id, p));
    }

    @Transactional(readOnly = true)
    public PageResponse<TicketResponse> search(Status status, Priority priority, Long assignedAgentId,
                                               String text, int page, int size, AuthPrincipal p) {
        Long requesterId = p.role() == Role.CUSTOMER ? p.userId() : null; // customers only see their own
        Specification<Ticket> spec = TicketSpecifications.filter(p.orgId(), requesterId, status, priority,
                assignedAgentId, text);
        Pageable pageable = PageRequest.of(Math.max(page, 0), Math.min(Math.max(size, 1), 100),
                Sort.by(Sort.Direction.DESC, "createdAt"));
        return PageResponse.of(ticketRepository.findAll(spec, pageable).map(TicketService::toResponse));
    }

    @Transactional(readOnly = true)
    public TicketSummaryResponse summary(AuthPrincipal p) {
        Long requesterId = p.role() == Role.CUSTOMER ? p.userId() : null;
        return new TicketSummaryResponse(
                ticketRepository.countVisibleByOrganizationAndRequesterAndStatus(p.orgId(), requesterId, null),
                ticketRepository.countVisibleByOrganizationAndRequesterAndStatus(p.orgId(), requesterId, Status.OPEN),
                ticketRepository.countVisibleByOrganizationAndRequesterAndStatus(p.orgId(), requesterId, Status.IN_PROGRESS),
                ticketRepository.countVisibleByOrganizationAndRequesterAndStatus(p.orgId(), requesterId, Status.RESOLVED),
                ticketRepository.countVisibleByOrganizationAndRequesterAndStatus(p.orgId(), requesterId, Status.CLOSED));
    }

    @Transactional
    public TicketResponse update(Long id, UpdateTicketRequest req, AuthPrincipal p) {
        requireStaff(p);
        Ticket t = findVisible(id, p);
        t.setSubject(req.subject().trim());
        t.setDescription(req.description().trim());
        t.setPriority(req.priority());
        return toResponse(ticketRepository.save(t));
    }

    @Transactional
    public TicketResponse changeStatus(Long id, Status newStatus, AuthPrincipal p) {
        Ticket t = findVisible(id, p);
        if (p.role() == Role.CUSTOMER && newStatus != Status.CLOSED) {
            throw new AccessDeniedException("Customers can only close their own tickets");
        }
        if (t.getStatus() == newStatus) {
            return toResponse(t);
        }
        String oldValue = t.getStatus().name();
        t.setStatus(newStatus);
        Ticket saved = ticketRepository.save(t);
        recordHistory(saved, HistoryType.STATUS_CHANGE, oldValue, newStatus.name(), loadUser(p));
        return toResponse(saved);
    }

    @Transactional
    public TicketResponse assign(Long id, Long agentId, AuthPrincipal p) {
        requireStaff(p);
        Ticket t = findVisible(id, p);
        User newAgent = null;
        if (agentId != null) {
            // agent must belong to the SAME organization
            newAgent = userRepository.findByIdAndOrganizationId(agentId, p.orgId())
                    .orElseThrow(() -> new BadRequestException("Agent not found in your organization"));
            if (newAgent.getRole() == Role.CUSTOMER) {
                throw new BadRequestException("Tickets can only be assigned to agents or admins");
            }
        }
        Long currentId = t.getAssignedAgent() == null ? null : t.getAssignedAgent().getId();
        Long newId = newAgent == null ? null : newAgent.getId();
        if (java.util.Objects.equals(currentId, newId)) {
            return toResponse(t);
        }
        String oldValue = agentLabel(t.getAssignedAgent());
        t.setAssignedAgent(newAgent);
        Ticket saved = ticketRepository.save(t);
        recordHistory(saved, HistoryType.ASSIGNMENT_CHANGE, oldValue, agentLabel(newAgent), loadUser(p));
        return toResponse(saved);
    }

    // ------------------------------------------------------------------ comments

    @Transactional
    public CommentResponse addComment(Long ticketId, CommentRequest req, AuthPrincipal p) {
        Ticket t = findVisible(ticketId, p);
        User author = loadUser(p);
        TicketComment c = new TicketComment();
        c.setTicket(t);
        c.setAuthor(author);
        c.setContent(req.content().trim());
        // customers can never create internal notes, whatever the request says
        c.setInternal(req.internal() && p.role() != Role.CUSTOMER);
        return toResponse(commentRepository.save(c), ticketId);
    }

    @Transactional(readOnly = true)
    public List<CommentResponse> listComments(Long ticketId, AuthPrincipal p) {
        findVisible(ticketId, p);
        List<TicketComment> comments = p.role() == Role.CUSTOMER
                ? commentRepository.findByTicketIdAndInternalFalseOrderByCreatedAtAsc(ticketId)
                : commentRepository.findByTicketIdOrderByCreatedAtAsc(ticketId);
        return comments.stream().map(c -> toResponse(c, ticketId)).toList();
    }

    // ------------------------------------------------------------------ history

    @Transactional(readOnly = true)
    public List<HistoryResponse> history(Long ticketId, AuthPrincipal p) {
        findVisible(ticketId, p);
        return historyRepository.findByTicketIdOrderByCreatedAtAsc(ticketId).stream()
                .map(h -> new HistoryResponse(h.getId(), h.getType(), h.getOldValue(), h.getNewValue(),
                        summary(h.getChangedBy()), h.getCreatedAt()))
                .toList();
    }

    // ------------------------------------------------------------------ helpers

    /** Tenant-safe lookup. Other org's ticket (or another customer's ticket) => 404. */
    private Ticket findVisible(Long id, AuthPrincipal p) {
        Ticket t = ticketRepository.findByIdAndOrganizationId(id, p.orgId())
                .orElseThrow(() -> new ResourceNotFoundException("Ticket not found"));
        if (p.role() == Role.CUSTOMER && !t.getRequester().getId().equals(p.userId())) {
            throw new ResourceNotFoundException("Ticket not found");
        }
        return t;
    }

    private User loadUser(AuthPrincipal p) {
        return userRepository.findByIdAndOrganizationId(p.userId(), p.orgId())
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
    }

    private void requireStaff(AuthPrincipal p) {
        if (!p.isStaff()) {
            throw new AccessDeniedException("Only agents and admins can perform this action");
        }
    }

    private void recordHistory(Ticket t, HistoryType type, String oldValue, String newValue, User by) {
        TicketHistory h = new TicketHistory();
        h.setTicket(t);
        h.setType(type);
        h.setOldValue(oldValue);
        h.setNewValue(newValue);
        h.setChangedBy(by);
        historyRepository.save(h);
    }

    private static String agentLabel(User u) {
        return u == null ? "Unassigned" : u.getName();
    }

    private static UserSummary summary(User u) {
        return u == null ? null : new UserSummary(u.getId(), u.getName());
    }

    static TicketResponse toResponse(Ticket t) {
        return new TicketResponse(t.getId(), t.getSubject(), t.getDescription(), t.getPriority(), t.getStatus(),
                summary(t.getRequester()), summary(t.getAssignedAgent()), t.getOrganization().getId(),
                t.getCreatedAt(), t.getUpdatedAt());
    }

    private static CommentResponse toResponse(TicketComment c, Long ticketId) {
        return new CommentResponse(c.getId(), ticketId, summary(c.getAuthor()), c.getContent(),
                c.isInternal(), c.getCreatedAt());
    }
}
