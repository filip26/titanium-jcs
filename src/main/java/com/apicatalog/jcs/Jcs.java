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
import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Locale;

import com.apicatalog.tree.io.TreeAdapter;
import com.apicatalog.tree.io.TreeComparison;
import com.apicatalog.tree.io.Tree.NodeType;
import com.apicatalog.tree.io.TreeIOException;

/**
 * An implementation of the <a href="https://www.rfc-editor.org/rfc/rfc8785">RFC
 * 8785 JSON Canonicalization Scheme (JCS)</a>.
 *
 * <p>
 * This class provides the primary API for canonicalizing JSON structures into a
 * deterministic string representation and for comparing JSON values for
 * canonical equality. All methods are static and thread-safe.
 * </p>
 *
 * <p>
 * The implementation is agnostic to the underlying JSON object model by using
 * the {@link com.apicatalog.tree.io.TreeAdapter} interface to interact with
 * JSON structures.
 * </p>
 *
 * <h2>Usage Examples</h2>
 * 
 * <pre>{@code
 * // Canonicalize a JSON value and get the result as a String
 * String canonicalJson = Jcs.canonize(jsonValue, adapter);
 *
 * // Canonicalize a JSON value and write to a Writer
 * Jcs.canonize(jsonValue, adapter, writer);
 *
 * // Compare two JSON values for canonical equality
 * boolean areEqual = Jcs.equals(jsonValue1, jsonValue2, adapter);
 * }</pre>
 *
 * @see #canonize(Object, com.apicatalog.tree.io.TreeAdapter)
 * @see #equals(Object, Object, com.apicatalog.tree.io.TreeAdapter)
 */
public final class Jcs {

    /**
     * Exponent notation format for numbers outside the range [10<sup>-21</sup>,
     * 10<sup>21</sup>).
     */
    private static final DecimalFormat E_FORMAT_BIG_DECIMAL = new DecimalFormat("0E00", new DecimalFormatSymbols(Locale.ENGLISH));

    /**
     * Plain notation format for numbers within the range [10<sup>-21</sup>,
     * 10<sup>21</sup>).
     */
    private static final DecimalFormat PLAIN_FORMAT = new DecimalFormat("0.#######", new DecimalFormatSymbols(Locale.ENGLISH));

    /**
     * Canonicalizes a JSON value according to JCS (RFC 8785) and returns the result
     * as a {@link String}.
     *
     * @param value   the JSON value to canonicalize (can be {@code null})
     * @param adapter the {@link TreeAdapter} used to inspect the JSON value
     * @return a string containing the canonical JSON representation
     * @throws TreeIOException
     */
    public static String canonize(final Object value, final TreeAdapter adapter) throws TreeIOException {
        final StringWriter writer = new StringWriter();
        try {
            canonize(value, adapter, writer);
        } catch (IOException e) {
            // This should not happen with a StringWriter
            throw new IllegalStateException("Unexpected IOException from StringWriter.", e);
        }
        return writer.toString();
    }

    /**
     * Canonicalizes a JSON value according to JCS (RFC 8785) and writes the output
     * to the provided {@link Writer}.
     *
     * @param value   the JSON value to canonicalize (can be {@code null})
     * @param adapter the {@link TreeAdapter} used to inspect the JSON value
     * @param writer  the {@link Writer} to which the canonical output is written
     * @throws IOException if an I/O error occurs
     */
    public static void canonize(final Object value, final TreeAdapter adapter, final Writer writer) throws IOException, TreeIOException {
        if (adapter.isNull(value)) {
            try {
                writer.write("null");
            } catch (IOException e) {
                throw new TreeIOException(e);
            }
            return;
        }
        (new JcsGenerator(writer)).node(value, adapter);
    }

    /**
     * Compares two JSON values for canonical equality under JCS (RFC 8785).
     *
     * <p>
     * Two JSON values are canonically equal if their data models are equivalent.
     * This involves comparing numbers by their canonical string representation and
     * objects by their members, sorted lexicographically by key.
     * </p>
     *
     * @param value1  the first JSON value to compare (can be {@code null})
     * @param value2  the second JSON value to compare (can be {@code null})
     * @param adapter the {@link TreeAdapter} used to inspect the JSON values
     * @return {@code true} if the values are canonically equal, {@code false}
     *         otherwise
     */
    public static boolean equals(final Object value1, final Object value2, final TreeAdapter adapter) {
        if (value1 == null) {
            return value2 == null || adapter.isNull(value2);

        } else if (value2 == null) {
            return adapter.isNull(value1);
        }

        NodeType nodeType1 = adapter.type(value1);
        NodeType nodeType2 = adapter.type(value2);

        if (nodeType1 != nodeType2) {
            return false;
        }

        switch (nodeType1) {
        case NULL:
        case TRUE:
        case FALSE:
            return true;

        case STRING:
            return escape(adapter.stringValue(value1)).equals(escape(adapter.stringValue(value2)));

        case NUMBER:
            return canonizeNumber(adapter.asDecimal(value1)).equals(canonizeNumber(adapter.asDecimal(value2)));

        case SEQUENCE:
            return arrayEquals(value1, value2, adapter);

        case MAP:
            return objectEquals(value1, value2, adapter);

        default:
            return false;
        }
    }

