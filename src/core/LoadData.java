package core;

import core.Relationships.*;
import core.Social.*;
import core.Technical.*;

import java.sql.*;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.Date;

//Class to load all the data to start the simulation
public class LoadData {


    //Functions to convert readings from text in DB to Actor type, etc.

    public static ActorAssetRelationshipType stringToActorAssetTypeRelType(String actorRelType) {

        if (actorRelType.equalsIgnoreCase("OWN"))
            return ActorAssetRelationshipType.OWN;
        if (actorRelType.equalsIgnoreCase("LEASE"))
            return ActorAssetRelationshipType.LEASE;
        if (actorRelType.equalsIgnoreCase("USE"))
            return ActorAssetRelationshipType.USE;


        return ActorAssetRelationshipType.OTHER;
    }

    public static ActorActorRelationshipType stringToActorActorTypeRelType(String actorRelType) {


        if (actorRelType.equalsIgnoreCase("BILLING"))
            return ActorActorRelationshipType.BILLING;
        if (actorRelType.equalsIgnoreCase("OTC"))
            return ActorActorRelationshipType.OTC;
        if (actorRelType.equalsIgnoreCase("ETF"))
            return ActorActorRelationshipType.ETF;
        if (actorRelType.equalsIgnoreCase("SPOT"))
            return ActorActorRelationshipType.SPOT;
        if (actorRelType.equalsIgnoreCase("ACCESS_FEE"))
            return ActorActorRelationshipType.ACCESS_FEE;
        if (actorRelType.equalsIgnoreCase("COMMISSION_FEE"))
            return ActorActorRelationshipType.COMMISSION_FEE;
        if (actorRelType.equalsIgnoreCase("P2P"))
            return ActorActorRelationshipType.P2P;
        if (actorRelType.equalsIgnoreCase("OWNS"))
            return ActorActorRelationshipType.OWNS;

        return ActorActorRelationshipType.OTHER;
    }



    // Select Arena Function

    public static void
    selectArena(Gr4spSim data) {
        String url = "jdbc:postgresql://localhost:5432/postgres?user=postgres"; //"jdbc:sqlite:Spm_archetypes.db";

        SimpleDateFormat stringToDate = new SimpleDateFormat("yyyy-MM-dd");


        String sql = "SELECT arenas.name as arenaname, arenas.type as arenatype, id" +
                " FROM  arenas ;";

        try (Connection conn = DriverManager.getConnection(url);
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            // loop through the result set
            while (rs.next()) {
                System.out.println("\t" + rs.getInt("id") + "\t" +
                        rs.getString("arenaname") + "\t" +
                        rs.getString("arenatype"));


                int arenaId = rs.getInt("id");

                //If arena doesn't exist, create it
                if (!data.getArena_register().containsKey(arenaId)) {
                    Arena arena = new Arena(arenaId, rs.getString("arenaname"), rs.getString("arenatype"));
                    data.getArena_register().put(arenaId, arena);
                }


            }
        } catch (SQLException e) {
            System.out.println(e.getMessage());
        }
    }

    // Select tariffs function

