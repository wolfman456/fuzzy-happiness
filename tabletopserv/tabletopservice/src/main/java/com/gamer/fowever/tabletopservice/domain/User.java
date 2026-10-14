package com.gamer.fowever.tabletopservice.domain;
import com.gamer.fowever.tabletopapi.AuthRole;
import com.gamer.fowever.tabletopservice.support.EncryptingLocalDateConverter;
import com.gamer.fowever.tabletopservice.support.EncryptingStringConverter;
import com.gamer.fowever.tabletopservice.support.PiiCrypto;

import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

@Entity
@Table(name = "users", uniqueConstraints = @UniqueConstraint(
        name = "uq_users_oauth_identity", columnNames = {"oauth_provider", "oauth_subject"}))
@Getter
@Setter
@NoArgsConstructor
public class User implements UserDetails {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 30)
    private String username;

    @Convert(converter = EncryptingStringConverter.class)
    @Column(name = "display_name", nullable = false, length = 512)
    private String displayName;

    @Convert(converter = EncryptingStringConverter.class)
    @Column(name = "real_name", nullable = true, length = 512)
    private String realName;

    @Convert(converter = EncryptingStringConverter.class)
    @Column(nullable = false, length = 512)
    private String email;

    /** Deterministic blind index of the email (HMAC-SHA256) that backs equality lookups and uniqueness. */
    @Column(name = "email_key", nullable = false, unique = true, length = 64)
    private String emailKey;

    @Column(nullable = false)
    private String passwordHash;

    @Convert(converter = EncryptingLocalDateConverter.class)
    @Column(name = "date_of_birth", nullable = true, length = 90)
    private LocalDate dateOfBirth;

    @Column(name = "email_verified", nullable = false)
    private boolean emailVerified;

    /** Social-login provider id (e.g. {@code google}), or {@code null} for local password accounts. */
    @Column(name = "oauth_provider", length = 32)
    private String oauthProvider;

    /** Provider-issued stable subject identifier paired with {@link #oauthProvider}. */
    @Column(name = "oauth_subject", length = 255)
    private String oauthSubject;

    @Enumerated(EnumType.STRING)
    @Column(name = "auth_role", nullable = false)
    private AuthRole authRole = AuthRole.USER;

    @Column(length = 512)
    private String avatar;

    @OneToMany(mappedBy = "owner")
    private List<Character> characters = new ArrayList<>();

    public User(String username, String displayName, String email, LocalDate dateOfBirth, String passwordHash) {
        this.username = username;
        this.displayName = displayName;
        this.email = email;
        this.emailKey = PiiCrypto.emailKey(email);
        this.dateOfBirth = dateOfBirth;
        this.passwordHash = passwordHash;
    }

    /**
     * Creates a social-login account. The date of birth is unknown (the provider asserts the user
     * meets its own minimum age), so the age gate is not applied; {@code passwordHash} holds an
     * unusable random value so password login can never succeed.
     */
    public User(String username, String displayName, String email, String passwordHash,
                String oauthProvider, String oauthSubject) {
        this(username, displayName, email, null, passwordHash);
        this.oauthProvider = oauthProvider;
        this.oauthSubject = oauthSubject;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of(new SimpleGrantedAuthority("ROLE_" + authRole.name()));
    }

    @Override
    public String getPassword() {
        return passwordHash;
    }

    @Override
    public String getUsername() {
        return username;
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return emailVerified;
    }
}