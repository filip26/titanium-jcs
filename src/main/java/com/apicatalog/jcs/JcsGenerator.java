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
import java.util.ArrayDeque;
import java.util.Iterator;

import com.apicatalog.tree.io.Features;
import com.apicatalog.tree.io.NodeType;
import com.apicatalog.tree.io.TreeAdapter;
import com.apicatalog.tree.io.TreeGenerator;
import com.apicatalog.tree.io.TreeIO;
import com.apicatalog.tree.io.TreeIOException;
import com.apicatalog.tree.io.traverse.Visitor;

/**
 * A non-recursive, streaming {@link TreeGenerator} implementation that writes a
 * canonical representation of a JSON document as defined by
 * <a href="https://tools.ietf.org/html/rfc8785">JSON Canonicalization Scheme
 * (JCS), RFC 8785</a>.
 */
public final class JcsGenerator extends Visitor implements TreeGenerator {

    protected final Writer writer;

    /**
     * Creates a new generator instance.
     *
     * @param writer The writer to which the canonical JSON is written.
     */
    public JcsGenerator(Writer writer) {
        super(new ArrayDeque<>());
        this.writer = writer;
    }

    /**
     * Generates a canonical representation of the given node structure.
     *
     * @param node    The root node of the structure to process.
     * @param adapter An adapter to navigate the provided node structure.
     * @throws TreeIOException       if an I/O error occurs.
     * @throws IllegalStateException if the document generation process ends in an
     *                               inconsistent state, indicating a malformed
     *                               input structure.
     */
    public void node(Object node, TreeAdapter adapter) throws TreeIOException {
        this.entryComparator = TreeIO.comparingStringKeys(adapter);
        root(node, adapter).traverse(this);
    }

    /**
     * Writes the beginning of a map (object).
     *
     * @throws IOException if an I/O error occurs.
     */
    @Override
    public void beginMap() throws TreeIOException {
        try {
            writer.write('{');
        } catch (IOException e) {
            throw new TreeIOException(e);
        }
    }

    /**
     * Writes the beginning of a collection (array).
     *
     * @throws TreeIOException if an I/O error occurs.
     */
    @Override
    public void beginList() throws TreeIOException {
        try {
            writer.write('[');
        } catch (IOException e) {
            throw new TreeIOException(e);
        }

    }

    @Override
    public void beginSet() throws TreeIOException {
        try {
            writer.write('[');
        } catch (IOException e) {
            throw new TreeIOException(e);
        }

    }

    /**
     * Writes the end of a map or collection.
     *
     * @throws TreeIOException       if an I/O error occurs.
     * @throws IllegalStateException if the generator is in an inconsistent state.
     */
    @Override
    public void end() throws TreeIOException {
        try {
            if (currentNodeType == NodeType.MAP) {
                writer.write('}');

            } else if (currentNodeType == NodeType.COLLECTION) {
                writer.write(']');

            } else {
                throw new IllegalStateException("Internal error. An unexpected node type [" + currentNodeType + "] was found when trying to end a structure.");
            }

            if (!stack.isEmpty()
                    && stack.peek() instanceof Iterator
                    && ((Iterator<?>) stack.peek()).hasNext()) {
                writer.write(',');
            }
        } catch (IOException e) {
            throw new TreeIOException(e);
        }

    }

    @Override
    public void nullValue() throws TreeIOException {
        try {
            writer.write("null");
            detectNext();
        } catch (IOException e) {
            throw new TreeIOException(e);
        }

    }

    @Override
    public void booleanValue(boolean node) throws TreeIOException {
        try {
            writer.write(node ? "true" : "false");
            detectNext();
        } catch (IOException e) {
            throw new TreeIOException(e);
        }

    }

    @Override
    public void stringValue(String node) throws TreeIOException {
        try {
            writer.write('"');
            writer.write(Jcs.escape(node));
            writer.write('"');
            if (currentNodeContext == Context.PROPERTY_KEY) {
                writer.write(':');
            } else {
                detectNext();
            }
        } catch (IOException e) {
            throw new TreeIOException(e);
        }
    }

    @Override
    public void numericValue(long node) throws TreeIOException {
        try {
            numericValue(BigDecimal.valueOf(node));
        } catch (NumberFormatException e) {
            throw new TreeIOException(e);
        }
    }

    @Override
    public void numericValue(BigInteger node) throws TreeIOException {
        try {
            numericValue(BigDecimal.valueOf(node.longValueExact()));
        } catch (NumberFormatException e) {
            throw new TreeIOException(e);
        }

    }

    @Override
    public void numericValue(double node) throws TreeIOException {
        try {
            numericValue(BigDecimal.valueOf(node));
        } catch (NumberFormatException e) {
            throw new TreeIOException(e);
        }

    }

    @Override
    public void numericValue(BigDecimal node) throws TreeIOException {
        try {
            writer.write(Jcs.canonizeNumber(node));
            detectNext();
        } catch (IOException e) {
            throw new TreeIOException(e);
        }

    }

    @Override
    public void binaryValue(byte[] node) throws TreeIOException {
        throw new UnsupportedOperationException();
    }

    protected void detectNext() throws IOException {
        if ((currentNodeContext == Context.COLLECTION_ELEMENT
                || currentNodeContext == Context.PROPERTY_VALUE)
                && ((Iterator<?>) stack.peek()).hasNext()) {
            writer.write(',');
        }
    }

    @Override
    public Features features() {
        return null;
    }
}