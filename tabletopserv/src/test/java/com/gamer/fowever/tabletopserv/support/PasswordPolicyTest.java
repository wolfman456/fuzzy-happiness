package com.gamer.fowever.tabletopserv.support;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;

class PasswordPolicyTest {

    @ParameterizedTest
    @ValueSource(strings = {"Password1!", "Tr0ub4dor&3!", "aB1!cdef", "p@ssW0rd9Z", "Correct Horse#7"})
    void acceptsStrongPasswords(String password) {
        assertThat(password).matches(PasswordPolicy.REGEX);
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "short1!",
            "ALLUPPER1!",
            "alllower1!",
            "NoDigits!",
            "NoSpecial1",
            "abcdefgh",
            "Ab1!cd"
    })
    void rejectsWeakPasswords(String password) {
        assertThat(password).doesNotMatch(PasswordPolicy.REGEX);
    }
}