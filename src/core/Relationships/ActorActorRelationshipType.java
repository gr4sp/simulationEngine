package core.Relationships;

public enum ActorActorRelationshipType {
    //These are the relationships represented in the Actor-Actor relationships table. Inverse relationships can be implied from any of these relationships.
    //e.g. if A is the HOLDER_PARENT company of B, then B is a subsidiary of A. The Holder status is reserved when a company completly owns another co.
    //however, if it only holds some share, then I used the PARTLY_OWNS relationship.
    NONE_INDEPENDENT, HOLDER_PARENT, CHANGE_NAME, BUYS_AQUIRES_ABSORBS, MANAGES_OPERATES_INTERMEDIARY,DISSAGREGATED_FROM,PARTNER_CO, PARTLY_OWNS,BILLING, OTHER;

    //todo: relationships of type: buy, sell, etc should be included...this depend on the type of actor that participates in certain type of arena.
    // e.g. household participates in Retail arena, retailers and generators in the wholesale and contract markets, and other actors, like industry, community energy, could participate
    // in demand management arenas or OTCs (PPAs)
}
