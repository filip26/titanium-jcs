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
import java.util.Iterator;
import java.util.Locale;
import java.util.Map.Entry;

import com.apicatalog.tree.io.NodeAdapter;
import com.apicatalog.tree.io.NodeModel;
import com.apicatalog.tree.io.NodeType;

import jakarta.json.JsonNumber;
import jakarta.json.JsonObject;
import jakarta.json.JsonValue;

/**
 * An implementation of the <a href="https://www.rfc-editor.org/rfc/rfc8785">
 * RFC 8785 JSON Canonicalization Scheme (JCS)</a> specification in Java.
 *
 * <p>
 * This class provides two main capabilities:
 * </p>
 * <ul>
 * <li><strong>Canonicalization</strong> – serializing JSON values into a
 * deterministic and standardized form, ensuring that the same JSON data model
 * always produces the same byte sequence.</li>
 * <li><strong>Equality comparison</strong> – comparing JSON values for
 * canonical equality, i.e., equality after applying JCS rules for numbers,
 * arrays, and objects.</li>
 * </ul>
 *
 * <p>
 * This implementation uses <a href="https://github.com/filip26/tree-io">Tree
 * I/O Processing</a> as input and can output canonicalized JSON to a
 * {@link java.io.Writer} or return it as a {@link java.lang.String}.
 * </p>
 *
 * <p>
 * <strong>Usage examples:</strong>
 * </p>
 *
 * <pre>{@code
 * // Canonicalize a JSON value and write to a writer
 * Jcs.canonize(nodeValue, writer);
 * // or
 * Jcs.canonize(jsonValue, adapter, writer);
 *
 * // Compare two JSON values for canonical equality
 * boolean equal = Jcs.equals(json1, json2);
 * }</pre>
 */
public final class Jcs {

    /**
     * Exponent notation used by JCS for very large/small {@link BigDecimal} values
     * (English locale).
     */
    protected static final DecimalFormat eFormatBigDecimal = new DecimalFormat("0E00", new DecimalFormatSymbols(Locale.ENGLISH));

    /**
     * Plain notation used by JCS for mid-range {@link BigDecimal} values (English
     * locale).
     */
    protected static final DecimalFormat eFormat = new DecimalFormat("0.#######", new DecimalFormatSymbols(Locale.ENGLISH));

    /**
     * Canonicalizes a JSON value according to the RFC 8785 JSON Canonicalization
     * Scheme (JCS) and returns the canonicalized JSON as a string.
     * <p>
     * This method serializes the given {@link JsonValue} in a deterministic and
     * standardized manner, ensuring a consistent output regardless of formatting
     * differences. It handles all JSON value types, including objects, arrays,
     * numbers, strings, and literals (true, false, null).
     * </p>
     *
     * @param value   the JSON value to be canonicalized
     * @param adapter
     * @return a string containing the canonicalized JSON representation of the
     *         input value
     */
    public static final String canonize(final Object value, final NodeAdapter adapter) {

        final StringWriter writer = new StringWriter();

        try {
            canonize(value, adapter, writer);
        } catch (IOException e) {
            // ignore
        }

        return writer.toString();
    }

    /**
     * Canonicalizes a JSON according to the RFC 8785 JSON Canonicalization Scheme
     * (JCS).
     * <p>
     * This method serializes the given {@link JsonValue} in a deterministic and
     * standardized manner, ensuring a consistent output regardless of formatting
     * differences. The canonicalized JSON is written to the provided
     * {@link Writer}.
     * </p>
     * <p>
     * This method handles different JSON value types, including objects, arrays,
     * numbers, strings, and literals (true, false, null).
     * </p>
     *
     * @param node    the JSON value to be canonicalized
     * @param adapter
     * @param writer  the writer to which the canonicalized JSON output is written
     * @throws IOException if an I/O error occurs while writing to the writer
     */
    public static final void canonize(final Object value, final NodeAdapter adapter, final Writer writer) throws IOException {
        final NodeType nodeType = value != null ? adapter.type(value) : null;

        if (nodeType == null || NodeType.NULL == nodeType) {
            writer.write("null");
            return;
        }

        switch (nodeType) {
        case COLLECTION:
            canonizeArray(value, adapter, writer);
            break;

        case MAP:
            canonizeObject(value, adapter, writer);
            break;

        case NUMBER:
            writer.write(canonizeNumber(adapter.asDecimal(value)));
            break;

        case STRING:
            writer.write('"');
            writer.write(escape(adapter.asString(value)));
            writer.write('"');
            break;

        case FALSE:
        case TRUE:
        case NULL:
            writer.write(adapter.asString(value));
            break;

        default:
            throw new IllegalArgumentException("Node type " + nodeType + " is not supported.");
        }
    }

