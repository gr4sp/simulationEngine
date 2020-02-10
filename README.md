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

To run the model, you need to install JAVA JRE and JDK.
## JAVA
### Ubuntu
```
sudo apt update
sudo apt install default-jre
sudo apt install default-jdk
```

See [this guide](https://www.digitalocean.com/community/tutorials/how-to-install-java-with-apt-on-ubuntu-18-04) more details on how to setup JAVA in ubuntu
### Windows

follow [this instructions](https://java.com/en/download/help/windows_manual_download.xml)


## Build/Compile Gr4sp Simulator

run this command from the root of the repo
```
javac -d classes -cp src/:/mnt/data/gr4sp/experiments/../libraries/bsh-2.0b4.jar:/mnt/data/gr4sp/experiments/../libraries/itext-1.2.jar:/mnt/data/gr4sp/experiments/../libraries/j3dcore.jar:/mnt/data/gr4sp/experiments/../libraries/j3dutils.jar:/mnt/data/gr4sp/experiments/../libraries/jcommon-1.0.21.jar:/mnt/data/gr4sp/experiments/../libraries/jfreechart-1.0.17.jar:/mnt/data/gr4sp/experiments/../libraries/jmf.jar:/mnt/data/gr4sp/experiments/../libraries/mason.19.jar:/mnt/data/gr4sp/experiments/../libraries/portfolio.jar:/mnt/data/gr4sp/experiments/../libraries/vecmath.jar:/mnt/data/gr4sp/experiments/../libraries/postgresql-42.2.6.jar:/mnt/data/gr4sp/experiments/../libraries/opencsv-4.6.jar:/mnt/data/gr4sp/experiments/../libraries/yamlbeans-1.13.jar  $(find src -name *.java)
```

## Set simulation YAML file

Make sure the folderOutput points to the folder above `src`. For example, if you clone the repo in windows in this folder `c:\\Users\\MyUserName\\Documents\\GitHub\\gr4sp`, then set the first variable in the yaml file

```
[src\core\settings\BAUVIC.yaml](src\core\settings\BAUVIC.yaml)
```

as

```
folderOutput: "C:\\Users\\MyUserName\\Documents\\GitHub\\gr4sp"

```

## Install PostGres and load database

Install postgres

```
sudo apt install postgresql postgresql-contrib
```

To create `postgres` username and database

```
sudo -u postgres psql
createdb postgres
\q
```

To load data into the database
```
sudo -u postgres pg_restore -U postgres -d postgres < backupDB/DB-2019-1-8.sql
```

Edit file pg_hba.conf. In ubuntu it can be found at `/etc/postgresql/10/main/pg_hba.conf`:

```
 sudo nano /etc/postgresql/10/main/pg_hba.conf
```

See [this guide](https://linuxize.com/post/how-to-use-nano-text-editor/#opening-and-creating-files) about how to edit files with nano
and make sure that at the end of the file, all the lines below finish with the word `trust`

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

Go to `src` folder and run

```
java -classpath ../classes:/mnt/data/gr4sp/experiments/../libraries/bsh-2.0b4.jar:/mnt/data/gr4sp/experiments/../libraries/itext-1.2.jar:/mnt/data/gr4sp/experiments/../libraries/j3dcore.jar:/mnt/data/gr4sp/experiments/../libraries/j3dutils.jar:/mnt/data/gr4sp/experiments/../libraries/jcommon-1.0.21.jar:/mnt/data/gr4sp/experiments/../libraries/jfreechart-1.0.17.jar:/mnt/data/gr4sp/experiments/../libraries/jmf.jar:/mnt/data/gr4sp/experiments/../libraries/mason.19.jar:/mnt/data/gr4sp/experiments/../libraries/portfolio.jar:/mnt/data/gr4sp/experiments/../libraries/vecmath.jar:/mnt/data/gr4sp/experiments/../libraries/postgresql-42.2.6.jar:/mnt/data/gr4sp/experiments/../libraries/opencsv-4.6.jar:/mnt/data/gr4sp/experiments/../libraries/yamlbeans-1.13.jar core.Gr4spSim

```

## Run Experiments with EMA Workbench

### Python dependencies
Install JPype, Pandas, ipyparallel, SALib, numpy, scipy, matplotlib:

```
pip update
pip install JPype1 pandas ipyparallel SALib numpy scipy matplotlib
```

### Set JVM library

open [experiments/settingsExperiments.json](experiments/settingsExperiments.json) file, and edit the file setting the variable `jvmPath` accordingly to `jvmPathWindows` or `jvmPathUbuntu` , making sure that the folders contain the `jvm.dll` or `libjvm.so` library.

### Run EET or Variance-based SA

go to `experiments` folder, edit the experiment you want to run in [experiments/runExperiments.py:26](experiments/runExperiments.py:26) and execute:

```
python runExperiments.py
```
## Analyze results

see notebooks in [experiments/notebookGr4sp](experiments/notebookGr4sp)
