package com.gamer.fowever.tabletopserv.domain;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;

import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DataJpaTest
class DomainModelPersistenceTest {

    @Autowired
    private TestEntityManager em;

    private Long persistOwner() {
        return em.persistAndFlush(new User("Aria", "aria@example.com", "hash")).getId();
    }

    private Long persistGame() {
        return em.persistAndFlush(new Game("dnd-5e", "D&D 5e", "{}")).getId();
    }

    @Test
    void persistsUser() {
        Long id = persistOwner();
        em.clear();

        User reloaded = em.find(User.class, id);
        assertThat(reloaded.getDisplayName()).isEqualTo("Aria");
        assertThat(reloaded.getEmail()).isEqualTo("aria@example.com");
        assertThat(reloaded.getPasswordHash()).isEqualTo("hash");
        assertThat(reloaded.getAvatar()).isNull();
        assertThat(reloaded.getCharacters()).isEmpty();
    }

    @Test
    void persistsGame() {
        Long id = persistGame();
        em.clear();

        Game reloaded = em.find(Game.class, id);
        assertThat(reloaded.getSlug()).isEqualTo("dnd-5e");
        assertThat(reloaded.getDisplayName()).isEqualTo("D&D 5e");
        assertThat(reloaded.getSheetSchema()).isEqualTo("{}");
    }

    @Test
    void persistsDnd5eCharacterAsDiscriminatedRow() {
        User owner = em.find(User.class, persistOwner());

        Dnd5eCharacter character = new Dnd5eCharacter();
        character.setOwner(owner);
        character.setName("Grumm");
        character.setPortrait("portrait-url");
        character.setDescription("A gruff mercenary captain.");
        character.setGameVersion("2014");
        character.setStrength(15);
        character.setDexterity(14);
        character.setConstitution(13);
        character.setIntelligence(12);
        character.setWisdom(11);
        character.setCharisma(10);
        character.setLevel(2);
        character.setScoreSource(ScoreSource.HOUSE_RULE_D20);
        character.setRaceIndex("dwarf");
        character.setClassIndex("fighter");
        character.setSubclassIndex("champion");
        character.setBackgroundIndex("soldier");
        character.setSkillPickIndexes(Set.of("athletics", "intimidation"));
        character.setSpellIndexes(Set.of("expeditious-retreat"));
        character.setFeatureIndexes(Set.of("action-surge"));
        character.setHitPoints(20);
        character.setArmorClass(16);
        character.setProficiencyBonus(2);
        character.setSheetSnapshot("{\"hp\":20}");

        em.persistAndFlush(character);
        Long id = character.getId();
        assertThat(em.getEntityManager().createNativeQuery("""
                        select game_key from characters where id = :id
                        """).setParameter("id", id).getSingleResult()).isEqualTo("DND5E");
        assertThat(em.getEntityManager().createNativeQuery("""
                        select score_source from characters where id = :id
                        """).setParameter("id", id).getSingleResult()).isEqualTo("HOUSE_RULE_D20");
        em.clear();

        Dnd5eCharacter reloaded = em.find(Dnd5eCharacter.class, id);
        assertThat(reloaded.getName()).isEqualTo("Grumm");
        assertThat(reloaded.getPortrait()).isEqualTo("portrait-url");
        assertThat(reloaded.getDescription()).isEqualTo("A gruff mercenary captain.");
        assertThat(reloaded.getGameVersion()).isEqualTo("2014");
        assertThat(reloaded.getStrength()).isEqualTo(15);
        assertThat(reloaded.getDexterity()).isEqualTo(14);
        assertThat(reloaded.getConstitution()).isEqualTo(13);
        assertThat(reloaded.getIntelligence()).isEqualTo(12);
        assertThat(reloaded.getWisdom()).isEqualTo(11);
        assertThat(reloaded.getCharisma()).isEqualTo(10);
        assertThat(reloaded.getLevel()).isEqualTo(2);
        assertThat(reloaded.getScoreSource()).isEqualTo(ScoreSource.HOUSE_RULE_D20);
        assertThat(reloaded.getRaceIndex()).isEqualTo("dwarf");
        assertThat(reloaded.getClassIndex()).isEqualTo("fighter");
        assertThat(reloaded.getSubclassIndex()).isEqualTo("champion");
        assertThat(reloaded.getBackgroundIndex()).isEqualTo("soldier");
        assertThat(reloaded.getSkillPickIndexes()).containsExactlyInAnyOrder("athletics", "intimidation");
        assertThat(reloaded.getSpellIndexes()).containsExactly("expeditious-retreat");
        assertThat(reloaded.getFeatureIndexes()).containsExactly("action-surge");
        assertThat(reloaded.getHitPoints()).isEqualTo(20);
        assertThat(reloaded.getArmorClass()).isEqualTo(16);
        assertThat(reloaded.getProficiencyBonus()).isEqualTo(2);
        assertThat(reloaded.getSheetSnapshot()).isEqualTo("{\"hp\":20}");
        assertThat(reloaded.getOwner().getId()).isEqualTo(owner.getId());
    }

