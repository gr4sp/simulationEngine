#!/bin/bash
CLASSES="classes/production/gr4sp"

java -classpath $CLASSES:/mnt/data/gr4sp/libraries/bsh-2.0b4.jar:/mnt/data/gr4sp/libraries/itext-1.2.jar:/mnt/data/gr4sp/libraries/j3dcore.jar:/mnt/data/gr4sp/libraries/j3dutils.jar:/mnt/data/gr4sp/libraries/jcommon-1.0.21.jar:/mnt/data/gr4sp/libraries/jfreechart-1.0.17.jar:/mnt/data/gr4sp/libraries/jmf.jar:/mnt/data/gr4sp/libraries/mason.19.jar:/mnt/data/gr4sp/libraries/portfolio.jar:/mnt/data/gr4sp/libraries/vecmath.jar:/mnt/data/gr4sp/libraries/postgresql-42.2.6.jar:/mnt/data/gr4sp/libraries/opencsv-4.6.jar:/mnt/data/gr4sp/libraries/yamlbeans-1.13.jar core.Gr4spSimUI
