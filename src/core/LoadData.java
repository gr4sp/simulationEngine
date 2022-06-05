package core;

import core.Relationships.*;
import core.Social.*;
import core.Technical.*;

import java.io.FileWriter;
import java.io.IOException;
import java.sql.*;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.Date;

import static java.lang.System.exit;

//Class to load all the data to start the simulation
public class LoadData implements java.io.Serializable {

    // Functions to convert readings from text in DB to Actor type, etc.
    public static ActorAssetRelationshipType stringToActorAssetTypeRelType(String actorAssetRelType) {

        if (actorAssetRelType.equalsIgnoreCase("OWN"))
            return ActorAssetRelationshipType.OWN;
        if (actorAssetRelType.equalsIgnoreCase("USE"))
            return ActorAssetRelationshipType.USE;
        if (actorAssetRelType.equalsIgnoreCase("LEASE"))
            return ActorAssetRelationshipType.LEASE;
        return ActorAssetRelationshipType.OTHER;
    }

    public static void selectInflation(Gr4spSim data) {
        // Loading Inflation data (from 1990 to 2019)
        String url = "jdbc:postgresql://localhost:543" + String.valueOf(data.db_id)
                + "/postgres?user=postgres&password=password"; // "jdbc:sqlite:Spm_archetypes.db";

        String sql = "SELECT  year, average FROM  historic_inflation";

        try (Connection conn = DriverManager.getConnection(url);
                Statement stmt = conn.createStatement();
                ResultSet rs = stmt.executeQuery(sql)) {

            // loop through the result set
            while (rs.next()) {
                int year = rs.getInt("year");
                Float inflation = rs.getFloat("average");
                data.LOGGER.info("\t" + "Inflation rate" + "\t" +
                        year + "\t" +
                        inflation + "\n");

                data.getAnnual_inflation().put(year, inflation);
            }
        } catch (SQLException e) {
            data.LOGGER.warning(e.getMessage());
            // System.out.println("selectInflation");
        }

    }

    // Select Forecast ISP annual consumption (GWh)
    // Not dependent on anything
    public static void selectForecastConsumption(Gr4spSim data) {
        String url = "jdbc:postgresql://localhost:543" + String.valueOf(data.db_id)
                + "/postgres?user=postgres&password=password"; // "jdbc:sqlite:Spm_archetypes.db";

        SimpleDateFormat stringToDate = new SimpleDateFormat("yyyy-MM-dd");

        String sql = "SELECT isp_annual_consumption.region as consumptionForecastRegion, isp_annual_consumption.year as consumptionForecastYear, "
                +
                "isp_annual_consumption.category as consumptionForecastCategory, isp_annual_consumption.subcategory as consumptionForecastSubCategory, "
                +
                "isp_annual_consumption.scenario as consumptionForecastScenario, isp_annual_consumption.annual_consumption as annualConsumptionForecast "
                +
                "FROM  isp_annual_consumption " +
                "WHERE (category = 'Operational' OR category = 'PriceImpact' OR category ='EnergyEfficiency' OR category = 'SmallNonScheduledGeneration' OR category = 'RooftopPV') "
                +
                "AND region = 'VIC' " +
                "AND (scenario = 'Actual' OR scenario ='" + data.settingsAfterBaseYear.getForecastScenarioConsumption()
                + "') ";
        try (Connection conn = DriverManager.getConnection(url);
                Statement stmt = conn.createStatement();
                ResultSet rs = stmt.executeQuery(sql)) {

            // loop through the result set
            while (rs.next()) {
                int year = rs.getInt("consumptionForecastYear");
                float gwh = rs.getFloat("annualConsumptionForecast");
                data.LOGGER.info("\t" + rs.getString("consumptionForecastRegion") + "\t" +
                        year + "\t" +
                        rs.getString("consumptionForecastCategory") + "\t" +
                        rs.getString("consumptionForecastSubCategory") + "\t" +
                        rs.getString("consumptionForecastScenario") + "\t" +
                        gwh);

                // If year consumption forecast doesn't exist, create it
                if (!data.getAnnual_forecast_consumption_register().containsKey(year)) {
                    data.getAnnual_forecast_consumption_register().put(year, gwh);
                } else {
                    // Sum the existing category forecast with the new value
                    // (Operational,PriceImpact, etc.)
                    float existingGWh = data.getAnnual_forecast_consumption_register().get(year);
                    data.getAnnual_forecast_consumption_register().put(year, gwh + existingGWh);
                }
            }
        } catch (SQLException e) {
            data.LOGGER.warning(e.getMessage());
        }
    }

    public static void selectForecastEnergyEfficency(Gr4spSim data) {
        String url = "jdbc:postgresql://localhost:543" + String.valueOf(data.db_id)
                + "/postgres?user=postgres&password=password"; // "jdbc:sqlite:Spm_archetypes.db";

        SimpleDateFormat stringToDate = new SimpleDateFormat("yyyy-MM-dd");

        String sql = "SELECT isp_annual_consumption.region as consumptionForecastRegion, isp_annual_consumption.year as consumptionForecastYear, "
                +
                "isp_annual_consumption.category as consumptionForecastCategory, isp_annual_consumption.subcategory as consumptionForecastSubCategory, "
                +
                "isp_annual_consumption.scenario as consumptionForecastScenario, isp_annual_consumption.annual_consumption as annualConsumptionForecast "
                +
                "FROM  isp_annual_consumption " +
                "WHERE (category ='EnergyEfficiency') " +
                "AND region = 'VIC' " +
                "AND (scenario ='" + data.settingsAfterBaseYear.getForecastScenarioEnergyEfficiency() + "') ";
        try (Connection conn = DriverManager.getConnection(url);
                Statement stmt = conn.createStatement();
                ResultSet rs = stmt.executeQuery(sql)) {

            // loop through the result set
            while (rs.next()) {
                int year = rs.getInt("consumptionForecastYear");
                float gwh = rs.getFloat("annualConsumptionForecast");
                data.LOGGER.info("\t" + rs.getString("consumptionForecastRegion") + "\t" +
                        year + "\t" +
                        rs.getString("consumptionForecastCategory") + "\t" +
                        rs.getString("consumptionForecastSubCategory") + "\t" +
                        rs.getString("consumptionForecastScenario") + "\t" +
                        gwh);

                // Subtract Energy Efficiency from Consumption
                float existingGWh = data.getAnnual_forecast_consumption_register().get(year);
                data.getAnnual_forecast_consumption_register().put(year, existingGWh - gwh);

            }
        } catch (SQLException e) {
            data.LOGGER.warning(e.getMessage());
            // System.out.println("selectForecastEfficient");
        }
    }

    public static void selectForecastOnsiteGeneration(Gr4spSim data) {
        String url = "jdbc:postgresql://localhost:543" + String.valueOf(data.db_id)
                + "/postgres?user=postgres&password=password"; // "jdbc:sqlite:Spm_archetypes.db";

        SimpleDateFormat stringToDate = new SimpleDateFormat("yyyy-MM-dd");

        String sql = "SELECT isp_annual_consumption.region as consumptionForecastRegion, isp_annual_consumption.year as consumptionForecastYear, "
                +
                "isp_annual_consumption.category as consumptionForecastCategory, isp_annual_consumption.subcategory as consumptionForecastSubCategory, "
                +
                "isp_annual_consumption.scenario as consumptionForecastScenario, isp_annual_consumption.annual_consumption as annualConsumptionForecast "
                +
                "FROM  isp_annual_consumption " +
                "WHERE (category ='SmallNonScheduledGeneration') " +
                "AND region = 'VIC' " +
                "AND (scenario ='" + data.settingsAfterBaseYear.getForecastScenarioOnsiteGeneration() + "') ";
        try (Connection conn = DriverManager.getConnection(url);
                Statement stmt = conn.createStatement();
                ResultSet rs = stmt.executeQuery(sql)) {

            // loop through the result set
            while (rs.next()) {
                int year = rs.getInt("consumptionForecastYear");
                float gwh = rs.getFloat("annualConsumptionForecast");
                data.LOGGER.info("\t" + rs.getString("consumptionForecastRegion") + "\t" +
                        year + "\t" +
                        rs.getString("consumptionForecastCategory") + "\t" +
                        rs.getString("consumptionForecastSubCategory") + "\t" +
                        rs.getString("consumptionForecastScenario") + "\t" +
                        gwh);

                // Subtract SmallNonScheduledGeneration from Consumption
                float existingGWh = data.getAnnual_forecast_consumption_register().get(year);
                data.getAnnual_forecast_consumption_register().put(year, existingGWh - gwh);

            }
        } catch (SQLException e) {
            // System.out.println("selectForecastOnsiteGeneration");
            data.LOGGER.warning(e.getMessage());
        }
    }

