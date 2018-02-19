package core;

import java.awt.Color;
import javax.swing.JFrame;
import sim.display.Controller;
import sim.display.Display2D;
import sim.display.GUIState;
import sim.engine.SimState;
import sim.portrayal.continuous.ContinuousPortrayal2D;
import sim.portrayal.network.NetworkPortrayal2D;
import sim.portrayal.network.SpatialNetwork2D;

public class Gr4spSimUI extends GUIState {
    public Display2D display;
    public JFrame displayFrame;
    NetworkPortrayal2D edgePortrayal = new NetworkPortrayal2D();
    ContinuousPortrayal2D nodePortrayal = new ContinuousPortrayal2D();

    public static void main(String[] args) {
        (new Gr4spSimUI()).createController();
    }

    public Gr4spSimUI() {
        super(new Gr4spSim(System.currentTimeMillis()));
    }

    public Gr4spSimUI(SimState state) {
        super(state);
    }

    public static String getName() {
        return "Gr4spSim: Generic Recursive Representation of Systems for Service Provision";
    }

    public Object getSimulationInspectedObject() {
        return this.state;
    }

    public void start() {
        super.start();
        this.setupPortrayals();
    }

    public void load(SimState state) {
        super.load(state);
        this.setupPortrayals();
    }

    public void setupPortrayals() {
        Gr4spSim gr4sp = (Gr4spSim) this.state;
        //this.edgePortrayal.setField(new SpatialNetwork2D(gr4sp.balls, gr4sp.bands));
        //this.edgePortrayal.setPortrayalForAll(new BandPortrayal2D());
        this.nodePortrayal.setField(gr4sp.layout);
        this.display.reset();
        this.display.setBackdrop(Color.white);
        this.display.repaint();
    }

    public void init(Controller c) {
        super.init(c);
        this.display = new Display2D(1000.0D, 1000.0D, this);
        this.display.setClipping(false);
        this.displayFrame = this.display.createFrame();
        this.displayFrame.setTitle("Gr4spSim Display");
        c.registerFrame(this.displayFrame);
        this.displayFrame.setVisible(true);
        //this.display.attach(this.edgePortrayal, "Bands");
        this.display.attach(this.nodePortrayal, "End Users and SPMs");
    }

    public void quit() {
        super.quit();
        if (this.displayFrame != null) {
            this.displayFrame.dispose();
        }

        this.displayFrame = null;
        this.display = null;
    }
}


