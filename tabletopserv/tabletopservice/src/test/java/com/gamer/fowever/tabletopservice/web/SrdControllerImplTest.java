package com.gamer.fowever.tabletopservice.web;

import com.gamer.fowever.tabletopservice.service.SrdClient;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SrdControllerImplTest {

    @Mock
    private SrdClient srdClient;

    private final JsonMapper objectMapper = JsonMapper.builder().build();

    @Test
    void listDelegatesToSrdClient() {
        SrdControllerImpl controller = new SrdControllerImpl(srdClient);
        JsonNode expected = objectMapper.readTree("{\"count\":2}");
        when(srdClient.list("races", Map.of())).thenReturn(expected);

        JsonNode result = controller.list("races", Map.of());

        assertThat(result).isSameAs(expected);
        verify(srdClient).list("races", Map.of());
    }

    @Test
    void detailDelegatesToSrdClient() {
        SrdControllerImpl controller = new SrdControllerImpl(srdClient);
        JsonNode expected = objectMapper.readTree("{\"name\":\"Dragonborn\"}");
        when(srdClient.detail("races", "dragonborn")).thenReturn(expected);

        JsonNode result = controller.detail("races", "dragonborn");

        assertThat(result).isSameAs(expected);
        verify(srdClient).detail("races", "dragonborn");
    }

    @Test
    void subresourceDelegatesToSrdClient() {
        SrdControllerImpl controller = new SrdControllerImpl(srdClient);
        JsonNode expected = objectMapper.readTree("[{\"level\":1}]");
        when(srdClient.subresource("classes", "cleric", "levels", Map.of())).thenReturn(expected);

        JsonNode result = controller.subresource("classes", "cleric", "levels", Map.of());

        assertThat(result).isSameAs(expected);
        verify(srdClient).subresource("classes", "cleric", "levels", Map.of());
    }
}