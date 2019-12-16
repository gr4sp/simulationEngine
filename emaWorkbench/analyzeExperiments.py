import numpy as np
import matplotlib.pyplot as plt

from ema_workbench import load_results, ema_logging

if __name__ == '__main__':
    from ema_workbench.analysis.pairs_plotting import (pairs_lines, pairs_scatter,
                                                       pairs_density)

    ema_logging.log_to_stderr(level=ema_logging.DEFAULT_LEVEL)

    # load the data
    fh = './data/gr4sp_2019-Dec-16.tar.gz'
    experiments, outcomes = load_results(fh)

    '''
    Plot
    '''

    import matplotlib.pyplot as plt
    from ema_workbench.analysis.plotting import lines


    figure = lines(experiments, outcomes)  # show lines, and end state density
    figure = pairs_scatter(experiments, outcomes, filter_scalar=False)
    #figure = pairs_lines(experiments, outcomes, filter_scalar=False)
    #figure = pairs_density(experiments, outcomes, filter_scalar=False)



    plt.show()  # show figure