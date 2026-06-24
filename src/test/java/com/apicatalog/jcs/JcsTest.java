package com.apicatalog.jcs;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.stream.Collectors;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import com.apicatalog.tree.io.TreeIOException;
import com.apicatalog.tree.io.jakarta.JakartaReader;

import jakarta.json.Json;

class JcsTest {

    static JakartaReader JAKARTA_READER = new JakartaReader(Json.createParserFactory(Map.of()));

//    final static ObjectMapper MAPPER = new ObjectMapper();
//
    @ParameterizedTest
    @ValueSource(strings = { "primitive-data-types", "uni-sort", "array", "object", "unicode", "french" })
    void testCanonizeJakarta(String name) throws IOException, TreeIOException {
        assertEquals(
                getResource(name + ".out.json"),
                Jcs.canonize(getJakartaJson(name + ".in.json")));
    }

//    @ParameterizedTest
//    @ValueSource(strings = { "primitive-data-types", "uni-sort", "array", "object", "unicode", "french" })
//    void testCanonizeJackson2(String name) throws IOException, TreeIOException {
//        assertEquals(
//                getResource(name + ".out.json"),
//                Jcs.canonize(getJacksonJson(name + ".in.json"), Jackson2Adapter.instance()));
//    }
//
    @ParameterizedTest
    @ValueSource(strings = { "primitive-data-types", "uni-sort", "array", "object", "unicode", "french" })
    void testCompareJakarta(String name) throws IOException, TreeIOException {
        assertTrue(Jcs.equals(
                getJakartaJson(name + ".in.json"),
                getJakartaJson(name + ".out.json")));
    }

//    @ParameterizedTest
//    @ValueSource(strings = { "primitive-data-types", "uni-sort", "array", "object", "unicode", "french" })
//    void testCompareJackson2(String name) throws IOException {
//        assertTrue(Jcs.valueEquals(
//                getJacksonJson(name + ".in.json"),
//                getJacksonJson(name + ".out.json"),
//                Jackson2Adapter.instance()));
//    }

    static String getResource(String name) throws IOException {
        try (var is = new BufferedInputStream(JcsTest.class.getResourceAsStream(name))) {
            return new BufferedReader(
                    new InputStreamReader(is, StandardCharsets.UTF_8))
                    .lines()
                    .collect(Collectors.joining("\n"));
        }
    }

    static Object getJakartaJson(String name) throws TreeIOException {

        return JAKARTA_READER.read(JcsTest.class.getResourceAsStream(name));

//        try (var reader = Json.createReader(
//                )) {
//            return reader.read();
//        }
    }
//
//    static JsonNode getJacksonJson(String name) throws IOException {
//        try (var is = JcsTest.class.getResourceAsStream(name);
//                var reader = new InputStreamReader(is, StandardCharsets.UTF_8)) {
//            return MAPPER.readTree(reader);
//        }
//    }
}