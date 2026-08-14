package com.Chrianto.TicketingSystem.entity.enums;

public enum TicketPriority {
    LOW("Χαμηλή"),
    MEDIUM("Μεσαία"),
    HIGH("Υψηλή");

    private final String displayName;

    TicketPriority(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
