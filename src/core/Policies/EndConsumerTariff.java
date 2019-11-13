package core.Policies;


/**
 * Max: simulates modern consumer choice where 90% have bad tariffs
 * MIN: represents informed consumers
 * RND: Balances consumers equally over all available tariffs
 * AVG: Average tariffs
 * */
public enum EndConsumerTariff {
    MAX, MIN, RND, AVG
}