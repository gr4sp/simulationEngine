package core.Policies;


import core.Gr4spSim;
import sim.engine.SimState;
import sim.engine.Steppable;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;

public class SimPolicies implements java.io.Serializable, Steppable {

    private EndConsumerTariff endConsumerTariff;
    private AccelerateSolarPV accelerateSolarPV;
    Random random;

    public SimPolicies() {
        this.endConsumerTariff = EndConsumerTariff.MAX;
        this.accelerateSolarPV = new AccelerateSolarPV(0.0);
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

    private Date getDate(String sdate ) {
        Date date = null;
        SimpleDateFormat stringToDate = new SimpleDateFormat("yyyy-MM-dd");
        try {
            date = stringToDate.parse(sdate);

        } catch (ParseException e) {
            System.err.println("Cannot parse Start Date: " + e.toString());
        }
        return date;
    }

    @Override
    public void step(SimState simState) {
        Gr4spSim data = (Gr4spSim) simState;

        //Apply Houshold conversion policy only from 2010
        if(data.getCurrentSimDate().after( getDate("2010-01-01"))) {
            double rnd = random.nextDouble();
            double uptakePercentage = rnd * accelerateSolarPV.getMonthlyHousholdsPercentageConversion();
            data.convertIntoNonConventionalHouseholds(uptakePercentage);
        }
    }

}