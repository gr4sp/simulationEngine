package core.Social;

public enum ActorType {
    //Most categories are taken from current NEM participant categories (see http://www.aemo.com.au/-/media/Files/Electricity/NEM/Participant_Information/Participant-Categories-in-the-NEM.pdf)
    GOVAUTHORITY, ELECPROV, HDCONSUMER, MARKETGENSCHD, MARKETGENONSCHD, REGULATOR, MARKETGENSEMISCHD, OMBUDSMAN, IMPLEMENT, MARKETGENANCILLARY, NONMARKETGENSCHD, STATUTORYAUTRHORITY, BROKER,
    NONMARKETGENNONSCHD, SPECULATOR, NONMARKETGENSEMISCHD, PUBLICSERVANT, SMALLGENAGGREG, OTHER, MARKETLOADANCILLARY, MARKETLOADSCHD, FIRSTTIERSCUST, SECONDTIERCUST, MARKETNETWORKPROV, NETWORKOP,
    TRANOPER, DISTRISOPER, REALLOCATOR, TRADER, INTENTPARTICIPANT, INVESTOR, ADVISORYAGENCY, RESOURCECO, ENERGYASSOC, TBD;
    //non-market units are going to be considered for participants before the NEM, specifically
    // as non scheduled, or NONMARKETGENNONSCHD
    //TRANOPER (TNSP - Transmission network service provider) acts as delegate of AEMO as System operator carryig "some or all of AEMO's rights, functions, and obligations under claise 4.3.3 of the NER".
    // In Victoria, this will be the role of AusNet
    //DISTRIOPER (DNSP - distribution network service provider) Also known in the NEM as Distribution system operator. In Vic: Citipower, Powercor, Ausnet, Jemena and United Energy
    //in deciding the actor type we are going to use cases, and a check on actor-asset relationships and actor-actor relationships
    //NETWORKOP Actors who are transmission and distribution operators
    // //instead of plugin the types into the DB:

    public String getActorType(int actorId) {
        String switchValue = "other";
        switch (switchValue) { //depending on the associated ID of the actor, it will be from one type or the other
            case "ELECPROV":
                System.out.printf("Electricity provider");
                break;
            case "MARKETGENSCHD":
                System.out.printf("Market generator scheduled");
                break;
            default:
                System.out.printf("Not Sure");
        }
        return "NA";
    }

    public static ActorType stringToActorType(String actorType) {
        if (actorType == null)
            return ActorType.TBD;

        if (actorType.equalsIgnoreCase("RETAILER"))
            return ActorType.MARKETLOADSCHD;
        if (actorType.equalsIgnoreCase("MARKETGENSCHD"))
            return ActorType.MARKETGENSCHD;
        if (actorType.equalsIgnoreCase("MARKETNETWORKPROV"))
            return ActorType.MARKETNETWORKPROV;
        if (actorType.equalsIgnoreCase("HDCONSUMER"))
            return ActorType.HDCONSUMER;
        if (actorType.equalsIgnoreCase("REGULATOR"))
            return ActorType.REGULATOR;
        if (actorType.equalsIgnoreCase("IMPLEMENT"))
            return ActorType.IMPLEMENT;
        if (actorType.equalsIgnoreCase("GOVAUTHORITY"))
            return ActorType.GOVAUTHORITY;
        if (actorType.equalsIgnoreCase("INVESTOR"))
            return ActorType.INVESTOR;
        if (actorType.equalsIgnoreCase("BROKER"))
            return ActorType.BROKER;
        if (actorType.equalsIgnoreCase("SPECULATOR"))
            return ActorType.SPECULATOR;
        else {
            return ActorType.OTHER;
        }
    }


}
