package core;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.geom.Ellipse2D.Double;
import sim.engine.SimState;
import sim.engine.Steppable;
import sim.portrayal.DrawInfo2D;
import sim.portrayal.SimplePortrayal2D;
import sim.util.Double2D;

import java.awt.*;


public class Enduse extends SimplePortrayal2D implements Steppable {

    private int id;
    private String location_code;
    private String spm_name;
    private String category_type;
    private String dwelling_type;

    private Spm spm_end_use;


    public Enduse (int id, String location_code, Spm spmEndUse, String spm_name, String category_type, String dwelling_type){
        this.id = id;
        this.location_code = location_code;
        this.spm_end_use = spmEndUse;
        this.spm_name = spm_name;
        this.category_type = category_type;
        this.dwelling_type = dwelling_type;
    }

    public Spm getSpm_end_use() {
        return spm_end_use;
    }

    public void setSpm_end_use(Spm spm_end_use) {
        this.spm_end_use = spm_end_use;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getLocation_code() {
        return location_code;
    }

    public void setLocation_code(String location_code) {
        this.location_code = location_code;
    }


    public String getSpm_name() {
        return spm_name;
    }

    public void setSpm_name(String spm_name) {
        this.spm_name = spm_name;
    }

    public String getCategory_type() {
        return category_type;
    }

    public void setCategory_type(String category_type) {
        this.category_type = category_type;
    }

    public String getDwelling_type() {
        return dwelling_type;
    }

    public void setDwelling_type(String dwelling_type) {
        this.dwelling_type = dwelling_type;
    }

    @Override
    public void step(SimState simState) {

    }
    public void draw(Object object, Graphics2D graphics, DrawInfo2D info) {
        double width = info.draw.width * 60;
        double height = info.draw.height * 60;

        graphics.setColor(Color.blue);


        int x = (int)(info.draw.x - width / 2.0D);
        int y = (int)(info.draw.y - height / 2.0D);
        int w = (int)width;
        int h = (int)height;
        graphics.fillOval(x, y, w, h);
    }

    public boolean hitObject(Object object, DrawInfo2D range) {
        double SLOP = 1.0D;
        double width = range.draw.width * 10;
        double height = range.draw.height * 10;
        Double ellipse = new Double(range.draw.x - width / 2.0D - 1.0D, range.draw.y - height / 2.0D - 1.0D, width + 2.0D, height + 2.0D);
        return ellipse.intersects(range.clip.x, range.clip.y, range.clip.width, range.clip.height);
    }


}