    public static void
    selectTariffs(Gr4spSim data, String startDate, String endDate, String areaCode) {
        String url = "jdbc:postgresql://localhost:5432/postgres?user=postgres"; //"jdbc:sqlite:Spm_archetypes.db";

        SimpleDateFormat stringToDate = new SimpleDateFormat("yyyy-MM-dd");

        String sql = "SELECT date, conversion_variable" +
                " FROM cpi_conversion;";

        //Loading CPI Conversion data
        try (Connection conn = DriverManager.getConnection(url);
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            // loop through the result set
            while (rs.next()) {
                System.out.println("\t" + rs.getString("date") + "\t" +
                        rs.getFloat("conversion_variable"));


                java.util.Date date = rs.getDate("date");
                Float conversion_rate = rs.getFloat("conversion_variable");

                data.getCpi_conversion().put(date, conversion_rate);


            }
        } catch (SQLException e) {
            System.out.println(e.getMessage());
        }


        sql = "SELECT id, casestudy_area, date, tariff_name, average_dckwh, chargeperroom_dmonth, secvij_gdgr_aud_quarter, standingcharge_aud_year" +
                " FROM tariffdata WHERE" +
                " casestudy_area = '" + areaCode + "'" +
                " AND date <= '" + endDate + "'" +
                " AND date >= '" + startDate + "';";

        //If areaCode is at state scale, do not filter tariffs by smaller areacodes, take all tariffs available
        //Agent will select a tariff according to simulation policy. For more details See SimulParameters()
        if (areaCode == "VIC")
            sql = "SELECT id, casestudy_area, date, tariff_name, average_dckwh, chargeperroom_dmonth, secvij_gdgr_aud_quarter, standingcharge_aud_year" +
                    " FROM tariffdata WHERE" +
                    " date <= '" + endDate + "'" +
                    " AND date >= '" + startDate + "';";


        try (Connection conn = DriverManager.getConnection(url);
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            // loop through the result set
            while (rs.next()) {
                System.out.println("\t" + rs.getInt("id") + "\t" +
                        rs.getString("casestudy_area") + "\t" +
                        rs.getString("date") + "\t" +
                        rs.getString("tariff_name") + "\t" +
                        rs.getString("average_dckwh") + "\t" +
                        rs.getString("chargeperroom_dmonth") + "\t" +
                        rs.getString("secvij_gdgr_aud_quarter") + "\t" +
                        rs.getString("standingcharge_aud_year"));

                //add tariffs to the Retail arena
                int arenaId = 2;

                Arena arena = data.getArena_register().get(arenaId);

                java.util.Date cStartDate = rs.getDate("date");

                //Compute end Date 1 year after
                Calendar cEndDate = Calendar.getInstance();
                try {
                    Date date = stringToDate.parse(rs.getString("date"));
                    cEndDate.setTime(date);
                } catch (ParseException e) {
                    System.err.println("Cannot parse Start Date: " + e.toString());
                }
                cEndDate.add(Calendar.YEAR, 1);

                //Get CPI conversion
                float conversion_rate = data.getCpi_conversion().get(cStartDate);

                //Compute serviceFee
                Float chargeperroom_dmonth = rs.getFloat("chargeperroom_dmonth") * conversion_rate;
                Float secvij_gdgr_aud_quarter = rs.getFloat("secvij_gdgr_aud_quarter") * conversion_rate;
                Float standingcharge_aud_year = rs.getFloat("standingcharge_aud_year") * conversion_rate;

                float serviceFee = 0;

                if (chargeperroom_dmonth != null)
                    serviceFee += chargeperroom_dmonth;
                if (secvij_gdgr_aud_quarter != null)
                    serviceFee += secvij_gdgr_aud_quarter / 3.0;
                if (standingcharge_aud_year != null)
                    serviceFee += standingcharge_aud_year / 12.0;

                //Actor seller, Actor buyer, Asset assetUsed, float dollarMWh, Date start, Date end, float capacityContracted

                /**
                 * NEED TO USE FORMULAT RBA TO UPDATE PRICES!
                 * */

                Contract contract = new Contract(
                        rs.getString("tariff_name"),
                        0,
                        arena.EndConsumer,
                        0,
                        rs.getFloat("average_dckwh") * conversion_rate,
                        serviceFee,
                        cStartDate,
                        cEndDate.getTime(),
                        0);


                //Add contract to arena
                if (arena.getType().equalsIgnoreCase("OTC") || arena.getType().equalsIgnoreCase("Retail")) {
                    arena.getBilateral().add(contract);
                }
                if (arena.getType().equalsIgnoreCase("FiTs")) {
                    arena.getFiTs().add(contract);
                }

            }
        } catch (SQLException e) {
            System.out.println(e.getMessage());
        }
    }


    //Select Demand monthly Historic

    public static void
    selectDemand(Gr4spSim data, String startDate, String endDate) {
        String url = "jdbc:postgresql://localhost:5432/postgres?user=postgres"; //"jdbc:sqlite:Spm_archetypes.db";

        SimpleDateFormat stringToDate = new SimpleDateFormat("yyyy-MM-dd");

        /**
         * Load Total Consumption per month
         * */
        String sql = "SELECT date, operational_demand_mw" +
                " FROM  generation_demand_historic WHERE " +
                " date <= '" + endDate + "'" +
                " AND date >= '" + startDate + "';";

        try (Connection conn = DriverManager.getConnection(url);
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            // loop through the result set
            while (rs.next()) {
                System.out.println("\t" + rs.getString("date") + "\t" +
                        rs.getString("operational_demand_mw"));


                Date d = rs.getDate("date");
                Double mw = rs.getDouble("operational_demand_mw");

                //If monthly consumption doesn't exist, create it
                if (!data.getMonthly_demand_register().containsKey(d)) {
                    data.getMonthly_demand_register().put(d, mw);
                }


            }
        } catch (SQLException e) {
            System.out.println(e.getMessage());
        }
    }

    //Select Demand Historic half hours for spot market

    public static void
    selectDemandHalfHour(Gr4spSim data, String startDate, String endDate) {
        String url = "jdbc:postgresql://localhost:5432/postgres?user=postgres"; //"jdbc:sqlite:Spm_archetypes.db";

        SimpleDateFormat stringToDate = new SimpleDateFormat("yyyy-MM-dd");

        /**
         * Load Total Consumption per month
         * */
        String sql = "SELECT settlement_date, total_demand, price" +
                " FROM  total_demand_halfhour WHERE " +
                " settlement_date <= '" + endDate + "'" +
                " AND settlement_date >= '" + startDate + "';";

        try (Connection conn = DriverManager.getConnection(url);
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            // loop through the result set
            while (rs.next()) {
                System.out.println("\t" + rs.getTimestamp("settlement_date") + "\t" +
                        rs.getFloat("total_demand")+ "\t" +
                        rs.getFloat("price"));


                Date d = rs.getTimestamp("settlement_date");
                Double mw = rs.getDouble("total_demand");

                //If half hour consumption doesn't exist, create it
                if (!data.getHalf_hour_demand_register().containsKey(d)) {
                    data.getHalf_hour_demand_register().put(d, mw);
                }


            }
        } catch (SQLException e) {
            System.out.println(e.getMessage());
        }
    }