    @Test
    void registersOwnedCharacterOnUser() {
        User owner = em.find(User.class, persistOwner());

        Dnd5eCharacter character = new Dnd5eCharacter();
        character.setOwner(owner);
        character.setName("Grumm");
        em.persistAndFlush(character);
        em.clear();

        User reloaded = em.find(User.class, owner.getId());
        assertThat(reloaded.getCharacters()).hasSize(1);
        assertThat(reloaded.getCharacters().getFirst().getId()).isEqualTo(character.getId());
    }

    @Test
    void persistsCharacterDraft() {
        User owner = em.find(User.class, persistOwner());

        CharacterDraft draft = new CharacterDraft();
        draft.setOwner(owner);
        draft.setStrength(15);
        draft.setDexterity(14);
        draft.setConstitution(13);
        draft.setIntelligence(12);
        draft.setWisdom(11);
        draft.setCharisma(10);
        draft.setScoreSource(ScoreSource.HOUSE_RULE_D20);
        draft.setStartingLevel(2);
        draft.setRaceIndex("dwarf");
        draft.setClassIndex("fighter");
        draft.setSubclassIndex("champion");
        draft.setBackgroundIndex("soldier");
        draft.setSkillPickIndexes(Set.of("athletics"));
        draft.setSpellIndexes(Set.of("expeditious-retreat"));
        draft.setEquipmentIndexes(Set.of("chain-mail"));

        em.persistAndFlush(draft);
        Long id = draft.getId();
        em.clear();

        CharacterDraft reloaded = em.find(CharacterDraft.class, id);
        assertThat(reloaded.getStrength()).isEqualTo(15);
        assertThat(reloaded.getDexterity()).isEqualTo(14);
        assertThat(reloaded.getConstitution()).isEqualTo(13);
        assertThat(reloaded.getIntelligence()).isEqualTo(12);
        assertThat(reloaded.getWisdom()).isEqualTo(11);
        assertThat(reloaded.getCharisma()).isEqualTo(10);
        assertThat(reloaded.getScoreSource()).isEqualTo(ScoreSource.HOUSE_RULE_D20);
        assertThat(reloaded.getStartingLevel()).isEqualTo(2);
        assertThat(reloaded.getRaceIndex()).isEqualTo("dwarf");
        assertThat(reloaded.getClassIndex()).isEqualTo("fighter");
        assertThat(reloaded.getSubclassIndex()).isEqualTo("champion");
        assertThat(reloaded.getBackgroundIndex()).isEqualTo("soldier");
        assertThat(reloaded.getSkillPickIndexes()).containsExactly("athletics");
        assertThat(reloaded.getSpellIndexes()).containsExactly("expeditious-retreat");
        assertThat(reloaded.getEquipmentIndexes()).containsExactly("chain-mail");
        assertThat(reloaded.getCreatedAt()).isNotNull();
        assertThat(reloaded.getOwner().getId()).isEqualTo(owner.getId());
    }

