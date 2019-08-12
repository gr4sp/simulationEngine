package core.Social;

//governance role: maker, follower, implementer
public enum GovRole {
    RULEFOLLOW, RULEMAKER, RULEIMPLEMENT, OTHER, TBD;

    public static GovRole stringToGovRole(String govRole) {
        if (govRole == null)
            return GovRole.TBD;

        if (govRole.equalsIgnoreCase("RULEFOLLOW"))
            return GovRole.RULEFOLLOW;
        if (govRole.equalsIgnoreCase("RULEMAKER"))
            return GovRole.RULEMAKER;
        if (govRole.equalsIgnoreCase("RULEIMPLEMENT"))
            return GovRole.RULEIMPLEMENT;
        else {

            return GovRole.OTHER;
        }
    }
}
