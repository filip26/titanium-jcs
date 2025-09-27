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
import java.util.ArrayDeque;
import java.util.Iterator;

import com.apicatalog.tree.io.NodeAdapter;
import com.apicatalog.tree.io.NodeGenerator;
import com.apicatalog.tree.io.NodeModel;
import com.apicatalog.tree.io.NodeType;

/**
 * A non-recursive, streaming {@link NodeGenerator} implementation that writes a
 * canonical representation of a JSON document as defined by
 * <a href="https://tools.ietf.org/html/rfc8785">JSON Canonicalization Scheme
 * (JCS), RFC 8785</a>.
 */
public final class JcsGenerator extends NodeGenerator {

    protected final Writer writer;

    /**
     * Creates a new generator instance.
     *
     * @param writer The writer to which the canonical JSON is written.
     */
    public JcsGenerator(Writer writer) {
        super(new ArrayDeque<>(), PropertyKeyPolicy.StringOnly);
        this.writer = writer;
        this.entryComparator = NodeModel.comparingEntry(e -> adapter.asString(e.getKey()));
    }

    /**
     * Generates a canonical representation of the given node structure.
     *
     * @param node    The root node of the structure to process.
     * @param adapter An adapter to navigate the provided node structure.
     * @throws IOException           if an I/O error occurs.
     * @throws IllegalStateException if the document generation process ends in an
     *                               inconsistent state, indicating a malformed
     *                               input structure.
     */
    @Override
    public void node(Object node, NodeAdapter adapter) throws IOException {

        reset(node, adapter);

        while (step()) {
            node();

            if (Context.PROPERTY_KEY == nodeContext) {
                writer.write(':');

            } else if (((NodeType.COLLECTION != nodeType
                    && NodeType.MAP != nodeType
                    && (Context.COLLECTION_ELEMENT == nodeContext
                            || Context.PROPERTY_VALUE == nodeContext))
                    && ((Iterator<?>) stack.peek()).hasNext())
                    || ((Context.END == nodeContext
                            && !stack.isEmpty()
                            && stack.peek() instanceof Iterator
                            && ((Iterator<?>) stack.peek()).hasNext()))) {
                writer.write(',');
            }
        }

        if (depth > 0) {
            throw new IllegalStateException("The generated document is malformed. A map or a collection is not properly closed.");
        }
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

    /**
     * Writes a scalar value to the output writer in canonical form.
     *
     * @throws IOException              if an I/O error occurs.
     * @throws IllegalArgumentException if the node type is an unsupported scalar
     *                                  type.
     */
    @Override
    protected void scalar(Object node) throws IOException {

        if (node == null) {
            writer.write("null");
            return;
        }

        switch (nodeType) {
        case NUMBER:
            writer.write(Jcs.canonizeNumber(adapter.asDecimal(node)));
            break;
        case STRING:
            writer.write('"');
            writer.write(escape(adapter.stringValue(node)));
            writer.write('"');
            break;
        case FALSE:
            writer.write("false");
            break;
        case TRUE:
            writer.write("true");
            break;
        case NULL:
            writer.write("null");
            break;
        default:
            throw new IllegalArgumentException("Unsupported scalar node type: " + nodeType);
        }
    }

    /**
     * Writes the beginning of a map (object).
     *
     * @throws IOException if an I/O error occurs.
     */
    @Override
    protected void beginMap() throws IOException {
        writer.write('{');
    }

    /**
     * Writes the beginning of a collection (array).
     *
     * @throws IOException if an I/O error occurs.
     */
    @Override
    protected void beginCollection() throws IOException {
        writer.write('[');
    }

    /**
     * Writes the end of a map or collection.
     *
     * @throws IOException           if an I/O error occurs.
     * @throws IllegalStateException if the generator is in an inconsistent state.
     */
    @Override
    protected void end() throws IOException {
        if (NodeType.MAP == nodeType) {
            writer.write('}');
        } else if (NodeType.COLLECTION == nodeType) {
            writer.write(']');
        } else {
            throw new IllegalStateException("Internal error. An unexpected node type [" + nodeType + "] was found when trying to end a structure.");
        }
    }
}