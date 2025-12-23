#!/bin/bash
cd "$(dirname "$0")/load-generator" || exit 1
mvn spring-boot:run
