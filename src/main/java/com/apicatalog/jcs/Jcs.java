/*
 * Copyright 2020 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.apicatalog.jcs;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Deque;
import java.util.HexFormat;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;

/**
 * A non-recursive implementation of the
 * <a href="https://www.rfc-editor.org/rfc/rfc8785">RFC 8785 JSON
 * Canonicalization Scheme (JCS)</a>.
 *
 * <p>
 * This class provides the primary API for canonicalizing Java object structures
 * (such as Map, Collection, String, Number, Boolean, or null) into a
 * deterministic byte array representation and for comparing Java objects for
 * canonical equality. All methods are static and thread-safe.
 * </p>
 *
 * <h2>Usage Examples</h2>
 * 
 * <pre>{@code
 * // Canonicalize a Java object and get the result as a String
 * byte[] canonical = Jcs.canonize(javaObject);
 *
 * // Canonicalize a Java object and write to a Writer
 * Jcs.canonize(javaObject, writer);
 *
 * // Compare two Java objects for canonical equality
 * boolean areEqual = Jcs.equals(javaObject1, javaObject2);
 * }</pre>
 *
 * @see #canonize(Object)
 * @see #canonize(Object, int)
 * @see #equals(Object, Object)
 */
public final class Jcs {

    public static byte[] NULL = "null".getBytes(StandardCharsets.UTF_8);
    public static byte[] TRUE = "true".getBytes(StandardCharsets.UTF_8);
    public static byte[] FALSE = "false".getBytes(StandardCharsets.UTF_8);
    public static byte[] EMPTY_MAP = "{}".getBytes(StandardCharsets.UTF_8);
    public static byte[] EMPTY_ARRAY = "[]".getBytes(StandardCharsets.UTF_8);

    private Jcs() { 
        //protected
    }
    
    /**
     * Compares two map entries by their string keys lexicographically.
     *
     * <p>
     * This comparator is used to enforce deterministic sorting of entry keys during
     * canonicalization processing.
     * </p>
     *
     * @param e1 the first map entry to compare
     * @param e2 the second map entry to compare
     * @return a negative integer, zero, or a positive integer as the first entry
     *         key is less than, equal to, or greater than the second entry key
     * @throws IllegalArgumentException if either entry key is not a {@link String}
     */
    public static int entryKeyComparator(Entry<?, ?> e1, Entry<?, ?> e2) {
        if (e1.getKey() instanceof String key1 && e2.getKey() instanceof String key2) {
            return key1.compareTo(key2);
        }
        throw new IllegalArgumentException();
    }

    /**
     * Canonicalizes a Java object (such as Map, Collection, String, Number,
     * Boolean, or null) according to JCS (RFC 8785) and returns the result as a
     * byte array.
     *
     * @param value the Java object to canonicalize (can be {@code null})
     * @return byte array containing the canonical representation
     */
    public static byte[] canonize(final Object value) {
        return canonize(value, -1);
    }

    /**
     * Canonicalizes a Java object (such as Map, Collection, String, Number,
     * Boolean, or null) according to JCS (RFC 8785) and returns the result as a
     * byte array.
     *
     * @param value      the Java object to canonicalize (can be {@code null})
     * @param maxVisited the maximum number of nodes to traverse, {@code -1} for
     *                   unlimited
     * @return byte array containing the canonical representation
     */
    public static byte[] canonize(final Object value, final int maxVisited) {
        return switch (value) {
        case null -> NULL;
        case Boolean bool -> bool ? TRUE : FALSE;
        case Number number -> canonizeNumber(number);
        case String string -> escapedString(string);
        case Map<?, ?> map -> {
            if (map.isEmpty()) {
                yield EMPTY_MAP;
            }

            var entries = new ArrayList<>(map.entrySet());
            Collections.sort(entries, Jcs::entryKeyComparator);
            var queue = new ArrayDeque<>();
            queue.push('}');
            queue.push(entries.iterator());
            queue.push('{');
            yield c14n(queue, maxVisited);
        }
        case Collection<?> col -> {
            if (col.isEmpty()) {
                yield EMPTY_ARRAY;
            }

            var queue = new ArrayDeque<>();
            queue.push(']');
            queue.push(col.iterator());
            queue.push('[');
            yield c14n(queue, maxVisited);
        }
        default -> throw new IllegalArgumentException("Unsupported type " + value);
        };
    }

