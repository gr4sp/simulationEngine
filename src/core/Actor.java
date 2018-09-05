package core;

import sim.util.Double2D;

public class Actor implements java.io.Serializable{
    private static final long serialVersionUID = 1;

    private int actor; // actor id
    private String name; //AGL, Angela, SunCorp, Powershop, AEMO, ACCC, COAG, Origin, etc.
    private String main_role; // rule follower, rule implementer, rule maker
    private String role; //
    private int minNumPeople; // minimum number of people to set up the organisation
    private int maxNumPeople; //// minimum number of people to set up the organisation
    private int numPeople; //number of people in the organisation
    private boolean liability; //limited (1) or non-limited (0) liability;
    private boolean taXemp; //tax exemptions or concessions;
    private String activity; // part of the supply chain or SPM owned/managed by the organistion: e.g. generator, network operator, ret
}
