package com.gamer.fowever.tabletopservice.security;

import org.junit.jupiter.api.Test;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class AppOAuth2UserTest {

    @Test
    void exposesAuthoritiesAttributesAndUserIdAsName() {
        List<SimpleGrantedAuthority> authorities = List.of(new SimpleGrantedAuthority("ROLE_USER"));
        Map<String, Object> attributes = Map.of("sub", "google-1");

        AppOAuth2User user = new AppOAuth2User(authorities, attributes, 7L);

        assertThat(user.getAuthorities()).isEqualTo(authorities);
        assertThat(user.getAttributes()).isEqualTo(attributes);
        assertThat(user.getName()).isEqualTo("7");
        assertThat(user.userId()).isEqualTo(7L);
    }
}
