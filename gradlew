#!/bin/sh
# Gradle wrapper bootstrap (cross-platform).
# When you open this project in Android Studio, AS will provision
# the gradle-wrapper.jar automatically. To bootstrap from CLI:
#   gradle wrapper --gradle-version 8.11.1
APP_HOME=$(cd "$(dirname "$0")" && pwd)
exec "$APP_HOME"/gradle/wrapper/gradle-wrapper.jar "$@"
