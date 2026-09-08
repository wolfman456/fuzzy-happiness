package com.gamer.fowever.tabletopservice.gateway;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientRequestException;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import java.util.Map;
import java.util.UUID;

@Component
public class GatewayClient {

    private static final int MAX_IN_MEMORY_BYTES = 2 * 1024 * 1024;

    private final WebClient webClient;

    public GatewayClient(@Value("${tabletopserv.gateway.url}") String baseUrl,
                         @Value("${tabletopserv.gateway.token}") String token) {
        this.webClient = WebClient.builder()
                .baseUrl(baseUrl)
                .defaultHeader("X-Gateway-Token", token)
                .codecs(configurer -> configurer.defaultCodecs().maxInMemorySize(MAX_IN_MEMORY_BYTES))
                .build();
    }

    public String get(String path, Map<String, String> params) {
        try {
            return webClient.get()
                    .uri(builder -> {
                        builder.path(path);
                        params.forEach(builder::queryParam);
                        return builder.build();
                    })
                    .header("X-Correlation-Id", UUID.randomUUID().toString())
                    .retrieve()
                    .onStatus(HttpStatusCode::isError, response ->
                            response.bodyToMono(String.class)
                                    .defaultIfEmpty("")
                                    .map(body -> new GatewayClientException(response.statusCode().value(), body)))
                    .bodyToMono(String.class)
                    .block();
        } catch (GatewayClientException ex) {
            throw ex;
        } catch (WebClientRequestException | WebClientResponseException ex) {
            throw new GatewayClientException(502, "gateway unreachable: " + ex.getMessage());
        }
    }
}