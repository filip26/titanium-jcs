/**
 * Provides an implementation of the
 * <a href="https://www.rfc-editor.org/rfc/rfc8785"> RFC 8785 JSON
 * Canonicalization Scheme (JCS)</a> in Java.
 *
 * <p>
 * The JCS specification defines a deterministic way to serialize JSON so that
 * the same JSON data model always produces the same byte sequence. This ensures
 * stable and repeatable representations of JSON values, which is critical for
 * use cases such as digital signatures, hashing, and integrity verification.
 * </p>
 *
 * <p>
 * The {@link com.apicatalog.jcs.Jcs} class offers methods for:
 * </p>
 * <ul>
 * <li><strong>Canonicalization</strong> – converting JSON values into their
 * canonical string form using
 * {@link com.apicatalog.jcs.Jcs#canonize(Object, com.apicatalog.tree.io.NodeAdapter)} and related
 * methods.</li>
 * <li><strong>Equality comparison</strong> – determining whether two JSON
 * values are canonically equal using
 * {@link com.apicatalog.jcs.Jcs#equals(Object, Object, com.apicatalog.tree.io.NodeAdapter)}.</li>
 * </ul>
 *
 */
package com.apicatalog.jcs;
