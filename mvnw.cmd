@REM Licensed to the Apache Software Foundation (ASF) under one
@REM or more contributor license agreements. See the NOTICE file
@REM distributed with this work for additional information.
@REM The ASF licenses this file to you under the Apache License, Version 2.0.
@echo off
setlocal

set "MVNW_PROJECT_DIRECTORY=%~dp0"
set "MVNW_MAVEN_VERSION=3.9.11"
set "MVNW_MAVEN_HOME=%MVNW_PROJECT_DIRECTORY%.mvn\wrapper\apache-maven-%MVNW_MAVEN_VERSION%"
set "MVNW_ARCHIVE=%MVNW_PROJECT_DIRECTORY%.mvn\wrapper\apache-maven-%MVNW_MAVEN_VERSION%-bin.zip"
set "MVNW_URL=https://repo.maven.apache.org/maven2/org/apache/maven/apache-maven/%MVNW_MAVEN_VERSION%/apache-maven-%MVNW_MAVEN_VERSION%-bin.zip"

if not exist "%MVNW_MAVEN_HOME%\bin\mvn.cmd" (
    if not exist "%MVNW_ARCHIVE%" powershell -NoProfile -ExecutionPolicy Bypass -Command "Invoke-WebRequest -UseBasicParsing -Uri '%MVNW_URL%' -OutFile '%MVNW_ARCHIVE%'"
    powershell -NoProfile -ExecutionPolicy Bypass -Command "Expand-Archive -Path '%MVNW_ARCHIVE%' -DestinationPath '%MVNW_PROJECT_DIRECTORY%.mvn\wrapper' -Force"
)

call "%MVNW_MAVEN_HOME%\bin\mvn.cmd" %*
endlocal
