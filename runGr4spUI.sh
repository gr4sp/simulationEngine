#!/bin/bash
CLASSES="classes/production/gr4sp"

java -classpath $CLASSES:libraries/bsh-2.0b4.jar:libraries/itext-1.2.jar:libraries/j3dcore.jar:libraries/j3dutils.jar:libraries/jcommon-1.0.21.jar:libraries/jfreechart-1.0.17.jar:libraries/jmf.jar:libraries/mason.19.jar:libraries/portfolio.jar:libraries/vecmath.jar:libraries/postgresql-42.2.6.jar:libraries/opencsv-4.6.jar:libraries/yamlbeans-1.13.jar core.Gr4spSimUI
