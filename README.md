bloomfilter implementation
==========================

[![Maven Central](https://maven-badges.herokuapp.com/maven-central/me.masahito/bloomfilter/badge.png)](https://maven-badges.herokuapp.com/maven-central/me.masahito/bloomfilter/badge.png)
[![Build Status](https://travis-ci.org/masahitojp/java-bloomfilter.svg?branch=master)](https://travis-ci.org/masahitojp/java-bloomfilter)
[![Coverage Status](https://coveralls.io/repos/masahitojp/java-bloomfilter/badge.png?branch=master)](https://coveralls.io/r/masahitojp/java-bloomfilter?branch=master)
[![License: Apache Licence, Version 2.0](https://img.shields.io/badge/license-Apache2-green.svg)](LICENSE)

Counting BloomFilter for Java SE 8.

## Usage

    // capacity: 1000, error_rate: 0.001(= 0.1%)
    final BloomFilter<String> bf1 = new BloomFilter<>(1000, 0.01);
    bf.add("test");
    bf.contains("test");   // => true
    bf.contains("blah");   // => false

    bf.delete("test");
    bf.contains("test");   // => false

## Use with Maven

you can get this artifact from Maven Central Repository :)

    <dependencies>
      <dependency>
          <groupId>me.masahito</groupId>
          <artifactId>bloomfilter</artifactId>
          <version>0.1.0</version>
      </dependency>
    </dependencies>

## Use with Gradle

    repositories {
        mavenCentral()
    }
    dependencies {
        compile 'me.masahito:bloomfilter:0.1.0'
    }

## Prerequisites

* JDK8+

# License

Apache License, Version 2.0

