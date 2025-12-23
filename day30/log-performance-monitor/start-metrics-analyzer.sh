#!/bin/bash
cd "$(dirname "$0")/metrics-analyzer" || exit 1
mvn spring-boot:run
