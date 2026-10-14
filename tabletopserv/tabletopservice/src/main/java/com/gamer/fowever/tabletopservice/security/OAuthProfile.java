package com.gamer.fowever.tabletopservice.security;

import java.util.Locale;
import java.util.Map;

/**
 * Normalises the provider-specific user-info attributes returned by Google / GitHub / Facebook
 * into a single shape. Unknown providers fall back to the common OIDC claim names.
 */
public record OAuthProfile(String subject, String email, boolean emailVerified,
                           String displayName, String avatarUrl) {

    public static OAuthProfile extract(String registrationId, Map<String, Object> attributes) {
        String id = registrationId == null ? "" : registrationId.toLowerCase(Locale.ROOT);
        return switch (id) {
            case "google" -> new OAuthProfile(
                    string(attributes.get("sub")),
                    string(attributes.get("email")),
                    truthy(attributes.get("email_verified")),
                    string(attributes.get("name")),
                    string(attributes.get("picture")));
            case "github" -> new OAuthProfile(
                    string(attributes.get("id")),
                    string(attributes.get("email")),
                    // GitHub's profile email is only present when public; it is not a verification claim.
                    false,
                    firstNonBlank(string(attributes.get("name")), string(attributes.get("login"))),
                    string(attributes.get("avatar_url")));
            case "facebook" -> new OAuthProfile(
                    string(attributes.get("id")),
                    string(attributes.get("email")),
                    // Facebook only returns verified email addresses.
                    true,
                    string(attributes.get("name")),
                    nested(attributes.get("picture")));
            default -> new OAuthProfile(
                    firstNonBlank(string(attributes.get("sub")), string(attributes.get("id"))),
                    string(attributes.get("email")),
                    truthy(attributes.get("email_verified")),
                    firstNonBlank(string(attributes.get("name")), string(attributes.get("preferred_username"))),
                    firstNonBlank(string(attributes.get("picture")), string(attributes.get("avatar_url"))));
        };
    }

    private static String nested(Object picture) {
        if (picture instanceof Map<?, ?> map) {
            Object data = map.get("data");
            if (data instanceof Map<?, ?> dataMap) {
                return string(dataMap.get("url"));
            }
            return string(map.get("url"));
        }
        return string(picture);
    }

    private static String firstNonBlank(String first, String second) {
        return (first != null && !first.isBlank()) ? first : second;
    }

    private static String string(Object value) {
        return value == null ? null : String.valueOf(value);
    }

    private static boolean truthy(Object value) {
        if (value instanceof Boolean bool) {
            return bool;
        }
        return value != null && "true".equalsIgnoreCase(String.valueOf(value));
    }
}
