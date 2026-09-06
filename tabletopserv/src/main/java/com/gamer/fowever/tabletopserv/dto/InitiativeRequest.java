package com.gamer.fowever.tabletopserv.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;

public record InitiativeRequest(
        @NotNull(message = "entries is required") @Valid @Size(max = 30, message = "At most 30 initiative entries")
        List<InitiativeEntryRequest> entries) {
}