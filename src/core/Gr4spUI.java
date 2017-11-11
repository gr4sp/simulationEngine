/*
  Copyright 2006 by Sean Luke and George Mason University
  Licensed under the Academic Free License version 3.0
  See the file "LICENSE" for more information
*/

package core;

import sim.portrayal3d.grid.*;
import sim.portrayal3d.grid.quad.*;
import sim.portrayal3d.simple.*;
import sim.engine.*;
import sim.field.continuous.Continuous2D;
import sim.portrayal.continuous.ContinuousPortrayal2D;
import sim.portrayal.simple.ImagePortrayal2D;
import sim.app.tutorial6.Tutorial6;
import sim.display.*;
import sim.display3d.*;
import sim.util.gui.*;
import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;

public class Gr4spUI extends GUIState
    {
    public Display2D display;
    public JFrame displayFrame;
    public ContinuousPortrayal2D mapPortrayal;
    public double width;
    public double height;
    
    
    
    public static void main(String[] args)
        {
        new Gr4spUI().createController();
        }

    /* 
     * constructor for Gr4spUI when doesn't receive a SimState 
     * creates the DataManager, which extends the SimState class.
     */
    public Gr4spUI() { 
    	super(new DataManager( System.currentTimeMillis())); 
    	
    	width = 600;
    	height = 600;
    }
    
    /* 
     * constructor. initializes the Gr4spUI with a SimState
     */
    public Gr4spUI(SimState state) { 
    	super(state); 
    	
    	width = 600;
    	height = 600;
    }
    
    public static String getName() { return "Gr4sp-Map Display with SPMs"; }


    public void start() {
        super.start();
        setupPortrayals();
    }

    public void load(SimState state) {
        super.load(state);
        setupPortrayals();
    }

    public void setupPortrayals() {

        DataManager data = (DataManager) state;
             
        
       
        // reschedule the displayer
        display.reset();
        //display.setBackdrop(null); //Color.white
        
        Image i = new ImageIcon(getClass().getResource("images/Parkville_SA1.png")).getImage();
        BufferedImage b = display.getGraphicsConfiguration().createCompatibleImage(i.getWidth(null), i.getHeight(null));
        Graphics g = b.getGraphics();
        g.drawImage(i,0,0,i.getWidth(null),i.getHeight(null),null);
        g.dispose();	
        display.setBackdrop(new TexturePaint(b, new Rectangle(0,0,i.getWidth(null),i.getHeight(null))));
                
        
        // redraw the display
        display.repaint();
    }

    public void quit() {
        super.quit();

        if (displayFrame!=null) displayFrame.dispose();
        displayFrame = null;
        display = null;
    }
    
    /** Gets an image relative to the DataManager directory */
    public static Image loadImage(String filename) { 
        return new ImageIcon(DataManager.class.getResource(filename)).getImage(); 
    }

    public void init(Controller c)
        {
        super.init(c);

        DataManager data = (DataManager) state;
        
        
        // make the displayer
        display = new Display2D(width,height,this);

        
        // turn off clipping
        display.setClipping(false);

        displayFrame = display.createFrame();
        displayFrame.setTitle("Gr4sp Display");
        c.registerFrame(displayFrame);   // register the frame so it appears in the "Display" list
        displayFrame.setVisible(true);
        }
    }
