package core;

import sim.util.Double2D;


public class Enduse {

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
}
