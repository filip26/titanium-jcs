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
import java.util.Locale;
import java.util.Set;

import jakarta.json.JsonArray;
import jakarta.json.JsonNumber;
import jakarta.json.JsonObject;
import jakarta.json.JsonValue;
import jakarta.json.JsonValue.ValueType;

/**
 * An implementation of the <a href="https://www.rfc-editor.org/rfc/rfc8785">
 * RFC 8785 JSON Canonicalization Scheme (JCS)</a> specification in Java.
 * <p>
 * This class utilizes <a href="https://github.com/eclipse-ee4j/jsonp"> Jakarta
 * JSON Processing</a> as input for canonicalization. The canonicalized JSON
 * output is written to a provided {@code Writer}.
 * </p>
 * 
 * <p>
 * Usage:
 * </p>
 * 
 * <pre>
 * {@link JsonCanonicalizer#canonize(JsonValue, Writer)}
 * </pre>
 */
public final class JsonCanonicalizer {

    private static final DecimalFormat eFormatBigDecimal = new DecimalFormat("0E00", new DecimalFormatSymbols(Locale.ENGLISH));

    private static final DecimalFormat eFormat = new DecimalFormat("0.#######", new DecimalFormatSymbols(Locale.ENGLISH));

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
            canonizeNumber((JsonNumber) value, writer);
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
     * standardized manner, ensuring a consistent numeric representation. The
     * canonicalized number is written to the provided {@link Writer}.
     * </p>
     *
     * @param number the JSON number to be canonicalized
     * @param writer the writer to which the canonicalized JSON output is written
     * @throws IOException if an I/O error occurs while writing to the writer
     */
    public static final void canonizeNumber(final JsonNumber number, final Writer writer) throws IOException {

        final String numberString;

        if (number.bigDecimalValue().compareTo(BigDecimal.ZERO) == 0) {
            numberString = "0";

        } else if (number.bigDecimalValue().compareTo(BigDecimal.ONE.movePointRight(21)) >= 0) {
            numberString = eFormatBigDecimal.format(number.bigDecimalValue()).replace("E", "e+");

        } else if (number.bigDecimalValue().compareTo(BigDecimal.ONE.movePointLeft(21)) <= 0) {
            numberString = eFormatBigDecimal.format(number.bigDecimalValue()).toLowerCase();

        } else {
            numberString = eFormat.format(number.bigDecimalValue());
        }

        writer.write(numberString);
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
    private static final void canonizeObject(final JsonObject value, final Writer writer) throws IOException {
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
}
