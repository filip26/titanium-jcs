# Titanium JCS

JSON Canonicalization Scheme (JCS) defines a deterministic way to serialize JSON so that the same JSON data model always produces the same byte sequence.  

This is essential for use cases such as digital signatures, hashing, and data integrity verification, where even minor differences in whitespace, member ordering, or number formatting would otherwise break validation.  

By normalizing JSON into a canonical form, JCS ensures interoperability and guarantees stable, repeatable representations of data.

Titanium JCS is a Java implementation of the [RFC 8785 JSON Canonicalization Scheme (JCS)](https://www.rfc-editor.org/rfc/rfc8785). It is built on top of [Jakarta JSON Processing (JSON-P)](https://github.com/eclipse-ee4j/jsonp) for standards-compliant JSON parsing and serialization.  


[![Java 8 CI](https://github.com/filip26/titanium-jcs/actions/workflows/java8-build.yml/badge.svg)](https://github.com/filip26/titanium-jcs/actions/workflows/java8-build.yml)
[![Codacy Badge](https://app.codacy.com/project/badge/Grade/af8879b14a3f45bd8205c7720a24612f)](https://app.codacy.com/gh/filip26/titanium-jcs/dashboard?utm_source=gh&utm_medium=referral&utm_content=&utm_campaign=Badge_grade)
[![Codacy Badge](https://app.codacy.com/project/badge/Coverage/af8879b14a3f45bd8205c7720a24612f)](https://app.codacy.com/gh/filip26/titanium-jcs/dashboard?utm_source=gh&utm_medium=referral&utm_content=&utm_campaign=Badge_coverage)
[![javadoc](https://javadoc.io/badge2/com.apicatalog/titanium-jcs/javadoc.svg)](https://javadoc.io/doc/com.apicatalog/titanium-jcs)
[![Maven Central](https://img.shields.io/maven-central/v/com.apicatalog/titanium-jcs.svg?label=Maven%20Central)](https://search.maven.org/search?q=g:com.apicatalog%20AND%20a:titanium-jcs)
[![License](https://img.shields.io/badge/License-Apache%202.0-blue.svg)](https://opensource.org/licenses/Apache-2.0)

## Example

```javascript

// The canonical version is written to a provided Writer.
JsonCanonicalizer.canonize(JsonValue, Writer);
```

## Installation

### Maven

```xml
<dependency>
    <groupId>com.apicatalog</groupId>
    <artifactId>titanium-jcs</artifactId>
    <version>1.0.0</version>
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

### Gradle

```gradle
implementation("com.apicatalog:titanium-jcs:1.0.0")
implementation("org.glassfish:jakarta.json:2.0.1")
```

## Contributing

All PR's welcome!


### Building

Fork and clone the project repository.

```bash
> cd titanium-jcs
> mvn package
```


## Resources

- [RFC 8785 JSON Canonicalization Scheme (JCS)](https://www.rfc-editor.org/rfc/rfc8785)
- [JSON Canonicalization Scheme (JCS)](https://github.com/cyberphone/json-canonicalization)

## Commercial Support

Commercial support and consulting are available.  
For inquiries, please contact: filip26@gmail.com
