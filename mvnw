#!/bin/sh
# Maven wrapper script for Unix/macOS
# This file is auto-generated - do not edit manually

# Resolve the real location of this script
MVNW_REPOURL="https://repo.maven.apache.org/maven2"
MVNW_USERNAME=""
MVNW_PASSWORD=""

if [ -z "$JAVA_HOME" ]; then
  JAVACMD="java"
else
  JAVACMD="$JAVA_HOME/bin/java"
fi

if [ ! -x "$MVNW_REPOURL" ]; then
  # Download and invoke Maven wrapper
  BASE_DIR=$(cd "$(dirname "$0")" && pwd)
  WRAPPER_JAR="$BASE_DIR/.mvn/wrapper/maven-wrapper.jar"
  WRAPPER_PROPERTIES="$BASE_DIR/.mvn/wrapper/maven-wrapper.properties"

  if [ ! -f "$WRAPPER_JAR" ]; then
    # Try to download
    if command -v curl > /dev/null; then
      curl -fsSL "https://repo.maven.apache.org/maven2/org/apache/maven/wrapper/maven-wrapper/3.2.0/maven-wrapper-3.2.0.jar" -o "$WRAPPER_JAR"
    elif command -v wget > /dev/null; then
      wget -q "https://repo.maven.apache.org/maven2/org/apache/maven/wrapper/maven-wrapper/3.2.0/maven-wrapper-3.2.0.jar" -O "$WRAPPER_JAR"
    fi
  fi

  exec "$JAVACMD" -jar "$WRAPPER_JAR" "$@"
fi
