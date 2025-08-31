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
import java.io.Writer;

import jakarta.json.JsonArray;
import jakarta.json.JsonNumber;
import jakarta.json.JsonValue;

/**
 * Legacy entry point for JSON canonicalization.
 *
 * <p>
 * This class has been superseded by {@link Jcs} and remains only for backward
 * compatibility. All methods in this class delegate directly to their
 * equivalents in {@code Jcs}.
 * </p>
 *
 * @deprecated Use {@link Jcs} instead.
 */
@Deprecated
public final class JsonCanonicalizer {

    /**
     * Canonicalizes a JSON value according to RFC 8785.
     *
     * @param value the JSON value to canonicalize
     * @return the canonicalized JSON string
     * @deprecated Use {@link Jcs#canonize(JsonValue)}.
     */
    @Deprecated
    public static final String canonize(final JsonValue value) {
        return Jcs.canonize(value);
    }

    /**
     * Canonicalizes a JSON value and writes the result to the given writer.
     *
     * @param value  the JSON value to canonicalize
     * @param writer the writer to output the canonicalized JSON
     * @throws IOException if an I/O error occurs while writing
     * @deprecated Use {@link Jcs#canonize(JsonValue, Writer)}.
     */
    @Deprecated
    public static final void canonize(final JsonValue value, final Writer writer) throws IOException {
        Jcs.canonize(value, writer);
    }

    /**
     * Canonicalizes a JSON number and writes the result to the given writer.
     *
     * @param number the JSON number to canonicalize
     * @param writer the writer to output the canonicalized representation
     * @throws IOException if an I/O error occurs while writing
     * @deprecated Use {@link Jcs#canonizeNumber(JsonNumber)}.
     */
    @Deprecated
    public static final void canonizeNumber(final JsonNumber number, final Writer writer) throws IOException {
        writer.write(Jcs.canonizeNumber(number));
    }

    /**
     * Canonicalizes a JSON array and writes the result to the given writer.
     *
     * @param value  the JSON array to canonicalize
     * @param writer the writer to output the canonicalized JSON
     * @throws IOException if an I/O error occurs while writing
     * @deprecated Use {@link Jcs#canonizeArray(JsonArray, Writer)}.
     */
    @Deprecated
    public static final void canonizeArray(final JsonArray value, final Writer writer) throws IOException {
        Jcs.canonizeArray(value, writer);
    }

    /**
     * Escapes a JSON property name according to canonicalization rules.
     *
     * @param value the property name to escape
     * @return the escaped property name
     * @deprecated Use {@link Jcs#escape(String)}.
     */
    @Deprecated
    public static final String escape(String value) {
        return Jcs.escape(value);
    }
}