    private static byte[] c14n(final Deque<Object> stack, int maxVisited) {

        var bos = new ByteArrayOutputStream();
        int visited = 0;

        try {
            do {
                var item = stack.peek();

                if (item instanceof Iterator<?> it) {
                    if (it.hasNext()) {

                        var element = it.next();

                        if (maxVisited > 0 && visited++ > maxVisited) {
                            throw new IllegalStateException(
                                    "The maximum visited nodes limit has exceeded, maxVisited=" + maxVisited);
                        }

                        if (it.hasNext()) {
                            stack.push(',');
                        } else {
                            stack.pop();
                        }

                        if (element instanceof Entry entry) {
                            bos.write(escapedString((String) entry.getKey()));
                            bos.write(':');
                            c14n(entry.getValue(), stack, bos);

                        } else {
                            c14n(element, stack, bos);
                        }

                    } else {
                        stack.pop();
                    }

                } else if (item instanceof Character ch) {
                    bos.write(ch);
                    stack.pop();
                }

            } while (!stack.isEmpty());

        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        return bos.toByteArray();
    }

    private static void c14n(Object value, Deque<Object> stack, OutputStream os) throws IOException {

        switch (value) {
        case null -> os.write(NULL);
        case Boolean bool -> os.write(bool ? TRUE : FALSE);
        case Number number -> os.write(canonizeNumber(number));
        case String string -> os.write(escapedString(string));
        case Map<?, ?> map -> {
            var entries = new ArrayList<>(map.entrySet());
            Collections.sort(entries, Jcs::entryKeyComparator);
            stack.push('}');
            stack.push(entries.iterator());
            stack.push('{');
        }
        case Collection<?> col -> {
            stack.push(']');
            stack.push(col.iterator());
            stack.push('[');
        }
        default -> throw new IllegalArgumentException("Unsupported type " + value);
        }
        ;
    }

    /**
     * Canonicalizes a Java object (such as Map, Collection, String, Number,
     * Boolean, or null) according to JCS (RFC 8785) and writes the output to the
     * provided {@link OutputStream}.
     *
     * @param value the Java object to canonicalize (can be {@code null})
     * @param os    the {@link OutputStream} to which the canonical output is
     *              written
     * @throws IOException if an error occurs during canonicalization processing or
     *                     writing
     */
//    public static void canonize(final Object value, OutputStream os) throws IOException {
//        switch (value) {
//        case null -> os.write(JcsEmitter.NULL);
//        case Boolean bool -> os.write(bool ? JcsEmitter.TRUE : JcsEmitter.FALSE);
//        case Number number -> os.write(canonizeNumber(number));
//        case String string -> os.write(escape(string));
//        case Map<?, ?> map -> {
//            new Jcs(map).canonize();
//        }
//        default -> canonize(new NativeTraverser(value, Jcs::entryKeyComparator), os);
//        }
//        ;
//    }

    /**
     * Compares two Java objects (such as Map, Collection, String, Number, Boolean,
     * or null) for canonical equality under JCS (RFC 8785).
     *
     * <p>
     * Two Java objects are canonically equal if their data models are equivalent.
     * This involves comparing numbers by their canonical string representation and
     * objects/maps by their members, sorted lexicographically by key.
     * </p>
     *
     * @param value1 the first Java object to compare (can be {@code null})
     * @param value2 the second Java object to compare (can be {@code null})
     * @return {@code true} if the values are canonically equal, {@code false}
     *         otherwise
     */
//    public static boolean equals(final Object value1, final Object value2) {
//
//        var cursor1 = new NativeTraverser(value1, Jcs::entryKeyComparator);
//        var cursor2 = new NativeTraverser(value2, Jcs::entryKeyComparator);
//
//        return equals(cursor1, cursor2);
//    }
//
//    protected static boolean equals(TreeTraverser<?> node1, TreeTraverser<?> node2) {
//        return Tree.identical(node1, node2, Jcs::scalarEquals);
//    }

    /**
     * Canonicalizes as JSON number according to JCS (RFC 8785).
     *
     * Numbers are strictly restricted to IEEE 754 double-precision format.
     * Serialization matches ECMAScript Number.prototype.toString() rules exactly.
     *
     * @param number the BigDecimal to canonicalize
     * @return the canonical byte array representation of the number
     * @throws IllegalArgumentException if the number overflows IEEE limits
     */
    public static byte[] canonizeNumber(final Number number) {
        double d = number.doubleValue();

        if (Double.isInfinite(d) || Double.isNaN(d)) {
            throw new IllegalArgumentException("RFC 8785 Compliance Error: Number exceeds IEEE 754 limits");
        }

        if (d == 0.0) {
            return new byte[] { '0' };
        }

        var string = Double.toString(d);

        var isNegative = d < 0.0;
        var start = isNegative ? 1 : 0;

        var eIndex = string.indexOf('E', start);
        if (eIndex == -1) {
            if (string.endsWith(".0")) {
                int length = string.length() - 2;
                byte[] res = new byte[length];
                for (int i = 0; i < length; i++) {
                    res[i] = (byte) string.charAt(i);
                }
                return res;
            }
            return string.getBytes(StandardCharsets.UTF_8);
        }

        int dot = string.indexOf('.', start);
        char firstDigit = string.charAt(start);

        int fracStart = (dot < 0) ? start + 1 : dot + 1;
        int fracEnd = eIndex;
        while (fracEnd > fracStart && string.charAt(fracEnd - 1) == '0') {
            fracEnd--;
        }
        int fracLen = fracEnd - fracStart;

        int exponent = Integer.parseInt(string, eIndex + 1, string.length(), 10);

        byte[] buf = new byte[128];
        int pos = 0;

        if (isNegative) {
            buf[pos++] = '-';
        }

        if (exponent >= 21 || exponent <= -7) {
            buf[pos++] = (byte) firstDigit;
            if (fracLen > 0) {
                buf[pos++] = '.';
                for (int i = fracStart; i < fracEnd; i++) {
                    buf[pos++] = (byte) string.charAt(i);
                }
            }
            buf[pos++] = 'e';
            if (exponent > 0) {
                buf[pos++] = '+';
            }
            String expStr = Integer.toString(exponent);
            for (int i = 0; i < expStr.length(); i++) {
                buf[pos++] = (byte) expStr.charAt(i);
            }
            return Arrays.copyOf(buf, pos);
        }

        if (exponent == 0) {
            buf[pos++] = (byte) firstDigit;
            if (fracLen > 0) {
                buf[pos++] = '.';
                for (int i = fracStart; i < fracEnd; i++) {
                    buf[pos++] = (byte) string.charAt(i);
                }
            }
        } else if (exponent > 0) {
            buf[pos++] = (byte) firstDigit;
            if (exponent >= fracLen) {
                for (int i = fracStart; i < fracEnd; i++) {
                    buf[pos++] = (byte) string.charAt(i);
                }
                int remainingZeros = exponent - fracLen;
                for (int i = 0; i < remainingZeros; i++) {
                    buf[pos++] = '0';
                }
            } else {
                for (int i = 0; i < exponent; i++) {
                    buf[pos++] = (byte) string.charAt(fracStart + i);
                }
                buf[pos++] = '.';
                for (int i = exponent; i < fracLen; i++) {
                    buf[pos++] = (byte) string.charAt(fracStart + i);
                }
            }
        } else {
            buf[pos++] = '0';
            buf[pos++] = '.';
            int leadingZeros = (-exponent) - 1;
            for (int i = 0; i < leadingZeros; i++) {
                buf[pos++] = '0';
            }
            buf[pos++] = (byte) firstDigit;
            for (int i = fracStart; i < fracEnd; i++) {
                buf[pos++] = (byte) string.charAt(i);
            }
        }

        return Arrays.copyOf(buf, pos);
    }

    /**
     * Escapes a string according to JCS (RFC 8785, Section 2.5) rules and encodes
     * the result directly to a UTF-8 byte array.
     *
     * @param value the string to escape
     * @return the escaped UTF-8 byte array
     * @throws IllegalArgumentException if invalid Unicode data (lone surrogates) is
     *                                  detected
     */
    static byte[] escapedString(String value) {
        final int length = value.length();
        final ByteArrayOutputStream out = new ByteArrayOutputStream(Math.max(length + 2, 18));
        final HexFormat hexFormat = HexFormat.of();

        out.write('"');

        for (int i = 0; i < length;) {
            int ch = value.codePointAt(i);
            switch (ch) {
            case '\t' -> {
                out.write('\\');
                out.write('t');
            }
            case '\b' -> {
                out.write('\\');
                out.write('b');
            }
            case '\n' -> {
                out.write('\\');
                out.write('n');
            }
            case '\r' -> {
                out.write('\\');
                out.write('r');
            }
            case '\f' -> {
                out.write('\\');
                out.write('f');
            }
            case '\"' -> {
                out.write('\\');
                out.write('"');
            }
            case '\\' -> {
                out.write('\\');
                out.write('\\');
            }
            default -> {
                if (ch <= 0x1F) {
                    out.write('\\');
                    out.write('u');
                    out.write('0');
                    out.write('0');
                    out.write(hexFormat.toHighHexDigit((byte) ch));
                    out.write(hexFormat.toLowHexDigit((byte) ch));

                } else if (ch >= 0xD800 && ch <= 0xDFFF) {
                    throw new IllegalArgumentException(
                            "RFC 8785 Compliance Error: Invalid Unicode data (lone surrogate) detected at index " + i);
                } else if (ch <= 0x7F) {
                    out.write(ch);

                } else if (ch <= 0x7FF) {
                    out.write(0xC0 | (ch >> 6));
                    out.write(0x80 | (ch & 0x3F));

                } else if (ch <= 0xFFFF) {
                    out.write(0xE0 | (ch >> 12));
                    out.write(0x80 | ((ch >> 6) & 0x3F));
                    out.write(0x80 | (ch & 0x3F));

                } else {
                    out.write(0xF0 | (ch >> 18));
                    out.write(0x80 | ((ch >> 12) & 0x3F));
                    out.write(0x80 | ((ch >> 6) & 0x3F));
                    out.write(0x80 | (ch & 0x3F));
                }
            }
            }
            i += Character.charCount(ch);
        }
        out.write('"');
        return out.toByteArray();
    }
}