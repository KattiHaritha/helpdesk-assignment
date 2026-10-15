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
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TicketServiceTest {

    @Mock TicketRepository ticketRepository;
    @Mock UserRepository userRepository;
    @Mock CommentRepository commentRepository;
    @Mock HistoryRepository historyRepository;
    @InjectMocks TicketService ticketService;

    // ---------- helpers ----------
    private static Organization org(Long id) {
        Organization o = new Organization();
        o.setId(id);
        o.setName("Org " + id);
        return o;
    }

    private static User user(Long id, Role role, Organization org) {
        User u = new User();
        u.setId(id);
        u.setName("User " + id);
        u.setEmail("u" + id + "@test.com");
        u.setRole(role);
        u.setOrganization(org);
        return u;
    }

    private static Ticket ticket(Long id, Organization org, User requester) {
        Ticket t = new Ticket();
        t.setId(id);
        t.setSubject("Subject");
        t.setDescription("Description");
        t.setPriority(Priority.MEDIUM);
        t.setStatus(Status.OPEN);
        t.setRequester(requester);
        t.setOrganization(org);
        return t;
    }

    private static AuthPrincipal principal(User u) {
        return new AuthPrincipal(u.getId(), u.getOrganization().getId(), u.getRole(), u.getEmail());
    }

    // ---------- tests ----------

    @Test
    void create_setsOrganizationRequesterStatusAndDefaultPriority() {
        Organization org = org(1L);
        User customer = user(3L, Role.CUSTOMER, org);
        when(userRepository.findByIdAndOrganizationId(3L, 1L)).thenReturn(Optional.of(customer));
        when(ticketRepository.save(any(Ticket.class))).thenAnswer(inv -> inv.getArgument(0));

        TicketResponse res = ticketService.create(new CreateTicketRequest("Printer broken", "It smokes", null),
                principal(customer));

        assertEquals(1L, res.organizationId());
        assertEquals(3L, res.requester().id());
        assertEquals(Status.OPEN, res.status());
        assertEquals(Priority.MEDIUM, res.priority());
        assertNull(res.assignedAgent());
    }

    @Test
    void get_ticketOfAnotherOrganization_isNotFound() {
        Organization org1 = org(1L);
        User agent = user(2L, Role.AGENT, org1);
        // repository is queried with the caller's org id, so a foreign ticket is never returned
        when(ticketRepository.findByIdAndOrganizationId(99L, 1L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> ticketService.get(99L, principal(agent)));
    }

    @Test
    void get_customerCannotSeeAnotherCustomersTicket() {
        Organization org = org(1L);
        User owner = user(10L, Role.CUSTOMER, org);
        User other = user(11L, Role.CUSTOMER, org);
        when(ticketRepository.findByIdAndOrganizationId(5L, 1L)).thenReturn(Optional.of(ticket(5L, org, owner)));

        assertThrows(ResourceNotFoundException.class, () -> ticketService.get(5L, principal(other)));
    }

    @Test
    void summary_staffCountsOnlyTicketsInTheirOrganization() {
        Organization org = org(1L);
        User agent = user(2L, Role.AGENT, org);
        when(ticketRepository.countVisibleByOrganizationAndRequesterAndStatus(1L, null, null)).thenReturn(7L);
        when(ticketRepository.countVisibleByOrganizationAndRequesterAndStatus(1L, null, Status.OPEN)).thenReturn(2L);
        when(ticketRepository.countVisibleByOrganizationAndRequesterAndStatus(1L, null, Status.IN_PROGRESS)).thenReturn(3L);
        when(ticketRepository.countVisibleByOrganizationAndRequesterAndStatus(1L, null, Status.RESOLVED)).thenReturn(1L);
        when(ticketRepository.countVisibleByOrganizationAndRequesterAndStatus(1L, null, Status.CLOSED)).thenReturn(1L);

        TicketSummaryResponse summary = ticketService.summary(principal(agent));

        assertEquals(new TicketSummaryResponse(7, 2, 3, 1, 1), summary);
        verify(ticketRepository, times(5))
                .countVisibleByOrganizationAndRequesterAndStatus(eq(1L), isNull(), any());
    }

    @Test
    void summary_customerCountsOnlyTheirOwnTickets() {
        Organization org = org(1L);
        User customer = user(10L, Role.CUSTOMER, org);
        when(ticketRepository.countVisibleByOrganizationAndRequesterAndStatus(1L, 10L, null)).thenReturn(4L);
        when(ticketRepository.countVisibleByOrganizationAndRequesterAndStatus(1L, 10L, Status.OPEN)).thenReturn(1L);
        when(ticketRepository.countVisibleByOrganizationAndRequesterAndStatus(1L, 10L, Status.IN_PROGRESS)).thenReturn(1L);
        when(ticketRepository.countVisibleByOrganizationAndRequesterAndStatus(1L, 10L, Status.RESOLVED)).thenReturn(0L);
        when(ticketRepository.countVisibleByOrganizationAndRequesterAndStatus(1L, 10L, Status.CLOSED)).thenReturn(2L);

        TicketSummaryResponse summary = ticketService.summary(principal(customer));

        assertEquals(new TicketSummaryResponse(4, 1, 1, 0, 2), summary);
        verify(ticketRepository, times(5))
                .countVisibleByOrganizationAndRequesterAndStatus(eq(1L), eq(10L), any());
    }

    @Test
    void assign_agentFromAnotherOrganization_isRejected() {
        Organization org1 = org(1L);
        User agent = user(2L, Role.AGENT, org1);
        User customer = user(3L, Role.CUSTOMER, org1);
        when(ticketRepository.findByIdAndOrganizationId(5L, 1L)).thenReturn(Optional.of(ticket(5L, org1, customer)));
        when(userRepository.findByIdAndOrganizationId(50L, 1L)).thenReturn(Optional.empty());

        assertThrows(BadRequestException.class, () -> ticketService.assign(5L, 50L, principal(agent)));
        verify(ticketRepository, never()).save(any());
    }

    @Test
    void assign_toCustomerUser_isRejected() {
        Organization org = org(1L);
        User admin = user(1L, Role.ORG_ADMIN, org);
        User customer = user(3L, Role.CUSTOMER, org);
        when(ticketRepository.findByIdAndOrganizationId(5L, 1L)).thenReturn(Optional.of(ticket(5L, org, customer)));
        when(userRepository.findByIdAndOrganizationId(3L, 1L)).thenReturn(Optional.of(customer));

        assertThrows(BadRequestException.class, () -> ticketService.assign(5L, 3L, principal(admin)));
    }

    @Test
    void assign_customerCallerIsForbidden() {
        Organization org = org(1L);
        User customer = user(3L, Role.CUSTOMER, org);

        assertThrows(AccessDeniedException.class, () -> ticketService.assign(5L, 2L, principal(customer)));
    }

    @Test
    void changeStatus_savesHistoryEntry() {
        Organization org = org(1L);
        User agent = user(2L, Role.AGENT, org);
        User customer = user(3L, Role.CUSTOMER, org);
        Ticket t = ticket(5L, org, customer);
        when(ticketRepository.findByIdAndOrganizationId(5L, 1L)).thenReturn(Optional.of(t));
        when(userRepository.findByIdAndOrganizationId(2L, 1L)).thenReturn(Optional.of(agent));
        when(ticketRepository.save(any(Ticket.class))).thenAnswer(inv -> inv.getArgument(0));

        TicketResponse res = ticketService.changeStatus(5L, Status.IN_PROGRESS, principal(agent));

        assertEquals(Status.IN_PROGRESS, res.status());
        ArgumentCaptor<TicketHistory> captor = ArgumentCaptor.forClass(TicketHistory.class);
        verify(historyRepository).save(captor.capture());
        TicketHistory h = captor.getValue();
        assertEquals(HistoryType.STATUS_CHANGE, h.getType());
        assertEquals("OPEN", h.getOldValue());
        assertEquals("IN_PROGRESS", h.getNewValue());
        assertEquals(2L, h.getChangedBy().getId());
    }

    @Test
    void changeStatus_customerCannotSetStatusOtherThanClosed() {
        Organization org = org(1L);
        User customer = user(3L, Role.CUSTOMER, org);
        when(ticketRepository.findByIdAndOrganizationId(5L, 1L)).thenReturn(Optional.of(ticket(5L, org, customer)));

        assertThrows(AccessDeniedException.class,
                () -> ticketService.changeStatus(5L, Status.RESOLVED, principal(customer)));
        verify(historyRepository, never()).save(any());
    }

    @Test
    void assign_savesAssignmentHistory() {
        Organization org = org(1L);
        User admin = user(1L, Role.ORG_ADMIN, org);
        User agent = user(2L, Role.AGENT, org);
        User customer = user(3L, Role.CUSTOMER, org);
        when(ticketRepository.findByIdAndOrganizationId(5L, 1L)).thenReturn(Optional.of(ticket(5L, org, customer)));
        when(userRepository.findByIdAndOrganizationId(2L, 1L)).thenReturn(Optional.of(agent));
        when(userRepository.findByIdAndOrganizationId(1L, 1L)).thenReturn(Optional.of(admin));
        when(ticketRepository.save(any(Ticket.class))).thenAnswer(inv -> inv.getArgument(0));

        TicketResponse res = ticketService.assign(5L, 2L, principal(admin));

        assertEquals(2L, res.assignedAgent().id());
        ArgumentCaptor<TicketHistory> captor = ArgumentCaptor.forClass(TicketHistory.class);
        verify(historyRepository).save(captor.capture());
        assertEquals(HistoryType.ASSIGNMENT_CHANGE, captor.getValue().getType());
        assertEquals("Unassigned", captor.getValue().getOldValue());
        assertEquals("User 2", captor.getValue().getNewValue());
    }

    @Test
    void addComment_customerInternalFlagIsIgnored() {
        Organization org = org(1L);
        User customer = user(10L, Role.CUSTOMER, org);
        when(ticketRepository.findByIdAndOrganizationId(5L, 1L)).thenReturn(Optional.of(ticket(5L, org, customer)));
        when(userRepository.findByIdAndOrganizationId(10L, 1L)).thenReturn(Optional.of(customer));
        when(commentRepository.save(any(TicketComment.class))).thenAnswer(inv -> inv.getArgument(0));

        CommentResponse res = ticketService.addComment(5L, new CommentRequest("hello", true), principal(customer));

        assertFalse(res.internal());
    }

    @Test
    void addComment_agentCanCreateInternalNote() {
        Organization org = org(1L);
        User agent = user(2L, Role.AGENT, org);
        User customer = user(10L, Role.CUSTOMER, org);
        when(ticketRepository.findByIdAndOrganizationId(5L, 1L)).thenReturn(Optional.of(ticket(5L, org, customer)));
        when(userRepository.findByIdAndOrganizationId(2L, 1L)).thenReturn(Optional.of(agent));
        when(commentRepository.save(any(TicketComment.class))).thenAnswer(inv -> inv.getArgument(0));

        CommentResponse res = ticketService.addComment(5L, new CommentRequest("private note", true), principal(agent));

        assertTrue(res.internal());
    }

    @Test
    void listComments_customerOnlyGetsPublicComments() {
        Organization org = org(1L);
        User customer = user(10L, Role.CUSTOMER, org);
        User agent = user(2L, Role.AGENT, org);
        when(ticketRepository.findByIdAndOrganizationId(5L, 1L)).thenReturn(Optional.of(ticket(5L, org, customer)));
        TicketComment publicComment = new TicketComment();
        publicComment.setId(1L);
        publicComment.setAuthor(agent);
        publicComment.setContent("visible");
        when(commentRepository.findByTicketIdAndInternalFalseOrderByCreatedAtAsc(5L)).thenReturn(List.of(publicComment));

        List<CommentResponse> res = ticketService.listComments(5L, principal(customer));

        assertEquals(1, res.size());
        verify(commentRepository, never()).findByTicketIdOrderByCreatedAtAsc(any());
    }
}
