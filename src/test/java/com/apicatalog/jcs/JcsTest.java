package com.apicatalog.jcs;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import com.apicatalog.tree.io.Tree;
import com.apicatalog.tree.io.jakcson.Jackson2Parser;
import com.fasterxml.jackson.core.JsonFactory;

class JcsTest {

    static JsonFactory JACKSON = JsonFactory.builder().build();

    @ParameterizedTest
    @MethodSource({ "resources" })
    void testCanonizeToBytes(String name) throws IOException {
        assertArrayEquals(
                getBytes(name + ".out.json"),
                Jcs.canonize(getJacksonJson(name + ".in.json")));
    }

    @ParameterizedTest
    @MethodSource({ "resources" })
    void testCanonizeToStream(String name) throws IOException {

        var bos = new ByteArrayOutputStream();

        Jcs.canonize(getJacksonJson(name + ".in.json"), bos);

        assertArrayEquals(
                getBytes(name + ".out.json"),
                bos.toByteArray());
    }

//    @ParameterizedTest
//    @MethodSource({ "resources" })
//    void testCompareJakarta(String name) throws IOException {
//        assertTrue(Jcs.equals(
//                getJakartaJson(name + ".in.json"),
//                getJakartaJson(name + ".out.json")));
//    }
//
//    @ParameterizedTest
//    @MethodSource({ "resources" })
//    void testCompareJackson2(String name) throws IOException {
//        assertTrue(Jcs.equals(
//                getJacksonJson(name + ".in.json"),
//                getJacksonJson(name + ".out.json")));
//    }

    static final Stream<String> resources() {
        return Stream.of(new File(JcsTest.class.getResource("").getPath()).listFiles())
                .filter(File::isFile)
                .map(File::getName)
                .filter(name -> name.endsWith(".in.json"))
                .map(name -> name.substring(0, name.length() - ".in.json".length()));
    }

    static byte[] getBytes(String name) throws IOException {
        try (var is = new BufferedInputStream(JcsTest.class.getResourceAsStream(name))) {
            return new BufferedReader(
                    new InputStreamReader(is, StandardCharsets.UTF_8))
                    .lines()
                    .collect(Collectors.joining("\n")).getBytes();
        }
    }

//    static Object getJakartaJson(String name) throws IOException {
//        try (var parser = JakartaParser.newParser(JcsTest.class.getResourceAsStream(name), JAKARTA)) {
//            return Tree.read(parser);
//        }
//    }

    static Object getJacksonJson(String name) throws IOException {
        try (var parser = Jackson2Parser.newParser(JcsTest.class.getResourceAsStream(name), JACKSON)) {
            return Tree.read(parser);
        }
    }
}