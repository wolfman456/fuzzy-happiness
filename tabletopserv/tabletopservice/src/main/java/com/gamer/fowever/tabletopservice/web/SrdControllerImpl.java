package com.gamer.fowever.tabletopservice.web;

import com.gamer.fowever.tabletopapi.SrdApi;
import com.gamer.fowever.tabletopservice.service.SrdClient;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import tools.jackson.databind.JsonNode;

import java.util.Map;

@RestController
@RequestMapping("/api/srd")
public class SrdControllerImpl implements SrdApi {

    private final SrdClient srdClient;

    public SrdControllerImpl(SrdClient srdClient) {
        this.srdClient = srdClient;
    }

    @Override
    public JsonNode list(@PathVariable String collection, @RequestParam Map<String, String> params) {
        return srdClient.list(collection, params);
    }

    @Override
    public JsonNode detail(@PathVariable String collection, @PathVariable String index) {
        return srdClient.detail(collection, index);
    }

    @Override
    public JsonNode subresource(@PathVariable String collection, @PathVariable String index,
                                @PathVariable String subresource,
                                @RequestParam Map<String, String> params) {
        return srdClient.subresource(collection, index, subresource, params);
    }
}
