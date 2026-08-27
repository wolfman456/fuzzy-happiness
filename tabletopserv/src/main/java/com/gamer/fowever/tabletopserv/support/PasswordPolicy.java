package com.gamer.fowever.tabletopserv.support;

public final class PasswordPolicy {

    public static final String REGEX = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[^A-Za-z0-9]).{8,}$";
    public static final String MESSAGE = "Password must be at least 8 characters and include an uppercase letter, a lowercase letter, a digit, and a special character.";
    public static final int MIN_AGE = 13;

    private PasswordPolicy() {
    }
}