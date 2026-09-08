package com.gamer.fowever.tabletopapi.dto;

import com.gamer.fowever.tabletopapi.AuthRole;

public record UserSummary(Long id, String username, String displayName, String email,
                          AuthRole role, boolean emailVerified) {
}