    //Select Consumption

    public static void
    selectConsumption(Gr4spSim data, String startDate, String endDate) {
        String url = "jdbc:postgresql://localhost:5432/postgres?user=postgres"; //"jdbc:sqlite:Spm_archetypes.db";

        SimpleDateFormat stringToDate = new SimpleDateFormat("yyyy-MM-dd");

        /**
         * Load Total Consumption per month
         * */
        String sql = "SELECT date, total_consumption_gwh" +
                " FROM  generation_demand_historic WHERE " +
                " date <= '" + endDate + "'" +
                " AND date >= '" + startDate + "';";

        try (Connection conn = DriverManager.getConnection(url);
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            // loop through the result set
            while (rs.next()) {
                System.out.println("\t" + rs.getString("date") + "\t" +
                        rs.getString("total_consumption_gwh"));


                Date d = rs.getDate("date");
                Double gwh = rs.getDouble("total_consumption_gwh");

                //If monthly consumption doesn't exist, create it
                if (!data.getMonthly_consumption_register().containsKey(d)) {
                    data.getMonthly_consumption_register().put(d, gwh);
                }


            }
        } catch (SQLException e) {
            System.out.println(e.getMessage());
        }

        /**
         * Load domestic consumers
         * */
        sql = "SELECT date, domesticconsumers" +
                " FROM  domestic_consumers WHERE " +
                " date <= '" + endDate + "'" +
                " AND date >= '" + startDate + "';";


        try (Connection conn = DriverManager.getConnection(url);
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            // loop through the result set
            while (rs.next()) {
  /*              System.out.println("\t" + rs.getString("date") + "\t" +
                        rs.getString("domesticconsumers"));
*/

                Date d = rs.getDate("date");
                int consumers = rs.getInt("domesticconsumers");

                //If arena doesn't exist, create it
                if (!data.getMonthly_domestic_consumers_register().containsKey(d)) {
                    data.getMonthly_domestic_consumers_register().put(d, consumers);
                }


            }
        } catch (SQLException e) {
            System.out.println(e.getMessage());
        }

        /**
         * Compute linear Monthly growth from Yearly data
         * */
        HashMap<Date, Integer> newMonthlyData = new HashMap<>();
        for (Map.Entry<Date, Integer> entry : data.getMonthly_domestic_consumers_register().entrySet()) {
            Date currentDate = entry.getKey();
            Integer consumers = entry.getValue();

            Calendar c = Calendar.getInstance();
            c.setTime(currentDate);

            //Get info about next year
            c.add(Calendar.YEAR, 1);
            Date nextYear = c.getTime();
            Integer consumersNextYear = data.getMonthly_domestic_consumers_register().get(nextYear);

            if (consumers == null || consumersNextYear == null) continue;

            Integer increment = (consumersNextYear - consumers) / 12;

            //Set calendar to current Date again
            c.setTime(currentDate);

            //Increment date by 1 month, and 1 increment of population, until we filled the 12 months
            for (int i = 0; i < 11; i++) {

                //add 1 month
                c.add(Calendar.MONTH, 1);
                Date newMonth = c.getTime();

                //add new consumers
                consumers += increment;

                //store the new data point
                newMonthlyData.put(newMonth, consumers);

            }
        }

        //Add new monthly data into our domestic consumer register
        for (Map.Entry<Date, Integer> entry : newMonthlyData.entrySet()) {
            data.getMonthly_domestic_consumers_register().put(entry.getKey(), entry.getValue());
        }

        /**
         * Update Total Consumption with monthly domestic consumers
         * and Percentage of domestic usage
         * */

        for (Date month : data.getMonthly_consumption_register().keySet()) {

            double gwh = data.getMonthly_consumption_register().get(month);

            //Save total consumption
            data.getTotal_monthly_consumption_register().put(month, gwh * 1000.0);

            Integer consumers = data.getMonthly_domestic_consumers_register().get(month);

            if (consumers == null) continue;

            //convert to MWh and only domestic demand
            double newMwh = (gwh * 1000.0 * data.getDomesticConsumptionPercentage()) / (double) consumers;
            //to check total demand in KWh
            //double newkwh = (gwh * 1000000);

            data.getMonthly_consumption_register().put(month, newMwh);
        }
    }

