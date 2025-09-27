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

public final class JcsGenerator extends NodeGenerator {

    protected final Writer writer;

    public JcsGenerator(Writer writer) {
        super(new ArrayDeque<>(), PropertyKeyPolicy.StringOnly);
        this.writer = writer;
        this.entryComparator = NodeModel.comparingEntry(e -> adapter.asString(e.getKey()));
    }

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
                    && (((Iterator<?>) stack.peek()).hasNext()))
                    || ((Context.END == nodeContext
                            && stack.peek() instanceof Iterator
                            && ((Iterator<?>) stack.peek()).hasNext()))) {
                writer.write(',');
            }
        }

        if (depth > 0) {
            throw new IllegalStateException();
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

    @Override
    protected void beginMap() throws IOException {
        writer.write('{');
    }

    @Override
    protected void beginCollection() throws IOException {
        writer.write('[');
    }

    @Override
    protected void end() throws IOException {
        if (NodeType.MAP == nodeType) {
            writer.write('}');
        } else if (NodeType.COLLECTION == nodeType) {
            writer.write(']');
        } else {
            throw new IllegalStateException();
        }
    }
}