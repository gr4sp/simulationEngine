def group_results(experiments, outcomes, group_by, grouping_specifiers,
                  grouping_labels):
    '''
    Helper function that takes the experiments and results and returns a list
    based on groupings. Each element in the dictionary contains the experiments
    and results for a particular group, the key is the grouping specifier.

    Parameters
    ----------
    experiments : DataFrame
    outcomes : dict
    group_by : str
               The column in the experiments array to which the grouping
               specifiers apply. If the name is'index' it is assumed that the
               grouping specifiers are valid indices for numpy.ndarray.
    grouping_specifiers : iterable
                    An iterable of grouping specifiers. A grouping
                    specifier is a unique identifier in case of grouping by
                    categorical uncertainties. It is a tuple in case of
                    grouping by a parameter uncertainty. In this cose, the code
                    treats the tuples as half open intervals, apart from the
                    last entry, which is treated as closed on both sides.
                    In case of 'index', the iterable should be a dictionary
                    with the name for each group as key and the value being a
                    valid index for numpy.ndarray.

    Returns
    -------
    dict
        A dictionary with the experiments and results for each group, the
        grouping specifier is used as key

    ..note:: In case of grouping by parameter uncertainty, the list of
             grouping specifiers is sorted. The traversal assumes half open
             intervals, where the upper limit of each interval is open, except
             for the last interval which is closed.

    '''
    groups = {}
    if group_by != 'index':
        column_to_group_by = experiments.loc[:, group_by]

    for label, specifier in zip(grouping_labels, grouping_specifiers):
        if isinstance(specifier, tuple):
            # the grouping is a continuous uncertainty
            lower_limit, upper_limit = specifier

            # check whether it is the last grouping specifier
            if grouping_specifiers.index(specifier) ==\
                    len(grouping_specifiers) - 1:
                # last case

                logical = (column_to_group_by >= lower_limit) &\
                    (column_to_group_by <= upper_limit)
            else:
                logical = (column_to_group_by >= lower_limit) &\
                    (column_to_group_by < upper_limit)
        elif group_by == 'index':
            # the grouping is based on indices
            logical = specifier
        else:
            # the grouping is an integer or categorical uncertainty
            logical = column_to_group_by == specifier

        group_outcomes = {}
        for key, value in outcomes.items():
            value = value[logical]
            group_outcomes[key] = value
        groups[label] = (experiments.loc[logical, :], group_outcomes)

    return groups