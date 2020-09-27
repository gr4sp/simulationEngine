package core.Policies;


import core.Gr4spSim;
import core.Technical.Generator;
import core.Technical.Spm;
import sim.engine.SimState;
import sim.engine.Steppable;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;

public class SimPolicies implements java.io.Serializable, Steppable {

    private EndConsumerTariff endConsumerTariff;
    private AccelerateSolarPV accelerateSolarPV;
    private int availableIDGen;
    Random random;

    public SimPolicies() {
        this.endConsumerTariff = EndConsumerTariff.MAX;
        this.accelerateSolarPV = new AccelerateSolarPV(0.0);
        availableIDGen = 1;
        random = new Random();
    }

    public EndConsumerTariff getEndConsumerTariff() {
        return endConsumerTariff;
    }

    public void setEndConsumerTariffs(EndConsumerTariff endConsumerTariff) {
        this.endConsumerTariff = endConsumerTariff;
    }

    public AccelerateSolarPV getAccelerateSolarPV() {
        return accelerateSolarPV;
    }

    public void setAccelerateSolarPV(AccelerateSolarPV accelerateSolarPV) {
        this.accelerateSolarPV = accelerateSolarPV;
    }

    private Date getDate(String sdate) {
        Date date = null;
        SimpleDateFormat stringToDate = new SimpleDateFormat("yyyy-MM-dd");
        try {
            date = stringToDate.parse(sdate);

        } catch (ParseException e) {
            System.err.println("Cannot parse Start Date: " + e.toString());
        }
        return date;
    }

    private int findAvailableIDGen( SimState simState ){
        Gr4spSim data = (Gr4spSim) simState;
        while ( data.getGen_register().containsKey(availableIDGen) ) availableIDGen++;
        return availableIDGen;
    }

