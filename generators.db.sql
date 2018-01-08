BEGIN TRANSACTION;
CREATE TABLE IF NOT EXISTS `Technology` (
	`id`	INTEGER NOT NULL PRIMARY KEY AUTOINCREMENT UNIQUE,
	`name`	TEXT NOT NULL,
	`maxcap`	REAL NOT NULL,
	`efficiency`	REAL NOT NULL,
	`lifecycle`	INTEGER NOT NULL,
	`EnergyType_id`	INTEGER,
	`FuelSource_id`	INTEGER
);
INSERT INTO `Technology` VALUES (1,'Coal fired',1500.0,0.7,20,1,1);
INSERT INTO `Technology` VALUES (2,'Solar PV',0.04,0.15,0,2,10);
CREATE TABLE IF NOT EXISTS `FuelSource` (
	`id`	INTEGER NOT NULL PRIMARY KEY AUTOINCREMENT UNIQUE,
	`name`	TEXT NOT NULL,
	`EnergyType_id`	INTEGER
);
INSERT INTO `FuelSource` VALUES (1,'Brown Coal',1);
INSERT INTO `FuelSource` VALUES (2,'Black Coal',1);
INSERT INTO `FuelSource` VALUES (3,'Narural Gas',1);
INSERT INTO `FuelSource` VALUES (4,'Gasoline',1);
INSERT INTO `FuelSource` VALUES (5,'Diesel',1);
INSERT INTO `FuelSource` VALUES (6,'Nuclear',1);
INSERT INTO `FuelSource` VALUES (7,'Wind',2);
INSERT INTO `FuelSource` VALUES (8,'Biomass',2);
INSERT INTO `FuelSource` VALUES (9,'Hydro',2);
INSERT INTO `FuelSource` VALUES (10,'Solar',2);
INSERT INTO `FuelSource` VALUES (11,'Geothermal',2);
CREATE TABLE IF NOT EXISTS `EnergyType` (
	`id`	INTEGER NOT NULL PRIMARY KEY AUTOINCREMENT UNIQUE,
	`name`	TEXT NOT NULL
);
INSERT INTO `EnergyType` VALUES (1,'Fossil Fuel');
INSERT INTO `EnergyType` VALUES (2,'Renewable');
COMMIT;
