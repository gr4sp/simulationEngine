package core;
import sim.util.Double2D;



    public class SocialOrgStructure implements java.io.Serializable
    {
        private static final long serialVersionUID = 1;

        private int OrgId; //id related to the organisation structure
        private String type; //private, public, partnertship
        private String subType; //individual, cooperative, governmental, private company
        private String entity; //non-for profit, for profit, hybrid
        private int minNumPeople; // minimum number of people to set up the organisation
        private int maxNumPeople; //// minimum number of people to set up the organisation
        private int numPeople; //number of people in the organisation
        private boolean liability; //limited (1) or non-limited (0) liability;
        private boolean taXemp; //tax exemptions or concessions;
        private String activity; // part of the supply chain or SPM owned/managed by the organistion: e.g. generator, network operator, retailer


        //TODO: what is the output of the organisaiton structure after attached to the SPM?

        public SocialOrgStructure(int OrgId, String type, String subType, String entity, int numPeople,
                                   boolean liability, boolean taXemp, String activity){

            this.OrgId = OrgId;
            this.type = type;
            this.subType = subType;
            this.entity = entity;
            this.numPeople = numPeople;
            this.liability = liability;
            this.taXemp = taXemp;
            this.activity = activity;


        }

        public int getOrgId() {
            return OrgId;
        }

        public void setOrgId(int orgId) {
            OrgId = orgId;
        }

        public String getType() {
            return type;
        }

        public void setType(String type) {
            this.type = type;
        }

        public String getSubType() {
            return subType;
        }

        public void setSubType(String subType) {
            this.subType = subType;
        }

        public String getEntity() {
            return entity;
        }

        public void setEntity(String entity) {
            this.entity = entity;
        }

        public int getMinNumPeople() {
            return minNumPeople;
        }

        public void setMinNumPeople(int minNumPeople) {
            this.minNumPeople = minNumPeople;
        }

        public int getMaxNumPeople() {
            return maxNumPeople;
        }

        public void setMaxNumPeople(int maxNumPeople) {
            this.maxNumPeople = maxNumPeople;
        }

        public int getNumPeople() {
            return numPeople;
        }

        public void setNumPeople(int numPeople) {
            this.numPeople = numPeople;
        }

        public boolean isLiability() {
            return liability;
        }

        public void setLiability(boolean liability) {
            this.liability = liability;
        }

        public boolean isTaXemp() {
            return taXemp;
        }

        public void setTaXemp(boolean taXemp) {
            this.taXemp = taXemp;
        }

        public String getActivity() {
            return activity;
        }

        public void setActivity(String activity) {
            this.activity = activity;
        }
    }

