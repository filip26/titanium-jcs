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

import com.apicatalog.tree.io.JakartaAdapter;
import com.apicatalog.tree.io.NodeModel;

import jakarta.json.Json;
import jakarta.json.JsonReader;
import jakarta.json.JsonValue;

class JcsTest {

    @ParameterizedTest
    @ValueSource(strings = { "primitive-data-types", "uni-sort", "array", "object", "unicode", "french" })
    void testCanonize(String name) throws IOException {
        assertEquals(getResource(name + ".out.json"), Jcs.canonize(getJson(name + ".in.json")));
    }

    @ParameterizedTest
    @ValueSource(strings = { "primitive-data-types", "uni-sort", "array", "object", "unicode", "french" })
    void testCompare(String name) throws IOException {
//FIXME        assertTrue(Jcs.equals(getJson(name + ".in.json"), getJson(name + ".out.json")));
    }
    
    static String getResource(String name) throws IOException {
        try (BufferedInputStream is = new BufferedInputStream(JcsTest.class.getResourceAsStream(name))) {
            return new BufferedReader(
                    new InputStreamReader(is, StandardCharsets.UTF_8))
                    .lines()
                    .collect(Collectors.joining("\n"));
        }
    }

    static NodeModel getJson(String name) {
        try (JsonReader reader = Json.createReader(
                JcsTest.class.getResourceAsStream(name))) {
            return new NodeModel(reader.read(), JakartaAdapter.instance());
        }
    }
}
