#!/bin/bash
CLASSES="build/classes/java/main"

java -classpath "$CLASSES:build/runtime-libs/*" core.Gr4spSim
