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

import java.io.IOException;
import java.io.StringWriter;
import java.io.Writer;
import java.util.HexFormat;
import java.util.Map.Entry;

import com.apicatalog.tree.io.Tree;
import com.apicatalog.tree.io.TreeCursor;
import com.apicatalog.tree.io.TreeTraverser;
import com.apicatalog.tree.io.java.NativeTraverser;

/**
 * An implementation of the <a href="https://www.rfc-editor.org/rfc/rfc8785">RFC
 * 8785 JSON Canonicalization Scheme (JCS)</a>.
 *
 * <p>
 * This class provides the primary API for canonicalizing Java object structures
 * (such as Map, Collection, String, Number, Boolean, or null) into a
 * deterministic string representation and for comparing Java objects for
 * canonical equality. All methods are static and thread-safe.
 * </p>
 *
 * <p>
 * The implementation is completely agnostic by using the
 * {@link com.apicatalog.tree.io.TreeTraverser} abstraction to interact with
 * structures.
 * </p>
 *
 * <h2>Usage Examples</h2>
 * 
 * <pre>{@code
 * // Canonicalize a Java object and get the result as a String
 * String canonical = Jcs.canonize(javaObject);
 *
 * // Canonicalize a Java object and write to a Writer
 * Jcs.canonize(javaObject, writer);
 *
 * // Compare two Java objects for canonical equality
 * boolean areEqual = Jcs.equals(javaObject1, javaObject2);
 * }</pre>
 *
 * @see #canonize(Object)
 * @see #equals(Object, Object)
 */
public class Jcs {

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
     * Compares two scalar nodes for canonical equality using agnostic cursors.
     *
     * <p>
     * Two scalar nodes are canonically equal if they represent equivalent scalar
     * types (null, boolean, string, or number) and their normalized values match
     * according to JCS rules.
     * </p>
     *
     * @param cursor1 the first agnostic {@link TreeCursor} pointing to a scalar
     *                node
     * @param cursor2 the second agnostic {@link TreeCursor} pointing to a scalar
     *                node
     * @return {@code true} if the scalar values are canonically equal,
     *         {@code false} otherwise
     * @throws IllegalArgumentException if a cursor points to a non-scalar node type
     */
    public static boolean scalarEquals(TreeCursor cursor1, TreeCursor cursor2) {
        return switch (cursor1.nodeType()) {
        case NULL, TRUE, FALSE -> true;

        case STRING -> escape(cursor1.stringValue()).equals(escape(cursor2.stringValue()));

        case NUMBER -> canonizeNumber(cursor1.numberValue()).equals(canonizeNumber(cursor2.numberValue()));

        default -> throw new IllegalArgumentException("Expected scalar node but got " + cursor1.nodeType());
        };
    }

    /**
     * Canonicalizes a Java object (such as Map, Collection, String, Number,
     * Boolean, or null) according to JCS (RFC 8785) and returns the result as a
     * {@link String}.
     *
     * @param value the Java object to canonicalize (can be {@code null})
     * @return a string containing the canonical representation
     */
    public static String canonize(final Object value) {
        try {
            var writer = new StringWriter();
            canonize(value, writer);
            return writer.toString();
        } catch (IOException e) {
            // should not happen for StringWriter()
            throw new IllegalStateException(e);
        }
    }

    /**
     * Canonicalizes a Java object (such as Map, Collection, String, Number,
     * Boolean, or null) according to JCS (RFC 8785) and writes the output to the
     * provided {@link Writer}.
     *
     * @param value  the Java object to canonicalize (can be {@code null})
     * @param writer the {@link Writer} to which the canonical output is written
     * @throws IOException if an error occurs during canonicalization processing or
     *                     writing
     */
    public static void canonize(final Object value, Writer writer) throws IOException {
        canonize(new NativeTraverser(value, Jcs::entryKeyComparator), writer);
    }

    /**
     * Canonicalizes structure agnostic traversal according to JCS (RFC 8785) and
     * returns the result as a {@link String}. The {@link TreeTraverser} must have
     * set {@link TreeTraverser#comparator(java.util.Comparator)} to
     * {@link #entryKeyComparator(Entry, Entry)} equivalent.
     *
     * @param traverser the agnostic {@link TreeTraverser} to canonicalize
     * @return a string containing the canonical representation
     */
    protected static String canonize(TreeTraverser<?> traverser) {
        try {
            final var writer = new StringWriter();
            canonize(traverser, writer);
            return writer.toString();
        } catch (IOException e) {
            // should not happen for StringWriter()
            throw new IllegalStateException(e);
        }
    }

    /**
     * Canonicalizes structure agnostic traversal according to JCS (RFC 8785) and
     * writes the output to the provided {@link Writer}. The {@link TreeTraverser}
     * must have set {@link TreeTraverser#comparator(java.util.Comparator)} to
     * {@link #entryKeyComparator(Entry, Entry)} equivalent.
     *
     * @param traverser the agnostic {@link TreeTraverser} to canonicalize
     * @param writer    the {@link Writer} to which the canonical output is written
     * @throws IOException if an error occurs during canonicalization processing or
     *                     writing
     */
    protected static void canonize(TreeTraverser<?> traverser, Writer writer) throws IOException {
        Tree.write(traverser, new JcsEmitter(writer));
    }

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
    public static boolean equals(final Object value1, final Object value2) {

        var cursor1 = new NativeTraverser(value1, Jcs::entryKeyComparator);
        var cursor2 = new NativeTraverser(value2, Jcs::entryKeyComparator);

        return equals(cursor1, cursor2);
    }

