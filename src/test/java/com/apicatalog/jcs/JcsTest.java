package com.apicatalog.jcs;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.stream.Collectors;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import com.apicatalog.tree.io.TreeIOException;
import com.apicatalog.tree.io.jakarta.JakartaAdapter;
import com.apicatalog.tree.io.jakcson.Jackson2Adapter;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import jakarta.json.Json;
import jakarta.json.JsonValue;

class JcsTest {

    final static ObjectMapper MAPPER = new ObjectMapper();

    @ParameterizedTest
    @ValueSource(strings = { "primitive-data-types", "uni-sort", "array", "object", "unicode", "french" })
    void testCanonizeJakarta(String name) throws IOException, TreeIOException {
        assertEquals(
                getResource(name + ".out.json"),
                Jcs.canonize(getJakartaJson(name + ".in.json"), JakartaAdapter.instance()));
    }

    @ParameterizedTest
    @ValueSource(strings = { "primitive-data-types", "uni-sort", "array", "object", "unicode", "french" })
    void testCanonizeJackson2(String name) throws IOException, TreeIOException {
        assertEquals(
                getResource(name + ".out.json"),
                Jcs.canonize(getJacksonJson(name + ".in.json"), Jackson2Adapter.instance()));
    }

    @ParameterizedTest
    @ValueSource(strings = { "primitive-data-types", "uni-sort", "array", "object", "unicode", "french" })
    void testCompareJakarta(String name) throws IOException {
        assertTrue(Jcs.equals(
                getJakartaJson(name + ".in.json"),
                getJakartaJson(name + ".out.json"),
                JakartaAdapter.instance()));
    }

    @ParameterizedTest
    @ValueSource(strings = { "primitive-data-types", "uni-sort", "array", "object", "unicode", "french" })
    void testCompareJackson2(String name) throws IOException {
        assertTrue(Jcs.equals(
                getJacksonJson(name + ".in.json"),
                getJacksonJson(name + ".out.json"),
                Jackson2Adapter.instance()));
    }

    static String getResource(String name) throws IOException {
        try (var is = new BufferedInputStream(JcsTest.class.getResourceAsStream(name))) {
            return new BufferedReader(
                    new InputStreamReader(is, StandardCharsets.UTF_8))
                    .lines()
                    .collect(Collectors.joining("\n"));
        }
    }

    static JsonValue getJakartaJson(String name) {
        try (var reader = Json.createReader(
                JcsTest.class.getResourceAsStream(name))) {
            return reader.read();
        }
    }

    static JsonNode getJacksonJson(String name) throws IOException {
        try (var is = JcsTest.class.getResourceAsStream(name);
                var reader = new InputStreamReader(is, StandardCharsets.UTF_8)) {
            return MAPPER.readTree(reader);
        }
    }
}