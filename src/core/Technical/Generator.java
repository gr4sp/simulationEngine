package core.Technical;

import core.Relationships.ActorAssetRelationship;
import core.Gr4spSim;
import sim.engine.SimState;
import sim.util.Double2D;

import java.time.Period;
import java.time.YearMonth;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;

/*** Definition of the class generator ****/

public class Generator implements java.io.Serializable, Asset{
    private static final long serialVersionUID = 1;

    private int id;//asset id
    private String region; //the list of generation assets includes all the regions of the NEM, therefore this has to be filtered out using VIC1 for  Victoria
    private String assetType; // options are: "Existing Plant", "Lighting System", "Non- Existing Plant", and "Project"
    private String name;//name of power plant or "Site Name" according to the GenerationInformation database from the NEM
    private String ownerName;//name of the owner
    private String techTypeDescriptor; //combustion steam-subcritical, combustion Open Cycle Gas turbines (OCGT) phtovoltaic flat panet, hydro-gravity, hydro-run of river, solar PV, wind onshore/offshore,
    private String fuelSourceDescriptor; //fuel source descriptor: according to AEMO it can be: brown coal, black coal, natural gas, Diesel, waste coal mine gas, landfill methane, etc.
    private String ownership;//public, private, PPP, Cooperative?
    private double maxCapacity; //I assume the maximum capacity as being the nameplate capacity in MW
    private double storageCapacity; // in MWh
    private String dispatchTypeDescriptor; // scheduled (S), Semi-scheduled (SS), non-scheduled (NS)

    private String duid;
    private int numUnits; // in NEM description
    private String fuelBucketSummary; // type of fuel

    private double efficiency; //0-1 efficiency of conversion
    private int lifecycle; //life cycle in years
    //private double constructionPeriod;//construction period
    //private double capex; //capital costs in AUD/capacity unit (kW)
    //private double fixedCosts; //fixed costs operation and maintenance in AUD/capacity unit per year
    //private double peakContribFactor; //peak contribution factor in percentage

    private double lcoe; //levelised cost of electricity

    private double historicCapacityFactor;
    private double historicGeneratedMW;
    private Double2D location;//coordinates where the generation is located

    //Start and end of operations
    private Date start;
    private int startYear;
    private Date end;

    //Visualization Parameters
    public double diameter;

    //TODO: Ownership and Management definition
    ArrayList<ActorAssetRelationship> assetRelationships;


    /**Create here other constructors that call the main constructor to include fix values to the parameters and make the class more flexible:
  * public Generator (); this *include here the parameters values.**/


    public Generator(int genId, String region, String assetType,
                     String genName, String owner, String techType, String fuelType,
                     Double gencap, String dispachType, Date start, Date expectedEnd, Date end,
                     String duid, int no_units, double storageCapacityMwh, String fuelBucketSummary)  {
        this.id = genId;
        this.region = region;
        this.assetType = assetType;
        this.name = genName;
        this.ownerName = owner;
        this.maxCapacity = gencap;
        this.techTypeDescriptor = techType.trim();
        this.fuelSourceDescriptor = fuelType.trim();
        this.dispatchTypeDescriptor = dispachType;
        this.assetRelationships = new ArrayList<>();
        this.diameter = 50.0;
        this.start = start;

        Calendar c = Calendar.getInstance();
        c.setTime(this.start);
        this.startYear = c.get(Calendar.YEAR);

        this.end = end;

        if( this.end == null )
            this.end = expectedEnd;

        this.duid = duid;
        this.numUnits = no_units;
        this.storageCapacity = storageCapacityMwh;
        this.fuelBucketSummary = null;
        if(fuelBucketSummary != null)
            this.fuelBucketSummary = fuelBucketSummary.trim();



        this.location = null;

        //default lifecycle of 30 years
        this.lifecycle = 30;

        historicCapacityFactor = getCapacityFactor(1);

    }

    static public double getSolarMonthlyGeneration(float solarExporsure, float capacitySolar){
        double generation = 0.0;

        //de-rating factor for manufacturing tolerance, dimensionless
        double fman = 1 - 0.03;
        //de-rating factor for dirt, dimensionless
        double fdirt = 1 - 0.05 ;
        //temperature de-rating factor, dimensionless, ƒtemp = 1 + (γ × (avg temp) y=-.005 * 20
        //y is the temperature coefficient, using example from CEC guidelines as -0.5%/C and average daily temperature of 20C
        //the de-rating factor increases with increasing average daily temperatures.
        double ftemp = 1 + (-0.005*20);
        //efficiency of the subsystem (cables) between the PV array
        //and the inverter (DC cable loss)
        double npv_inv = 1 - 0.03;
        //efficiency of the inverter. Typically 0.9
        double ninv = 1 - 0.1;
        //efficiency of the subsystem (cables) between the inverter and the switchboard (AC cable loss)
        // recommended voltage drop between inverter and main swithc shouldn't be greater than 1%
        double ninv_sb = 1 - 0.01 ;
        //TODO: check units!
        generation = capacitySolar *fman * fdirt * ftemp * solarExporsure  * npv_inv * ninv * ninv_sb;

        //in kWh
        return generation;
    }

