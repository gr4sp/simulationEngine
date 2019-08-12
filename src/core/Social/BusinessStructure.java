package core.Social;




public enum BusinessStructure {
    INCORPASSOCIATION, PTYLTD, PRIVATELTD, PUBLICCOMP, TRUST, PERSON, SOLETRADER, LIMITEDPARTNERSHIP, COOPERATIVE, INFORMAL,
    GRASSROOTS, NOTFORPROF, STATE, COMMONWEALTH, BODYCORP, OTHER, TBD;

    public static BusinessStructure stringToBusinessStructure(String businessStructure) {
        if (businessStructure == null)
            return BusinessStructure.TBD;

        if (businessStructure.equalsIgnoreCase("INCORPASSOCIATION"))
            return BusinessStructure.INCORPASSOCIATION;
        if (businessStructure.equalsIgnoreCase("PTYLTD"))
            return BusinessStructure.PTYLTD;
        if (businessStructure.equalsIgnoreCase("PUBLICCOMP"))
            return BusinessStructure.PUBLICCOMP;
        if (businessStructure.equalsIgnoreCase("TRUST"))
            return BusinessStructure.TRUST;
        if (businessStructure.equalsIgnoreCase("SOLETRADER"))
            return BusinessStructure.SOLETRADER;
        if (businessStructure.equalsIgnoreCase("LIMITEDPARTNERSHIP"))
            return BusinessStructure.LIMITEDPARTNERSHIP;
        if (businessStructure.equalsIgnoreCase("COOPERATIVE"))
            return BusinessStructure.COOPERATIVE;
        if (businessStructure.equalsIgnoreCase("INFORMAL"))
            return BusinessStructure.INFORMAL;
        if (businessStructure.equalsIgnoreCase("GRASSROOTS"))
            return BusinessStructure.GRASSROOTS;
        if (businessStructure.equalsIgnoreCase("NOTFORPROF"))
            return BusinessStructure.NOTFORPROF;
        if (businessStructure.equalsIgnoreCase("STATE"))
            return BusinessStructure.STATE;
        else {
            return BusinessStructure.OTHER;
        }
    }

}