package org.onextel.db2_pick_app.util;

import java.util.UUID;

public class UUIDGenerator {
    public String generateKey() {
        return UUID.randomUUID().toString();
    }
}