    //returns MWh
    public double getGeneration(Gr4spSim data, Date d, HashMap<Date, Integer> newHouseholdsPerDate){
        double generation = 0.0;

        if(fuelSourceDescriptor.equalsIgnoreCase("solar")){
            if( data.getSolar_exposure().containsKey(d) ) {

                //Get number of days in month
                Calendar c = Calendar.getInstance();
                c.setTime(d);
                int month = c.get(Calendar.MONTH)+1;
                int year = c.get(Calendar.YEAR);
                YearMonth yearMonthObject = YearMonth.of( year , month );
                int daysInMonth = yearMonthObject.lengthOfMonth(); //28

                //get Generation in MWh
                float solarExposure = (data.getSolar_exposure().get(d));//mean daily solar exposure in MJ/m^2 TODO: correct for MWh (dividing by 3.6?)

                //Compute generation taking into account the date when households installed solar (and number), and the avg size.
                for (HashMap.Entry pair : newHouseholdsPerDate.entrySet()) {

                    //date of new household installation solar pv
                    Date newHouseholdDate = (Date) pair.getKey();

                    //num households installed solar pv
                    int numNewHouseholds = (int) pair.getValue();

                    //ave solar installation on that date
                    float solar_installation_capacity = 0;

                    if(data.getSolar_installation_kw().containsKey(newHouseholdDate)) {
                        solar_installation_capacity = data.getSolar_installation_kw().get(newHouseholdDate);
                    }

                    //daily generation in MWh given sun
                    double dailyGeneration = getSolarMonthlyGeneration(solarExposure, solar_installation_capacity) / 1000.0;

                    //Generation per month over all households that installed solarpv on that month
                    generation += (dailyGeneration * (double)daysInMonth) * numNewHouseholds;

                }


            }
        }else{
            throw new java.lang.UnsupportedOperationException("Not supported yet. Only support on-site Solar generation"); //TODO: and solar utility?
        }
        return generation;

    }

    //Once the generator has been running for longer than its lifecycle (30 years by default), the Emission Factor will stop growing,
    //It will remain at the 30 year level
    //Brown Coal generators prior 1964 (year Hazelwood started operating) had less thermal efficiency
    public double getEmissionsFactor(int currentYear) {

        double minEF = 0;
        double linRateEF = 0;
        double expRateEF = 0;

        if (this.getFuelSourceDescriptor().equalsIgnoreCase("Brown Coal")){
            if(this.startYear < 1964 ) {
                minEF = 1.19;
                linRateEF = 0.01;
                expRateEF = 0.178;
            }
            else{
                minEF = 0.49;
                linRateEF = 0.01;
                expRateEF = 0.151;
            }
        }
        // Natural gas with two different technologies
        if (this.getFuelSourceDescriptor().contains("Natural Gas") &&
                (this.getTechTypeDescriptor().contains("OCGT")) ){
            minEF = 1.19;
            linRateEF = 0.01;
            expRateEF = 0.178;
        }
        if (this.getFuelSourceDescriptor().contains("Natural Gas") &&
                (this.getTechTypeDescriptor().equalsIgnoreCase("Turbine - Steam Sub Critical")) ){
            minEF = 1.19;
            linRateEF = 0.01;
            expRateEF = 0.178;
        }
        if (this.getFuelSourceDescriptor().equalsIgnoreCase("Water")){
            minEF = 0;
            linRateEF = 0;
            expRateEF = 0;
        }
        if (this.getFuelSourceDescriptor().equalsIgnoreCase("Wind")){
            minEF = 0;
            linRateEF = 0;
            expRateEF = 0;
        }

        int age = currentYear - startYear;
        if(age > this.lifecycle) age = this.lifecycle;

        double emissionsFactor =  minEF +  ( linRateEF * (Math.exp( expRateEF * (age) ) ) );

        return emissionsFactor;

    }