    public static void selectForecastSolarUptake(Gr4spSim data) {
        String url = "jdbc:postgresql://localhost:543" + String.valueOf(data.db_id)
                + "/postgres?user=postgres&password=password"; // "jdbc:sqlite:Spm_archetypes.db";

        SimpleDateFormat stringToDate = new SimpleDateFormat("yyyy-MM-dd");

        String sql = "SELECT isp_annual_consumption.region as consumptionForecastRegion, isp_annual_consumption.year as consumptionForecastYear, "
                +
                "isp_annual_consumption.category as consumptionForecastCategory, isp_annual_consumption.subcategory as consumptionForecastSubCategory, "
                +
                "isp_annual_consumption.scenario as consumptionForecastScenario, isp_annual_consumption.annual_consumption as annualConsumptionForecast "
                +
                "FROM  isp_annual_consumption " +
                "WHERE (category = 'RooftopPV') " +
                "AND region = 'VIC' " +
                "AND (scenario ='" + data.settingsAfterBaseYear.getForecastScenarioSolarUptake() + "') ";

        try (Connection conn = DriverManager.getConnection(url);
                Statement stmt = conn.createStatement();
                ResultSet rs = stmt.executeQuery(sql)) {

            // loop through the result set
            while (rs.next()) {
                int year = rs.getInt("consumptionForecastYear");
                float gwh = rs.getFloat("annualConsumptionForecast");
                data.LOGGER.info("\t" + rs.getString("consumptionForecastRegion") + "\t" +
                        year + "\t" +
                        rs.getString("consumptionForecastCategory") + "\t" +
                        rs.getString("consumptionForecastSubCategory") + "\t" +
                        rs.getString("consumptionForecastScenario") + "\t" +
                        gwh);

                // solar pv installations has been higher than predicted
                // Increase rooftopPV by 25% to concile the latest info in pv-map. ISP used
                // solar forecast from CSRIO model and real data on
                gwh *= 1.25;
                float rooftopPvGwh = gwh;

                if (data.settingsAfterBaseYear.getRooftopPVForecast().equals("both") == true) {
                    rooftopPvGwh = gwh;
                } else if (rs.getString("consumptionForecastSubCategory").equals("Residential") == true &&
                        data.settingsAfterBaseYear.getRooftopPVForecast().equals("residential") == false) {
                    rooftopPvGwh = 0;
                } else if (rs.getString("consumptionForecastSubCategory").equals("Business") == true &&
                        data.settingsAfterBaseYear.getRooftopPVForecast().equals("business") == false) {
                    rooftopPvGwh = 0;
                }

                if (!data.getAnnual_forecast_rooftopPv_register().containsKey(year)) {
                    data.getAnnual_forecast_rooftopPv_register().put(year, rooftopPvGwh);
                } else {

                    // Sum the existing category forecast with the new value
                    // (Operational,PriceImpact, etc.)
                    float existingGWh = data.getAnnual_forecast_rooftopPv_register().get(year);
                    data.getAnnual_forecast_rooftopPv_register().put(year, rooftopPvGwh + existingGWh);

                }

            }
        } catch (SQLException e) {
            // System.out.println("selectForecastSolarUptake");
            data.LOGGER.warning(e.getMessage());
        }
    }

    // Select Forecast ISP maximum demand (MW)
    public static void selectMaximumDemandForecast(Gr4spSim data) {
        String url = "jdbc:postgresql://localhost:543" + String.valueOf(data.db_id)
                + "/postgres?user=postgres&password=password"; // "jdbc:sqlite:Spm_archetypes.db";

        SimpleDateFormat stringToDate = new SimpleDateFormat("yyyy-MM-dd");

        String sql = "SELECT isp_maximum_demand.region as maxDemandForecastRegion, isp_maximum_demand.year as maxDemandForecastYear, "
                +
                "isp_maximum_demand.category as maxDemandForecastCategory, isp_maximum_demand.subcategory as maxDemandForecastSubCategory, "
                +
                "isp_maximum_demand.scenario as maxDemandForecastScenario, isp_maximum_demand.maximum_demand as maxDemandForecast "
                +
                "FROM  isp_maximum_demand";

        try (Connection conn = DriverManager.getConnection(url);
                Statement stmt = conn.createStatement();
                ResultSet rs = stmt.executeQuery(sql)) {

            // loop through the result set
            while (rs.next()) {
                data.LOGGER.info("\t" + rs.getString("maxDemandForecastRegion") + "\t" +
                        rs.getInt("maxDemandForecastYear") + "\t" +
                        rs.getString("maxDemandForecastCategory") + "\t" +
                        rs.getString("maxDemandForecastSubCategory") + "\t" +
                        rs.getString("maxDemandForecastScenario") + "\t" +
                        rs.getFloat("maxDemandForecast"));

            }
        } catch (SQLException e) {
            data.LOGGER.warning(e.getMessage());
        }
    }

    // Select Forecast ISP minimum demand (MW)
    public static void selectMinimumDemandForecast(Gr4spSim data) {
        String url = "jdbc:postgresql://localhost:543" + String.valueOf(data.db_id)
                + "/postgres?user=postgres&password=password"; // "jdbc:sqlite:Spm_archetypes.db";

        SimpleDateFormat stringToDate = new SimpleDateFormat("yyyy-MM-dd");

        String sql = "SELECT isp_minimum_demand.region as minDemandForecastRegion, isp_minimum_demand.year as minDemandForecastYear, isp_minimum_demand.category as minDemandForecastCategory, isp_minimum_demand.subcategory as minDemandForecastSubCategory, isp_minimum_demand.scenario as minDemandForecastScenario, isp_minimum_demand.maximum_demand as minDemandForecast FROM  isp_minimum_demand";

        try (Connection conn = DriverManager.getConnection(url);
                Statement stmt = conn.createStatement();
                ResultSet rs = stmt.executeQuery(sql)) {

            // loop through the result set
            while (rs.next()) {
                data.LOGGER.info("\t" + rs.getString("minDemandForecastRegion") + "\t" +
                        rs.getInt("minDemandForecastYear") + "\t" +
                        rs.getString("minDemandForecastCategory") + "\t" +
                        rs.getString("minDemandForecastSubCategory") + "\t" +
                        rs.getString("minDemandForecastScenario") + "\t" +
                        rs.getFloat("minDemandForecast"));

            }
        } catch (SQLException e) {
            data.LOGGER.warning(e.getMessage());
        }
    }
    // Select Arena Function

    public static void selectArena(Gr4spSim data) {
        String url = "jdbc:postgresql://localhost:543" + String.valueOf(data.db_id)
                + "/postgres?user=postgres&password=password"; // "jdbc:sqlite:Spm_archetypes.db";

        SimpleDateFormat stringToDate = new SimpleDateFormat("yyyy-MM-dd");

        String sql = "SELECT arenas.name as arenaname, arenas.type as arenatype, id" +
                " FROM  arenas ;";

        try (Connection conn = DriverManager.getConnection(url);
                Statement stmt = conn.createStatement();
                ResultSet rs = stmt.executeQuery(sql)) {

            // loop through the result set
            while (rs.next()) {
                data.LOGGER.info("\t" + rs.getInt("id") + "\t" +
                        rs.getString("arenaname") + "\t" +
                        rs.getString("arenatype"));

                int arenaId = rs.getInt("id");

                // If arena doesn't exist, create it
                if (!data.getArena_register().containsKey(arenaId)) {
                    Arena arena = new Arena(arenaId, rs.getString("arenaname"), rs.getString("arenatype"), data);
                    data.getArena_register().put(arenaId, arena);
                }

            }
        } catch (SQLException e) {
            data.LOGGER.warning(e.getMessage());
        }
    }

    // Select tariffs function

    public static void selectTariffs(Gr4spSim data, String startDate, String endDate, String areaCode) {
        String url = "jdbc:postgresql://localhost:543" + String.valueOf(data.db_id)
                + "/postgres?user=postgres&password=password"; // "jdbc:sqlite:Spm_archetypes.db";

        SimpleDateFormat stringToDate = new SimpleDateFormat("yyyy-MM-dd");

        String sql = "SELECT date, conversion_variable" +
                " FROM cpi_conversion;";

        // Loading CPI Conversion data
        try (Connection conn = DriverManager.getConnection(url);
                Statement stmt = conn.createStatement();
                ResultSet rs = stmt.executeQuery(sql)) {

            // loop through the result set
            while (rs.next()) {
                data.LOGGER.info("\t" + rs.getString("date") + "\t" +
                        rs.getFloat("conversion_variable"));

                java.util.Date date = rs.getDate("date");
                Float conversion_rate = rs.getFloat("conversion_variable");

                data.getCpi_conversion().put(date, conversion_rate);

            }
        } catch (SQLException e) {
            data.LOGGER.warning(e.getMessage());
        }

        createCPIForecast(data);

        sql = "SELECT id, casestudy_area,casestudy_area_code, date, organisation_tariff_name, average_dckwh" +
                " FROM tariffshistoric WHERE" +
                " casestudy_area_code SIMILAR TO '" + areaCode + "%' " +
                " AND date <= '" + endDate + "'" +
                " AND date >= '" + startDate + "';";

        // If areaCode is at state scale, do not filter tariffs by smaller areacodes,
        // take all tariffs available
        // Agent will select a tariff according to simulation policy. For more details
        // See SimulParameters()
        if (areaCode.equalsIgnoreCase("VIC"))
            sql = "SELECT id, casestudy_area, casestudy_area_code, date, organisation_tariff_name, average_dckwh" +
                    " FROM tariffshistoric WHERE" +
                    " date <= '" + endDate + "'" +
                    " AND date >= '" + startDate + "';";

        try (Connection conn = DriverManager.getConnection(url);
                Statement stmt = conn.createStatement();
                ResultSet rs = stmt.executeQuery(sql)) {

            // loop through the result set
            while (rs.next()) {
                data.LOGGER.info("\t" + rs.getInt("id") + "\t" +
                        rs.getString("casestudy_area") + "\t" +
                        rs.getString("casestudy_area_code") + "\t" +
                        rs.getString("date") + "\t" +
                        rs.getString("organisation_tariff_name") + "\t" +
                        rs.getString("average_dckwh") + "\t");

                // add tariffs to the Retail arena
                int arenaId = 2;

                Arena arena = data.getArena_register().get(arenaId);

                java.util.Date cStartDate = rs.getDate("date");

                // Compute end Date 1 year after
                Calendar cEndDate = Calendar.getInstance();
                try {
                    Date date = stringToDate.parse(rs.getString("date"));
                    cEndDate.setTime(date);
                } catch (ParseException e) {
                    System.err.println("Cannot parse Start Date: " + e.toString());
                }
                cEndDate.add(Calendar.YEAR, 1);

                // Get CPI conversion
                float conversion_rate = data.getCpi_conversion().get(cStartDate);

                Contract contract = new Contract(
                        rs.getString("organisation_tariff_name"),
                        Arena.EndConsumer,
                        rs.getFloat("average_dckwh") * conversion_rate,
                        cStartDate,
                        cEndDate.getTime());

                // Add contract to arena
                if (arena.getType().equalsIgnoreCase("OTC") || arena.getType().equalsIgnoreCase("Retail")) {
                    arena.getBilateral().add(contract);
                }
                if (arena.getType().equalsIgnoreCase("FiTs")) {
                    arena.getFiTs().add(contract);
                }

            }
        } catch (SQLException e) {
            data.LOGGER.warning(e.getMessage());
        }

        // Load Tariff percentage contribution wholesale

        sql = "SELECT year, wholesale" +
                " FROM historic_tariff_contribution;";

        try (Connection conn = DriverManager.getConnection(url);
                Statement stmt = conn.createStatement();
                ResultSet rs = stmt.executeQuery(sql)) {

            // loop through the result set
            while (rs.next()) {
                data.LOGGER.info("\t" + rs.getInt("year") + "\t" +
                        rs.getFloat("wholesale"));

                int year = rs.getInt("year");
                float wholesale = rs.getFloat("wholesale");

                data.getTariff_contribution_wholesale_register().put(year, wholesale);

            }
        } catch (SQLException e) {
            data.LOGGER.warning(e.getMessage());
        }
    }

