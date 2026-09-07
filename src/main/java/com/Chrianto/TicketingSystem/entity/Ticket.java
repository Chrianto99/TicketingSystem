package com.Chrianto.TicketingSystem.entity;

import com.Chrianto.TicketingSystem.entity.enums.TicketPriority;
import com.Chrianto.TicketingSystem.entity.enums.TicketSource;
import com.Chrianto.TicketingSystem.entity.enums.TicketStatus;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

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

    // Offer pool for the "assign to multiple, first to claim gets it" flow —
    // populated only while the ticket is up for grabs (assignedUser is null in
    // that state); cleared as soon as someone claims it or gets assigned directly.
    @ManyToMany
    @JoinTable(name = "ticket_candidate",
            joinColumns = @JoinColumn(name = "ticket_id"),
            inverseJoinColumns = @JoinColumn(name = "user_id"))
    private List<User> candidates = new ArrayList<>();

    @ManyToOne @JoinColumn(name = "department_id")
    private Department department;

    @ManyToOne @JoinColumn(name = "category_id")
    private Category category;

    @ManyToOne @JoinColumn(name = "subcategory_id")
    private Subcategory subcategory;

    @ManyToOne
    @JoinColumn(name = "incident_id")
    private Incident incident;

    @Enumerated(EnumType.STRING)
    private TicketStatus status;

    @Enumerated(EnumType.STRING)
    private TicketPriority priority;

    private String callerName;

    private String phoneNumber;

    private String ipAddress;

    // physical column stays "description" so existing summary text isn't orphaned by the rename
    @Column(name = "description", length = 2000)
    private String summary;

    @Column(name = "details", length = 2000)
    private String description;

    @Column(length = 2000)
    private String resolution;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TicketSource source;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