    private int monthsInSpot(SimState state){
        Gr4spSim data = (Gr4spSim) state;

        Date today = data.getCurrentSimDate();
        Calendar c = Calendar.getInstance();
        c.setTime(today);
        int currentMonth = c.get(Calendar.MONTH) + 1;
        int currentYear = c.get(Calendar.YEAR) + 1;
        if(this.start.after( data.getStartSpotMarketDate() )) {
            c.setTime(this.start);
        }
        else{
            c.setTime(data.getStartSpotMarketDate());
        }
        int startMonth = c.get(Calendar.MONTH) + 1;
        int startYear = c.get(Calendar.YEAR) + 1;

        int months = (currentYear-startYear)*12 + currentMonth - startMonth;

        return months;
    }
    public void updateHistoricCapacityFactor(SimState state) {


        int rounds = monthsInSpot(state);

        // Update Capacity based on historic amount sold, and reset every year
//        if(rounds%12 > 0)
//            historicCapacityFactor = historicGeneratedMW / (maxCapacity * (double)rounds);
//        else
//            historicCapacityFactor = getCapacityFactor(1);

        // Update Capacity based on historic amount sold
        if(rounds > 0)
            historicCapacityFactor = historicGeneratedMW / (maxCapacity * (double)rounds);

    }

    public double priceMWhLCOE(){

        double result =  priceMinMWh() +  ( (priceMaxMWh()-priceMinMWh()) * (Math.exp( - priceRateParameterMWh() * historicCapacityFactor)) );
        //double meanResult =

        return result;
    }



    public double priceMinMWh(){
        double dollarMWh = 0.0;

        if (this.getFuelSourceDescriptor().equalsIgnoreCase("Brown Coal")){ dollarMWh = 43; }
        // Natural gas with two different technologies
        if (this.getFuelSourceDescriptor().contains("Natural Gas") &&
                (this.getTechTypeDescriptor().contains("OCGT")) ){
            dollarMWh = 63;
        }
        if (this.getFuelSourceDescriptor().contains("Natural Gas") &&
                (this.getTechTypeDescriptor().equalsIgnoreCase("Turbine - Steam Sub Critical")) ){
            dollarMWh = 58;
        }
        if (this.getFuelSourceDescriptor().equalsIgnoreCase("Water")){ dollarMWh = 57; }
        if (this.getFuelSourceDescriptor().equalsIgnoreCase("Wind")){ dollarMWh = 44; }

        return dollarMWh;
    }

    public double priceMaxMWh(){
        double dollarMWh = 0.0;

        if (this.getFuelSourceDescriptor().equalsIgnoreCase("Brown Coal")){ dollarMWh = 250; }
        if (this.getFuelSourceDescriptor().contains("Natural Gas") &&
                (this.getTechTypeDescriptor().contains("OCGT")) ){ dollarMWh = 800; }
        if (this.getFuelSourceDescriptor().contains("Natural Gas") &&
                (this.getTechTypeDescriptor().equalsIgnoreCase("Turbine - Steam Sub Critical")) ){ dollarMWh = 600; }
        if (this.getFuelSourceDescriptor().equalsIgnoreCase("Water")){ dollarMWh = 900; }
        if (this.getFuelSourceDescriptor().equalsIgnoreCase("Wind")){ dollarMWh = 250; }

        return dollarMWh;
    }

    public double priceRateParameterMWh(){
        double dollarMWh = 0.0;

        if (this.getFuelSourceDescriptor().equalsIgnoreCase("Brown Coal")){ dollarMWh = 7; }
        if (this.getFuelSourceDescriptor().contains("Natural Gas") &&
                (this.getTechTypeDescriptor().contains("OCGT")) ){ dollarMWh = 5; }
        if (this.getFuelSourceDescriptor().contains("Natural Gas") &&
                (this.getTechTypeDescriptor().equalsIgnoreCase("Turbine - Steam Sub Critical")) ){ dollarMWh = 5; }
        if (this.getFuelSourceDescriptor().equalsIgnoreCase("Water")){ dollarMWh = 7; }
        if (this.getFuelSourceDescriptor().equalsIgnoreCase("Wind")){ dollarMWh = 7; }

        return dollarMWh;
    }

//    public double priceMWh(){
//        double dollarMWh = 0.0;
//
//        if (this.getFuelSourceDescriptor().equalsIgnoreCase("Brown Coal")){ dollarMWh = 50; }
//        if (this.getFuelSourceDescriptor().equalsIgnoreCase("Natural Gas")){ dollarMWh = 100; }
//        if (this.getFuelSourceDescriptor().equalsIgnoreCase("Water")){ dollarMWh = 60; }
//        if (this.getFuelSourceDescriptor().equalsIgnoreCase("Wind")){ dollarMWh = 100; }
//
//        return dollarMWh;
//    }