    public static void
    selectGenerationHistoricData(Gr4spSim data, String startDate, String endDate) {
        String url = "jdbc:postgresql://localhost:5432/postgres?user=postgres"; //"jdbc:sqlite:Spm_archetypes.db";

        SimpleDateFormat stringToDate = new SimpleDateFormat("yyyy-MM-dd");

        /**
         * Load Total Generation per month
         * */
        String sql = "SELECT date, volumeweightedprice_dollarmwh, temperaturec, solar_roofpv_dollargwh, solar_roofpv_gwh," +
                "solar_utility_dollargwh, solar_utility_gwh, wind_dollargwh, wind_gwh, hydro_dollargwh," +
                "hydro_gwh, battery_disch_dollargwh, battery_disch_gwh, gas_ocgt_dollargwh, gas_ocgt_gwh," +
                "gas_steam_dollargwh, gas_steam_gwh, browncoal_dollargwh, browncoal_gwh, imports_dollargwh," +
                "imports_gwh, exports_dollargwh, exports_gwh, total_gen_gwh" +
                " FROM  generation_demand_historic WHERE " +
                " date <= '" + endDate + "'" +
                " AND date >= '" + startDate + "';";


        //volumeweightedprice_dollarMWh, temperaturec, solar_roofpv_dollargwh, solar_roofpv_gwh, solar_utility_dollargwh, solar_utility_gwh, wind_dollargwh, wind_gwh, hydro_dollargwh, hydro_gwh, battery_disch_dollargwh, battery_disch_gwh, gas_ocgt_dollargwh, gas_ocgt_gwh,gas_steam_dollargwh, gas_steam_gwh, browncoal_dollargwh, browncoal_gwh, imports_dollargwh, imports_gwh, exports_dollargwh, exports_gwh, total_gen_gwh
        try (Connection conn = DriverManager.getConnection(url);
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            // loop through the result set
            while (rs.next()) {


 /*               System.out.println("\t" + rs.getString("date") + "\t" +
                        rs.getString("temperaturec") + "\t" +
                        rs.getString("total_gen_gwh"));
*/

                Date d = rs.getDate("date");

                Generation genData = new Generation(
                        rs.getDate("date"), rs.getFloat("volumeweightedprice_dollarmwh"), rs.getFloat("temperaturec"),
                        rs.getFloat("solar_roofpv_dollargwh"), rs.getFloat("solar_roofpv_gwh"), rs.getFloat("solar_utility_dollargwh"),
                        rs.getFloat("solar_utility_gwh"), rs.getFloat("wind_dollargwh"), rs.getFloat("wind_gwh"), rs.getFloat("hydro_dollargwh"),
                        rs.getFloat("hydro_gwh"), rs.getFloat("battery_disch_dollargwh"), rs.getFloat("battery_disch_gwh"), rs.getFloat("gas_ocgt_dollargwh"),
                        rs.getFloat("gas_ocgt_gwh"), rs.getFloat("gas_steam_dollargwh"), rs.getFloat("gas_steam_gwh"), rs.getFloat("browncoal_dollargwh"),
                        rs.getFloat("browncoal_gwh"), rs.getFloat("imports_dollargwh"), rs.getFloat("imports_gwh"), rs.getFloat("exports_dollargwh"),
                        rs.getFloat("exports_gwh"), rs.getFloat("total_gen_gwh")
                );

                //If monthly gen datapoint doesn't exist, create it
                if (!data.getMonthly_generation_register().containsKey(d)) {
                    data.getMonthly_generation_register().put(d, genData);
                }
            }
        } catch (SQLException e) {
            System.out.println(e.getMessage());
        }
    }

    public static void

    selectSolarExposure(Gr4spSim data) {
        String url = "jdbc:postgresql://localhost:5432/postgres?user=postgres"; //"jdbc:sqlite:Spm_archetypes.db";

        SimpleDateFormat stringToDate = new SimpleDateFormat("yyyy-MM-dd");

        String sql = "SELECT year, month, solar_exposure" +
                " FROM solar_exposure_monthly;";

        //Loading Solar Exposure data
        try (Connection conn = DriverManager.getConnection(url);
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            // loop through the result set
            while (rs.next()) {
                int year = rs.getInt("year");
                int month = rs.getInt("month");
                String sdate = year + "-" + month + "-1";
                Date date = null;
                float toKwh = (float) 3.6;


                float solarExposure = rs.getFloat("solar_exposure") / toKwh;

                System.out.println("\t" + year + "\t" +
                        month + "\t" +
                        solarExposure
                );


                try {
                    date = stringToDate.parse(sdate);
                } catch (ParseException e) {
                }

                data.getSolar_exposure().put(date, solarExposure);


            }
        } catch (SQLException e) {
            System.out.println(e.getMessage());
        }
    }

    public static void
    selectSolarInstallation(Gr4spSim data) {
        String url = "jdbc:postgresql://localhost:5432/postgres?user=postgres"; //"jdbc:sqlite:Spm_archetypes.db";

        SimpleDateFormat stringToDate = new SimpleDateFormat("yyyy-MM-dd");

        String sql = "SELECT year, month, average_installation_kw" +
                " FROM solar_installation_monthly;";

        //Loading Solar installation data
        try (Connection conn = DriverManager.getConnection(url);
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            // loop through the result set
            while (rs.next()) {
                int year = rs.getInt("year");
                int month = rs.getInt("month");
                String sdate = year + "-" + month + "-1";
                Date date = null;


                float solarInstallation = rs.getFloat("average_installation_kw");

                System.out.println("\t" + year + "\t" +
                        month + "\t" +
                        solarInstallation
                );


                try {
                    date = stringToDate.parse(sdate);
                } catch (ParseException e) {
                }

                data.getSolar_installation_kw().put(date, solarInstallation);
            }
        } catch (SQLException e) {
            System.out.println(e.getMessage());
        }
    }

