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
## Dependencies

To run the model, you need to install JAVA JRE and JDK.
## JAVA 
### Ubuntu
```
sudo apt install default-jre
sudo apt install default-jdk
```

See [this guide](https://www.digitalocean.com/community/tutorials/how-to-install-java-with-apt-on-ubuntu-18-04) more details on how to setup JAVA in ubuntu
### Windows
...

### Set JVM library

open [experiments/settingsExperiments.json](experiments/settingsExperiments.json) file, and edit the file setting the variable `jvmPath` accordingly to `jvmPathWindows` or `jvmPathUbuntu` , making sure that the folders contain the `jvm.dll` or `libjvm.so` library.

## Python

Install JPype, Pandas, ipyparallel, SALib, numpy, scipy, matplotlib:

```pip install JPype1 pandas ipyparallel SALib numpy scipy matplotlib```

## compile java files

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
## Run

Go to `src` folder and run

```
java -classpath ../classes:/mnt/data/gr4sp/experiments/../libraries/bsh-2.0b4.jar:/mnt/data/gr4sp/experiments/../libraries/itext-1.2.jar:/mnt/data/gr4sp/experiments/../libraries/j3dcore.jar:/mnt/data/gr4sp/experiments/../libraries/j3dutils.jar:/mnt/data/gr4sp/experiments/../libraries/jcommon-1.0.21.jar:/mnt/data/gr4sp/experiments/../libraries/jfreechart-1.0.17.jar:/mnt/data/gr4sp/experiments/../libraries/jmf.jar:/mnt/data/gr4sp/experiments/../libraries/mason.19.jar:/mnt/data/gr4sp/experiments/../libraries/portfolio.jar:/mnt/data/gr4sp/experiments/../libraries/vecmath.jar:/mnt/data/gr4sp/experiments/../libraries/postgresql-42.2.6.jar:/mnt/data/gr4sp/experiments/../libraries/opencsv-4.6.jar:/mnt/data/gr4sp/experiments/../libraries/yamlbeans-1.13.jar core.Gr4spSim

```


```
