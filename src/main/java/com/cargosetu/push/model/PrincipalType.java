package com.cargosetu.push.model;

/**
 * Which login table a push token's owner belongs to. Staff logins and OTP
 * Customer logins both populate LoggedInUser.staffId with their own table's
 * primary key, so a bare "user id" would let the two collide - a token is
 * always owned by (principalType, principalId) together.
 */
public enum PrincipalType {
    STAFF,
    CUSTOMER
}