    //Select actors function

    public static void
    selectActors(Gr4spSim data, String tableName, String startDate, String endDate) {
        String url = "jdbc:postgresql://localhost:5432/postgres?user=postgres"; //"jdbc:sqlite:Spm_archetypes.db";


        String sql = "SELECT " + tableName + ".id as id, actortype.name as typename, " + tableName + ".name as name, govrole, businessstructure, ownershipmodel, startdate, actorschange.changedate" +
                " FROM " + tableName + ", actorschange, actortype WHERE " +
                "actortype.id = actors.type AND actors.id = actorschange.idactora AND actors.startdate <= '" + endDate + "'" +
                " AND actorschange.changedate > '" + startDate + "';";


        try (Connection conn = DriverManager.getConnection(url);
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            // loop through the result set
            while (rs.next()) {
                System.out.println("\t" + rs.getInt("id") + "\t" +
                        rs.getString("typename") + "\t" +
                        rs.getString("name") + "\t" +
                        rs.getString("govrole") + "\t" +
                        rs.getString("businessStructure") + "\t" +
                        rs.getDate("startdate") + "\t" +
                        rs.getDate("changedate") + "\t" +
                        rs.getString("ownershipModel"));


                Actor actor = new Actor(rs.getInt("id"),
                        ActorType.valueOf(rs.getString("typename")),
                        rs.getString("name"),
                        core.Social.GovRole.stringToGovRole(rs.getString("govrole")),
                        core.Social.BusinessStructure.stringToBusinessStructure(rs.getString("businessStructure")),
                        core.Social.OwnershipModel.stringToOwnershipModel(rs.getString("ownershipModel")),
                        rs.getDate("startdate"),
                        rs.getDate("changedate"));

                int idActor = rs.getInt("id");

                //Insert in Map the actor with key = id, and add the new actor
                if (!data.getActor_register().containsKey(idActor))
                    data.getActor_register().put(idActor, actor);
                else
                    System.err.println("Two actors cannot have the same ID");


                data.setNumActors(data.getNumActors() + 1);

                //Add actor to Array. This will be used in the constructor of SPM
                //actors.add(actor);

            }
        } catch (SQLException e) {
            System.out.println(e.getMessage());
        }
        //return actors;
    }

    // Actor asset relationships function
    public static void
    selectActorAssetRelationships(Gr4spSim data, String tableName) {
        String url = "jdbc:postgresql://localhost:5432/postgres?user=postgres";//"jdbc:sqlite:Spm_archetypes.db";

        for (Map.Entry<Integer, Actor> entry : data.getActor_register().entrySet()) {
            Actor actor = entry.getValue();

            String sql = "SELECT actorid, assetid, reltype, assettype, percentage" +
                    " FROM " + tableName + " WHERE actorid = " + actor.getId();


            try (Connection conn = DriverManager.getConnection(url);
                 Statement stmt = conn.createStatement();
                 ResultSet rs = stmt.executeQuery(sql)) {

                // loop through the result set
                while (rs.next()) {
                    System.out.println("\t" + rs.getInt("ActorId") + "\t" +
                            rs.getInt("AssetId") + "\t" +
                            rs.getDouble("Percentage") + "\t" +
                            rs.getString("AssetType") + "\t" +
                            rs.getString("RelType"));


                    //Actor actor = actor_register.get(rs.getInt("ActorId"));
                    Vector<Asset> asset;

                    double percentage = rs.getDouble("Percentage");
                    String assetType = rs.getString("AssetType");

                    if (assetType.equalsIgnoreCase("Generation")) {
                        Vector<Generator> gens = data.getGen_register().get(rs.getInt("AssetId"));

                        //Need to make sure that the SPM_gen_mapping is correct
                        //For this we need to create another SPM for 90s, and make sure that it uses
                        //The gen from the 90s.
                        if (gens == null) {
                            System.out.println("Actor " + actor.getName() + " - Generation Asset id " + rs.getInt("AssetId") + " Relationship not existent for this period of time");
                        } else {
                            for (Generator gen : gens) {
                                ActorAssetRelationship actorRel = new ActorAssetRelationship(actor, gen,
                                        stringToActorAssetTypeRelType(rs.getString("RelType")), percentage);

                                //Add actorAsset relationship into the asset list
                                gen.addAssetRelationship(actorRel);
                                //Add all actorAsset relationships into the simulation engine global asset list
                                data.getActorAssetRelationships().add(actorRel);

                            }
                        }
                    } else if (assetType.equalsIgnoreCase("Network asset")) {
                        Vector<NetworkAssets> nets = data.getNetwork_register().get(rs.getInt("AssetId"));
                        if (nets == null) {
                            System.out.println("Actor " + actor.getName() + " - Network Asset id " + rs.getInt("AssetId") + " Relationship not existent for this period of time");
                        } else {
                            for (NetworkAssets net : nets) {
                                ActorAssetRelationship actorRel = new ActorAssetRelationship(actor, net,
                                        stringToActorAssetTypeRelType(rs.getString("RelType")), percentage);
                                net.addAssetRelationship(actorRel);
                                data.getActorAssetRelationships().add(actorRel);

                            }
                        }
                    } else if (assetType.equalsIgnoreCase("SPM")) {
                        Vector<Spm> spms = data.getSpm_register().get(rs.getInt("AssetId"));
                        if (spms == null) {
                            System.out.println("Actor " + actor.getName() + " - SPM Asset id " + rs.getInt("AssetId") + " Relationship not existent for this period of time");
                        } else {
                            for (Spm spm : spms) {
                                ActorAssetRelationship actorRel = new ActorAssetRelationship(actor, spm,
                                        stringToActorAssetTypeRelType(rs.getString("RelType")), percentage);
                                spm.addAssetRelationship(actorRel);
                                data.getActorAssetRelationships().add(actorRel);

                            }
                        }
                    }

                }
            } catch (SQLException e) {
                System.out.println(e.getMessage());
            }
        }
    }