    @Test
    void persistsSessionWithParticipantsAndEvents() {
        User gm = em.find(User.class, persistOwner());
        User player = em.persistAndFlush(new User("Ivo", "ivo@example.com", "hash"));
        em.clear();
        Game game = em.find(Game.class, persistGame());

        GameSession session = new GameSession();
        session.setName("Grumm's Revenge");
        session.setInviteCode("invite-1");
        session.setGame(game);
        session.setCreatedBy(gm);

        Participant gmParticipation = new Participant(session, gm, Role.GM);
        Participant playerParticipation = new Participant(session, player, Role.PLAYER);
        SessionEvent chat = new SessionEvent(session, EventType.CHAT, "{\"text\":\"hi all\"}");

        em.persistAndFlush(session);
        em.persistAndFlush(gmParticipation);
        em.persistAndFlush(playerParticipation);
        em.persistAndFlush(chat);
        Long sessionId = session.getId();

        assertThat(em.getEntityManager().createNativeQuery("""
                        select status from game_sessions where id = :id
                        """).setParameter("id", sessionId).getSingleResult()).isEqualTo("OPEN");
        assertThat(em.getEntityManager().createNativeQuery("""
                        select role from participants where id = :id
                        """).setParameter("id", playerParticipation.getId()).getSingleResult()).isEqualTo("PLAYER");
        assertThat(em.getEntityManager().createNativeQuery("""
                        select type from session_events where id = :id
                        """).setParameter("id", chat.getId()).getSingleResult()).isEqualTo("CHAT");
        em.clear();

        GameSession reloaded = em.find(GameSession.class, sessionId);
        assertThat(reloaded.getName()).isEqualTo("Grumm's Revenge");
        assertThat(reloaded.getInviteCode()).isEqualTo("invite-1");
        assertThat(reloaded.getStatus()).isEqualTo(SessionStatus.OPEN);
        assertThat(reloaded.getGame().getId()).isEqualTo(game.getId());
        assertThat(reloaded.getCreatedBy().getId()).isEqualTo(gm.getId());

        List<Participant> participants = reloaded.getParticipants();
        assertThat(participants).extracting("role").containsExactlyInAnyOrder(Role.GM, Role.PLAYER);
        assertThat(participants).extracting("user").extracting("id").containsExactlyInAnyOrder(gm.getId(), player.getId());
        assertThat(participants).extracting(Participant::getJoinedAt).allMatch(joinedAt -> !joinedAt.isAfter(java.time.LocalDateTime.now().plusSeconds(1)));

        SessionEvent reloadedEvent = reloaded.getEvents().getFirst();
        assertThat(reloadedEvent.getType()).isEqualTo(EventType.CHAT);
        assertThat(reloadedEvent.getPayload()).isEqualTo("{\"text\":\"hi all\"}");
        assertThat(reloadedEvent.getCreatedAt()).isNotNull();
    }

    @Test
    void persistsSpectatorRoleAsString() {
        User gm = em.find(User.class, persistOwner());
        em.clear();
        Game game = em.find(Game.class, persistGame());

        GameSession session = new GameSession();
        session.setName("Observation Deck");
        session.setInviteCode("invite-2");
        session.setGame(game);
        session.setCreatedBy(gm);
        em.persistAndFlush(session);

        Participant spectator = em.persistAndFlush(new Participant(session, gm, Role.SPECTATOR));
        assertThat(em.getEntityManager().createNativeQuery("""
                        select role from participants where id = :id
                        """).setParameter("id", spectator.getId()).getSingleResult()).isEqualTo("SPECTATOR");
    }

    @Test
    void rejectsDuplicateEmail() {
        em.persistAndFlush(new User("Aria", "dupe@example.com", "hash"));
        em.clear();

        assertThatThrownBy(() -> em.persistAndFlush(new User("Ivo", "dupe@example.com", "hash2"))).hasRootCauseInstanceOf(java.sql.SQLIntegrityConstraintViolationException.class);
    }

    @Test
    void rejectsDuplicateSessionInviteCode() {
        User gm = em.find(User.class, persistOwner());
        em.clear();
        Game game = em.find(Game.class, persistGame());

        em.persistAndFlush(sessionWith("dupe-invite", game, gm));
        em.clear();

        Game managedGame = em.find(Game.class, game.getId());
        User managedGm = em.find(User.class, gm.getId());
        assertThatThrownBy(() -> em.persistAndFlush(sessionWith("dupe-invite", managedGame, managedGm))).hasRootCauseInstanceOf(java.sql.SQLIntegrityConstraintViolationException.class);
    }

    @Test
    void rejectsDuplicateParticipant() {
        User gm = em.find(User.class, persistOwner());
        em.clear();
        Game game = em.find(Game.class, persistGame());
        GameSession session = em.persistAndFlush(sessionWith("invite-3", game, gm));
        em.persistAndFlush(new Participant(session, gm, Role.GM));
        em.clear();

        GameSession managedSession = em.find(GameSession.class, session.getId());
        User managedGm = em.find(User.class, gm.getId());
        assertThatThrownBy(() -> em.persistAndFlush(new Participant(managedSession, managedGm, Role.PLAYER))).hasRootCauseInstanceOf(java.sql.SQLIntegrityConstraintViolationException.class);
    }

    private GameSession sessionWith(String inviteCode, Game game, User createdBy) {
        GameSession session = new GameSession();
        session.setName("Session");
        session.setInviteCode(inviteCode);
        session.setGame(game);
        session.setCreatedBy(createdBy);
        return session;
    }
}