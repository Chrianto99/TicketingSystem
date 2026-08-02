package com.Chrianto.TicketingSystem.entity.enums;

public enum UserRole {
    USER("Χρήστης"),
    ADMIN("Διαχειριστής");

    private final String displayName;

    UserRole(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