    /**
     * select all rows in the Actor table
     */

    public static void
    selectActorActorRelationships(Gr4spSim data, String tableName) {
        String url = "jdbc:postgresql://localhost:5432/postgres?user=postgres"; //"jdbc:sqlite:Spm_archetypes.db";


        String sql = "SELECT Actor1, Actor2, RelType" +
                " FROM " + tableName;


        try (Connection conn = DriverManager.getConnection(url);
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            // loop through the result set
            while (rs.next()) {
                System.out.println("\t" + rs.getInt("Actor1") + "\t" +
                        rs.getInt("Actor2") + "\t" +
                        rs.getString("RelType"));


                Actor act1 = data.getActor_register().get(rs.getInt("Actor1"));
                Actor act2 = data.getActor_register().get(rs.getInt("Actor2"));

                ActorActorRelationship actorRel = new ActorActorRelationship(act1, act2, stringToActorActorTypeRelType(rs.getString("RelType")));

                data.getActorActorRelationships().add(actorRel);

            }
        } catch (SQLException e) {
            System.out.println(e.getMessage());
        }
    }

    /**
     * select all rows in the Generation Technologies table
     */
    public static ArrayList<Generator> selectGenTech(Gr4spSim data, String idgen) {
        String url = "jdbc:postgresql://localhost:5432/postgres?user=postgres"; // url for sqlite "jdbc:sqlite:Spm_archetypes.db";


        SimpleDateFormat DateToString = new SimpleDateFormat("yyyy-MM-dd");

        String sql = "SELECT asset_id, region, asset_type, site_name, owner_name, technology_type, fuel_type, nameplate_capacity_mw, dispatch_type, full_commercial_use_date, closure_date, " +
                " duid, no_units, storage_capacity_mwh, expected_closure_date, fuel_bucket_summary" +
                " FROM generationassets WHERE asset_id = '" + idgen + "' AND full_commercial_use_date <= '" + DateToString.format(data.getEndSimDate()) + "'" +
                " AND ( closure_date > '" + DateToString.format(data.getStartSimDate()) + "'"+
                " OR expected_closure_date > '" + DateToString.format(data.getStartSimDate()) + "');";

        ArrayList<Generator> gens = new ArrayList<Generator>();
        try (Connection conn = DriverManager.getConnection(url);
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            // loop through the result set
            while (rs.next()) {
                /*System.out.println("\t Generator: " +
                        rs.getInt("asset_id")+ "\t" +
                        rs.getString("region")+ "\t" +
                        rs.getString("asset_type")+ "\t" +
                        rs.getString("site_name")+ "\t" +
                        rs.getString("owner_name")+ "\t" +
                        rs.getString("technology_type")+ "\t" +
                        rs.getString("fuel_type")+ "\t" +
                        rs.getDouble("nameplate_capacity_mw")+ "\t" +
                        rs.getString("dispatch_type")+ "\t" +
                        rs.getDate("full_commercial_use_date")+ "\t" +
                        rs.getDate("expected_closure_date")+ "\t" +
                        rs.getDate("closure_date")+ "\t" +
                        rs.getString("duid")+ "\t" +
                        rs.getInt("no_units")+ "\t" +
                        rs.getFloat("storage_capacity_mwh")+ "\t" +
                        rs.getString("fuel_bucket_summary")

                );*/

                Generator gen = new Generator(
                        rs.getInt("asset_id"),
                        rs.getString("region"),
                        rs.getString("asset_type"),
                        rs.getString("site_name"),
                        rs.getString("owner_name"),
                        rs.getString("technology_type"),
                        rs.getString("fuel_type"),
                        rs.getDouble("nameplate_capacity_mw"),
                        rs.getString("dispatch_type"),
                        rs.getDate("full_commercial_use_date"),
                        rs.getDate("expected_closure_date"),
                        rs.getDate("closure_date"),
                        rs.getString("duid"),
                        rs.getInt("no_units"),
                        rs.getFloat("storage_capacity_mwh"),
                        rs.getString("fuel_bucket_summary")
                );

                int idGen = rs.getInt("asset_id");
                //Get from Map the vector of GENERATORS with key = idGen, and add the new Generator to the vector
                if (!data.getGen_register().containsKey(idGen))
                    data.getGen_register().put(idGen, new Vector<>());

                data.getGen_register().get(idGen).add(gen);

                data.setNumGenerators(data.getNumGenerators() + 1 );

                //Add gen to ArrayList<Generator>. This will be used in the constructor of SPM
                gens.add(gen);

            }
        } catch (SQLException e) {
            System.out.println(e.getMessage());
        }
        return gens;
    }


