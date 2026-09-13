package com.gamer.fowever.tabletopservice.gateway;

import com.gamer.fowever.tabletopservice.support.CorrelationIdFilter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
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

    private static final Logger log = LoggerFactory.getLogger(GatewayClient.class);

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
        String correlationId = MDC.get(CorrelationIdFilter.MDC_KEY);
        if (correlationId == null || correlationId.isBlank()) {
            correlationId = UUID.randomUUID().toString();
        }
        long start = System.nanoTime();
        log.debug("gateway call start corr={} path={} params={}", correlationId, path, params);
        try {
            String body = webClient.get()
                    .uri(builder -> {
                        builder.path(path);
                        params.forEach(builder::queryParam);
                        return builder.build();
                    })
                    .header("X-Correlation-Id", correlationId)
                    .retrieve()
                    .onStatus(HttpStatusCode::isError, response ->
                            response.bodyToMono(String.class)
                                    .defaultIfEmpty("")
                                    .map(errorBody -> new GatewayClientException(response.statusCode().value(), errorBody)))
                    .bodyToMono(String.class)
                    .block();
            log.debug("gateway call done corr={} path={} tookMs={} bytes={}",
                    correlationId, path, elapsedMs(start), body == null ? 0 : body.length());
            return body;
        } catch (GatewayClientException ex) {
            log.debug("gateway call failed corr={} path={} status={} tookMs={}",
                    correlationId, path, ex.getStatus(), elapsedMs(start));
            throw ex;
        } catch (WebClientRequestException | WebClientResponseException ex) {
            log.warn("gateway unreachable corr={} path={} tookMs={} error={}",
                    correlationId, path, elapsedMs(start), ex.getMessage());
            throw new GatewayClientException(502, "gateway unreachable: " + ex.getMessage());
        }
    }

    private static long elapsedMs(long startNanos) {
        return (System.nanoTime() - startNanos) / 1_000_000L;
    }
}
