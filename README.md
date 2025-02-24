# Titanium JCS

An implementation of the [RFC 8785 JSON Canonicalization Scheme (JCS)](https://www.rfc-editor.org/rfc/rfc8785) specification in Java, utilizing [Jakarta JSON Processing](https://github.com/eclipse-ee4j/jsonp) as input.

Formerly part of [Titanium JSON-LD](https://github.com/filip26/titanium-json-ld).

[![Java 8 CI](https://github.com/filip26/titanium-jcs/actions/workflows/java8-build.yml/badge.svg)](https://github.com/filip26/titanium-jcs/actions/workflows/java8-build.yml)
[![Codacy Badge](https://app.codacy.com/project/badge/Grade/af8879b14a3f45bd8205c7720a24612f)](https://app.codacy.com/gh/filip26/titanium-jcs/dashboard?utm_source=gh&utm_medium=referral&utm_content=&utm_campaign=Badge_grade)
[![Codacy Badge](https://app.codacy.com/project/badge/Coverage/af8879b14a3f45bd8205c7720a24612f)](https://app.codacy.com/gh/filip26/titanium-jcs/dashboard?utm_source=gh&utm_medium=referral&utm_content=&utm_campaign=Badge_coverage)
[![Maven Central](https://img.shields.io/maven-central/v/com.apicatalog/titanium-jcs.svg?label=Maven%20Central)](https://search.maven.org/search?q=g:com.apicatalog%20AND%20a:titanium-jcs)
[![License](https://img.shields.io/badge/License-Apache%202.0-blue.svg)](https://opensource.org/licenses/Apache-2.0)

## Example

```javascript

// The canonical version is written to a provided `Writer`.
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

## Documentation

* [![javadoc](https://javadoc.io/badge2/com.apicatalog/titanium-jcs/javadoc.svg)](https://javadoc.io/doc/com.apicatalog/titanium-jcs)

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