    public double getCapacityFactor( int currentMonth ){
        //capactiy 0.95 (Disesl, Waste Water, LandFill, etc.)
        double capacityFactor = 0.95;

        if (this.getFuelSourceDescriptor().equalsIgnoreCase("Brown Coal")){
            if( currentMonth > 11  && currentMonth < 3 ) { //Summer from December to March 1
                capacityFactor = 0.9; // all brown coal generators offer 90% of their nameplate capacity for summer periods
            }else {
                capacityFactor = 0.95; // all brown coal generators offer 95% of their nameplate capacity for summer periods
            }
        }
        if (this.getFuelSourceDescriptor().contains("Natural Gas")){
            capacityFactor = 0.85;
        }
        if (this.getFuelSourceDescriptor().equalsIgnoreCase("Water")){
            capacityFactor = 0.95;
        }
        if (this.getFuelSourceDescriptor().equalsIgnoreCase("Wind")){
            if( currentMonth > 11  && currentMonth < 3 ) { //Summer from December to March 1
                capacityFactor = 0.081;
            }else {
                capacityFactor = 0.073;
            }
        }
        return capacityFactor;
    }
    //returns MW
    public double computeMonthlyAvailableCapacity(SimState state) {
        Gr4spSim data = (Gr4spSim) state;
        Date today = data.getCurrentSimDate();
        Calendar c = Calendar.getInstance();
        c.setTime(today);
        int currentMonth = c.get(Calendar.MONTH) + 1;

        //Solar doesn't use capacity factor, but instead uses the available solar exposure by month
        if (this.getFuelSourceDescriptor().equalsIgnoreCase("Solar")){
            double dollarMWh = 0;

            //get Generation in MWh
            float solarExposure = data.getSolar_exposure().get(today);

            //daily generation in MWh given sun
            double dailyGeneration = this.getSolarMonthlyGeneration(solarExposure, (float)this.getMaxCapacity() ) ;

            //convert to MW by dividing duration of a day
            double availableCapacity = dailyGeneration / 24;

            return  availableCapacity;

        }

        double capacityFactor = getCapacityFactor( currentMonth );

        double availableCapacity = this.getMaxCapacity() * capacityFactor;

        return availableCapacity;
    }

    @Override
    public double diameter() {
        return diameter;
    }

    @Override
    public String toString() {
        return "Generator [id=" + id +  ", name=" + name + ", fuelSourceDescriptor="
                + fuelSourceDescriptor + ", ownership=" + ownership + ", techTypeDescriptor=" + techTypeDescriptor
                + ", maxCapacity=" + maxCapacity + ", efficiency=" + efficiency
                + ", lifecycle=" + lifecycle + ", " +
                 ", location=" + location
                + "]";//need to include capex in this constructor if used
    }

    @Override
    public void addAssetRelationship( ActorAssetRelationship newAssetRel){
        this.assetRelationships.add(newAssetRel);
    }

    public int getId() {
        return id;
    }


    public void setId(int id) {
        this.id = id;
    }


    public String getfuelSourceDescriptor() {
        return fuelSourceDescriptor;
    }


    public void setfuelSourceDescriptor(String fuelSourceDescriptor) {
        this.fuelSourceDescriptor = fuelSourceDescriptor;
    }


    public double getHistoricGeneratedMW() {
        return historicGeneratedMW;
    }

    public void setHistoricGeneratedMW(double historicGeneratedMW) {
        this.historicGeneratedMW = historicGeneratedMW;
    }

    public Date getStart() {
        return start;
    }

    public Date getEnd() {
        return end;
    }

    public String getName() {
        return name;
    }


    public void setName(String name) {
        this.name = name;
    }


    public double getMaxCapacity() {
        return maxCapacity;
    }


    public void setMaxCapacity(double maxCapacity) {
        this.maxCapacity = maxCapacity;
    }


    public double getEfficiency() {
        return efficiency;
    }


    public void setEfficiency(double efficiency) {
        this.efficiency = efficiency;
    }


    public int getLifecycle() {
        return lifecycle;
    }


    public void setLifecycle(int lifecycle) {
        this.lifecycle = lifecycle;
    }




    public Double2D getLocation() {
        return location;
    }


    public void setLocation(Double2D location) {
        this.location = location;
    }


    public String getOwnership() {
        return ownership;
    }


    public void setOwnership(String ownership) {
        this.ownership = ownership;
    }


    public String getFuelSourceDescriptor() {
        return fuelSourceDescriptor;
    }


    public void setFuelSourceDescriptor(String fuelSourceDescriptor) {
        this.fuelSourceDescriptor = fuelSourceDescriptor;
    }


    public String getTechTypeDescriptor() {
        return techTypeDescriptor;
    }


    public void setTechTypeDescriptor(String techTypeDescriptor) {
        this.techTypeDescriptor = techTypeDescriptor;
    }


    public double getHistoricCapacityFactor() {
        return historicCapacityFactor;
    }

    public String getDispatchTypeDescriptor() {
        return dispatchTypeDescriptor;
    }
}