    @Override
    public void step(SimState simState) {
        Gr4spSim data = (Gr4spSim) simState;

        //Apply Houshold conversion policy only from 2010

        //TODO: Use Diffusion model as S-Curve logistic function (see https://github.com/angelara/gr4sp/issues/2)
        // INTERACTIVE LINK WITH FORMULA: https://www.desmos.com/calculator/agxuc5gip8
        // IMPLEMENTATION IN JAVA: https://commons.apache.org/proper/commons-math/javadocs/api-3.1.1/org/apache/commons/math3/analysis/function/Logistic.html
//
//        if(data.getCurrentSimDate().after( getDate("2010-01-01"))) {
//            double rnd = random.nextDouble();
//            double uptakePercentage = rnd * accelerateSolarPV.getMonthlyHousholdsPercentageConversion();
//            data.convertIntoNonConventionalHouseholds(uptakePercentage);
//        }

        Calendar c = Calendar.getInstance();
        c.setTime(data.getCurrentSimDate());
        int currentMonth = c.get(Calendar.MONTH) + 1;
        int currentYear = c.get(Calendar.YEAR);

        //Improve capacity factors of Wind renewable generators as a proxy of Technology Improvement (YAML)
        //Improve price LCOE according to learning curve.
        //&& currentYear > data.settings.getBaseYearConsumptionForecast()
        if(currentMonth==1 ) {
            double factor = data.settings.getForecastTechnologicalImprovement();
            double learningCurve = data.settings.getLearningCurve();

            // WIND
            double WindBasePriceMWh = data.settings.getBasePriceMWh("Wind","") * (1-learningCurve);
            double WindMarketPriceCap = data.settings.getMarketPriceCap("Wind","") * (1-learningCurve);
            double WindMaxCapacityFactor = data.settings.getMaxCapacityFactor("Wind","");
            double WindMaxCapacityFactorSummer = data.settings.getMaxCapacityFactorSummer("Wind","");

            // Assuming a limit of 50% maximum for Wind technology (https://reneweconomy.com.au/new-australian-wind-farms-reach-nearly-50-capacity-factor-99179/)
            if (WindMaxCapacityFactor + factor <= 0.5)
                data.settings.setMaxCapacityFactor("Wind","", WindMaxCapacityFactor + factor);
            if (WindMaxCapacityFactorSummer + factor <= 0.5)
                data.settings.setMaxCapacityFactorSummer("Wind","", WindMaxCapacityFactorSummer + factor);
            data.settings.setBasePriceMWh("Wind","",WindBasePriceMWh);
            data.settings.setMarketPriceCap("Wind","",WindMarketPriceCap);

            //SOLAR
            double SolarBasePriceMWh = data.settings.getBasePriceMWh("Solar","") * (1-learningCurve);
            double SolarMarketPriceCap = data.settings.getMarketPriceCap("Solar","") * (1-learningCurve);
            double solarEfficiency = data.settings.getSolarEfficiency("Solar","");

            data.settings.setBasePriceMWh("Solar","", SolarBasePriceMWh);
            data.settings.setMarketPriceCap("Solar","",SolarMarketPriceCap);
            if (solarEfficiency + factor < 1.0) data.settings.improveSolarEfficiency( factor );


//
//            for (Vector<Generator> gens : data.getGen_register().values()) {
//                for (Generator g : gens) {
//                    if (g.getfuelSourceDescriptor().equals("Wind")) {
//
//                        if (g.maxCapacityFactor + factor < 0.5) g.maxCapacityFactor += factor;
//                        if (g.maxCapacityFactorSummer + factor < 0.5) g.maxCapacityFactorSummer += factor;
//
//                        g.priceMinMWh = g.priceMinMWh * (1-learningCurve);
//                        g.priceMaxMWh = g.priceMaxMWh * (1-learningCurve);
//                    }else if (g.getfuelSourceDescriptor().equals("Solar")) {
//                        if (g.solarEfficiency + factor < 1.0) g.solarEfficiency += factor;
//
//                        g.priceMinMWh = g.priceMinMWh * (1-learningCurve);
//                        g.priceMaxMWh = g.priceMaxMWh * (1-learningCurve);
//                    }
//                }
//            }
        }

        Date date = data.getCurrentSimDate();
        if (data.getSolar_number_installs().containsKey(date) == true) {

            float solar_capacity_agg = data.getSolar_aggregated_kw().get(date) / (float) 1000.0; //to MW

            //Create the end date 30 years after
            c.setTime(date);
            c.add(Calendar.YEAR, 30);
            Date endDate = c.getTime();

            // If a new Generator is added, make sure its id hasn't been used
            int idGen = findAvailableIDGen(data);

            //created a Vector to access the subdistribution SPMs
            Vector<Spm> subdistribution_spms = data.getSpm_register().get(6);
            int num_subdistribution_spms = subdistribution_spms.size();

            // for loop to get the each subdistribution SPM and distribute equally the aggregated solar capacity of the new generators
            for (int i = 0; i < num_subdistribution_spms; i++) {

                //Divide agg capacity by num of SPMs
                double solar_cap_per_spm = (double) solar_capacity_agg /  (double)  num_subdistribution_spms;

                //Divide num_installations by num of SPMs
                int num_installs = data.getSolar_number_installs().get(date) / num_subdistribution_spms;

                Generator gen = new Generator(idGen, "VIC1", "Rooftop Solar PV",
                        "Solar Rooftop", "Household", "Solar - Rooftop",
                        "Solar", solar_cap_per_spm , "NS", date, endDate, endDate,
                        "", num_installs, 0.0, "Solar", "In Service",data.settings);

                //Get from Map the vector of GENERATORS with key = idGen, and add the new Generator to the vector
                if (!data.getGen_register().containsKey(idGen))
                    data.getGen_register().put(idGen, new Vector<>());

                data.getGen_register().get(idGen).add(gen);

                data.setNumGenerators(data.getNumGenerators() + 1);

                //Add solar generators to SPMs
                subdistribution_spms.get(i).addGenerator(gen);
            }

        }
    }
}