package com.Chrianto.TicketingSystem.entity;

import com.Chrianto.TicketingSystem.entity.enums.TicketAction;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "ticket_history")
@Getter @Setter @NoArgsConstructor
public class TicketHistory {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne @JoinColumn(name = "comment_id")
    private Comment comment;

    @Column(length = 2000)
    private String description;

    @NonNull @ManyToOne @JoinColumn(name = "ticket_id")
    private Ticket ticket;

    @NonNull @ManyToOne @JoinColumn(name = "performed_by_id")
    private User performedBy;

    @ManyToOne @JoinColumn(name = "assigned_to_id")
    private User assignedTo;

    @NonNull @Enumerated(EnumType.STRING)
    private TicketAction action;

    private LocalDateTime timestamp;
}