    /**
     * Compares two JSON objects for canonical equality.
     * <p>
     * Objects are equal if they have the same set of keys and the corresponding
     * values for each key are canonically equal.
     * </p>
     *
     * @param object1 the first object
     * @param object2 the second object
     * @param adapter the node adapter
     * @return {@code true} if the objects are canonically equal, {@code false}
     *         otherwise
     */
    static boolean objectEquals(final Object object1, final Object object2, final TreeAdapter adapter) {

        final var entries1 = adapter.entryStream(object1)
                .sorted(TreeComparison.comparingEntryKey(adapter::asString))
                .iterator();

        final var entries2 = adapter.entryStream(object2)
                .sorted(TreeComparison.comparingEntryKey(adapter::asString))
                .iterator();

        while (entries1.hasNext() && entries2.hasNext()) {

            final var entry1 = entries1.next();
            final var entry2 = entries2.next();

            if (!adapter.asString(entry1.getKey()).equals(adapter.asString(entry2.getKey()))
                    || !equals(entry1.getValue(), entry2.getValue(), adapter)) {
                return false;
            }
        }

        return !entries1.hasNext() && !entries2.hasNext();
    }

    /**
     * Compares two JSON arrays for canonical equality.
     * <p>
     * Arrays are equal if they have the same length and their elements are
     * canonically equal in the same order.
     * </p>
     *
     * @param array1  the first array
     * @param array2  the second array
     * @param adapter the node adapter
     * @return {@code true} if the arrays are canonically equal, {@code false}
     *         otherwise
     */
    static boolean arrayEquals(final Object array1, final Object array2, final TreeAdapter adapter) {

        final var it1 = adapter.elements(array1).iterator();
        final var it2 = adapter.elements(array2).iterator();

        while (it1.hasNext() && it2.hasNext()) {
            if (!equals(it1.next(), it2.next(), adapter)) {
                return false;
            }
        }
        return !it1.hasNext() && !it2.hasNext();
    }

    /**
     * Canonicalizes a JSON number according to JCS (RFC 8785).
     * <p>
     * Numbers are serialized using plain notation if they are within the range
     * [10<sup>-21</sup>, 10<sup>21</sup>), and exponential notation otherwise.
     * </p>
     *
     * @param number the {@link BigDecimal} to canonicalize
     * @return the canonical string representation of the number
     */
    static String canonizeNumber(final BigDecimal number) {
        if (number.compareTo(BigDecimal.ZERO) == 0) {
            return "0";
        }
        if (number.compareTo(BigDecimal.ONE.movePointRight(21)) >= 0) {
            return E_FORMAT_BIG_DECIMAL.format(number).replace("E", "e+");
        }
        if (number.compareTo(BigDecimal.ONE.movePointLeft(21)) < 0 && number.compareTo(BigDecimal.ZERO) != 0) {
            return E_FORMAT_BIG_DECIMAL.format(number).toLowerCase();
        }
        return PLAIN_FORMAT.format(number);
    }

    /**
     * Escapes a string according to JCS (RFC 8785, Section 2.5) rules.
     *
     * @param value the string to escape
     * @return the escaped string
     */
    static String escape(String value) {
        final StringBuilder escaped = new StringBuilder();
        int[] codePoints = value.codePoints().toArray();

        for (int ch : codePoints) {
            switch (ch) {
            case '\t':
                escaped.append("\\t");
                break;
            case '\b':
                escaped.append("\\b");
                break;
            case '\n':
                escaped.append("\\n");
                break;
            case '\r':
                escaped.append("\\r");
                break;
            case '\f':
                escaped.append("\\f");
                break;
            case '\"':
                escaped.append("\\\"");
                break;
            case '\\':
                escaped.append("\\\\");
                break;
            default:
                if (ch >= 0x00 && ch <= 0x1F) {
                    escaped.append(String.format("\\u%04x", ch));
                } else {
                    escaped.appendCodePoint(ch);
                }
                break;
            }
        }
        return escaped.toString();
    }
}