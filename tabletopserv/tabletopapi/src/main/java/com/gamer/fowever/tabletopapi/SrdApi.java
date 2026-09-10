package com.gamer.fowever.tabletopapi;

import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import tools.jackson.databind.JsonNode;

import java.util.Map;

@RequestMapping("/api/srd")
public interface SrdApi {

    @GetMapping(value = "/{collection}", produces = MediaType.APPLICATION_JSON_VALUE)
    JsonNode list(@PathVariable String collection, @RequestParam Map<String, String> params);

    @GetMapping(value = "/{collection}/{index}", produces = MediaType.APPLICATION_JSON_VALUE)
    JsonNode detail(@PathVariable String collection, @PathVariable String index);

    /** Class subresources the character builder relies on (e.g. spells / levels). */
    @GetMapping(value = "/{collection}/{index}/{subresource}", produces = MediaType.APPLICATION_JSON_VALUE)
    JsonNode subresource(@PathVariable String collection, @PathVariable String index,
                         @PathVariable String subresource, @RequestParam Map<String, String> params);

}