    // Select Demand Historic half hours for spot market
    // Not dependent on anything
    public static void selectDemandHalfHour(Gr4spSim data, String startDate, String endDate) {
        String url = "jdbc:postgresql://localhost:543" + String.valueOf(data.db_id)
                + "/postgres?user=postgres&password=password"; // "jdbc:sqlite:Spm_archetypes.db";

        /**
         * Load Total Consumption per month
         */
        String sql = "SELECT settlement_date, total_demand, price" +
                " FROM  total_demand_halfhour WHERE " +
                " settlement_date <= '" + endDate + "'" +
                " AND settlement_date >= '" + startDate + "';";

        try (Connection conn = DriverManager.getConnection(url);
                Statement stmt = conn.createStatement();
                ResultSet rs = stmt.executeQuery(sql)) {

            // loop through the result set
            while (rs.next()) {
                data.LOGGER.info("\t" + rs.getTimestamp("settlement_date") + "\t" +
                        rs.getFloat("total_demand") + "\t" +
                        rs.getFloat("price"));

                Date d = rs.getTimestamp("settlement_date");
                Double mw = rs.getDouble("total_demand");

                // If half hour consumption doesn't exist, create it
                if (!data.getHalfhour_demand_register().containsKey(d)) {
                    data.getHalfhour_demand_register().put(d, mw);
                }

            }
        } catch (SQLException e) {
            data.LOGGER.warning(e.getMessage());
            exit(1);
        }
    }

    // Aggregates to custom demand window
    public static void convertDemandHalfHourCustom(Gr4spSim data) {

        Date curr = null;

        HashMap<Date, Integer> count = new HashMap<>();
        for (HashMap.Entry<Date, Double> entry : data.getHalfhour_demand_register().entrySet()) {
            curr = (Date) entry.getKey().clone();
            curr.setMinutes(0);
            int hour = curr.getHours();

            hour = hour - (hour % data.merit_freq);
            curr.setHours(hour);

            Date newKey = new Date(curr.getTime());

            if (data.getDaily_demand_register().containsKey(newKey)) {
                data.getDaily_demand_register().put(newKey,
                        data.getDaily_demand_register().get(newKey) + entry.getValue());

                count.put(newKey, count.get(newKey) + 1);

            } else {
                data.getDaily_demand_register().put(newKey, entry.getValue());
                count.put(newKey, 1);
            }
        }

        // Average out hour
        if (data.setAve) {
            for (Date d : data.getDaily_demand_register().keySet()) {
                data.getDaily_demand_register().put(d, data.getDaily_demand_register().get(d) / count.get(d));
            }
        }

    }

    // Aggregates to custom demand window
    public static void convertSolarHalfHourCustom(Gr4spSim data) {

        Date curr = null;

        // How many hours between merit

        HashMap<Date, Integer> count = new HashMap<>();
        for (HashMap.Entry<Date, Float> entry : data.getHalfhour_solar_exposure().entrySet()) {
            curr = (Date) entry.getKey().clone();
            curr.setMinutes(0);
            int hour = curr.getHours();

            hour = hour - (hour % data.merit_freq);
            curr.setHours(hour);

            Date newKey = new Date(curr.getTime());

            if (data.getDaily_solar_exposure().containsKey(newKey)) {
                data.getDaily_solar_exposure().put(newKey,
                        data.getDaily_solar_exposure().get(newKey) + entry.getValue());

                count.put(newKey, count.get(newKey) + 1);

            } else {
                data.getDaily_solar_exposure().put(newKey, entry.getValue());
                count.put(newKey, 1);
            }
        }

        // Average out hour
        // if (data.setAve){
        for (Date d : data.getDaily_solar_exposure().keySet()) {
            data.getDaily_solar_exposure().put(d, data.getDaily_solar_exposure().get(d) / count.get(d));
        }
        // }

    }

    // Select Consumption
    public static void createDemandForecast(Gr4spSim data) {

        // Set Base Date
        Date baseDate = null;
        int baseYear = data.settings.getBaseYearConsumptionForecast();

        SimpleDateFormat stringToDate = new SimpleDateFormat("yyyy-MM-dd");
        String dateInString = baseYear + "-01-01";
        try {
            baseDate = stringToDate.parse(dateInString);
        } catch (java.text.ParseException e) {
            data.LOGGER.warning(e.toString());
        }

        GregorianCalendar cal = (GregorianCalendar) GregorianCalendar.getInstance();
        Calendar c = Calendar.getInstance();
        Calendar fc = Calendar.getInstance();

        c.setTime(data.getEndSimDate());
        int endYear = c.get(Calendar.YEAR) - 1;

        c.setTime(baseDate);

        while (baseYear < endYear) {
            float baseConsumption = data.getAnnual_forecast_consumption_register().get(baseYear);
            float nextConsumption = data.getAnnual_forecast_consumption_register().get(baseYear + 1);
            float percentageChange = (nextConsumption - baseConsumption) / baseConsumption;

            // Leap year day
            Date leapYearFeb = null;
            dateInString = baseYear + "-02-29";
            boolean isLeap = cal.isLeapYear(baseYear);
            if (isLeap) {
                try {
                    leapYearFeb = stringToDate.parse(dateInString);
                } catch (java.text.ParseException e) {
                    data.LOGGER.warning(e.toString());
                }
            }

            // Set the month to January for Demand forecast
            c.set(Calendar.MONTH, 0);
            double mw = 0.0;
            while (c.get(Calendar.YEAR) == baseYear) {
                baseDate = c.getTime();

                // Truncate time to get the correct data in leap year 29th of Feb
                c.set(Calendar.MINUTE, 0);
                c.set(Calendar.HOUR_OF_DAY, 0);
                Date alwaysMidnight = c.getTime();
                c.setTime(baseDate);

                // ForecastDemand Half Hour
                if (data.getHalfhour_demand_register().containsKey(baseDate)) {
                    mw = data.getHalfhour_demand_register().get(baseDate);
                }

                // If the basis is a leap year, then fill it with the info from the day before,
                // And do not try to copy info from 29th of Feb to the next year forecast as it
                // will fail
                if (leapYearFeb != null && alwaysMidnight.compareTo(leapYearFeb) == 0) {
                    c.add(Calendar.DAY_OF_MONTH, -1);
                    Date yesterdayDate = c.getTime();
                    double yesterdayMw = data.getHalfhour_demand_register().get(yesterdayDate);
                    data.getHalfhour_demand_register().put(baseDate, yesterdayMw);
                    c.add(Calendar.DAY_OF_MONTH, 1);
                } else {
                    double forecastedMw = mw + (mw * percentageChange);

                    // Set the 1 year forecast date
                    fc.setTime(c.getTime());
                    fc.add(Calendar.YEAR, 1);
                    Date forecastedDate = fc.getTime();

                    data.getHalfhour_demand_register().put(forecastedDate, forecastedMw);
                }

                // Set the month for the forecast
                c.add(Calendar.MINUTE, 30);

            }

            baseYear++;

        }
    }

    // Create CPI forecast
    public static void createCPIForecast(Gr4spSim data) {

        // Set Base Date
        Date baseDate = null;
        int baseYear = data.settings.getBaseYearConsumptionForecast();

        SimpleDateFormat stringToDate = new SimpleDateFormat("yyyy-MM-dd");
        String dateInString = baseYear + "-01-01";
        try {
            baseDate = stringToDate.parse(dateInString);
        } catch (java.text.ParseException e) {
            data.LOGGER.warning(e.toString());
        }
        Calendar c = Calendar.getInstance();

        c.setTime(data.getEndSimDate());
        int endYear = c.get(Calendar.YEAR) - 1;

        c.setTime(baseDate);

        // This is applied to the tariffs (see EndUser class). If price rises in year
        // baseYear+1 is 2%, then annualCpi should be 0.02.
        // AnnualCPI corrects the value given as inflation below.
        while (baseYear < endYear) {
            float baseCPI = data.getCpi_conversion().get(baseDate);
            float forecastedCPI = baseCPI * (1 - (float) data.settingsAfterBaseYear.getAnnualCpiForecast());

            // Add 1 year to the base reference date
            c.add(Calendar.YEAR, 1);
            Date forecastedDate = c.getTime();

            data.getCpi_conversion().put(forecastedDate, forecastedCPI);

            baseDate = c.getTime();
            baseYear++;

        }
    }

