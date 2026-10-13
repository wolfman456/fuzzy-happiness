package com.gamer.fowever.tabletopservice.gateway;

import com.gamer.fowever.tabletopservice.support.CorrelationIdFilter;
import io.netty.channel.ChannelOption;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientRequestException;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.netty.http.client.HttpClient;

import java.time.Duration;
import java.util.Map;
import java.util.UUID;

@Component
public class GatewayClient {

    private static final Logger log = LoggerFactory.getLogger(GatewayClient.class);

    private static final int MAX_IN_MEMORY_BYTES = 2 * 1024 * 1024;
    static final int DEFAULT_CONNECT_TIMEOUT_MILLIS = 2_000;
    static final int DEFAULT_RESPONSE_TIMEOUT_MILLIS = 10_000;

    private final WebClient webClient;

    @Autowired
    public GatewayClient(@Value("${tabletopserv.gateway.url}") String baseUrl,
                         @Value("${tabletopserv.gateway.token}") String token,
                         @Value("${tabletopserv.gateway.connect-timeout-millis:2000}") int connectTimeoutMillis,
                         @Value("${tabletopserv.gateway.response-timeout-millis:10000}") int responseTimeoutMillis) {
        HttpClient httpClient = HttpClient.create()
                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, Math.max(1, connectTimeoutMillis))
                .responseTimeout(Duration.ofMillis(Math.max(1, responseTimeoutMillis)));
        this.webClient = WebClient.builder()
                .baseUrl(baseUrl)
                .clientConnector(new ReactorClientHttpConnector(httpClient))
                .defaultHeader("X-Gateway-Token", token)
                .codecs(configurer -> configurer.defaultCodecs().maxInMemorySize(MAX_IN_MEMORY_BYTES))
                .build();
    }

    public GatewayClient(String baseUrl, String token) {
        this(baseUrl, token, DEFAULT_CONNECT_TIMEOUT_MILLIS, DEFAULT_RESPONSE_TIMEOUT_MILLIS);
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
