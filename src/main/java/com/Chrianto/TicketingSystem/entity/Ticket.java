package com.Chrianto.TicketingSystem.entity;

import com.Chrianto.TicketingSystem.entity.enums.TicketPriority;
import com.Chrianto.TicketingSystem.entity.enums.TicketSource;
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

    @ManyToOne @JoinColumn(name = "category_id")
    private Category category;

    @ManyToOne @JoinColumn(name = "subcategory_id")
    private Subcategory subcategory;

//    @ManyToOne
//    @JoinColumn(name = "incident_report_id")
//    private IncidentReport incidentReport;

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

//    @Enumerated(EnumType.STRING)
//    @Column(nullable = false)
//    private TicketSource source;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