    // Increase solar capacity from forecast, dividing the increased amount of KW
    // across 12 months every year
    public static void createSolarInstallationForecast(Gr4spSim data) {

        // Set Base Date
        Date baseDate = null;
        int baseYear = 2020;

        SimpleDateFormat stringToDate = new SimpleDateFormat("yyyy-MM-dd");
        String dateInString = baseYear + "-01-01";
        try {
            baseDate = stringToDate.parse(dateInString);
        } catch (java.text.ParseException e) {
            data.LOGGER.warning(e.toString());
        }

        GregorianCalendar cal = (GregorianCalendar) GregorianCalendar.getInstance();
        Calendar c = Calendar.getInstance();
        Calendar fc = Calendar.getInstance();

        c.setTime(baseDate);

        while (baseYear < 2050) {
            float baseGwh = data.getAnnual_forecast_rooftopPv_register().get(baseYear);
            float nextGwh = data.getAnnual_forecast_rooftopPv_register().get(baseYear + 1);
            float percentageChange = (nextGwh - baseGwh) / baseGwh;
            float increasedGwh = nextGwh - baseGwh;
            float inceasedKW = (increasedGwh / 8760) * 1000000; // divided #hours in a year, and transform to KW from GW
            inceasedKW /= 0.20; // divided the avd generation factor of RooftopPV

            for (int month = 0; month < 12; month++) {

                // Set the month for the forecast
                c.set(Calendar.MONTH, month);
                baseDate = c.getTime();

                Integer aggregated_capacity_kw = (int) inceasedKW / 12;
                Float system_capacity = (float) data.settings.getForecastSolarInstallCapacity();
                Integer number_installations = (int) (aggregated_capacity_kw / system_capacity);

                // Set the 1 year forecast date
                c.add(Calendar.YEAR, 1);
                Date forecastedDate = c.getTime();
                c.add(Calendar.YEAR, -1);

                data.getSolar_number_installs().put(forecastedDate, number_installations);
                data.getSolar_aggregated_kw().put(forecastedDate, aggregated_capacity_kw);
                data.getSolar_system_capacity_kw().put(forecastedDate, system_capacity);

            }

            c.add(Calendar.YEAR, 1);
            baseYear++;

        }
    }

    public static void createForecastDomesticConsumers(Gr4spSim data) {
        String url = "jdbc:postgresql://localhost:543" + String.valueOf(data.db_id)
                + "/postgres?user=postgres&password=password"; // "jdbc:sqlite:Spm_archetypes.db";

        SimpleDateFormat stringToDate = new SimpleDateFormat("yyyy-MM-dd");

        /**
         * Load Forecast domestic consumers
         */
        String sql = "SELECT year, all_household_types" +
                " FROM  household_forecast_victoria WHERE " +
                " region = '" + data.settings.getAreaCode() + "';";

        ArrayList<Map.Entry<Integer, Integer>> forecast = new ArrayList<>();

        try (Connection conn = DriverManager.getConnection(url);
                Statement stmt = conn.createStatement();
                ResultSet rs = stmt.executeQuery(sql)) {

            // loop through the result set
            while (rs.next()) {
                /*
                 * data.LOGGER.info("\t" + rs.getString("date") + "\t" +
                 * rs.getString("domesticconsumers"));
                 */

                int year = rs.getInt("year");
                int consumers = rs.getInt("all_household_types");

                forecast.add(Map.entry(year, consumers));
                // data.getMonthly_domestic_consumers_register().put(date, consumers);

            }
        } catch (SQLException e) {
            data.LOGGER.warning(e.getMessage());
        }

        // Create linear interpolation of the forecasted years
        for (int i = 0; i < forecast.size() - 1; i++) {

            int j = i + 1;
            int firstYear = forecast.get(i).getKey();
            int nextYear = forecast.get(j).getKey();

            int firstConsumers = forecast.get(i).getValue();
            int nextConsumers = forecast.get(j).getValue();

            // Get growth per year in order to interpolate
            float growthPerYear = (nextConsumers - firstConsumers) / (nextYear - firstYear);

            // Interpolate from firstYear until NextYear
            int consumers = firstConsumers;
            for (int year = firstYear; year <= nextYear; year++) {

                // Set Date
                Date date = null;
                String dateInString = year + "-01-01";
                try {
                    date = stringToDate.parse(dateInString);
                } catch (java.text.ParseException e) {
                    data.LOGGER.warning(e.toString());
                }

                data.getMonthly_domestic_consumers_register().put(date, consumers);

                consumers += growthPerYear;
            }
        }
    }

    // Select Consumption
    public static void createHalfHourSolarExposureForecast(Gr4spSim data) {

        // Set Base Date
        Date baseDate = null;
        int baseYear = data.settings.getBaseYearConsumptionForecast();

        SimpleDateFormat stringToDate = new SimpleDateFormat("yyyy-MM-dd");
        String dateInString = baseYear + "-01-01";
        try {
            baseDate = stringToDate.parse(dateInString);
        } catch (java.text.ParseException e) {
            data.LOGGER.warning(e.toString());
        }

        GregorianCalendar cal = (GregorianCalendar) GregorianCalendar.getInstance();
        Calendar c = Calendar.getInstance();
        Calendar fc = Calendar.getInstance();

        c.setTime(data.getEndSimDate());
        int endYear = c.get(Calendar.YEAR) - 1;

        c.setTime(baseDate);

        while (baseYear < endYear) {

            // Leap year day
            Date leapYearFeb = null;
            dateInString = baseYear + "-02-29";
            boolean isLeap = cal.isLeapYear(baseYear);
            if (isLeap) {
                try {
                    leapYearFeb = stringToDate.parse(dateInString);
                } catch (java.text.ParseException e) {
                    data.LOGGER.warning(e.toString());
                }
            }

            // Set the month to January for Demand forecast
            c.set(Calendar.MONTH, 0);
            float solarHalfhourExposureKwh = (float) 0.0;
            while (c.get(Calendar.YEAR) == baseYear) {
                baseDate = c.getTime();

                // Truncate time to get the correct data in leap year 29th of Feb
                c.set(Calendar.MINUTE, 0);
                c.set(Calendar.HOUR_OF_DAY, 0);
                Date alwaysMidnight = c.getTime();
                c.setTime(baseDate);

                // ForecastDemand Half Hour
                if (data.getHalfhour_solar_exposure().containsKey(baseDate)) {
                    solarHalfhourExposureKwh = data.getHalfhour_solar_exposure().get(baseDate);
                }

                // If the basis is a leap year, then fill it with the info from the day before,
                // And do not try to copy info from 29th of Feb to the next year forecast as it
                // will fail
                if (leapYearFeb != null && alwaysMidnight.compareTo(leapYearFeb) == 0) {
                    c.add(Calendar.DAY_OF_MONTH, -1);
                    Date yesterdayDate = c.getTime();
                    float yesterdaySolarHalfhourExposureKwh = data.getHalfhour_solar_exposure().get(yesterdayDate);
                    data.getHalfhour_solar_exposure().put(baseDate, yesterdaySolarHalfhourExposureKwh);
                    c.add(Calendar.DAY_OF_MONTH, 1);
                } else {

                    // Set the 1 year forecast date
                    fc.setTime(c.getTime());
                    fc.add(Calendar.YEAR, 1);
                    Date forecastedDate = fc.getTime();

                    data.getHalfhour_solar_exposure().put(forecastedDate, solarHalfhourExposureKwh);
                }

                // Set the month for the forecast
                c.add(Calendar.MINUTE, 30);

            }

            baseYear++;

        }
    }

    // Select Consumption
    private static void createForecastConsumptionFromDemand(Gr4spSim data) {

        // Set current Date
        Date currentDate = null;
        SimpleDateFormat stringToDate = new SimpleDateFormat("yyyy-MM-dd");
        try {
            currentDate = stringToDate.parse(data.settings.getStartDemandForecast());
        } catch (ParseException e) {
            data.LOGGER.warning(e.toString());
        }
        Calendar c = Calendar.getInstance();

        c.setTime(data.getEndSimDate());
        int endYear = c.get(Calendar.YEAR);

        c.setTime(currentDate);

        // Set the month to January to start consumption computation
        // c.set(Calendar.MONTH, 0);

        double TotalMw = 0.0;
        double currentMonth = c.get(Calendar.MONTH);
        Date dateConsumption = c.getTime();
        int mul_factor = 2 * data.merit_freq;

        while (c.get(Calendar.YEAR) < endYear) {

            // If month finished, save the demand data
            if (currentMonth != c.get(Calendar.MONTH)) {

                // save demand data. From MW 30min -> GWh
                // Convert to capacity over each merit order timestep
                // double TotalGWh = (data.merit_freq *TotalMw) / 1000;

                // double TotalGWh = (TotalMw * mul_factor ) / 2000;
                double TotalGWh = TotalMw / 2000;
                data.getMonthly_consumption_register().put(dateConsumption, TotalGWh);
                // reset counter and update current month
                TotalMw = 0.0;
                currentMonth = c.get(Calendar.MONTH);
                c.set(Calendar.MINUTE, 0);
                c.set(Calendar.HOUR_OF_DAY, 0);
                dateConsumption = c.getTime();

            }
            currentDate = c.getTime();

            // ForecastDemand Half Hour
            if (data.getHalfhour_demand_register().containsKey(currentDate)) {
                TotalMw += data.getHalfhour_demand_register().get(currentDate);
            }

            // if(data.getDaily_demand_register().containsKey(currentDate)) {
            // TotalMw += data.getDaily_demand_register().get(currentDate);
            // }

            // Set the month for the forecast
            // c.add(Calendar.MINUTE, data.merit_freq * 60);

            c.add(Calendar.MINUTE, 30);

        }

        // save last month demand data. From MW 30min -> GWh
        // double TotalGWh = (mul_factor * TotalMw) / 2000;
        double TotalGWh = TotalMw / 2000;

        data.getMonthly_consumption_register().put(dateConsumption, TotalGWh);

    }

    public static void loadCustomDemand(Gr4spSim data) {

        // data.getMonthly_consumption_register()

        String url = "jdbc:postgresql://localhost:543" + String.valueOf(data.db_id)
                + "/postgres?user=postgres&password=password"; // "jdbc:sqlite:Spm_archetypes.db";

        // SimpleDateFormat stringToDate = new SimpleDateFormat("yyyy-MM-dd");

        String sql = "SELECT time, demand" +
                " FROM demand_24 WHERE scenario ='" + data.settingsAfterBaseYear.getForecastScenarioConsumption()
                + "';";
        // int count = 0;
        // Loading CPI Conversion data
        try (Connection conn = DriverManager.getConnection(url);
                Statement stmt = conn.createStatement();
                ResultSet rs = stmt.executeQuery(sql)) {

            // loop through the result set
            while (rs.next()) {
                // count++;
                data.LOGGER.info("\t" + rs.getString("time") + "\t" +
                        rs.getDouble("demand"));

                java.util.Date date = rs.getTimestamp("time");
                Double val = rs.getDouble("demand");

                data.getDaily_demand_register().put(date, val);
                // temp.put(date,val);

            }
        } catch (SQLException e) {
            data.LOGGER.warning(e.getMessage());
        }
    }

