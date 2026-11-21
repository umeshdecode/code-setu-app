package com.cargosetu.push.util;

import com.cargosetu.config.dto.LoggedInUser;
import com.cargosetu.push.model.PrincipalType;

/**
 * Resolves which account table a logged-in principal actually belongs to.
 * OtpService's customer login reuses LoggedInUser.staffId to carry the
 * Customer's own primary key and tags the session with role "CUSTOMER"
 * (see OtpService#237) - everything else is a real Staff login.
 */
public record PrincipalRef(PrincipalType type, Long id) {

    public static PrincipalRef of(LoggedInUser user) {
        PrincipalType type = user.getRoles() != null && user.getRoles().contains("CUSTOMER")
                ? PrincipalType.CUSTOMER
                : PrincipalType.STAFF;
        return new PrincipalRef(type, user.getStaffId());
    }

    public String recipientKey() {
        return type.name() + ":" + id;
    }

    /** Parses the "STAFF:12" / "CUSTOMER:7" strings packed into GenericNotificationEvent.recipients(). */
    public static PrincipalRef parse(String recipientKey) {
        String[] parts = recipientKey.split(":", 2);
        if (parts.length != 2) {
            throw new IllegalArgumentException("Invalid push recipient key: " + recipientKey);
        }
        return new PrincipalRef(PrincipalType.valueOf(parts[0]), Long.valueOf(parts[1]));
    }
}
