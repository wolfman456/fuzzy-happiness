package com.gamer.fowever.tabletopapi.dto;

import com.gamer.fowever.tabletopapi.support.PasswordPolicy;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record ChangePasswordRequest(
        @NotBlank(message = "Current password is required")
        String currentPassword,
        @Pattern(regexp = PasswordPolicy.REGEX, message = PasswordPolicy.MESSAGE)
        String newPassword,
        @NotBlank(message = "Password confirmation is required")
        String confirmPassword
) {
}