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
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;

import jakarta.json.JsonArray;
import jakarta.json.JsonNumber;
import jakarta.json.JsonObject;
import jakarta.json.JsonValue;
import jakarta.json.JsonValue.ValueType;

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
 * This implementation uses
 * <a href="https://github.com/eclipse-ee4j/jsonp">Jakarta JSON Processing
 * (JSON-P)</a> as input and can output canonicalized JSON to a
 * {@link java.io.Writer} or return it as a {@link java.lang.String}.
 * </p>
 *
 * <p>
 * <strong>Usage examples:</strong>
 * </p>
 *
 * <pre>{@code
 * // Canonicalize a JSON value and write to a writer
 * Jcs.canonize(jsonValue, writer);
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
     * @param value the JSON value to be canonicalized
     * @return a string containing the canonicalized JSON representation of the
     *         input value
     */
    public static final String canonize(final JsonValue value) {

        final StringWriter writer = new StringWriter();

        try {
            canonize(value, writer);
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
     * @param value  the JSON value to be canonicalized
     * @param writer the writer to which the canonicalized JSON output is written
     * @throws IOException if an I/O error occurs while writing to the writer
     */
    public static final void canonize(final JsonValue value, final Writer writer) throws IOException {

        final ValueType valueType = value != null ? value.getValueType() : null;

        if (valueType == null || ValueType.NULL == valueType) {
            writer.write("null");
            return;
        }

        switch (valueType) {
        case ARRAY:
            canonizeArray(value.asJsonArray(), writer);
            break;

        case OBJECT:
            canonizeObject(value.asJsonObject(), writer);
            break;

        case NUMBER:
            writer.write(canonizeNumber((JsonNumber) value));
            break;

        case FALSE:
        case TRUE:
        case STRING:
        case NULL:
            writer.write(value.toString());
            break;
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
    public static final String canonizeNumber(final JsonNumber number) {
        if (number.bigDecimalValue().compareTo(BigDecimal.ZERO) == 0) {
            return "0";
        }

        if (number.bigDecimalValue().compareTo(BigDecimal.ONE.movePointRight(21)) >= 0) {
            return eFormatBigDecimal.format(number.bigDecimalValue()).replace("E", "e+");
        }

        if (number.bigDecimalValue().compareTo(BigDecimal.ONE.movePointLeft(21)) <= 0) {
            return eFormatBigDecimal.format(number.bigDecimalValue()).toLowerCase();
        }

        return eFormat.format(number.bigDecimalValue());
    }

    /**
     * Canonicalizes a JSON array according to the RFC 8785 JSON Canonicalization
     * Scheme (JCS).
     * <p>
     * This method serializes the given {@link JsonArray} in a deterministic and
     * standardized manner, ensuring a consistent output. The canonicalized JSON
     * array is written to the provided {@link Writer}.
     * </p>
     *
     * @param value  the JSON array to be canonicalized
     * @param writer the writer to which the canonicalized JSON output is written
     * @throws IOException if an I/O error occurs while writing to the writer
     */
    public static final void canonizeArray(final JsonArray value, final Writer writer) throws IOException {
        boolean next = false;

        writer.write("[");

        for (final JsonValue item : value.asJsonArray()) {

            if (next) {
                writer.write(",");
            }

            canonize(item, writer);

            next = true;
        }

        writer.write("]");
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
    public static final void canonizeObject(final JsonObject value, final Writer writer) throws IOException {
        boolean next = false;

        writer.write("{");

        final Set<String> properties = value.keySet();

        if (properties != null && !properties.isEmpty()) {
            final ArrayList<String> sortedProperties = new ArrayList<>(properties);

            Collections.sort(sortedProperties);

            for (final String propertyName : sortedProperties) {

                if (next) {
                    writer.write(",");
                }

                writer.write("\"");
                writer.write(escape(propertyName));
                writer.write("\":");

                JsonValue propertyValue = value.get(propertyName);

                canonize(propertyValue, writer);

                next = true;
            }
        }

        writer.write("}");
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
    public static final String escape(String value) {

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
     * @param value1 the first JSON value (may be {@code null})
     * @param value2 the second JSON value (may be {@code null})
     * @return {@code true} if the two values are canonically equal; {@code false}
     *         otherwise
     */
    public static final boolean equals(final JsonValue value1, final JsonValue value2) {

        if (value1 == null) {
            return value2 == null || value2.getValueType() == ValueType.NULL;

        } else if (value2 == null) {
            return value1.getValueType() == ValueType.NULL;
        }

        if (value1.getValueType() != value2.getValueType()) {
            return false;
        }

        switch (value1.getValueType()) {
        case NULL:
        case TRUE:
        case FALSE:
            return true;

        case STRING:
            return value1.equals(value2);

        case NUMBER:
            return numberEquals((JsonNumber) value1, (JsonNumber) value2);

        case ARRAY:
            return arrayEquals(value1.asJsonArray(), value2.asJsonArray());

        case OBJECT:
            return objectEquals(value1.asJsonObject(), value2.asJsonObject());

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
     * @param number1 the first JSON number
     * @param number2 the second JSON number
     * @return {@code true} if both numbers have the same canonical representation;
     *         {@code false} otherwise
     */
    public static final boolean numberEquals(final JsonNumber number1, final JsonNumber number2) {
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
    public static final boolean objectEquals(final JsonObject object1, final JsonObject object2) {

        if (object1.size() != object2.size()) {
            return false;
        }

        if (object1.isEmpty()) {
            return true;
        }

        final List<String> keys1 = object1.keySet().stream()
                .sorted()
                .collect(Collectors.toList());

        final List<String> keys2 = object2.keySet().stream()
                .sorted()
                .collect(Collectors.toList());

        for (int index = 0; index < keys1.size(); index++) {

            final String k1 = keys1.get(index);
            final String k2 = keys2.get(index);

            if (!Jcs.escape(k1).equals(Jcs.escape(k2))
                    || !equals(object1.get(k1), object2.get(k2))) {
                return false;
            }
        }
        return true;
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
     * @param array1 the first JSON array
     * @param array2 the second JSON array
     * @return {@code true} if the two arrays are canonically equal; {@code false}
     *         otherwise
     */
    public static final boolean arrayEquals(final JsonArray array1, final JsonArray array2) {

        if (array1.size() != array2.size()) {
            return false;
        }

        if (array1.isEmpty()) {
            return true;
        }

        for (int index = 0; index < array1.size(); index++) {
            if (!equals(array1.get(index), array2.get(index))) {
                return false;
            }
        }
        return true;
    }
}
