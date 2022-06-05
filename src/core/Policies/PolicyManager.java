package core.Policies;

import java.lang.reflect.Array;
import java.text.SimpleDateFormat;
import java.util.*;

import core.Gr4spSim;
import core.Relationships.Arena;
import core.Relationships.SpotMarket;
import core.Technical.Generator;
import sim.engine.SimState;
import sim.engine.Steppable;

public class PolicyManager implements Steppable, java.io.Serializable {

    public HashMap<Integer, Integer> actions;
    public Integer currAction = -1;
    public Gr4spSim data;
    private SimpleDateFormat dateToString = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

    private double og_learning;
    private double og_tech;

    private Queue<Integer> actionQueue = new LinkedList<>();

    // A pathway is defined at instantiation. True for DAPP.
    public Boolean pathwayDefined = false;

    public Calendar cal;

    public static int yearsBetweenActions = 4;

    // public Boolean tippingPoint = false;
    public double[] indValues;

    public Integer[] pastActions;


    public PolicyManager(Gr4spSim data) {
        this.data = data;

        og_learning = this.data.settingsAfterBaseYear.getLearningCurve();
        og_tech = this.data.settingsAfterBaseYear.getForecastTechnologicalImprovement();

        cal = Calendar.getInstance();
        pastActions = new Integer[8];

        if (Gr4spSim.pathway != null) {
            setPathway(Arrays.asList(Gr4spSim.pathway));
            if (this.data.tippingPoint) this.setQueue(Arrays.asList(Gr4spSim.pathway));
        } else {
            pathwayDefined = false;
        }

    }

    public void setQueue(List<Integer> pathway){
        for(Integer i: pathway) this.actionQueue.add(i);
    }

    public void setPathway(List<Integer> pathway) {
        int c = 0;
    
        this.actions = new HashMap<>();
        for (Integer i : pathway) {
            this.actions.put(2022 + c, i);
            c += yearsBetweenActions;
        }

        pathwayDefined = true;
    }

    public Integer getCurrAction() {
        return currAction;
    }

    public Integer getCurrAction(Integer year) {
        return actions.get(year);
    }

    public Boolean hasTipped(int year){
        double[] newTips = this.data.getAveIndsTipping(year);
        int tipped = 0;
        for(int i = 0; i < newTips.length; i++){
            if (i != 1){
                if (newTips[i] > indValues[i]){
                    tipped++;

                }
            } else{
                if (newTips[i] < indValues[i]){
                    tipped++;
                }
            }
        }
        this.indValues = newTips;
        return tipped >= 3;

    }

    @Override
    public void step(SimState state) {
        Gr4spSim data = (Gr4spSim) state;

        // Reset all actions
        resetActions();

        cal.setTime(data.getCurrentSimDate());
        int year = cal.get(Calendar.YEAR);
        if (this.data.tippingPoint && year == 2022){
            this.indValues = this.data.getAveIndsTipping(year);
            currAction = this.actionQueue.poll();
        }
        // Activate next action
        if (pathwayDefined) {
            if (!this.data.tippingPoint){

                currAction = year == 2050 ? currAction : actions.get(year);
            } else{
                if (this.hasTipped(year)){
                    currAction = this.actionQueue.poll();
                }
            }
        }

        pastActions[(year-2022)/4] = currAction;

       switch (currAction) {
           case 0:
               // Price is handled already to adjust for inflation
               Generator.carbonTax = true;
               
               break;
           case 1:
               Gr4spSim.isSecondary = true;
               Gr4spSim.isSecondaryVal = 30.00;
               this.data.settings.setSemiSecondary("secondary", Gr4spSim.isSecondaryVal);
               this.data.settingsAfterBaseYear.setSemiSecondary("secondary", Gr4spSim.isSecondaryVal);
               
               break;
           case 2:

               Gr4spSim.learning = true;
               this.data.settings.setLearningCurve(this.og_learning * 1.15);
               this.data.settingsAfterBaseYear.setLearningCurve(this.og_learning * 1.15);
               
               break;
           case 3:
               Arena.reduceTopPct = false;
               Arena.reducedCapacity = true;
               Arena.reducedCapacityValue = 0.2;
               Arena.reducedCapacityGens = 1;
               
               break;
           case 4:
               Arena.reduceTopPct = false;
               Arena.reducedCapacity = true;
               Arena.reducedCapacityValue = 0.1;
               Arena.reducedCapacityGens = 1;
               
               break;
           case 5:
               Gr4spSim.techno = true;
               this.data.settings.setForecastTechnologicalImprovement(this.og_tech * 1.15);
               this.data.settingsAfterBaseYear.setForecastTechnologicalImprovement(this.og_tech * 1.15);
               
               break;
           case 6:
               SpotMarket.efMerit = true;
               
               break;
           case 7:
               Arena.reduceTopPct = true;
               Arena.reducedCapacity = true;
               Arena.reducedCapacityValue = 0.05;
               Arena.reducedCapacityGens = 0.01;
               
               break;
           case 8:
               Generator.subExists = true;
               Generator.renewableSubVal = 1.0 - 0.1;
               
               break;
           default:
               break;

       }

    }

    /**
     * Reset all action flags in the program.
     */
    public void resetActions() {

        Generator.carbonTax = false;

        Gr4spSim.isSecondary = false;
        Gr4spSim.isSecondaryVal = 30.00;
        this.data.settings.setSemiSecondary("primary", Gr4spSim.isSecondaryVal);
        this.data.settingsAfterBaseYear.setSemiSecondary("primary", Gr4spSim.isSecondaryVal);

        Gr4spSim.learning = false;
        this.data.settings.setLearningCurve(this.og_learning);
        this.data.settingsAfterBaseYear.setLearningCurve(this.og_learning);

        Arena.reducedCapacity = false;

        this.data.settings.setForecastTechnologicalImprovement(this.og_tech);
        this.data.settingsAfterBaseYear.setForecastTechnologicalImprovement(this.og_tech);

        SpotMarket.efMerit = false;

        Generator.subExists = false;

    }
}
