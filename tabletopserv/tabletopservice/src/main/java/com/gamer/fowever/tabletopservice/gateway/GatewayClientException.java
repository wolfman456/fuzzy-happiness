package com.gamer.fowever.tabletopservice.gateway;

public class GatewayClientException extends RuntimeException {

    private final int status;

    public GatewayClientException(int status, String message) {
        super(message);
        this.status = status;
    }

    public int getStatus() {
        return status;
    }
}