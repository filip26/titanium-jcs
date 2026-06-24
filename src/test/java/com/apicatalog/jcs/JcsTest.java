package com.apicatalog.jcs;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import com.apicatalog.tree.io.jakarta.JakartaReader;
import com.apicatalog.tree.io.jakcson.Jackson2Reader;
import com.fasterxml.jackson.core.JsonFactory;

import jakarta.json.Json;

class JcsTest {

    static JakartaReader JAKARTA_READER = new JakartaReader(Json.createParserFactory(Map.of()));
    static Jackson2Reader JACKSON_READER = new Jackson2Reader(JsonFactory.builder().build());

    @ParameterizedTest
    @MethodSource({ "resources" })
    void testCanonizeJakarta(String name) throws IOException {
        assertEquals(
                getResource(name + ".out.json"),
                Jcs.canonize(getJakartaJson(name + ".in.json")));
    }

    @ParameterizedTest
    @MethodSource({ "resources" })
    void testCanonizeJackson2(String name) throws IOException {
        assertEquals(
                getResource(name + ".out.json"),
                Jcs.canonize(getJacksonJson(name + ".in.json")));
    }

    @ParameterizedTest
    @MethodSource({ "resources" })
    void testCompareJakarta(String name) throws IOException {
        assertTrue(Jcs.equals(
                getJakartaJson(name + ".in.json"),
                getJakartaJson(name + ".out.json")));
    }

    @ParameterizedTest
    @MethodSource({ "resources" })
    void testCompareJackson2(String name) throws IOException {
        assertTrue(Jcs.equals(
                getJacksonJson(name + ".in.json"),
                getJacksonJson(name + ".out.json")));
    }

    static final Stream<String> resources() {
        return Stream.of(new File(JcsTest.class.getResource("").getPath()).listFiles())
                .filter(File::isFile)
                .map(File::getName)
                .filter(name -> name.endsWith(".in.json"))
                .map(name -> name.substring(0, name.length() - ".in.json".length()));
    }

    static String getResource(String name) throws IOException {
        try (var is = new BufferedInputStream(JcsTest.class.getResourceAsStream(name))) {
            return new BufferedReader(
                    new InputStreamReader(is, StandardCharsets.UTF_8))
                    .lines()
                    .collect(Collectors.joining("\n"));
        }
    }

    static Object getJakartaJson(String name) throws IOException {
        return JAKARTA_READER.read(JcsTest.class.getResourceAsStream(name));
    }

    static Object getJacksonJson(String name) throws IOException {
        return JACKSON_READER.read(JcsTest.class.getResourceAsStream(name));
    }
}