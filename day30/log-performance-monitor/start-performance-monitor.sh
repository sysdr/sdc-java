#!/bin/bash
cd "$(dirname "$0")/performance-monitor" || exit 1
mvn spring-boot:run