    public static void loadCustomMonthlyConsumption(Gr4spSim data) {

        // data.getMonthly_consumption_register()

        String url = "jdbc:postgresql://localhost:543" + String.valueOf(data.db_id)
                + "/postgres?user=postgres&password=password"; // "jdbc:sqlite:Spm_archetypes.db";

        SimpleDateFormat stringToDate = new SimpleDateFormat("yyyy-MM-dd");

        String sql = "SELECT time, val" +
                " FROM monthly_consumption_temp WHERE scenario ='"
                + data.settingsAfterBaseYear.getForecastScenarioConsumption() + "';";

        // Loading CPI Conversion data
        try (Connection conn = DriverManager.getConnection(url);
                Statement stmt = conn.createStatement();
                ResultSet rs = stmt.executeQuery(sql)) {

            // loop through the result set
            while (rs.next()) {
                data.LOGGER.info("\t" + rs.getString("time") + "\t" +
                        rs.getDouble("val"));

                java.util.Date date = rs.getDate("time");
                Double val = rs.getDouble("val");

                data.getMonthly_consumption_register().put(date, val);

            }
        } catch (SQLException e) {
            data.LOGGER.warning(e.getMessage());
        }
    }

    // Select Consumption
    private static void calibrate30minHistoricDemandFromConsumption(Gr4spSim data) {

        // Set current Date
        Date currentDate;
        Date endCalibrationDate = null;
        Calendar c = Calendar.getInstance();

        SimpleDateFormat stringToDate = new SimpleDateFormat("yyyy-MM-dd");
        try {
            currentDate = stringToDate.parse(data.settings.getStartDemandForecast());
            c.setTime(currentDate);
            endCalibrationDate = c.getTime();
            // Calibrate up to the end simulation year if it happens before the start of the
            // forecast
            if (endCalibrationDate.after(data.getEndSimDate()))
                endCalibrationDate = data.getEndSimDate();

            currentDate = stringToDate.parse(data.settings.getStartDateSpotMarket());
            // Calibrate from the start simulation year if it happens after the start of the
            // spot market
            if (currentDate.before(data.getStartSimDate()))
                currentDate = data.getStartSimDate();
            c.setTime(currentDate);
        } catch (ParseException e) {
            data.LOGGER.warning(e.toString());
        }

        // Set the month to January to start consumption computation
        // c.set(Calendar.MONTH, 0);

        double TotalMw = 0.0;
        int currentMonth = c.get(Calendar.MONTH);
        Date dateConsumption = c.getTime();

        while (!c.getTime().after(endCalibrationDate)) {

            // If month finished, save the demand data
            if (currentMonth != c.get(Calendar.MONTH)) {

                // save demand data. From MW 30min -> GWh
                double TotalDemandGWh = TotalMw / 2000;
                double TotalConsumptionGWh = data.getMonthly_consumption_register().get(dateConsumption);
                double calibrationDemand = TotalConsumptionGWh / TotalDemandGWh;

                // Calibrate Demand of for the past month
                if (currentMonth == 11) {
                    // If it's end of december, we need to substract the 1 year to update the
                    // correct demand
                    c.set(Calendar.YEAR, c.get(Calendar.YEAR) - 1);
                }
                c.set(Calendar.MONTH, currentMonth);
                c.set(Calendar.MINUTE, 0);
                c.set(Calendar.HOUR_OF_DAY, 0);

                // Date printDate = c.getTime();
                // double TotalDemandGwhCorrected = 0.0;

                while (currentMonth == c.get(Calendar.MONTH)) {
                    currentDate = c.getTime();
                    // Calibrate Demand Half Hour
                    if (data.getHalfhour_demand_register().containsKey(currentDate)) {
                        double demand = data.getHalfhour_demand_register().get(currentDate) * calibrationDemand;
                        // TotalDemandGwhCorrected+=demand;
                        data.getHalfhour_demand_register().put(currentDate, demand);
                    }

                    // Set the month for the forecast
                    c.add(Calendar.MINUTE, 30);
                }
                // TotalDemandGwhCorrected = TotalDemandGwhCorrected / 2000.0;
                // System.out.println(printDate+", "+TotalDemandGWh+",
                // "+TotalDemandGwhCorrected+", "+TotalConsumptionGWh+",
                // "+(calibrationDemand-1.0)*100.0 + "%");

                // reset counter and update current month
                TotalMw = 0.0;
                currentMonth = c.get(Calendar.MONTH);

                c.set(Calendar.MINUTE, 0);
                c.set(Calendar.HOUR_OF_DAY, 0);
                dateConsumption = c.getTime();

            }
            currentDate = c.getTime();

            // ForecastDemand Half Hour
            if (data.getHalfhour_demand_register().containsKey(currentDate)) {
                TotalMw += data.getHalfhour_demand_register().get(currentDate);
            }

            // Set the month for the forecast
            c.add(Calendar.MINUTE, 30);

        }

    }

    public static void selectConsumption(Gr4spSim data, String startDate, String startSpotDate, String endDate) {
        String url = "jdbc:postgresql://localhost:543" + String.valueOf(data.db_id)
                + "/postgres?user=postgres&password=password"; // "jdbc:sqlite:Spm_archetypes.db";

        SimpleDateFormat stringToDate = new SimpleDateFormat("yyyy-MM-dd");

        /**
         * Load historic Total Consumption per month up to start of Spot market
         */

        String sql = "SELECT date, total_consumption_gwh, hydro_gwh" +
                " FROM  generation_consumption_historic WHERE " +
                " date <= '" + endDate + "'" +
                " AND date >= '" + startDate + "';";

        try (Connection conn = DriverManager.getConnection(url);
                Statement stmt = conn.createStatement();
                ResultSet rs = stmt.executeQuery(sql)) {

            // loop through the result set
            while (rs.next()) {
                data.LOGGER.info("\t" + rs.getString("date") + "\t" +
                        rs.getString("total_consumption_gwh"));

                Date d = rs.getDate("date");
                Double gwh = rs.getDouble("total_consumption_gwh");
                Double renewable_gwh = rs.getDouble("hydro_gwh");

                // If monthly consumption doesn't exist, create it
                if (!data.getMonthly_consumption_register().containsKey(d)) {
                    data.getMonthly_consumption_register().put(d, gwh);
                }

                // If monthly hydro doesn't exist, create it
                if (!data.getMonthly_renewable_historic_register().containsKey(d)) {
                    data.getMonthly_renewable_historic_register().put(d, renewable_gwh);
                }

            }
        } catch (SQLException e) {
            data.LOGGER.warning(e.getMessage());
        }

        LoadData.loadCustomDemand(data);

        loadCustomMonthlyConsumption(data);

        /**
         * Load domestic consumers
         */
        sql = "SELECT date, domesticconsumers" +
                " FROM  domestic_consumers WHERE " +
                " date <= '" + endDate + "'" +
                " AND date >= '" + startDate + "';";

        try (Connection conn = DriverManager.getConnection(url);
                Statement stmt = conn.createStatement();
                ResultSet rs = stmt.executeQuery(sql)) {

            // loop through the result set
            while (rs.next()) {
                Date d = rs.getDate("date");
                int consumers = rs.getInt("domesticconsumers");

                // If data doesn't exist, create it
                if (!data.getMonthly_domestic_consumers_register().containsKey(d)) {
                    data.getMonthly_domestic_consumers_register().put(d, consumers);
                }

            }
        } catch (SQLException e) {
            data.LOGGER.warning(e.getMessage());
        }

        createForecastDomesticConsumers(data);

        /**
         * Compute linear Monthly growth from Yearly data
         */
        HashMap<Date, Integer> newMonthlyData = new HashMap<>();
        for (Map.Entry<Date, Integer> entry : data.getMonthly_domestic_consumers_register().entrySet()) {
            Date currentDate = entry.getKey();
            Integer consumers = entry.getValue();

            Calendar c = Calendar.getInstance();
            c.setTime(currentDate);

            // Get info about next year
            c.add(Calendar.YEAR, 1);
            Date nextYear = c.getTime();
            Integer consumersNextYear = data.getMonthly_domestic_consumers_register().get(nextYear);

            if (consumers == null || consumersNextYear == null)
                continue;

            Integer increment = (consumersNextYear - consumers) / 12;

            // Set calendar to current Date again
            c.setTime(currentDate);

            // Increment date by 1 month, and 1 increment of population, until we filled the
            // 12 months
            for (int i = 0; i < 11; i++) {

                // add 1 month
                c.add(Calendar.MONTH, 1);
                Date newMonth = c.getTime();

                // add new consumers
                consumers += increment;

                // store the new data point
                newMonthlyData.put(newMonth, consumers);

            }
        }

        // Add new monthly data into our domestic consumer register
        for (Map.Entry<Date, Integer> entry : newMonthlyData.entrySet()) {
            data.getMonthly_domestic_consumers_register().put(entry.getKey(), entry.getValue());
        }

        /**
         * Update Total Consumption with monthly domestic consumers
         * and Percentage of domestic usage
         */

        // Set Base Date
        Date baseDate = null;
        int baseYear = data.settings.getBaseYearConsumptionForecast();

        String dateInString = baseYear + "-01-01";
        try {
            baseDate = stringToDate.parse(dateInString);
        } catch (java.text.ParseException e) {
            data.LOGGER.warning(e.toString());
        }

        for (Date month : data.getMonthly_consumption_register().keySet()) {

            double gwh = data.getMonthly_consumption_register().get(month);

            // Save total consumption
            data.getTotal_monthly_consumption_register().put(month, gwh * 1000.0);

            Integer consumers = data.getMonthly_domestic_consumers_register().get(month);

            if (consumers == null)
                continue;

            double domesticConsumptionPercentage = data.settings.getDomesticConsumptionPercentage();

            if (month.after(baseDate))
                domesticConsumptionPercentage = data.settingsAfterBaseYear.getDomesticConsumptionPercentage();

            // convert to MWh and only domestic demand
            double newMwh = (gwh * 1000.0 * domesticConsumptionPercentage) / (double) consumers;
            // to check total demand in KWh
            // double newkwh = (gwh * 1000000);

            data.getMonthly_consumption_register().put(month, newMwh);
        }

    }

