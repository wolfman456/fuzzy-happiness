package com.gamer.fowever.tabletopservice.security;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Boots the app with a (fake) Google registration to prove the opt-in social-login wiring:
 * the provider surfaces through {@code /api/auth/providers} and the authorization endpoint
 * starts a real redirect. Credentials are dummies — no provider is ever contacted.
 */
@SpringBootTest(properties = {
        "spring.security.oauth2.client.registration.google.client-id=test-client-id",
        "spring.security.oauth2.client.registration.google.client-secret=test-client-secret",
        "spring.security.oauth2.client.registration.google.scope=openid,email"
})
@AutoConfigureMockMvc
class SocialLoginWiringTest {

    @Autowired
    private MockMvc mvc;

    @Test
    void configuredProviderIsExposed() throws Exception {
        mvc.perform(get("/api/auth/providers"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].id").value("google"))
                .andExpect(jsonPath("$[0].label").value("Google"))
                .andExpect(jsonPath("$[0].authorizationPath").value("/oauth2/authorization/google"));
    }

    @Test
    void authorizationEndpointRedirectsToProvider() throws Exception {
        mvc.perform(get("/oauth2/authorization/google"))
                .andExpect(status().is3xxRedirection())
                .andExpect(header().string("Location", containsString("accounts.google.com")));
    }
}
