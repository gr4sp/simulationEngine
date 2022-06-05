import numpy as np
import random
import pandas as pd
from gr4spModelPathway import getModel

from EMAworkbench.ema_workbench.em_framework.samplers import sample_levers
from EMAworkbench.ema_workbench.em_framework.salib_samplers import SobolSampler


np.random.seed(0)
random.seed(0)

# Sample all uncertainties
model = getModel()
n_levers = 10
scenarios = sample_levers(model, n_levers)


scenarios_df = pd.DataFrame(scenarios)
print(len(scenarios_df))
scenarios_df.to_csv('levers/pathway_nfe.csv', index=False)

