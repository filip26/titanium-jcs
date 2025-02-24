package com.apicatalog.jcs;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.stream.Collectors;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import jakarta.json.Json;
import jakarta.json.JsonReader;
import jakarta.json.JsonValue;

class JsonCanonicalizerTest {

    @ParameterizedTest
    @ValueSource(strings = { "primitive-data-types", "uni-sort", "array", "object", "unicode", "french" })
    void testCanonize(String name) throws IOException {
        assertEquals(getResource(name + ".out.json"), JsonCanonicalizer.canonize(getJson(name + ".in.json")));
    }

    static String getResource(String name) throws IOException {
        try (BufferedInputStream is = new BufferedInputStream(JsonCanonicalizerTest.class.getResourceAsStream(name))) {
            return new BufferedReader(
                    new InputStreamReader(is, StandardCharsets.UTF_8))
                    .lines()
                    .collect(Collectors.joining("\n"));
        }
    }

    static JsonValue getJson(String name) {
        try (JsonReader reader = Json.createReader(
                JsonCanonicalizerTest.class.getResourceAsStream(name))) {
            return reader.read();
        }
    }
}