    /**
     * Canonicalizes a JSON number according to the RFC 8785 JSON Canonicalization
     * Scheme (JCS).
     * <p>
     * This method serializes the given {@link JsonNumber} in a deterministic and
     * standardized manner, ensuring a consistent numeric representation.
     * </p>
     *
     * @param number the JSON number to be canonicalized
     * @return the canonicalized representation
     */
    static final String canonizeNumber(final BigDecimal number) {
        if (number.compareTo(BigDecimal.ZERO) == 0) {
            return "0";
        }

        if (number.compareTo(BigDecimal.ONE.movePointRight(21)) >= 0) {
            return eFormatBigDecimal.format(number).replace("E", "e+");
        }

        if (number.compareTo(BigDecimal.ONE.movePointLeft(21)) <= 0) {
            return eFormatBigDecimal.format(number).toLowerCase();
        }

        return eFormat.format(number);
    }

    static final void canonizeArray(final Object value, final NodeAdapter adapter, final Writer writer) throws IOException {
        boolean next = false;

        writer.write('[');

        for (final Object item : adapter.asIterable(value)) {

            if (next) {
                writer.write(',');
            }

            canonize(item, adapter, writer);

            next = true;
        }

        writer.write(']');
    }

    /**
     * Canonicalizes a JSON object according to the RFC 8785 JSON Canonicalization
     * Scheme (JCS).
     * <p>
     * This method serializes the given {@link JsonObject} in a deterministic and
     * standardized manner, ensuring a consistent output. The canonicalized JSON is
     * written to the provided {@link Writer}.
     * </p>
     *
     * @param value  the JSON object to be canonicalized
     * @param writer the writer to which the canonicalized JSON output is written
     * @throws IOException if an I/O error occurs while writing to the writer
     */
    static final void canonizeObject(final Object value, final NodeAdapter adapter, final Writer writer) throws IOException {
        writer.write('{');

        final Iterator<Entry<?, ?>> sorted = adapter.streamEntries(value)
                .sorted(NodeModel.comparingEntry(e -> adapter.asString(e.getKey())))
                .iterator();

        while (sorted.hasNext()) {

            final Entry<?, ?> entry = sorted.next();

            writer.write('"');
            writer.write(escape(adapter.asString(entry.getKey())));
            writer.write("\":");

            canonize(entry.getValue(), adapter, writer);

            if (sorted.hasNext()) {
                writer.write(',');
            }
        }

        writer.write('}');
    }

    /**
     * Escapes special characters in a JSON property name according to JSON
     * canonicalization rules.
     * <p>
     * This method ensures that control characters and other necessary characters
     * are properly escaped to maintain a valid and consistent JSON representation.
     * </p>
     *
     * @param value the JSON property name to escape
     * @return the escaped property name
     */
    static final String escape(String value) {

        final StringBuilder escaped = new StringBuilder();

        int[] codePoints = value.codePoints().toArray();

        for (int ch : codePoints) {

            if (ch == 0x9) {
                escaped.append("\\t");

            } else if (ch == 0x8) {
                escaped.append("\\b");

            } else if (ch == 0xa) {
                escaped.append("\\n");

            } else if (ch == 0xd) {
                escaped.append("\\r");

            } else if (ch == 0xc) {
                escaped.append("\\f");

            } else if (ch == '"') {
                escaped.append("\\\"");

            } else if (ch == '\\') {
                escaped.append("\\\\");

            } else if (ch >= 0x0 && ch <= 0x1f || ch == 0x7f) {
                escaped.append(String.format("\\u%04x", ch));

            } else {
                escaped.appendCodePoint(ch);
            }
        }
        return escaped.toString();
    }

