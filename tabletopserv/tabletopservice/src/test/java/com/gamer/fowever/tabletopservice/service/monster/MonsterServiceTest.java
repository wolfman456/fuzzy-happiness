package com.gamer.fowever.tabletopservice.service.monster;

import com.gamer.fowever.tabletopapi.MonsterEdition;
import com.gamer.fowever.tabletopapi.MonsterRole;
import com.gamer.fowever.tabletopapi.dto.GenerateMonsterRequest;
import com.gamer.fowever.tabletopapi.dto.MonsterDto;
import com.gamer.fowever.tabletopservice.domain.Monster;
import com.gamer.fowever.tabletopservice.domain.User;
import com.gamer.fowever.tabletopservice.repository.MonsterRepository;
import com.gamer.fowever.tabletopapi.support.ApiException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import tools.jackson.databind.json.JsonMapper;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MonsterServiceTest {

    @Mock
    private MonsterRepository monsterRepository;

    private final JsonMapper objectMapper = JsonMapper.builder().build();

    private MonsterService service;

    @BeforeEach
    void setUp() {
        service = new MonsterService(monsterRepository, objectMapper);
    }

    @Test
    void generateAppliesAutoRoleFromConcept() {
        User actor = user(7L);
        MonsterMathEngine.Statblock statblock = MonsterMathEngine.generate(
                "Deep Stalker", "10", MonsterRole.LURKER, MonsterEdition.SRD_2014);
        when(monsterRepository.save(any(Monster.class))).thenAnswer(invocation -> {
            Monster monster = invocation.getArgument(0);
            monster.setId(1L);
            return monster;
        });

        MonsterDto result = service.generate(actor,
                new GenerateMonsterRequest("10", MonsterRole.AUTO, null,
                        "Deep Stalker", "a lurking horror", 0));

        assertThat(result.id()).isEqualTo(1L);
        assertThat(result.name()).isEqualTo("Deep Stalker");
        assertThat(result.role()).isEqualTo(MonsterRole.LURKER);
        assertThat(result.cr()).isEqualTo("10");
        assertThat(result.hitPoints()).isEqualTo(statblock.hitPoints());

        ArgumentCaptor<Monster> captor = ArgumentCaptor.forClass(Monster.class);
        verify(monsterRepository).save(captor.capture());
        Monster saved = captor.getValue();
        assertThat(saved.getOwner().getId()).isEqualTo(7L);
        assertThat(saved.getCombatRole()).isEqualTo(MonsterRole.LURKER);
        assertThat(saved.getCr()).isEqualTo("10");
        assertThat(saved.getStatblockJson()).contains("\"name\":\"Deep Stalker\"");
        assertThat(saved.getCreatedAt()).isNotNull();
    }

    @Test
    void generatePersistsExplicitRoleAndEdition() {
        User actor = user(7L);
        when(monsterRepository.save(any(Monster.class))).thenAnswer(invocation -> {
            Monster monster = invocation.getArgument(0);
            monster.setId(3L);
            return monster;
        });

        MonsterDto result = service.generate(actor,
                new GenerateMonsterRequest("5", MonsterRole.DEFENDER, MonsterEdition.SRD_2024,
                        "Bastion", "tower shield", 42));

        assertThat(result.role()).isEqualTo(MonsterRole.DEFENDER);
        assertThat(result.edition()).isEqualTo(MonsterEdition.SRD_2024);
        assertThat(result.armorClass()).isEqualTo(MonsterMathEngine.generate(
                "Bastion", "5", MonsterRole.DEFENDER, MonsterEdition.SRD_2024).armorClass());
        assertThat(result.description()).contains("2024");
    }

    @Test
    void generateRejectsUnsupportedCr() {
        User actor = user(7L);

        assertThatThrownBy(() -> service.generate(actor,
                new GenerateMonsterRequest("47", MonsterRole.BALANCED, null, "X", null, 0)))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("unsupported challenge rating: 47");

        verify(monsterRepository, never()).save(any());
    }

    @Test
    void mineReturnsOwnedMonstersNewestFirst() {
        Monster first = monster(1L, "Gravetusk", "7", MonsterRole.BRUTE,
                MonsterMathEngine.generate("Gravetusk", "7", MonsterRole.BRUTE, MonsterEdition.SRD_2014));
        Monster second = monster(2L, "Vinebomb", "3", MonsterRole.ARTILLERY,
                MonsterMathEngine.generate("Vinebomb", "3", MonsterRole.ARTILLERY, MonsterEdition.SRD_2014));
        when(monsterRepository.findByOwnerIdOrderByCreatedAtDesc(7L)).thenReturn(List.of(second, first));

        List<MonsterDto> result = service.mine(user(7L));

        assertThat(result).hasSize(2);
        assertThat(result.get(0).id()).isEqualTo(2L);
        assertThat(result.get(0).name()).isEqualTo("Vinebomb");
        assertThat(result.get(1).name()).isEqualTo("Gravetusk");
        assertThat(result.get(1).role()).isEqualTo(MonsterRole.BRUTE);
    }

    private static User user(Long id) {
        User user = new User("aria", "Aria", "aria@example.com",
                java.time.LocalDate.of(1990, 1, 15), "hash");
        user.setId(id);
        return user;
    }

    private Monster monster(Long id, String name, String cr, MonsterRole role,
                            MonsterMathEngine.Statblock statblock) {
        Monster monster = new Monster(user(7L), name, cr, role, MonsterEdition.SRD_2014,
                objectMapper.writeValueAsString(statblock), java.time.Instant.now());
        monster.setId(id);
        return monster;
    }
}