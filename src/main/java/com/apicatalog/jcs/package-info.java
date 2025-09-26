/**
 * An implementation of the <a href="https://www.rfc-editor.org/rfc/rfc8785">RFC
 * 8785 JSON Canonicalization Scheme (JCS)</a>.
 *
 * <p>
 * This package provides a complete API to serialize JSON into a deterministic
 * format, ensuring that any given JSON data structure always produces the exact
 * same byte sequence. This is essential for applications requiring stable JSON
 * representations, such as digital signatures, content addressing, and data
 * integrity checks.
 * </p>
 *
 * <p>
 * The main entry point is the {@link com.apicatalog.jcs.Jcs} class, which
 * provides static methods for:
 * </p>
 *
 * <ul>
 * <li><b>Canonicalization:</b> Converting a JSON object into its canonical
 * string form via the {@code canonize()} methods.</li>
 * <li><b>Equality:</b> Comparing two JSON objects for canonical equality using
 * the {@code equals()} method.</li>
 * </ul>
 *
 * @see com.apicatalog.jcs.Jcs
 */
package com.apicatalog.jcs;