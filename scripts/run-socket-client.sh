#!/usr/bin/env bash
# Build first:  mvn clean package
# Optional DB overrides: JAVA_OPTS="-Ddb.user=root -Ddb.password=secret"
cd "$(dirname "$0")/.." || exit 1
exec java $JAVA_OPTS -cp network/target/shopsphere-network-1.0.0.jar com.shopsphere.network.client.ClientMain "$@"