    protected static boolean equals(TreeTraverser<?> node1, TreeTraverser<?> node2) {
        return Tree.equals(node1, node2, Jcs::scalarEquals);
    }

    /**
     * Canonicalizes as JSON number according to JCS (RFC 8785).
     *
     * Numbers are strictly restricted to IEEE 754 double-precision format.
     * Serialization matches ECMAScript Number.prototype.toString() rules exactly.
     *
     * @param number the BigDecimal to canonicalize
     * @return the canonical string representation of the number
     * @throws IllegalArgumentException if the number overflows IEEE limits
     */
    public static String canonizeNumber(final Number number) {
        // JCS assumes inputs are already parsed as IEEE-754 doubles.
        // Conversion natively applies standard IEEE rounding to arbitrary precision
        // inputs.
        double d = number.doubleValue();

        if (Double.isInfinite(d) || Double.isNaN(d)) {
            throw new IllegalArgumentException("RFC 8785 Compliance Error: Number exceeds IEEE 754 limits");
        }

        if (d == 0.0) {
            return "0";
        }

        // Java 21 Double.toString() (almost always) uses Ryu and produces the shortest
        // round-trip IEEE-754 representation, which matches ECMAScript
        // Number serialization for all finite doubles.
        String javaString = Double.toString(d);

        boolean isNegative = d < 0.0;
        int start = isNegative ? 1 : 0;

        int eIndex = javaString.indexOf('E', start);
        if (eIndex == -1) {
            // Truncate Java's forced ".0" for integers to match ECMAScript
            if (javaString.endsWith(".0")) {
                String res = javaString.substring(start, javaString.length() - 2);
                return isNegative ? "-" + res : res;
            }
            return javaString;
        }

        String mantissa = javaString.substring(start, eIndex);
        int dot = mantissa.indexOf('.');

        char firstDigit;
        String fractionDigits;

        if (dot < 0) {
            firstDigit = mantissa.charAt(0);
            fractionDigits = mantissa.length() > 1 ? mantissa.substring(1) : "";
        } else {
            firstDigit = mantissa.charAt(0);
            fractionDigits = mantissa.substring(dot + 1);

            // Defensive trailing zero truncation to guarantee ECMAScript compliance
            // regardless of underlying JVM formatter drift.
            int len = fractionDigits.length();
            while (len > 0 && fractionDigits.charAt(len - 1) == '0') {
                len--;
            }
            if (len != fractionDigits.length()) {
                fractionDigits = fractionDigits.substring(0, len);
            }
        }

        int exponent = Integer.parseInt(javaString.substring(eIndex + 1));

        if (exponent >= 21 || exponent <= -7) {
            StringBuilder sb = new StringBuilder(32);
            if (isNegative)
                sb.append('-');
            sb.append(firstDigit);
            if (!fractionDigits.isEmpty()) {
                sb.append('.').append(fractionDigits);
            }
            sb.append('e');
            if (exponent > 0) {
                sb.append('+');
            }
            sb.append(exponent);
            return sb.toString();
        }

        // Plain expansion for ES6 overlapping limits (-6 <= exponent <= 20)
        StringBuilder sb = new StringBuilder(64);
        if (isNegative)
            sb.append('-');

        if (exponent == 0) {
            sb.append(firstDigit);
            if (!fractionDigits.isEmpty()) {
                sb.append('.').append(fractionDigits);
            }
        } else if (exponent > 0) {
            sb.append(firstDigit);
            if (exponent >= fractionDigits.length()) {
                sb.append(fractionDigits);
                int remainingZeros = exponent - fractionDigits.length();
                for (int i = 0; i < remainingZeros; i++) {
                    sb.append('0');
                }
            } else {
                sb.append(fractionDigits, 0, exponent);
                sb.append('.');
                sb.append(fractionDigits, exponent, fractionDigits.length());
            }
        } else {
            sb.append("0.");
            int leadingZeros = (-exponent) - 1;
            for (int i = 0; i < leadingZeros; i++) {
                sb.append('0');
            }
            sb.append(firstDigit);
            sb.append(fractionDigits);
        }

        return sb.toString();
    }

    /**
     * Escapes a string according to JCS (RFC 8785, Section 2.5) rules.
     *
     * @param value the string to escape
     * @return the escaped string
     * @throws IllegalArgumentException if invalid Unicode data (lone surrogates) is
     *                                  detected
     */
    public static String escape(String value) {
        final StringBuilder escaped = new StringBuilder();
        final HexFormat hexFormat = HexFormat.of();
        final int length = value.length();

        for (int i = 0; i < length;) {
            int ch = value.codePointAt(i);
            switch (ch) {
            case '\t' -> escaped.append("\\t");
            case '\b' -> escaped.append("\\b");
            case '\n' -> escaped.append("\\n");
            case '\r' -> escaped.append("\\r");
            case '\f' -> escaped.append("\\f");
            case '\"' -> escaped.append("\\\"");
            case '\\' -> escaped.append("\\\\");
            default -> {
                if (ch <= 0x1F) {
                    escaped.append("\\u").append(hexFormat.toHexDigits((char) ch));
                } else if (ch >= 0xD800 && ch <= 0xDFFF) {
                    throw new IllegalArgumentException(
                            "RFC 8785 Compliance Error: Invalid Unicode data (lone surrogate) detected at index " + i);
                } else {
                    escaped.appendCodePoint(ch);
                }
            }
            }
            i += Character.charCount(ch);
        }
        return escaped.toString();
    }
}