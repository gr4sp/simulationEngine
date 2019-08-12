package core.Social;


public enum OwnershipModel {
    SHARES, DONATION_BASED, COMMUNITY_INVEST, COMM_DEV_PARTNER, COMM_COUNCIL_PARTNER,
    MULTIHOUSEHOLD, INDIVIDUAL, STATE, OTHER, TBD;

    public static OwnershipModel stringToOwnershipModel(String ownership) {
        if (ownership == null)
            return OwnershipModel.TBD;

        if (ownership.equalsIgnoreCase("SHARES"))
            return OwnershipModel.SHARES;
        if (ownership.equalsIgnoreCase("DONATION_BASED"))
            return OwnershipModel.DONATION_BASED;
        if (ownership.equalsIgnoreCase("COMMUNITY_INVEST"))
            return OwnershipModel.COMMUNITY_INVEST;
        if (ownership.equalsIgnoreCase("COMM_DEV_PARTNER"))
            return OwnershipModel.COMM_DEV_PARTNER;
        if (ownership.equalsIgnoreCase("COMM_COUNCIL_PARTNER"))
            return OwnershipModel.COMM_COUNCIL_PARTNER;
        if (ownership.equalsIgnoreCase("MULTIHOUSEHOLD"))
            return OwnershipModel.MULTIHOUSEHOLD;
        if (ownership.equalsIgnoreCase("INDIVIDUAL"))
            return OwnershipModel.INDIVIDUAL;
        if (ownership.equalsIgnoreCase("STATE"))
            return OwnershipModel.STATE;
        else {
            return OwnershipModel.OTHER;
        }
    }
}