    /**
     * Compares two JSON values for canonical equality under RFC 8785 (JCS).
     *
     * <p>
     * Values are considered equal if their JSON data models are the same after
     * applying JCS canonicalization rules. That includes canonical number
     * formatting and lexicographic member ordering for objects.
     * </p>
     *
     * @param value1  the first JSON value (may be {@code null})
     * @param value2  the second JSON value (may be {@code null})
     * @param adapter
     * @return {@code true} if the two values are canonically equal; {@code false}
     *         otherwise
     */
    public static final boolean equals(final Object value1, final Object value2, final NodeAdapter adapter) {

        if (value1 == null) {
            return value2 == null || adapter.isNull(value2);

        } else if (value2 == null) {
            return adapter.isNull(value1);
        }

        NodeType nodeType = adapter.type(value1);

        if (nodeType != adapter.type(value2)) {
            return false;
        }

        switch (nodeType) {
        case NULL:
        case TRUE:
        case FALSE:
            return true;

        case STRING:
            return value1.equals(value2);

        case NUMBER:
            return numberEquals(adapter.asDecimal(value1), adapter.asDecimal(value2));

        case COLLECTION:
            return arrayEquals(value1, value2, adapter);

        case MAP:
            return objectEquals(value1, value2, adapter);

        default:
            return false;
        }
    }

    /**
     * Compares two JSON numbers for canonical equality under RFC 8785 (JCS).
     *
     * <p>
     * The comparison is performed by canonicalizing each number using
     * {@link #canonizeNumber(JsonNumber)} and comparing the resulting strings.
     * </p>
     *
     * @param number1 the first number
     * @param number2 the second number
     * @return {@code true} if both numbers have the same canonical representation;
     *         {@code false} otherwise
     */
    static final boolean numberEquals(final BigDecimal number1, final BigDecimal number2) {
        return canonizeNumber(number1).equals(canonizeNumber(number2));
    }

    /**
     * Compares two JSON objects for canonical equality under RFC 8785 (JCS).
     *
     * <p>
     * Objects are equal if they contain the same set of member names (after
     * escaping per JCS) and corresponding member values are canonically equal as
     * determined by {@link #equals(JsonValue, JsonValue)}.
     * </p>
     *
     * @param object1 the first JSON object
     * @param object2 the second JSON object
     * @return {@code true} if the two objects are canonically equal; {@code false}
     *         otherwise
     */
    static final boolean objectEquals(final Object object1, final Object object2, final NodeAdapter adapter) {

        int size = adapter.size(object1);

        if (size != adapter.size(object2)) {
            return false;
        }

        if (size == 0) {
            return true;
        }

        final Iterator<Entry<?, ?>> entries1 = adapter.streamEntries(object1)
                .sorted(NodeModel.comparingEntry(e -> adapter.asString(e.getKey())))
                .iterator();

        final Iterator<Entry<?, ?>> entries2 = adapter.streamEntries(object2)
                .sorted(NodeModel.comparingEntry(e -> adapter.asString(e.getKey())))
                .iterator();

        while (entries1.hasNext() && entries2.hasNext()) {

            final Entry<?, ?> entry1 = entries1.next();
            final Entry<?, ?> entry2 = entries2.next();

            if (!Jcs.escape(adapter.asString(entry1.getKey())).equals(Jcs.escape(adapter.asString(entry2.getKey())))
                    || !equals(entry1.getValue(), entry2.getValue(), adapter)) {
                return false;
            }
        }

        return !entries1.hasNext() && !entries2.hasNext();
    }

    /**
     * Compares two JSON arrays for canonical equality under RFC 8785 (JCS).
     *
     * <p>
     * Arrays are equal if they have the same length and each element at position
     * {@code i} is canonically equal as determined by
     * {@link #equals(JsonValue, JsonValue)}.
     * </p>
     *
     * @param array1  the first JSON array
     * @param array2  the second JSON array
     * @param adapter
     * @return {@code true} if the two arrays are canonically equal; {@code false}
     *         otherwise
     */
    static final boolean arrayEquals(final Object array1, final Object array2, final NodeAdapter adapter) {

        final int size = adapter.size(array1);

        if (size != adapter.size(array2)) {
            return false;
        }

        if (size == 0) {
            return true;
        }

        final Iterator<?> it1 = adapter.items(array1).iterator();
        final Iterator<?> it2 = adapter.items(array2).iterator();

        while (it1.hasNext() && it2.hasNext()) {
            if (!equals(it1.next(), it2.next(), adapter)) {
                return false;
            }
        }
        return !it1.hasNext() && !it2.hasNext();
    }
}
