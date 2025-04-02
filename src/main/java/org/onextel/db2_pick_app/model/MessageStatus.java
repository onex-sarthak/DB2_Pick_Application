package org.onextel.db2_pick_app.model;

public enum MessageStatus {
    PENDING(0),
    PROCESSING(1),
    COMPLETED(2),
    FAILED(3);

    private final Integer dbValue;

    MessageStatus(Integer dbValue) {
        this.dbValue = dbValue;
    }

    public Integer getDbValue() {
        return dbValue;
    }

    public static MessageStatus fromDbValue(Integer dbValue) {
        for (MessageStatus status : values()) {
            if (status.getDbValue().equals(dbValue)) {
                return status;
            }
        }
        throw new IllegalArgumentException("Unknown status: " + dbValue);
    }
}
