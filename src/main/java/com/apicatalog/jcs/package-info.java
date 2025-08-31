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
 * {@link com.apicatalog.jcs.Jcs#canonize(javax.json.JsonValue)} and related
 * methods.</li>
 * <li><strong>Equality comparison</strong> – determining whether two JSON
 * values are canonically equal using
 * {@link com.apicatalog.jcs.Jcs#equals(javax.json.JsonValue, javax.json.JsonValue)}.</li>
 * </ul>
 *
 * <p>
 * This package relies on
 * <a href="https://github.com/eclipse-ee4j/jsonp">Jakarta JSON Processing
 * (JSON-P)</a> for parsing and representing JSON data.
 * </p>
 */
package com.apicatalog.jcs;
