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
import com.apicatalog.tree.io.TreeIOException;

/**
 * A non-recursive, streaming {@link TreeEmitter} implementation that writes a
 * canonical representation of a JSON document as defined by
 * <a href="https://tools.ietf.org/html/rfc8785">JSON Canonicalization Scheme
 * (JCS), RFC 8785</a>.
 */
public final class JcsEmitter implements TreeEmitter {

    private final Writer writer;

    /**
     * Creates a new generator instance.
     *
     * @param writer The writer to which the canonical JSON is written.
     */
    public JcsEmitter(Writer writer) {
        super();
        this.writer = writer;
    }

    /**
     * Writes the beginning of a map (object).
     *
     * @throws TreeIOException if an I/O error occurs.
     */
    @Override
    public void beginMap(NodeContext context) throws TreeIOException {
        try {
            writer.write('{');
        } catch (IOException e) {
            throw new TreeIOException(e);
        }
    }

    @Override
    public void endMap(NodeContext context) throws TreeIOException {
        try {
            writer.write('}');
            next(context);
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
    public void beginSequence(NodeContext context) throws TreeIOException {
        try {
            writer.write('[');
        } catch (IOException e) {
            throw new TreeIOException(e);
        }

    }

    @Override
    public void endSequence(NodeContext context) throws TreeIOException {
        try {
            writer.write(']');
            next(context);
        } catch (IOException e) {
            throw new TreeIOException(e);
        }

    }

    @Override
    public void nullValue(NodeContext context) throws TreeIOException {
        try {
            writer.write("null");
            next(context);
        } catch (IOException e) {
            throw new TreeIOException(e);
        }

    }

    @Override
    public void booleanValue(NodeContext context, boolean node) throws TreeIOException {
        try {
            writer.write(node ? "true" : "false");
            next(context);
        } catch (IOException e) {
            throw new TreeIOException(e);
        }

    }

    @Override
    public void stringValue(NodeContext context, String node) throws TreeIOException {
        try {
            writer.write('"');
            writer.write(Jcs.escape(node));
            writer.write('"');
            if (context == NodeContext.ENTRY_KEY) {
                writer.write(':');
            } else if (context == NodeContext.ELEMENT || context == NodeContext.ENTRY_VALUE) {
                writer.write(',');
            }
        } catch (IOException e) {
            throw new TreeIOException(e);
        }
    }

    @Override
    public void numericValue(NodeContext context, long node) throws TreeIOException {
        try {
            numericValue(context, BigDecimal.valueOf(node));
        } catch (NumberFormatException e) {
            throw new TreeIOException(e);
        }
    }

    @Override
    public void numericValue(NodeContext context, BigInteger node) throws TreeIOException {
        try {
            numericValue(context, BigDecimal.valueOf(node.longValueExact()));
        } catch (NumberFormatException e) {
            throw new TreeIOException(e);
        }

    }

    @Override
    public void numericValue(NodeContext context, double node) throws TreeIOException {
        try {
            numericValue(context, BigDecimal.valueOf(node));
        } catch (NumberFormatException e) {
            throw new TreeIOException(e);
        }

    }

    @Override
    public void numericValue(NodeContext context, BigDecimal node) throws TreeIOException {
        try {
            writer.write(Jcs.canonizeNumber(node));
            next(context);
        } catch (IOException e) {
            throw new TreeIOException(e);
        }

    }

    @Override
    public void binaryValue(NodeContext context, byte[] node) throws TreeIOException {
        throw new UnsupportedOperationException();
    }
    
    private void next(NodeContext context) throws IOException {
        if (context == NodeContext.ELEMENT || context == NodeContext.ENTRY_VALUE) {
            writer.write(',');
        }        
    }
}