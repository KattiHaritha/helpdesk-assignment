package com.haritha.helpdesk.controller;

import com.haritha.helpdesk.dto.Dtos.*;
import com.haritha.helpdesk.entity.Priority;
import com.haritha.helpdesk.entity.Status;
import com.haritha.helpdesk.security.AuthPrincipal;
import com.haritha.helpdesk.service.TicketService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/tickets")
@Tag(name = "Tickets")
@SecurityRequirement(name = "bearerAuth")
public class TicketController {
    private final TicketService ticketService;

    public TicketController(TicketService ticketService) {
        this.ticketService = ticketService;
    }

    @Operation(summary = "Create a ticket")
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public TicketResponse create(@Valid @RequestBody CreateTicketRequest req, @AuthenticationPrincipal AuthPrincipal p) {
        return ticketService.create(req, p);
    }

    @Operation(summary = "Search/filter tickets with pagination")
    @GetMapping
    public PageResponse<TicketResponse> list(@RequestParam(required = false) Status status,
                                             @RequestParam(required = false) Priority priority,
                                             @RequestParam(required = false) Long assignedAgentId,
                                             @RequestParam(required = false) String search,
                                             @RequestParam(defaultValue = "0") int page,
                                             @RequestParam(defaultValue = "10") int size,
                                             @AuthenticationPrincipal AuthPrincipal p) {
        return ticketService.search(status, priority, assignedAgentId, search, page, size, p);
    }

    @Operation(summary = "Get ticket totals for the current organization and user")
    @GetMapping("/summary")
    public TicketSummaryResponse summary(@AuthenticationPrincipal AuthPrincipal p) {
        return ticketService.summary(p);
    }

    @Operation(summary = "Get one ticket")
    @GetMapping("/{id}")
    public TicketResponse get(@PathVariable Long id, @AuthenticationPrincipal AuthPrincipal p) {
        return ticketService.get(id, p);
    }

    @Operation(summary = "Update subject/description/priority (agent/admin)")
    @PutMapping("/{id}")
    public TicketResponse update(@PathVariable Long id, @Valid @RequestBody UpdateTicketRequest req,
                                 @AuthenticationPrincipal AuthPrincipal p) {
        return ticketService.update(id, req, p);
    }

    @Operation(summary = "Change status (agent/admin; customers may only close their own ticket)")
    @PatchMapping("/{id}/status")
    public TicketResponse changeStatus(@PathVariable Long id, @Valid @RequestBody StatusRequest req,
                                       @AuthenticationPrincipal AuthPrincipal p) {
        return ticketService.changeStatus(id, req.status(), p);
    }

    @Operation(summary = "Close a ticket")
    @PostMapping("/{id}/close")
    public TicketResponse close(@PathVariable Long id, @AuthenticationPrincipal AuthPrincipal p) {
        return ticketService.changeStatus(id, Status.CLOSED, p);
    }

    @Operation(summary = "Assign (or unassign with agentId=null) a ticket (agent/admin)")
    @PatchMapping("/{id}/assign")
    public TicketResponse assign(@PathVariable Long id, @RequestBody AssignRequest req,
                                 @AuthenticationPrincipal AuthPrincipal p) {
        return ticketService.assign(id, req.agentId(), p);
    }

    @Operation(summary = "Add a comment (internal=true only honoured for agents/admins)")
    @PostMapping("/{id}/comments")
    @ResponseStatus(HttpStatus.CREATED)
    public CommentResponse addComment(@PathVariable Long id, @Valid @RequestBody CommentRequest req,
                                      @AuthenticationPrincipal AuthPrincipal p) {
        return ticketService.addComment(id, req, p);
    }

    @Operation(summary = "List comments (customers never see internal notes)")
    @GetMapping("/{id}/comments")
    public List<CommentResponse> comments(@PathVariable Long id, @AuthenticationPrincipal AuthPrincipal p) {
        return ticketService.listComments(id, p);
    }

    @Operation(summary = "Status and assignment history")
    @GetMapping("/{id}/history")
    public List<HistoryResponse> history(@PathVariable Long id, @AuthenticationPrincipal AuthPrincipal p) {
        return ticketService.history(id, p);
    }
}
