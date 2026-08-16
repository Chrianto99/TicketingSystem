package com.Chrianto.TicketingSystem.entity.enums;

public enum IncidentStatus {

        OPEN("Ανοιχτό"),
        CLOSED("Κλειστό");

        private final String displayName;

        IncidentStatus(String displayName) {
            this.displayName = displayName;
        }

        public String getDisplayName() {
            return displayName;
        }

}
