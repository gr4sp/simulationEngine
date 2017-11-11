/*
  Copyright 2006 by Sean Luke and George Mason University
  Licensed under the Academic Free License version 3.0
  See the file "LICENSE" for more information
*/

package core;
import sim.engine.*;
import sim.portrayal.*;
import sim.util.*;
import sim.field.network.*;
import sim.field.continuous.*;
import java.awt.*;
import java.awt.geom.*;


public class Spm extends SimplePortrayal2D implements Steppable
    {
    private static final long serialVersionUID = 1;

    //Generators
    public Bag generators;
    
    //Distribution Networdk
    public EnergyGrid gridType;
    public Network GridNetwork;
    
    //Storage
    public Storage storageType;
    public boolean hasStorage;
    
    //Interface: Connection Point
    public ConnectionPoint connectionPoint;
    
    //Efficiency
    public double efficiency;
    
    //Embodied GHG Emissions
    public double embGHG;
    
    //Costs
    public double installCosts;
    public double maintenanceCosts;
        
    
    //Ownerships
    //TODO: add list of owners
        
    
    public Spm()
        {
        
        }

        
    public void step(SimState state)
        {
        DataManager tut = (DataManager) state;
        
        
        // position = position + velocity
        //Double2D pos = tut.balls.getObjectLocation(this);
        //Double2D newpos = new Double2D(pos.x+velocityx, pos.y + velocityy);
        //tut.balls.setObjectLocation(this,newpos);
        
        }
    
    
    public void draw(Object object, Graphics2D graphics, DrawInfo2D info)
        {
        final double width = info.draw.width;
        final double height = info.draw.height;

        graphics.setColor(Color.blue);

        final int x = (int)(info.draw.x - width / 2.0);
        final int y = (int)(info.draw.y - height / 2.0);
        final int w = (int)(width);
        final int h = (int)(height);

        // draw centered on the origin
        graphics.fillOval(x,y,w,h);
        }
    
    
    }
    
