package com.funding.security;

import lombok.Getter;

@Getter
public enum UserRole {
    ADMIN("ROLE_ADMIN"),
    USER("ROLE_USER"),
	ARTIST("ROLE_ARTIST");

    UserRole(String value) {
        this.value = value;
    }

    private String value;
}