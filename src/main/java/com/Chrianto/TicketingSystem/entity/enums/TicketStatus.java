package com.Chrianto.TicketingSystem.entity.enums;


public enum TicketStatus {
    OPEN("Ανοιχτό"),
    RESOLVED("Επιλύθηκε"),
    CANCELLED("Ακυρώθηκε");

    private final String displayName;

    TicketStatus(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