    public static ArrayList<Storage> selectStorage(Gr4spSim data, String idst) {
        String url = "jdbc:postgresql://localhost:5432/postgres?user=postgres"; //"jdbc:sqlite:Spm_archetypes.db";


        String sql = "SELECT storage_id, storage_name, storagetype, storageoutputcap , storagecapacity, ownership," +
                " storage_cyclelife, storage_costrange  FROM storage WHERE storage_id = '" + idst + "' ";
        ArrayList<Storage> strs = new ArrayList<Storage>();
        try (Connection conn = DriverManager.getConnection(url);
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            // loop through the result set
            while (rs.next()) {
                /*System.out.println("\t" + rs.getInt("storage_id") + "\t" +
                        rs.getString("storage_name") + "\t" +
                        rs.getInt("storageType") + "\t" +
                        rs.getDouble("storageOutputCap") + "\t" +
                        rs.getDouble("storageCapacity") + "\t" +
                        rs.getInt("Ownership") + "\t" +
                        rs.getDouble("storage_cycleLife") + "\t" +
                        rs.getDouble("storage_costRange"));
*/
                Storage str = new Storage(rs.getInt("storage_id"),
                        rs.getString("storage_name"),
                        rs.getInt("StorageType"),
                        rs.getDouble("storageOutputCap"),
                        rs.getDouble("storageCapacity"),
                        rs.getInt("Ownership"),
                        rs.getDouble("storage_cycleLife"),
                        rs.getDouble("storage_costRange"));

                data.setNumStorage(data.getNumStorage() + 1 );
                strs.add(str);
            }
        } catch (SQLException e) {
            System.out.println(e.getMessage());
        }
        return strs;
    }


    //Select and create the type of energy grid
    public static ArrayList<NetworkAssets> selectNetwork(Gr4spSim data, String id) {
        String url = "jdbc:postgresql://localhost:5432/postgres?user=postgres"; //"jdbc:sqlite:Spm_archetypes.db";


        String sql = "SELECT networkassets.id as netid, networkassettype.type as type, networkassettype.subtype as subtype, " +
                "networkassettype.grid as grid, assetname, grid_node_name, location_mb, gridlosses, gridvoltage, owner, startdate, enddate " +
                " FROM networkassets " +
                "JOIN networkassettype ON networkassets.assettype = networkassettype.id " +
                "and  networkassets.id = '" + id + "' ";
        ArrayList<NetworkAssets> nets = new ArrayList<NetworkAssets>();
        try (Connection conn = DriverManager.getConnection(url);
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            // loop through the result set
            while (rs.next()) {
               /* System.out.println("\t" + rs.getInt("netId") + "\t" +
                        rs.getString("type") + "\t" +
                        rs.getString("subtype") + "\t" +
                        rs.getString("grid") + "\t" +
                        rs.getString("assetName") + "\t" +
                        rs.getString("grid_node_name") + "\t" +
                        rs.getString("location_MB") + "\t" +
                        rs.getDouble("gridLosses") + "\t" +
                        rs.getInt("gridVoltage") + "\t" +
                        rs.getDate("startdate") + "\t" +
                        rs.getDate("enddate") + "\t" +
                        rs.getString("owner"));
*/
                int idNet = rs.getInt("netId");
                NetworkAssets grid = new NetworkAssets(idNet,
                        rs.getString("type"),
                        rs.getString("subtype"),
                        rs.getString("grid"),
                        rs.getString("assetName"),
                        rs.getString("grid_node_name"),
                        rs.getString("location_MB"),
                        rs.getDouble("gridLosses"),
                        rs.getInt("gridVoltage"),
                        rs.getString("owner"),
                        rs.getDate("startdate"),
                        rs.getDate("enddate"));
                nets.add(grid);

                //Get from Map the vector of Networks with key = idNet, and add the new Network to the vector
                if (!data.getNetwork_register().containsKey(idNet))
                    data.getNetwork_register().put(idNet, new Vector<>());

                data.getNetwork_register().get(idNet).add(grid);

            }
        } catch (SQLException e) {
            System.out.println(e.getMessage());
        }
        return nets;
    }


