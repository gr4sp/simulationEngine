import numpy as np


def ninety(outcomes):

    labels = ["ghg", "renew", "tariff", "wholsesale", "unmet"]
    mult = [-1, 1, -1, -1, -1]
    vals = []

    for i, j in zip(labels, mult):
        if j == -1:
            vals.append(np.percentile(outcomes[i], q=90))
        else:
            vals.append(np.percentile(outcomes[i], q=10))

    return vals[0], vals[1], vals[2], vals[3], vals[4]


def maximin(outcomes):

    labels = ["ghg", "renew", "tariff", "wholsesale", "unmet"]
    mult = [-1, 1, -1, -1, -1]
    vals = []

    for i, j in zip(labels, mult):
        if j == -1:
            vals.append(max(outcomes[i]))
        else:
            vals.append(min(outcomes[i]))

    return vals[0], vals[1], vals[2], vals[3], vals[4]


def maximax(outcomes):

    labels = ["ghg", "renew", "tariff", "wholsesale", "unmet"]
    mult = [-1, 1, -1, -1, -1]
    vals = []

    for i, j in zip(labels, mult):
        if j == -1:
            vals.append(min(outcomes[i]))
        else:
            vals.append(max(outcomes[i]))

    return vals[0], vals[1], vals[2], vals[3], vals[4]


def mean(outcomes):

    labels = ["ghg", "renew", "tariff", "wholsesale", "unmet"]
    ret = []
    for i in labels:
        ret.append(np.mean(outcomes[i]))

    return ret[0], ret[1], ret[2], ret[3], ret[4]


def mean_var(outcomes):

    labels = ["ghg", "renew", "tariff", "wholsesale", "unmet"]
    ret = []
    for i in labels:
        val = (np.mean(outcomes[i]) + 1) / (np.std(outcomes[i]) + 1)
        ret.append(val)

    return ret[0], ret[1], ret[2], ret[3], ret[4]


def kwakkel(outcomes):
    # https://link.springer.com/chapter/10.1007/978-3-319-33121-8_10
    # Metric 2, equation 10.2
    labels = ["ghg", "renew", "tariff", "wholsesale", "unmet"]
    ret = []
    for i in labels:
        if i == 'ghg':

            val = (np.mean(outcomes[i]) + 1) / (np.std(outcomes[i]) + 1)
            ret.append(val)
        else:
            val = (np.mean(outcomes[i]) + 1) * (np.std(outcomes[i]) + 1)
            ret.append(val)

    return ret[0], ret[1], ret[2], ret[3], ret[4]


def obj_func(outcomes):
    # we want to minimize the median damage and the
    # dispersion of the damage
    # the lower the median, the lower score 1
    # the lower the dispersion, the lower the score
    outcome_1 = outcomes["ghg"]
    q1 = np.percentile(outcome_1, 25)
    median = np.percentile(outcome_1, 50)
    q3 = np.percentile(outcome_1, 75)
    dispersion = abs(q3-q1)
    score_1 = median * (dispersion+1)

    # we want to minimize the median casualties and the
    # dispersion of the casualties
    # the lower the median, the lower score 1
    # the lower the dispersion, the lower the score
    outcome_2 = outcomes["renew"]
    q1 = np.percentile(outcome_2, 25)
    median = np.percentile(outcome_2, 50)
    q3 = np.percentile(outcome_2, 75)
    dispersion = abs(q3-q1)
    score_2 = median * (dispersion+1)

    # we want to minimize the median costs and the
    # dispersion of the costs
    # the lower the median, the lower score 1
    # the lower the dispersion, the lower the score
    outcome_3 = outcomes["tariff"]
    q1 = np.percentile(outcome_3, 25)
    median = np.percentile(outcome_3, 50)
    q3 = np.percentile(outcome_3, 75)
    dispersion = abs(q3-q1)
    score_3 = median * (dispersion+1)

    outcome_4 = outcomes["wholsesale"]
    q1 = np.percentile(outcome_4, 25)
    median = np.percentile(outcome_4, 50)
    q3 = np.percentile(outcome_4, 75)
    dispersion = abs(q3-q1)
    score_4 = median * (dispersion+1)

    outcome_5 = outcomes["unmet"]
    q1 = np.percentile(outcome_5, 25)
    median = np.percentile(outcome_5, 50)
    q3 = np.percentile(outcome_5, 75)
    dispersion = abs(q3-q1)
    score_5 = median * (dispersion+1)

    return score_1, score_2, score_3, score_4, score_5
