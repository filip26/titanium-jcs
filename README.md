# Titanium JCS

The JSON Canonicalization Scheme (JCS) defines a deterministic way to serialize JSON so that the same JSON data model always produces the same byte sequence.  

This is critical for use cases such as digital signatures, hashing, and data integrity verification, where even small differences in whitespace, member ordering, or number formatting would otherwise break validation.  

By normalizing JSON into a canonical form, JCS ensures interoperability across systems and guarantees stable, repeatable representations of data.  

Titanium JCS is a Java implementation of the [RFC 8785 JSON Canonicalization Scheme (JCS)](https://www.rfc-editor.org/rfc/rfc8785).


[![Java 8 CI](https://github.com/filip26/titanium-jcs/actions/workflows/java8-build.yml/badge.svg)](https://github.com/filip26/titanium-jcs/actions/workflows/java8-build.yml)
[![Codacy Badge](https://app.codacy.com/project/badge/Grade/af8879b14a3f45bd8205c7720a24612f)](https://app.codacy.com/gh/filip26/titanium-jcs/dashboard?utm_source=gh&utm_medium=referral&utm_content=&utm_campaign=Badge_grade)
[![Codacy Badge](https://app.codacy.com/project/badge/Coverage/af8879b14a3f45bd8205c7720a24612f)](https://app.codacy.com/gh/filip26/titanium-jcs/dashboard?utm_source=gh&utm_medium=referral&utm_content=&utm_campaign=Badge_coverage)
[![javadoc](https://javadoc.io/badge2/com.apicatalog/titanium-jcs/javadoc.svg)](https://javadoc.io/doc/com.apicatalog/titanium-jcs)
[![Maven Central](https://img.shields.io/maven-central/v/com.apicatalog/titanium-jcs.svg?label=Maven%20Central)](https://search.maven.org/search?q=g:com.apicatalog%20AND%20a:titanium-jcs)
[![License](https://img.shields.io/badge/License-Apache%202.0-blue.svg)](https://opensource.org/licenses/Apache-2.0)

## ✨ Features

- **Canonical JSON Writer**  
  Deterministically serializes JSON values into a stable, repeatable form 
  following [RFC 8785](https://www.rfc-editor.org/rfc/rfc8785).  

- **Canonical JSON Equality Comparator**  
  Compares JSON values for equality under JCS rules, ensuring numbers, objects, 
  and arrays are compared in their canonical form rather than raw text.

## Example

```javascript
// Canonicalize a JSON value and return the canonical string
var canonicalJson = Jcs.canonize(json);

// Canonicalize a JSON value and write canonical JSON to a writer
Jcs.canonize(json, writer);

// Compare two JSON values for canonical equality
if (Jcs.equals(json1, json2)) {
  // values are equal under RFC 8785 rules
}
```

## Installation

### Maven

```xml
<dependency>
    <groupId>com.apicatalog</groupId>
    <artifactId>titanium-jcs</artifactId>
    <version>${titanium-jcs.version}</version>
</dependency>
```

Ensure that the JSON-P provider is added to the classpath if it is not already present.

```xml

<dependency>
    <groupId>org.glassfish</groupId>
    <artifactId>jakarta.json</artifactId>
    <version>2.0.1</version>
</dependency>
```

## 🛠️ LD-CLI
[LD-CLI](https://github.com/filip26/ld-cli) is a command-line utility for
working with JSON, JSON-LD, CBOR-LD, multiformats, and related specifications.

It provides encoding, decoding, detection, analysis, and format conversion
features, making it useful for inspecting identifiers, testing content
addressing, and integrating multiformats into development workflows.

### Example

Canonicalize JSON and write canonical JSON to stdout

```bash
cat test.json | ld-cli jcs
```

## 🤝 Contributing

Contributions are welcome! Please submit a pull request.


### Building

Fork and clone the repository, then build with Maven:

```bash
> cd titanium-jcs
> mvn package
```


## Resources

- [RFC 8785 JSON Canonicalization Scheme (JCS)](https://www.rfc-editor.org/rfc/rfc8785)
- [JSON Canonicalization Scheme (JCS)](https://github.com/cyberphone/json-canonicalization)
- [Titanium RDF Canon](https://github.com/filip26/titanium-rdf-canon)
- [LD-CLI](https://github.com/filip26/ld-cli)

## 💼 Commercial Support

Commercial support and consulting are available.  
For inquiries, please contact: filip26@gmail.com