    //creation of random interface or connection point. This interface changes as well depending on the scale of study. A house with
    //own gene4ration off the grid will have the same house (smart metered or not) as connection point, a neighborhood can have one or more feeders and sub-stations,
    //it is basically the closest point where supply meets demand without going through any significant extra technological "treatment"
    //TODO knowledge and energy hubs within prosumer communities, where to include them?

    public static ArrayList<ConnectionPoint> selectConnectionPoint(Gr4spSim data, String name) {
        String url = "jdbc:postgresql://localhost:5432/postgres?user=postgres"; //"jdbc:sqlite:Spm_archetypes.db";


        String sql = "SELECT cpoint_id, cpoint_name, cpoint_type, distancetodemand, cpoint_locationcode, cpoint_owner, ownership FROM connectionpoint WHERE cpoint_name = '" + name + "' ";
        ArrayList<ConnectionPoint> cpoints = new ArrayList<ConnectionPoint>();
        try (Connection conn = DriverManager.getConnection(url);
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            // loop through the result set
            while (rs.next()) {
               /* System.out.println("\t" + rs.getInt("cpoint_id") + "\t" +
                        rs.getString("cpoint_name") + "\t" +
                        rs.getInt("CPoint_Type") + "\t" +
                        rs.getDouble("distanceToDemand") + "\t" +
                        rs.getInt("cpoint_locationCode") + "\t" +
                        rs.getString("cpoint_owner") + "\t" +
                        rs.getInt("Ownership"));*/

                ConnectionPoint cpoint = new ConnectionPoint(rs.getInt("cpoint_id"),
                        rs.getString("cpoint_name"),
                        rs.getInt("CPoint_Type"),
                        rs.getDouble("distanceToDemand"),
                        rs.getInt("cpoint_locationCode"),
                        rs.getString("cpoint_owner"),
                        rs.getInt("Ownership"));

                data.setNumcpoints(data.getNumcpoints() + 1 );
                cpoints.add(cpoint);

            }
        } catch (SQLException e) {
            System.out.println(e.getMessage());
        }
        return cpoints;
    }

    public static void
    selectArenaAndContracts(Gr4spSim data, String startDate, String endDate) {
        String url = "jdbc:postgresql://localhost:5432/postgres?user=postgres"; //"jdbc:sqlite:Spm_archetypes.db";

        SimpleDateFormat stringToDate = new SimpleDateFormat("yyyy-MM-dd");


        String sql = "SELECT arenas.name as arenaname, arenas.type as arenatype, seller, buyer, assetused, pricemwh, startdate, enddate, capacitycontracted, arenaid" +
                " FROM contracts, arenas WHERE " +
                "contracts.arenaid = arenas.id AND contracts.startdate <= '" + endDate + "'" +
                " AND contracts.enddate > '" + startDate + "';";


        try (Connection conn = DriverManager.getConnection(url);
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            // loop through the result set
            while (rs.next()) {
                System.out.println("\t" + rs.getInt("arenaid") + "\t" +
                        rs.getString("arenaname") + "\t" +
                        rs.getString("arenatype") + "\t" +
                        rs.getString("seller") + "\t" +
                        rs.getString("buyer") + "\t" +
                        rs.getString("pricemwh") + "\t" +
                        rs.getString("assetused") + "\t" +
                        rs.getDate("startdate") + "\t" +
                        rs.getDate("enddate") + "\t" +
                        rs.getString("capacitycontracted"));


                int arenaId = rs.getInt("arenaid");

                //If arena doesn't exist, create it
                if (!data.getArena_register().containsKey(arenaId)) {
                    Arena arena = new Arena(arenaId, rs.getString("arenaname"), rs.getString("arenatype"));
                    data.getArena_register().put(arenaId, arena);
                }

                Arena arena = data.getArena_register().get(arenaId);

                //Actor seller, Actor buyer, Asset assetUsed, float dollarMWh, Date start, Date end, float capacityContracted

                Contract contract = new Contract("",
                        rs.getInt("seller"),
                        rs.getInt("buyer"),
                        rs.getInt("assetused"),
                        rs.getFloat("pricemwh"),
                        (float) 0.0,
                        rs.getDate("startdate"),
                        rs.getDate("enddate"),
                        rs.getFloat("capacitycontracted"));


                //Add contract to arena
                if (arena.getType().equalsIgnoreCase("OTC") || arena.getType().equalsIgnoreCase("Retail")) {
                    arena.getBilateral().add(contract);
                }
                if (arena.getType().equalsIgnoreCase("FiTs")) {
                    arena.getFiTs().add(contract);
                }

            }
        } catch (SQLException e) {
            System.out.println(e.getMessage());
        }
    }

}
