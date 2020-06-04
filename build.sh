#!/bin/bash
OUTPUT="classes/production/gr4sp"

javac -d $OUTPUT -cp src/:experiments/../libraries/bsh-2.0b4.jar:experiments/../libraries/itext-1.2.jar:experiments/../libraries/j3dcore.jar:experiments/../libraries/j3dutils.jar:experiments/../libraries/jcommon-1.0.21.jar:experiments/../libraries/jfreechart-1.0.17.jar:experiments/../libraries/jmf.jar:experiments/../libraries/mason.19.jar:experiments/../libraries/portfolio.jar:experiments/../libraries/vecmath.jar:experiments/../libraries/postgresql-42.2.6.jar:experiments/../libraries/opencsv-4.6.jar:experiments/../libraries/yamlbeans-1.13.jar src/core/*/*.java

echo 'GR4SP Message: Ignore message about uses unchecked or unsafe operations and Xlint. This is due to jdk installing different API versions. Compiled code should be available in ./classes/ folder '
