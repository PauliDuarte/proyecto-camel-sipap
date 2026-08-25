#!/bin/sh

# Licensed to the Apache Software Foundation (ASF) under one
# or more contributor license agreements. See the NOTICE file
# distributed with this work for additional information.
# The ASF licenses this file to you under the Apache License, Version 2.0.

set -e

MVNW_PROJECT_DIRECTORY=$(CDPATH= cd -- "$(dirname -- "$0")" && pwd)
MVNW_MAVEN_VERSION=3.9.11
MVNW_MAVEN_HOME="$MVNW_PROJECT_DIRECTORY/.mvn/wrapper/apache-maven-$MVNW_MAVEN_VERSION"
MVNW_ARCHIVE="$MVNW_PROJECT_DIRECTORY/.mvn/wrapper/apache-maven-$MVNW_MAVEN_VERSION-bin.zip"
MVNW_URL=https://repo.maven.apache.org/maven2/org/apache/maven/apache-maven/$MVNW_MAVEN_VERSION/apache-maven-$MVNW_MAVEN_VERSION-bin.zip

if [ ! -x "$MVNW_MAVEN_HOME/bin/mvn" ]; then
    if [ ! -f "$MVNW_ARCHIVE" ]; then
        if command -v curl >/dev/null 2>&1; then
            curl --fail --location --output "$MVNW_ARCHIVE" "$MVNW_URL"
        elif command -v wget >/dev/null 2>&1; then
            wget --output-document="$MVNW_ARCHIVE" "$MVNW_URL"
        else
            echo "Se necesita curl o wget para descargar Maven." >&2
            exit 1
        fi
    fi
    unzip -q -o "$MVNW_ARCHIVE" -d "$MVNW_PROJECT_DIRECTORY/.mvn/wrapper"
fi

exec "$MVNW_MAVEN_HOME/bin/mvn" "$@"