    /**
     * Data used prior the start of the spot Market specified in the YAML
     * configuration file
     */
    public static void selectGenerationHistoricData(Gr4spSim data, String startDate, String endDate) {
        String url = "jdbc:postgresql://localhost:543" + String.valueOf(data.db_id)
                + "/postgres?user=postgres&password=password"; // "jdbc:sqlite:Spm_archetypes.db";

        SimpleDateFormat stringToDate = new SimpleDateFormat("yyyy-MM-dd");

        /**
         * Load Total Generation per month
         */
        String sql = "SELECT date, volumeweightedprice_dollarmwh, temperaturec, solar_roofpv_dollargwh, solar_roofpv_gwh,"
                +
                "solar_utility_dollargwh, solar_utility_gwh, wind_dollargwh, wind_gwh, hydro_dollargwh," +
                "hydro_gwh, battery_disch_dollargwh, battery_disch_gwh, gas_ocgt_dollargwh, gas_ocgt_gwh," +
                "gas_steam_dollargwh, gas_steam_gwh, browncoal_dollargwh, browncoal_gwh, imports_dollargwh," +
                "imports_gwh, exports_dollargwh, exports_gwh" +
                " FROM  generation_consumption_historic WHERE " +
                " date <= '" + endDate + "'" +
                " AND date >= '" + startDate + "';";

        try (Connection conn = DriverManager.getConnection(url);
                Statement stmt = conn.createStatement();
                ResultSet rs = stmt.executeQuery(sql)) {

            // loop through the result set
            while (rs.next()) {

                Date d = rs.getDate("date");

                Generation genData = new Generation(
                        rs.getDate("date"), rs.getFloat("volumeweightedprice_dollarmwh"), rs.getFloat("temperaturec"),
                        rs.getFloat("solar_roofpv_dollargwh"), rs.getFloat("solar_roofpv_gwh"),
                        rs.getFloat("solar_utility_dollargwh"),
                        rs.getFloat("solar_utility_gwh"), rs.getFloat("wind_dollargwh"), rs.getFloat("wind_gwh"),
                        rs.getFloat("hydro_dollargwh"),
                        rs.getFloat("hydro_gwh"), rs.getFloat("battery_disch_dollargwh"),
                        rs.getFloat("battery_disch_gwh"), rs.getFloat("gas_ocgt_dollargwh"),
                        rs.getFloat("gas_ocgt_gwh"), rs.getFloat("gas_steam_dollargwh"), rs.getFloat("gas_steam_gwh"),
                        rs.getFloat("browncoal_dollargwh"),
                        rs.getFloat("browncoal_gwh"), rs.getFloat("imports_dollargwh"), rs.getFloat("imports_gwh"),
                        rs.getFloat("exports_dollargwh"),
                        rs.getFloat("exports_gwh"));

                // If monthly gen datapoint doesn't exist, create it
                if (!data.getMonthly_generation_register().containsKey(d)) {
                    data.getMonthly_generation_register().put(d, genData);
                }
            }
        } catch (SQLException e) {
            data.LOGGER.warning(e.getMessage());
        }
    }

    public static void selectCustomDailySolarExposure(Gr4spSim data) {
        String url = "jdbc:postgresql://localhost:543" + String.valueOf(data.db_id)
                + "/postgres?user=postgres&password=password"; // "jdbc:sqlite:Spm_archetypes.db";

        String sql = "SELECT time , ghi" +
                " FROM solar_ghi_24_ave;";

        // Loading Solar Exposure data
        try (Connection conn = DriverManager.getConnection(url);
                Statement stmt = conn.createStatement();
                ResultSet rs = stmt.executeQuery(sql)) {

            // loop through the result set
            while (rs.next()) {
                Date date = rs.getTimestamp("time");
                // float toKwh = (float) 1000.0;

                // float solarHalfhourExposure = rs.getFloat("ghi") / toKwh;

                /*
                 * data.LOGGER.info("\t" + date + "\t" +
                 * solarHalfhourExposure
                 * );
                 */
                // float val = rs.getFloat("ghi");
                data.getDaily_solar_exposure().put(date, rs.getFloat("ghi"));

                // temp.put(date, val);
            }
        } catch (SQLException e) {
            data.LOGGER.warning(e.getMessage());
        }
    }

    public static void selectHalfHourSolarExposure(Gr4spSim data) {
        String url = "jdbc:postgresql://localhost:543" + String.valueOf(data.db_id)
                + "/postgres?user=postgres&password=password"; // "jdbc:sqlite:Spm_archetypes.db";

        String sql = "SELECT time , ghi" +
                " FROM solar_ghi;";

        // Loading Solar Exposure data
        try (Connection conn = DriverManager.getConnection(url);
                Statement stmt = conn.createStatement();
                ResultSet rs = stmt.executeQuery(sql)) {

            // loop through the result set
            while (rs.next()) {
                Date date = rs.getTimestamp("time");
                float toKwh = (float) 1000.0;

                float solarHalfhourExposure = rs.getFloat("ghi") / toKwh;

                /*
                 * data.LOGGER.info("\t" + date + "\t" +
                 * solarHalfhourExposure
                 * );
                 */

                data.getHalfhour_solar_exposure().put(date, solarHalfhourExposure);

            }
        } catch (SQLException e) {
            data.LOGGER.warning(e.getMessage());
        }
    }

    public static void selectSolarInstallation(Gr4spSim data) {
        String url = "jdbc:postgresql://localhost:543" + String.valueOf(data.db_id)
                + "/postgres?user=postgres&password=password"; // "jdbc:sqlite:Spm_archetypes.db";

        SimpleDateFormat stringToDate = new SimpleDateFormat("yyyy-MM-dd");

        String sql = "SELECT year, month, number_installations, aggregated_capacity_kw, system_capacity" +
                " FROM solar_installation_monthly;";

        // Loading Solar installation data
        try (Connection conn = DriverManager.getConnection(url);
                Statement stmt = conn.createStatement();
                ResultSet rs = stmt.executeQuery(sql)) {

            // loop through the result set
            while (rs.next()) {
                int year = rs.getInt("year");
                int month = rs.getInt("month");
                String sdate = year + "-" + month + "-1";
                Date date = null;

                Integer number_installations = rs.getInt("number_installations");
                Integer aggregater_capacity_kw = rs.getInt("aggregated_capacity_kw");
                Float system_capacity = rs.getFloat("system_capacity");

                data.LOGGER.info("\t" + year + "\t" +
                        month + "\t" +
                        number_installations);

                try {
                    date = stringToDate.parse(sdate);
                } catch (ParseException e) {
                }

                data.getSolar_number_installs().put(date, number_installations);
                data.getSolar_aggregated_kw().put(date, aggregater_capacity_kw);
                data.getSolar_system_capacity_kw().put(date, system_capacity);

            }
        } catch (SQLException e) {
            data.LOGGER.warning(e.getMessage());
        }
    }

    // Select actors function

    public static void selectActors(Gr4spSim data, String startDate, String changeDate) {
        String url = "jdbc:postgresql://localhost:543" + String.valueOf(data.db_id)
                + "/postgres?user=postgres&password=password"; // "jdbc:sqlite:Spm_archetypes.db";

        String sql = "SELECT id, name, registration_date, change_date, reg_number, region, role, business_structure" +
                " FROM  actors  WHERE region = '" + data.settings.getAreaCode() + "';";

        try (Connection conn = DriverManager.getConnection(url);
                Statement stmt = conn.createStatement();
                ResultSet rs = stmt.executeQuery(sql)) {

            // loop through the result set
            while (rs.next()) {
                data.LOGGER.info("\t" + rs.getInt("id") + "\t" +
                        rs.getString("name") + "\t" +
                        rs.getDate("registration_date") + "\t" +
                        rs.getDate("change_date") + "\t" +
                        rs.getString("reg_number") + "\t" +
                        rs.getString("region") + "\t" +
                        rs.getString("business_structure"));

                Actor actor = new Actor(rs.getInt("id"),
                        rs.getString("name"),
                        rs.getDate("registration_date"),
                        rs.getDate("change_date"),
                        rs.getString("reg_number"),
                        rs.getString("region"),
                        rs.getString("role"),
                        rs.getString("business_structure"));

                int idActor = rs.getInt("id");

                // Insert in Map the actor with key = id, and add the new actor
                if (!data.getActor_register().containsKey(idActor))
                    data.getActor_register().put(idActor, actor);
                // else
                // System.err.println("Two actors cannot have the same ID");

                data.setNumActors(data.getNumActors() + 1);

                // Add actor to Array. This will be used in the constructor of SPM
                // actors.add(actor);

            }
        } catch (SQLException e) {
            data.LOGGER.warning(e.getMessage());
        }
        // return actors;
    }

