# gr4sp



## Set up Gr4sp

Clone repo:

```git clone https://github.com/angelara/gr4sp.git```

go to the root folder `gr4sp` and run

```
mdkir logs
mkdir csv
mkdir plots
```

## JAVA 
To run the model, you need to install JAVA JRE and JDK.

### Ubuntu
```
sudo apt install default-jre
sudo apt install default-jdk
```

See [this guide](https://www.digitalocean.com/community/tutorials/how-to-install-java-with-apt-on-ubuntu-18-04) more details on how to setup JAVA in ubuntu
### Windows

follow [this instructions](https://java.com/en/download/help/windows_manual_download.xml)


## Build/Compile Gr4sp Simulator

You can use [InteliJ](https://www.jetbrains.com/idea/). Once installed, open the project clicking on the gr4sp.iml file. You can compile/build and run from InteliJ.

Alternatively, run this command from the root of the repo to make the bash script executable

```
chmod +x *.sh
```

and run

```
./build.sh
```

The command executed by the bash script is:

```
javac -d classes/production/gr4sp -cp src/:/mnt/data/gr4sp/experiments/../libraries/bsh-2.0b4.jar:/mnt/data/gr4sp/experiments/../libraries/itext-1.2.jar:/mnt/data/gr4sp/experiments/../libraries/j3dcore.jar:/mnt/data/gr4sp/experiments/../libraries/j3dutils.jar:/mnt/data/gr4sp/experiments\
/../libraries/jcommon-1.0.21.jar:/mnt/data/gr4sp/experiments/../libraries/jfreechart-1.0.17.jar:/mnt/data/gr4sp/experiments/../libraries/jmf.jar:/mnt/data/gr4sp/experiments/../libraries/mason.19.jar:/mnt/data/gr4sp/experiments/../libraries/portfolio.jar:/mnt/data/gr4sp/\
experiments/../libraries/vecmath.jar:/mnt/data/gr4sp/experiments/../libraries/postgresql-42.2.6.jar:/mnt/data/gr4sp/experiments/../libraries/opencsv-4.6.jar:/mnt/data/gr4sp/experiments/../libraries/yamlbeans-1.13.jar src/core/*/*.java -Xlint:unchecked
```

## Set simulation YAML file

Make sure the folderOutput points to the folder above `src`. For example, if you clone the repo in windows in this folder `c:\\Users\\MyUserName\\Documents\\GitHub\\gr4sp`, then set the first variable in the yaml file

```
[simulationSettings\BAUVIC.yaml](simulationSettings\BAUVIC.yaml)
```

as 

```
folderOutput: "C:\\Users\\MyUserName\\Documents\\GitHub\\gr4sp"

## Install PostGres and load database

```
cd backupDB
sudo -u postgres psql
createdb postgres
\q
sudo -u postgres pg_restore -U postgres -d postgres < DB-2019-1-8.sql
```

edit file pg_hba.conf. In ubuntu it can be found at `/etc/postgresql/10/main/pg_hba.conf`


```local   all             postgres                                trust 

# "local" is for Unix domain socket connections only
local   all             all                                     trust                                                                                                                                                                         # IPv4 local connections:
host    all             all             127.0.0.1/32            trust                                                                                                                                                                        # IPv6 local connections:
host    all             all             ::1/128                 trust   
```

and then run 

```
sudo service postgresql restart
```
## Run Gr4sp Simulation

You can run from InteliJ.

Otherwise, make sure you added executable rights to `run_gr4sp.sh` by running 

```
chmod +x *.sh
```

and then simply run:

```
./runGr4sp.sh
```

if you want to run it with the Graphical interface to see the progress of the simulation with the live plots, then run

```
./runGr4spUI.sh

```

alternatively, the command being used is as follows:

```
java -classpath classes/production/gr4sp:/mnt/data/gr4sp/libraries/bsh-2.0b4.jar:/mnt/data/gr4sp/libraries/itext-1.2.jar:/mnt/data/gr4sp/libraries/j3dcore.jar:/mnt/data/gr4sp/libraries/j3dutils.jar:/mnt/data/gr4sp/libraries/jcommon-1.0.21.jar:/mnt/data/gr4sp/libraries/jfreechart-1.0.17.jar:/mnt/data/gr4sp/libraries/jmf.jar:/mnt/data/gr4sp/libraries/mason.19.jar:/mnt/data/gr4sp/libraries/portfolio.jar:/mnt/data/gr4sp/libraries/vecmath.jar:/mnt/data/gr4sp/libraries/postgresql-42.2.6.jar:/mnt/data/gr4sp/libraries/opencsv-4.6.jar:/mnt/data/gr4sp/libraries/yamlbeans-1.13.jar core.Gr4spSim

```

## Run Experiments with EMA Workbench

### Python dependencies
Install JPype, Pandas, ipyparallel, SALib, numpy, scipy, matplotlib:

```pip install JPype1 pandas ipyparallel SALib numpy scipy matplotlib```

### Set JVM library

open [experiments/settingsExperiments.json](experiments/settingsExperiments.json) file, and edit the file setting the variable `jvmPath` accordingly to `jvmPathWindows` or `jvmPathUbuntu` , making sure that the folders contain the `jvm.dll` or `libjvm.so` library.

### Run EET or Variance-based SA

go to `experiments` folder, edit the experiment you want to run in [experiments/runExperiments.py:26](experiments/runExperiments.py:26) and execute:

```
python runExperiments.py
```
## Analyze results

see notebooks in [experiments/notebookGr4sp](experiments/notebookGr4sp)
