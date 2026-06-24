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
import java.math.BigDecimal;
import java.math.BigInteger;

import com.apicatalog.tree.io.Tree.NodeContext;
import com.apicatalog.tree.io.TreeEmitter;

/**
 * A non-recursive, streaming {@link TreeEmitter} implementation that writes a
 * canonical representation of a structure as defined by
 * <a href="https://tools.ietf.org/html/rfc8785">JSON Canonicalization Scheme
 * (JCS), RFC 8785</a>.
 */
public final class JcsEmitter implements TreeEmitter {

    private final Writer writer;

    /**
     * Creates a new instance.
     *
     * @param writer The writer to which the canonical output is written.
     */
    public JcsEmitter(Writer writer) {
        super();
        this.writer = writer;
    }

    /**
     * Writes the beginning of a map structure.
     *
     * @param context the current node context
     * @throws IOException if an I/O error occurs.
     */
    @Override
    public void beginMap(NodeContext context) throws IOException {
        writer.write('{');
    }

    /**
     * Writes the end of a map structure.
     *
     * @param context the current node context
     * @throws IOException if an I/O error occurs.
     */
    @Override
    public void endMap(NodeContext context) throws IOException {
        writer.write('}');
        next(context);
    }

    /**
     * Writes the beginning of a collection structure.
     *
     * @param context the current node context
     * @throws IOException if an I/O error occurs.
     */
    @Override
    public void beginSequence(NodeContext context) throws IOException {
        writer.write('[');
    }

    /**
     * Writes the end of a collection structure.
     *
     * @param context the current node context
     * @throws IOException if an I/O error occurs.
     */
    @Override
    public void endSequence(NodeContext context) throws IOException {
        writer.write(']');
        next(context);
    }

    /**
     * Writes a null value literal.
     *
     * @param context the current node context
     * @throws IOException if an I/O error occurs.
     */
    @Override
    public void nullValue(NodeContext context) throws IOException {
        writer.write("null");
        next(context);
    }

    /**
     * Writes a boolean value literal.
     *
     * @param context the current node context
     * @param node    the boolean value to write
     * @throws IOException if an I/O error occurs.
     */
    @Override
    public void booleanValue(NodeContext context, boolean node) throws IOException {
        writer.write(node ? "true" : "false");
        next(context);
    }

    /**
     * Writes a string value literal, escaping it according to JCS rules.
     *
     * @param context the current node context
     * @param node    the string value to write
     * @throws IOException if an I/O error occurs.
     */
    @Override
    public void stringValue(NodeContext context, String node) throws IOException {
        writer.write('"');
        writer.write(Jcs.escape(node));
        writer.write('"');
        if (context == NodeContext.ENTRY_KEY) {
            writer.write(':');
        } else if (context == NodeContext.ELEMENT || context == NodeContext.ENTRY_VALUE) {
            writer.write(',');
        }
    }

    /**
     * Writes a numeric value from a primitive long.
     *
     * @param context the current node context
     * @param node    the long value to write
     * @throws IOException if an error occurs during canonicalization or I/O
     *                     processing
     */
    @Override
    public void numericValue(NodeContext context, long node) throws IOException {
        writer.write(Jcs.canonizeNumber(node));
        next(context);
    }

    /**
     * Writes a numeric value from a BigInteger.
     *
     * @param context the current node context
     * @param node    the BigInteger value to write
     * @throws IOException if an error occurs during canonicalization or I/O
     *                     processing
     */
    @Override
    public void numericValue(NodeContext context, BigInteger node) throws IOException {
        writer.write(Jcs.canonizeNumber(node));
        next(context);
    }

    /**
     * Writes a numeric value from a primitive double.
     *
     * @param context the current node context
     * @param node    the double value to write
     * @throws IOException if an error occurs during canonicalization or I/O
     *                     processing
     */
    @Override
    public void numericValue(NodeContext context, double node) throws IOException {
        writer.write(Jcs.canonizeNumber(node));
        next(context);
    }

    /**
     * Writes a numeric value from a BigDecimal, formatting it into its canonical
     * string representation.
     *
     * @param context the current node context
     * @param node    the BigDecimal value to write
     * @throws IOException if an I/O error occurs.
     */
    @Override
    public void numericValue(NodeContext context, BigDecimal node) throws IOException {
        writer.write(Jcs.canonizeNumber(node));
        next(context);
    }

    /**
     * Throws an exception as binary values are not supported by JCS.
     *
     * @param context the current node context
     * @param node    the binary data array
     * @throws always throws UnsupportedOperationException
     */
    @Override
    public void binaryValue(NodeContext context, byte[] node) {
        throw new UnsupportedOperationException();
    }

    /**
     * Appends structural delimiters if dictated by the structural position context.
     *
     * @param context the current node context
     * @throws IOException if an I/O error occurs.
     */
    private void next(NodeContext context) throws IOException {
        if (context == NodeContext.ELEMENT || context == NodeContext.ENTRY_VALUE) {
            writer.write(',');
        }
    }
}