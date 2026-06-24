/**
 * RFC 8785 JSON Canonicalization Scheme (JCS) operations for converting object
 * structures (maps, collections, scalars) into deterministic byte sequences.
 *
 * <h3>Core Components</h3>
 * <ul>
 * <li>{@link com.apicatalog.jcs.Jcs} - Static methods for immediate
 * canonicalization and deep canonical comparison.</li>
 * <li>{@link com.apicatalog.jcs.JcsEmitter} - Streaming support for sequential
 * output generation.</li>
 * </ul>
 *
 * <h3>Examples</h3>
 * 
 * <pre>{@code
 * // Canonicalize an object to a string representation
 * String canonicalJson = Jcs.canonize(dataObject);
 *
 * // Evaluate two distinct structures for canonical equivalence
 * boolean matches = Jcs.equals(dataObject1, dataObject2);
 * }</pre>
 *
 * @see com.apicatalog.jcs.Jcs
 * @see com.apicatalog.jcs.JcsEmitter
 */
package com.apicatalog.jcs;