    // Actor asset relationships function
    public static void selectActorAssetRelationships(Gr4spSim data, String tableName) {
        String url = "jdbc:postgresql://localhost:543" + String.valueOf(data.db_id)
                + "/postgres?user=postgres&password=password";// "jdbc:sqlite:Spm_archetypes.db";

        for (Map.Entry<Integer, Actor> entry : data.getActor_register().entrySet()) {
            Actor actor = entry.getValue();

            String sql = "SELECT actorid, assetid, reltype, assettype, percentage" +
                    " FROM " + tableName + " WHERE actorid = " + actor.getId();

            try (Connection conn = DriverManager.getConnection(url);
                    Statement stmt = conn.createStatement();
                    ResultSet rs = stmt.executeQuery(sql)) {

                // loop through the result set
                while (rs.next()) {
                    data.LOGGER.info("\t" + rs.getInt("ActorId") + "\t" +
                            rs.getInt("AssetId") + "\t" +
                            rs.getDouble("Percentage") + "\t" +
                            rs.getString("AssetType") + "\t" +
                            rs.getString("RelType"));

                    // Actor actor = actor_register.get(rs.getInt("ActorId"));
                    Vector<Asset> asset;

                    double percentage = rs.getDouble("Percentage");
                    String assetType = rs.getString("AssetType");

                    if (assetType.equalsIgnoreCase("Generation")) {
                        Vector<Generator> gens = data.getGen_register().get(rs.getInt("AssetId"));

                        // Need to make sure that the SPM_gen_mapping is correct
                        // For this we need to create another SPM for 90s, and make sure that it uses
                        // The gen from the 90s.
                        if (gens == null) {
                            data.LOGGER.info("Actor " + actor.getName() + " - Generation Asset id "
                                    + rs.getInt("AssetId") + " Relationship not existent for this period of time");
                        } else {
                            for (Generator gen : gens) {
                                ActorAssetRelationship actorRel = new ActorAssetRelationship(actor, gen,
                                        stringToActorAssetTypeRelType(rs.getString("RelType")), percentage);

                                // Add actorAsset relationship into the asset list
                                gen.addAssetRelationship(actorRel);
                                // Add all actorAsset relationships into the simulation engine global asset list
                                data.getActorAssetRelationships().add(actorRel);

                            }
                        }
                    } else if (assetType.equalsIgnoreCase("Network asset")) {
                        Vector<NetworkAssets> nets = data.getNetwork_register().get(rs.getInt("AssetId"));
                        if (nets == null) {
                            data.LOGGER.info("Actor " + actor.getName() + " - Network Asset id " + rs.getInt("AssetId")
                                    + " Relationship not existent for this period of time");
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
                            data.LOGGER.info("Actor " + actor.getName() + " - SPM Asset id " + rs.getInt("AssetId")
                                    + " Relationship not existent for this period of time");
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
                data.LOGGER.warning(e.getMessage());
            }
        }
    }

    /**
     * select all rows in the Generation Technologies table
     */
    public static ArrayList<Generator> selectGenTech(Gr4spSim data, String min_nameplate_capacity,
            String max_nameplate_capacity) {
        String url = "jdbc:postgresql://localhost:543" + String.valueOf(data.db_id)
                + "/postgres?user=postgres&password=password"; // url for sqlite "jdbc:sqlite:Spm_archetypes.db";

        SimpleDateFormat DateToString = new SimpleDateFormat("yyyy-MM-dd");

        String sql = "SELECT asset_id, region, asset_type, site_name, owner_name, technology_type, fuel_type, nameplate_capacity_mw, dispatch_type, full_commercial_use_date, closure_date, "
                +
                " duid, no_units, storage_capacity_mwh, expected_closure_date, fuel_bucket_summary, unit_status" +
                " FROM generationassets WHERE nameplate_capacity_mw >= '" + min_nameplate_capacity
                + "'AND nameplate_capacity_mw < '" + max_nameplate_capacity +
                "'AND full_commercial_use_date <= '" + DateToString.format(data.getEndSimDate()) + "'" +
                " AND region = '" + data.settings.getAreaCode() + "'" +
                " AND ( closure_date > '" + DateToString.format(data.getStartSimDate()) + "'" +
                " OR expected_closure_date > '" + DateToString.format(data.getStartSimDate()) + "')";

        // to include publically announced projects or not.
        if (data.settingsAfterBaseYear.getIncludePublicallyAnnouncedGen() == false) {
            sql += "AND unit_status != 'Publically Announced'";
        }

        ArrayList<Generator> gens = new ArrayList<Generator>();
        try (Connection conn = DriverManager.getConnection(url);
                Statement stmt = conn.createStatement();
                ResultSet rs = stmt.executeQuery(sql)) {

            // loop through the result set
            while (rs.next()) {
                /*
                 * data.LOGGER.info("\t Generator: " +
                 * rs.getInt("asset_id")+ "\t" +
                 * rs.getString("region")+ "\t" +
                 * rs.getString("asset_type")+ "\t" +
                 * rs.getString("site_name")+ "\t" +
                 * rs.getString("owner_name")+ "\t" +
                 * rs.getString("technology_type")+ "\t" +
                 * rs.getString("fuel_type")+ "\t" +
                 * rs.getDouble("nameplate_capacity_mw")+ "\t" +
                 * rs.getString("dispatch_type")+ "\t" +
                 * rs.getDate("full_commercial_use_date")+ "\t" +
                 * rs.getDate("expected_closure_date")+ "\t" +
                 * rs.getDate("closure_date")+ "\t" +
                 * rs.getString("duid")+ "\t" +
                 * rs.getInt("no_units")+ "\t" +
                 * rs.getFloat("storage_capacity_mwh")+ "\t" +
                 * rs.getString("fuel_bucket_summary")
                 * 
                 * );
                 */

                // Shift forecast for Coal generators retirement
                Date expectedEndDate = rs.getDate("expected_closure_date");
                Calendar cEndDate = Calendar.getInstance();
                if (expectedEndDate != null && rs.getString("fuel_type").equals("Brown Coal")) {
                    cEndDate.setTime(expectedEndDate);
                    cEndDate.add(Calendar.YEAR, data.settingsAfterBaseYear.getForecastGeneratorRetirement());

                    expectedEndDate = cEndDate.getTime();
                }

                double nameplateCapacity = rs.getDouble("nameplate_capacity_mw") * data.settings
                        .getNameplateCapacityChange(rs.getString("fuel_type"), rs.getString("technology_type"));

                // Publically announced Generators are rolled out incrementally (each with
                // capacity/rolloutYears)
                // across a period specified in YAML settings.
                String unit_status = rs.getString("unit_status");
                if (unit_status.equals("Publically Announced") || unit_status.equals("Committed")
                        || unit_status.equals("Committed*")
                        || unit_status.equals("Emerging") || unit_status.equals("In Commissioning")) {
                    int rolloutPeriod = data.settingsAfterBaseYear.getForecastGenerationRolloutPeriod();
                    for (int year = 0; year < rolloutPeriod; year++) {

                        Calendar cStartDate = Calendar.getInstance();
                        try {
                            Date startDate = DateToString.parse(rs.getString("full_commercial_use_date"));
                            cStartDate.setTime(startDate);
                        } catch (ParseException e) {
                            System.err.println("Cannot parse Start Date: " + e.toString());
                        }
                        cStartDate.add(Calendar.YEAR, year);

                        int idGen = (rs.getInt("asset_id") * 1000) + year;

                        Generator gen = new Generator(
                                idGen,
                                rs.getString("region"),
                                rs.getString("asset_type"),
                                rs.getString("site_name"),
                                rs.getString("owner_name"),
                                rs.getString("technology_type"),
                                rs.getString("fuel_type"),
                                nameplateCapacity / rolloutPeriod,
                                rs.getString("dispatch_type"),
                                cStartDate.getTime(),
                                expectedEndDate,
                                rs.getDate("closure_date"),
                                rs.getString("duid"),
                                rs.getInt("no_units"),
                                rs.getFloat("storage_capacity_mwh"),
                                rs.getString("fuel_bucket_summary"),
                                rs.getString("unit_status"),
                                data.settings);

                        // Get from Map the vector of GENERATORS with key = idGen, and add the new
                        // Generator to the vector
                        if (!data.getGen_register().containsKey(idGen))
                            data.getGen_register().put(idGen, new Vector<>());

                        data.getGen_register().get(idGen).add(gen);

                        data.setNumGenerators(data.getNumGenerators() + 1);

                        // Add gen to ArrayList<Generator>. This will be used in the constructor of SPM
                        gens.add(gen);
                    }
                } else {
                    Generator gen = new Generator(
                            rs.getInt("asset_id"),
                            rs.getString("region"),
                            rs.getString("asset_type"),
                            rs.getString("site_name"),
                            rs.getString("owner_name"),
                            rs.getString("technology_type"),
                            rs.getString("fuel_type"),
                            nameplateCapacity,
                            rs.getString("dispatch_type"),
                            rs.getDate("full_commercial_use_date"),
                            expectedEndDate,
                            rs.getDate("closure_date"),
                            rs.getString("duid"),
                            rs.getInt("no_units"),
                            rs.getFloat("storage_capacity_mwh"),
                            rs.getString("fuel_bucket_summary"),
                            rs.getString("unit_status"),
                            data.settings);

                    int idGen = rs.getInt("asset_id");
                    // Get from Map the vector of GENERATORS with key = idGen, and add the new
                    // Generator to the vector
                    if (!data.getGen_register().containsKey(idGen))
                        data.getGen_register().put(idGen, new Vector<>());

                    data.getGen_register().get(idGen).add(gen);

                    data.setNumGenerators(data.getNumGenerators() + 1);

                    // Add gen to ArrayList<Generator>. This will be used in the constructor of SPM
                    gens.add(gen);
                }

            }
        } catch (SQLException e) {
            data.LOGGER.warning(e.getMessage());
        }
        return gens;
    }

    public static ArrayList<Storage> selectStorage(Gr4spSim data, String idst) {
        String url = "jdbc:postgresql://localhost:543" + String.valueOf(data.db_id)
                + "/postgres?user=postgres&password=password"; // "jdbc:sqlite:Spm_archetypes.db";

        String sql = "SELECT storage_id, storage_name, storagetype, storageoutputcap , storagecapacity, ownership," +
                " storage_cyclelife, storage_costrange  FROM storage WHERE storage_id = '" + idst + "' ";
        ArrayList<Storage> strs = new ArrayList<Storage>();
        try (Connection conn = DriverManager.getConnection(url);
                Statement stmt = conn.createStatement();
                ResultSet rs = stmt.executeQuery(sql)) {

            // loop through the result set
            while (rs.next()) {
                /*
                 * data.LOGGER.info("\t" + rs.getInt("storage_id") + "\t" +
                 * rs.getString("storage_name") + "\t" +
                 * rs.getInt("storageType") + "\t" +
                 * rs.getDouble("storageOutputCap") + "\t" +
                 * rs.getDouble("storageCapacity") + "\t" +
                 * rs.getInt("Ownership") + "\t" +
                 * rs.getDouble("storage_cycleLife") + "\t" +
                 * rs.getDouble("storage_costRange"));
                 */
                Storage str = new Storage(rs.getInt("storage_id"),
                        rs.getString("storage_name"),
                        rs.getInt("StorageType"),
                        rs.getDouble("storageOutputCap"),
                        rs.getDouble("storageCapacity"),
                        rs.getInt("Ownership"),
                        rs.getDouble("storage_cycleLife"),
                        rs.getDouble("storage_costRange"));

                data.setNumStorage(data.getNumStorage() + 1);
                strs.add(str);
            }
        } catch (SQLException e) {
            data.LOGGER.warning(e.getMessage());
        }
        return strs;
    }

    // Select and create the type of energy grid
    public static ArrayList<NetworkAssets> selectNetwork(Gr4spSim data, String subname) {
        String url = "jdbc:postgresql://localhost:543" + String.valueOf(data.db_id)
                + "/postgres?user=postgres&password=password"; // "jdbc:sqlite:Spm_archetypes.db";

        String sql = "SELECT networkassets.id as netid, networkassettype.type as type, networkassettype.subtype as subtype, "
                +
                "networkassettype.grid as grid, assetname, ownername, locationmb, gridlosses, gridvoltage, startdate, changedate "
                +
                " FROM networkassets " +
                "JOIN networkassettype ON networkassets.assettype = networkassettype.id " +
                "and  networkassets.assetname SIMILAR TO '" + subname + "%' ";
        ArrayList<NetworkAssets> nets = new ArrayList<NetworkAssets>();
        try (Connection conn = DriverManager.getConnection(url);
                Statement stmt = conn.createStatement();
                ResultSet rs = stmt.executeQuery(sql)) {

            // loop through the result set
            while (rs.next()) {
                int idNet = rs.getInt("netid");
                NetworkAssets grid = new NetworkAssets(idNet,
                        rs.getString("type"),
                        rs.getString("subtype"),
                        rs.getString("grid"),
                        rs.getString("assetName"),
                        rs.getString("ownername"),
                        rs.getString("locationmb"),
                        rs.getDouble("gridLosses"),
                        rs.getString("gridVoltage"),
                        rs.getDate("startdate"),
                        rs.getDate("changedate"));
                nets.add(grid);

                // Get from Map the vector of Networks with key = idNet, and add the new Network
                // to the vector
                if (!data.getNetwork_register().containsKey(idNet))
                    data.getNetwork_register().put(idNet, new Vector<>());

                data.getNetwork_register().get(idNet).add(grid);

            }
        } catch (SQLException e) {
            data.LOGGER.warning(e.getMessage());
        }
        return nets;
    }

    // creation of random interface or connection point. This interface changes as
    // well depending on the scale of study. A house with
    // own gene4ration off the grid will have the same house (smart metered or not)
    // as connection point, a neighborhood can have one or more feeders and
    // sub-stations,
    // it is basically the closest point where supply meets demand without going
    // through any significant extra technological "treatment"

    public static ArrayList<ConnectionPoint> selectConnectionPoint(Gr4spSim data, String name) {
        String url = "jdbc:postgresql://localhost:543" + String.valueOf(data.db_id)
                + "/postgres?user=postgres&password=password"; // "jdbc:sqlite:Spm_archetypes.db";

        String sql = "SELECT cpoint_id, cpoint_name, cpoint_type, distancetodemand, cpoint_locationcode, cpoint_owner, ownership FROM connectionpoint WHERE cpoint_name = '"
                + name + "' ";
        ArrayList<ConnectionPoint> cpoints = new ArrayList<ConnectionPoint>();
        try (Connection conn = DriverManager.getConnection(url);
                Statement stmt = conn.createStatement();
                ResultSet rs = stmt.executeQuery(sql)) {

            // loop through the result set
            while (rs.next()) {
                ConnectionPoint cpoint = new ConnectionPoint(rs.getInt("cpoint_id"),
                        rs.getString("cpoint_name"),
                        rs.getInt("CPoint_Type"),
                        rs.getDouble("distanceToDemand"),
                        rs.getInt("cpoint_locationCode"),
                        rs.getString("cpoint_owner"),
                        rs.getInt("Ownership"));

                data.setNumcpoints(data.getNumcpoints() + 1);
                cpoints.add(cpoint);

            }
        } catch (SQLException e) {
            data.LOGGER.warning(e.getMessage());
        }
        return cpoints;
    }

    /**
     * Create SPM function
     */

    public static Spm createSpm(Gr4spSim data, int idSpmActor) {

        /**
         * Check if spm.id is shared. If so, check if we already created an object for
         * that spm.id.
         * If it's the first time this spm.id is needed, then we create its object,
         * If it already exists, we just retrieve the object from the spm_register
         */
        String url = "jdbc:postgresql://localhost:543" + String.valueOf(data.db_id)
                + "/postgres?user=postgres&password=password"; // "jdbc:sqlite:Spm_archetypes.db";

        String spmGenSql = "SELECT id, spm.shared, spm_contains, generation, network_assets, interface, storage, description FROM spm WHERE spm.id = '"
                + idSpmActor + "' ";

        try (Connection conn = DriverManager.getConnection(url);
                Statement stmt = conn.createStatement();
                ResultSet rs = stmt.executeQuery(spmGenSql)) {

            // loop through the result set
            while (rs.next()) {

                int shared = rs.getInt("shared");

                // If it is shared and
                // we already created the spm, then the id should exist in the spm_register
                // if so, we can return the existing object
                if (shared == 1 && data.spm_register.containsKey(idSpmActor)) {
                    return data.spm_register.get(idSpmActor).firstElement();
                }

                /**
                 * Get list of SPMs recursively from DB. Base case when there's no
                 * spm_contains.contained_id
                 */
                ArrayList<Spm> spms_contained = new ArrayList<Spm>();

                String[] spm_contains = rs.getString("spm_contains").split(",");
                for (String id : spm_contains) {
                    if (id.equalsIgnoreCase(""))
                        break;
                    int contained_id = Integer.parseInt(id);

                    // Create SPM if contained_id is different than NULL
                    // Base Case of recursion
                    if (contained_id != 0) {
                        // data.LOGGER.info("SPM '" + idSpmActor + "' contains:" + contained_id);

                        // Recursive call to load the contained SPMs before creating the current SPM
                        // with idSpmActor
                        Spm spm_returned = LoadData.createSpm(data, contained_id);
                        // if (spm_returned == null) System.out.println("spm_returned " + spmGenSql);
                        // Add Spms created to the ArrayList<Spm>
                        spms_contained.add(spm_returned);
                    }
                }

                /**
                 * Get list of Generators from DB
                 */

                ArrayList<Generator> gens = null;
                switch (idSpmActor) {
                    case 9: // ID for Generators connected at the large gen unit SPM (generators with
                            // minimum 30 MW inclusive)
                        gens = LoadData.selectGenTech(data, "30", "100000");
                        break;
                    case 5: // ID for Generators connected at the Distribution SPM. Generators with minimum
                            // 100KW inclusive and max 30 MW non inclusive.
                        gens = LoadData.selectGenTech(data, "0.1", "30");
                        break;
                    case 6: // ID for generators connected to the subdistribution SPM. Generators with
                            // minimum 0 and maximum 100 KW non inclusive.
                        gens = LoadData.selectGenTech(data, "0.0", "0.1");
                        break;
                    default:
                        gens = new ArrayList<>();
                        break;
                }

                /**
                 * Get list of STORAGE from DB
                 */

                ArrayList<Storage> strs = new ArrayList<Storage>();
                String[] st_contained = rs.getString("storage").split(",");
                for (String id : st_contained) {
                    if (id.equalsIgnoreCase(""))
                        break;
                    // data.LOGGER.info("SPM '" + rs.getString("id") + "' Storages:");

                    ArrayList<Storage> SpmStrs = LoadData.selectStorage(data, id);
                    strs.addAll(SpmStrs);
                }

                /**
                 * Get list of NetworkAssets from DB
                 */

                ArrayList<NetworkAssets> networkAssets = null;

                switch (idSpmActor) {
                    case 1: // ID for Household nets
                        networkAssets = LoadData.selectNetwork(data, "customer");
                        break;
                    case 5: // ID for Distribution nets
                        networkAssets = LoadData.selectNetwork(data, "distribution");
                        break;
                    case 6: // ID for SubDistribution nets
                        networkAssets = LoadData.selectNetwork(data, "400V");
                        break;
                    case 7: // ID for Transmission nets
                        networkAssets = LoadData.selectNetwork(data, "transmission");
                        break;
                    default:
                        networkAssets = new ArrayList<>();
                        break;
                }

                /**
                 * Create new SPM
                 */
                // System.out.println("SPMS: " + spms_contained.size());
                Spm spmx = new Spm(idSpmActor, spms_contained, gens, networkAssets, strs, null);

                // Get from Map the vector of SPM with key = idSpmActor, and add the new spm to
                // the vector
                if (!data.spm_register.containsKey(idSpmActor))
                    data.spm_register.put(idSpmActor, new Vector<>());

                data.spm_register.get(idSpmActor).add(spmx);

                return spmx;

            }
        } catch (SQLException e) {
            data.LOGGER.warning(e.getMessage());
            // System.out.println("DID NOT GET IT");

        }

        return null;

    }

}
