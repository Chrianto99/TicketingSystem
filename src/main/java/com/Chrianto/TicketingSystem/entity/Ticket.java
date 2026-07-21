package com.Chrianto.TicketingSystem.entity;

import com.Chrianto.TicketingSystem.entity.enums.TicketPriority;
import com.Chrianto.TicketingSystem.entity.enums.TicketStatus;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "ticket")
@Getter @Setter @NoArgsConstructor
public class Ticket {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne @JoinColumn(name = "creator_id")
    private User creator;

    @ManyToOne @JoinColumn(name = "last_modified_id")
    private User lastModifiedBy;

    @ManyToOne @JoinColumn(name = "assigned_user_id")
    private User assignedUser;

    @ManyToOne @JoinColumn(name = "department_id")
    private Department department;

    @ManyToOne @JoinColumn(name = "problem_category_id")
    private ProblemType problemType;

    private String callerName;

    @Enumerated(EnumType.STRING)
    private TicketStatus status;

    @Enumerated(EnumType.STRING)
    private TicketPriority priority;

    private String phoneNumber;

    private String ipAddress;

    @Column(length = 2000)
    private String description;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
