package com.gamer.fowever.tabletopservice.web;

import com.gamer.fowever.tabletopapi.MonsterEdition;
import com.gamer.fowever.tabletopapi.MonsterRole;
import com.gamer.fowever.tabletopapi.dto.GenerateMonsterRequest;
import com.gamer.fowever.tabletopapi.dto.MonsterDto;
import com.gamer.fowever.tabletopservice.domain.User;
import com.gamer.fowever.tabletopservice.service.monster.MonsterService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.Authentication;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MonsterControllerImplTest {

    @Mock
    private MonsterService monsterService;

    private static final User CURRENT_USER = user();

    private static final MonsterDto MONSTER = new MonsterDto(
            1L, "Gravetusk", "7", 2900, MonsterEdition.SRD_2014, MonsterRole.BRUTE,
            3, 14, 219, "Large", "giant", "unaligned", 30,
            16, 8, 16, 5, 8, 6, 7, 15, 30,
            "A brute of challenge rating 7.", List.of(new MonsterDto.MonsterAction("Slam", "Smash.")),
            List.of("Hardy Frame."), Instant.parse("2026-01-01T00:00:00Z"));

    private static Authentication authentication() {
        return new TestingAuthenticationToken(CURRENT_USER, null);
    }

    @Test
    void generateReturnsCreatedWithMonster() {
        MonsterControllerImpl controller = new MonsterControllerImpl(monsterService);
        GenerateMonsterRequest request =
                new GenerateMonsterRequest("7", MonsterRole.BRUTE, MonsterEdition.SRD_2014,
                        "Gravetusk", null, 5);
        when(monsterService.generate(CURRENT_USER, request)).thenReturn(MONSTER);

        var response = controller.generate(request, authentication());

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(response.getBody()).isSameAs(MONSTER);
        verify(monsterService).generate(CURRENT_USER, request);
    }

    @Test
    void mineReturnsOwnedMonsters() {
        MonsterControllerImpl controller = new MonsterControllerImpl(monsterService);
        when(monsterService.mine(CURRENT_USER)).thenReturn(List.of(MONSTER));

        List<MonsterDto> result = controller.mine(authentication());

        assertThat(result).containsExactly(MONSTER);
        verify(monsterService).mine(CURRENT_USER);
    }

    private static User user() {
        User user = new User("aria", "Aria", "aria@example.com",
                java.time.LocalDate.of(1990, 1, 15), "hash");
        user.setId(7L);
        return user;
    }
}