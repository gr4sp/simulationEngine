import gym
from gym import error, spaces, utils
from gym.utils import seeding
import numpy as np
# Import module
import jpype

# Enable Java imports
import jpype.imports
from jpype.types import JInt

import numpy as np
from random import randint
import os
import json
import gc
import sys
import pandas as pd
import random
import time

from discord_webhook import DiscordWebhook
discord_url = ''

MAX_ACTIONS = 10

NUM_ACTORS = 10

'''
Percentage change of indicator, relative to what the Nominal would be at this timestep.
'''


class Gr4spAveEvalEnv(gym.Env):
    #   metadata = {'render.modes': ['human']}

    def __init__(self):
        super(Gr4spAveEvalEnv, self).__init__()
        # print("Norm")

        # Example when using discrete actions:
        self.action_space = spaces.Discrete(MAX_ACTIONS,)

        # Example for using image as input:
        self.observation_space = spaces.Box(
            low=0, high=np.inf, shape=(4, 20), dtype=np.float32)

        self.gr4spObj = None

        self.weights = np.array([-1, 1, -1, -1, -1])

        # Load scenarios into memory
        self.scen_id = (os.getpid() % NUM_ACTORS)
        self.folder = "/home/ray/ray_results/DQNParamRandomRL"
        self.df = pd.read_csv("scenarios/eval_300621.csv")
        self.scenarios_designs = list(self.df.to_records(index=False))[self.scen_id::NUM_ACTORS]
        self.completed = -1
        self.experiment_results = []

        # Set database ID according to process ID
        self.db_id = (os.getpid() % 4) + 4
        print(self.db_id)
        self.finished = False



    def handle_broken(self):
        sys.exit()

        self.last_inds = self.curr_inds.copy()
        self.curr_year += 4
        self.curr_obs = self.get_default_obs(self.curr_year)
        self.curr_inds = np.array(
            [sum([j[i] for j in self.curr_obs])/4 for i in range(1, 6)])

        self.reward = self.reward_function()
        #print(self.reward, self.curr_year, self.taken_actions, "BROKEN")
        # Reward if reduce
        self.experiment_results += list(self.curr_obs)
        if self.curr_year == 2050:
            self.save_results()
        print("HERHE")
        return self.curr_obs, self.reward, self.curr_year == 2050, {}

    def step(self, action):
        #print(self.curr_year, action)

        if self.finished:
            return self.get_default_obs(2022), 0, True, {}

        # self.broken = True
        import java.lang

        self.taken_actions.append(action)
        if self.broken:
            print("BROKEN")
            return self.handle_broken()
        # Save last obs
        self.last_inds = self.curr_inds.copy()
        # self.last_inds

        # TODO: Create function that handles broken runs

        try:

            self.gr4spObj.policyManager.currAction = JInt(action)

            self.gr4spObj.runFourYears()

        except java.lang.Exception as ex:
            print("Exception: " + ex)
            self.broken = True
            return self.handle_broken()

        self.curr_year += 4

        try:
            self.curr_obs = np.array(
                list(self.gr4spObj.getAveInds(JInt(self.curr_year))))
            #print(self.curr_obs.shape)
        except java.lang.Exception as ex:
            print("Exception: " + ex)
            self.curr_obs = self.get_default_obs(self.curr_year)
            self.broken = True
            return self.handle_broken()

        # Calculate reward
        self.curr_inds = np.array(
            [sum([j[i] for j in self.curr_obs])/4 for i in range(1, 6)])

        # Run end to generate save data
        if self.curr_year == 2050:
            self.gr4spObj.runFourYears()
            self.gr4spObj.runToEnd()


        self.reward = self.reward_function()

        #print(self.reward, self.curr_year, self.taken_actions)
        #self.experiment_results += [o + [action] for o in list(self.curr_obs)]
        self.experiment_results += [list(o) + [action] for o in list(self.curr_obs)]
        #print(self.curr_obs)
        #print(len(self.experiment_results), len(self.experiment_results[0]))
        #print([len(i) for i in self.experiment_results])
        #temp = [list(o) + [action] for o in list(self.curr_obs)] 
        #print([len(i) for i in temp])
        if self.curr_year == 2050:
            self.save_results()

        return self.curr_obs, self.reward, self.curr_year == 2050, {}

    def save_results(self):
        cols = ['Year', 'GHG Emissions (tCO2-e) per household', 'Percentage Renewable Production',
                'Avg Tariff (c/KWh) per household',  'Primary Wholesale ($/MWh)',
                'Primary Total Unmet Demand (Days)', 'System Production Primary Spot',
                'System Production Secondary Spot', 'System Production Off Spot',
                'Primary Total Unmet Demand (MWh)', 'Secondary Total Unmet Demand (MWh)',
               'Secondary Total Unmet Demand (Days)', 'Consumption (KWh) per household',
                'System Production Rooftop PV', 'System Production Coal',
                'System Production Water', 'System Production Wind', 'System Production Gas',
                'System Production Solar','System Production Battery', 'Action']
       # cols = ['Year', 'GHG Emissions (tCO2-e) per household', 'Percentage Renewable Production',
       #         'Avg Tariff (c/KWh) per household',  'Primary Wholesale ($/MWh)',
       #         'Primary Total Unmet Demand (Days)', 'System Production Primary Spot',
       #         'Consumption (KWh) per household', 'System Production Rooftop PV', 'System Production Coal','Dwellings', 'Action']
        df = pd.DataFrame(self.experiment_results, columns=cols)
        df.to_csv(f"{self.folder}/{self.experimentId}.csv")
        #self.gr4spObj.saveData.saveDataLight
        return

    def handle_finished(self):
        self.finished = True
        with open(f"{self.folder}/finished_{os.getpid()}.txt", 'w') as file:
            file.write("")

        files = list(os.listdir(self.folder))
        files = list(filter(lambda a: 'finished' in a, files))
        if len(files) == NUM_ACTORS:
            #sys.exit()
            webhook = DiscordWebhook(url=discord_url, content="Finished")
            response = webhook.execute()
            sys.exit()
        return self.get_default_obs(2022)

    def reset(self):
        '''
        Reset environment and returns initial observation.
        '''
        self.completed += 1
        if not jpype.isJVMStarted():
            print("JVM")
            self.startJVM()
        import java.lang
        # Indicates if this run of the simulation has broken
        self.broken = False

        if self.gr4spObj is not None:
            # print("Clean")
            try:
                self.gr4spObj.cleanup()
                self.gr4spObj = None
            except java.lang.Exception as ex:
                self.broken = True
                print("Exception: " + ex)

        self.curr_obs = []
        self.curr_year = 2022

        self.taken_actions = []
        self.reward = 0.0

        # Experiment Id
        self.experimentId = self.scen_id + (self.completed * NUM_ACTORS)

        self.experiment_results = []

        # Load random scenario
        if self.scenarios_designs:
            vals = self.scenarios_designs.pop(0)
        else:

            return self.handle_finished()

        # Load values
        annualCpi = vals[0]
        consumption = vals[1]
        domesticConsumptionPercentage = vals[2]
        generationRolloutPeriod = vals[3]
        generatorRetirement = vals[4]
        importPriceFactor = vals[5]
        nameplateCapacityChangeBrownCoal = vals[6]
        nameplateCapacityChangeWind = vals[7]
        priceChangePercentageBrownCoal = vals[8]
        priceChangePercentageWater = vals[9]
        priceChangePercentageWind = vals[10]
        wholesaleTariffContribution = vals[11]

        # self.runGr4sp(experimentId)

        self.runGr4sp(self.experimentId, annualCpi=vals[0], consumption=vals[1],
                      domesticConsumptionPercentage=vals[2], generationRolloutPeriod=vals[3],
                      generatorRetirement=vals[4], importPriceFactor=vals[5],
                      nameplateCapacityChangeBrownCoal=vals[6], nameplateCapacityChangeWind=vals[7],
                      priceChangePercentageBrownCoal=vals[8], priceChangePercentageWater=vals[9],
                      priceChangePercentageWind=vals[10], wholesaleTariffContribution=vals[11])
        try:

            self.gr4spObj.runFromPythonEMAFirst()

            self.curr_obs = np.array(
                list(self.gr4spObj.getAveInds(JInt(self.curr_year))))

        except java.lang.exception as ex:
            print("Exception: " + ex)
            self.broken = True
            self.curr_obs = self.get_default_obs(self.curr_year)

        self.curr_inds = np.array(
            [sum([j[i] for j in self.curr_obs])/4 for i in range(1, 6)])

        return self.curr_obs

    def get_default_obs(self, year):
        if year == 2022:
            return np.array([[2.02100000e+03, 4.27070131e+00, 1.77695523e-01, 5.50233078e+00,
                                1.36402102e+01, 0.00000000e+00, 6.79558257e+05, 0.00000000e+00,
                                7.41546463e+04, 0.00000000e+00, 0.00000000e+00, 0.00000000e+00,
                                4.08079573e+03, 3.22984888e+04, 6.02814974e+05, 6.76501841e+04,
                                2.95239204e+04, 4.79074327e+03, 1.82769574e+03, 0.00000000e+00],
                            [2.02000000e+03, 5.19550747e+00, 2.29900538e-01, 2.15575733e+01,
                                3.86005743e+01, 2.00000000e+00, 8.74105798e+05, 0.00000000e+00,
                                9.21878926e+04, 7.67307162e+02, 0.00000000e+00, 0.00000000e+00,
                                5.30199632e+03, 4.29003597e+04, 7.21790957e+05, 1.02726496e+05,
                                6.99589770e+04, 8.95723120e+03, 2.56702957e+03, 0.00000000e+00],
                            [2.01900000e+03, 5.48483747e+00, 2.32783671e-01, 2.17897949e+01,
                                8.41318839e+01, 1.10000000e+01, 8.97731159e+05, 0.00000000e+00,
                                7.98531172e+04, 3.80987653e+03, 0.00000000e+00, 0.00000000e+00,
                                5.59640177e+03, 4.04473620e+04, 7.23922708e+05, 1.13161576e+05,
                                6.95547541e+04, 1.88762489e+04, 2.21142224e+03, 0.00000000e+00],
                            [2.01800000e+03, 5.56646548e+00, 2.34912535e-01, 2.54150753e+01,
                                7.18853499e+01, 1.90000000e+01, 9.24353179e+05, 0.00000000e+00,
                                6.71234917e+04, 6.60548570e+03, 0.00000000e+00, 0.00000000e+00,
                                5.73571793e+03, 3.05695889e+04, 7.23456287e+05, 1.25855267e+05,
                                7.32982554e+04, 2.84727732e+04, 1.14926647e+03, 0.00000000e+00]])
        if year == 2026:
            return np.array([[2.02500000e+03, 3.99858183e+00, 1.76307045e-01, 4.56252813e+00,
                                1.58793932e+01, 0.00000000e+00, 6.59091083e+05, 0.00000000e+00,
                                8.10345624e+04, 0.00000000e+00, 0.00000000e+00, 0.00000000e+00,
                                3.79143572e+03, 4.40823232e+04, 5.96918526e+05, 5.43074471e+04,
                                2.80900571e+04, 2.66794327e+03, 1.85843013e+03, 0.00000000e+00],
                            [2.02400000e+03, 4.80220716e+00, 1.75550465e-01, 4.70097589e+00,
                                1.43890362e+01, 0.00000000e+00, 7.55286926e+05, 0.00000000e+00,
                                1.07717489e+05, 0.00000000e+00, 0.00000000e+00, 0.00000000e+00,
                                4.56045678e+03, 6.01229090e+04, 6.93926129e+05, 5.60398304e+04,
                                2.96781392e+04, 3.21027928e+03, 2.60073669e+03, 0.00000000e+00],
                            [2.02300000e+03, 4.80129686e+00, 1.82916966e-01, 4.63693714e+00,
                                1.43586052e+01, 0.00000000e+00, 7.78881626e+05, 0.00000000e+00,
                                1.05204418e+05, 0.00000000e+00, 0.00000000e+00, 0.00000000e+00,
                                4.61854873e+03, 5.50721469e+04, 7.02436754e+05, 7.22168063e+04,
                                2.87450517e+04, 5.74889193e+03, 2.50455805e+03, 0.00000000e+00],
                            [2.02200000e+03, 4.93115822e+00, 1.76705037e-01, 4.47768450e+00,
                                1.38700394e+01, 0.00000000e+00, 7.78568191e+05, 0.00000000e+00,
                                1.01569907e+05, 0.00000000e+00, 0.00000000e+00, 0.00000000e+00,
                                4.70754964e+03, 5.14184877e+04, 7.04567924e+05, 6.90762176e+04,
                                2.94410346e+04, 5.74889193e+03, 2.52062726e+03, 0.00000000e+00]])
        if year == 2030:
            return np.array([[2.02900000e+03, 3.43869617e+00, 2.46935767e-01, 4.96942616e+00,
                                2.24353426e+01, 0.00000000e+00, 6.79909554e+05, 0.00000000e+00,
                                8.11570656e+04, 0.00000000e+00, 0.00000000e+00, 0.00000000e+00,
                                3.56267483e+03, 4.58843426e+04, 5.62021155e+05, 8.70532421e+04,
                                5.03909231e+04, 2.66794327e+03, 1.83755031e+03, 0.00000000e+00],
                            [2.02800000e+03, 4.39677792e+00, 1.94516987e-01, 4.80800581e+00,
                                1.70444115e+01, 0.00000000e+00, 7.87716422e+05, 0.00000000e+00,
                                1.06175075e+05, 0.00000000e+00, 0.00000000e+00, 0.00000000e+00,
                                4.26853839e+03, 6.20874745e+04, 7.04935361e+05, 7.54352943e+04,
                                3.09707680e+04, 3.21027928e+03, 2.51733981e+03, 0.00000000e+00],
                            [2.02700000e+03, 4.48173362e+00, 1.86160404e-01, 4.81393766e+00,
                                1.62176674e+01, 0.00000000e+00, 7.74629761e+05, 0.00000000e+00,
                                1.05934200e+05, 0.00000000e+00, 0.00000000e+00, 0.00000000e+00,
                                4.30717310e+03, 6.19321797e+04, 7.01472530e+05, 6.70046740e+04,
                                2.97131853e+04, 3.20153193e+03, 2.53947421e+03, 0.00000000e+00],
                            [2.02600000e+03, 4.53974558e+00, 1.88595867e-01, 4.74085426e+00,
                                1.57675885e+01, 0.00000000e+00, 7.76191014e+05, 0.00000000e+00,
                                1.06376425e+05, 0.00000000e+00, 0.00000000e+00, 0.00000000e+00,
                                4.37612211e+03, 6.16474923e+04, 7.00987084e+05, 7.02110024e+04,
                                2.92628632e+04, 3.20153193e+03, 2.55424783e+03, 0.00000000e+00]])
        if year == 2034:
            return np.array([[2.03300000e+03, 2.46648153e+00, 4.46257057e-01, 1.06702538e+01,
                                5.58186743e+01, 0.00000000e+00, 7.11133510e+05, 0.00000000e+00,
                                7.86350792e+04, 0.00000000e+00, 0.00000000e+00, 0.00000000e+00,
                                3.46493933e+03, 4.84265007e+04, 4.28347051e+05, 1.18363701e+05,
                                1.80394238e+05, 4.70161946e+03, 1.80605847e+03, 0.00000000e+00],
                            [2.03200000e+03, 3.03422568e+00, 4.30098269e-01, 7.23904896e+00,
                                4.62645497e+01, 0.00000000e+00, 8.17727158e+05, 0.00000000e+00,
                                1.07919517e+05, 0.00000000e+00, 0.00000000e+00, 0.00000000e+00,
                                4.15688734e+03, 6.69364519e+04, 5.15108458e+05, 1.37467022e+05,
                                1.85333521e+05, 4.69365722e+03, 2.55167062e+03, 0.00000000e+00],
                            [2.03100000e+03, 3.45161460e+00, 3.53728211e-01, 5.98997927e+00,
                                2.96453654e+01, 0.00000000e+00, 7.90995101e+05, 0.00000000e+00,
                                1.09180158e+05, 0.00000000e+00, 0.00000000e+00, 0.00000000e+00,
                                4.16424279e+03, 6.64613091e+04, 5.69720586e+05, 1.25878064e+05,
                                1.18724077e+05, 3.29728329e+03, 2.57020862e+03, 0.00000000e+00],
                            [2.03000000e+03, 3.88019744e+00, 2.76986650e-01, 5.73647547e+00,
                                2.22490055e+01, 0.00000000e+00, 7.69810770e+05, 0.00000000e+00,
                                1.08704239e+05, 0.00000000e+00, 0.00000000e+00, 0.00000000e+00,
                                4.17763602e+03, 6.59319559e+04, 6.22196327e+05, 9.77624857e+04,
                                7.33082753e+04, 3.20153193e+03, 2.58740716e+03, 0.00000000e+00]])
        if year == 2038:
            return np.array([[2.03700000e+03, 2.32113460e+00, 4.69801471e-01, 1.44601517e+01,
                                9.37929543e+01, 2.00000000e+00, 7.37021495e+05, 0.00000000e+00,
                                9.67766269e+04, 2.92698360e+02, 0.00000000e+00, 0.00000000e+00,
                                3.42453797e+03, 7.00222790e+04, 4.28287726e+05, 1.16052809e+05,
                                2.00240550e+05, 9.76826954e+03, 1.83962718e+03, 0.00000000e+00],
                            [2.03600000e+03, 2.88724213e+00, 4.44133897e-01, 1.30654230e+01,
                                6.85380912e+01, 1.00000000e+00, 8.26036979e+05, 0.00000000e+00,
                                1.22865399e+05, 1.30881248e+02, 0.00000000e+00, 0.00000000e+00,
                                4.04641558e+03, 8.87051036e+04, 5.14683524e+05, 1.34286917e+05,
                                1.91703360e+05, 7.56593372e+03, 2.57900590e+03, 0.00000000e+00],
                            [2.03500000e+03, 2.87434742e+00, 4.46357892e-01, 1.23891325e+01,
                                6.16689823e+01, 1.00000000e+00, 8.33828633e+05, 0.00000000e+00,
                                1.14533292e+05, 3.56048799e+01, 0.00000000e+00, 0.00000000e+00,
                                4.03960496e+03, 7.78322977e+04, 5.13553756e+05, 1.34369538e+05,
                                2.04389099e+05, 6.32396100e+03, 2.54539629e+03, 0.00000000e+00],
                            [2.03400000e+03, 2.91271656e+00, 4.42871462e-01, 1.14941406e+01,
                                5.56004426e+01, 0.00000000e+00, 8.34246155e+05, 0.00000000e+00,
                                1.07432595e+05, 0.00000000e+00, 0.00000000e+00, 0.00000000e+00,
                                4.06536719e+03, 7.07848819e+04, 5.13703231e+05, 1.34989520e+05,
                                2.04631440e+05, 5.72968411e+03, 2.50069456e+03, 0.00000000e+00]])
        if year == 2042:
            return np.array([[2.04100000e+03, 2.20215934e+00, 4.64705089e-01, 3.76238480e+01,
                                2.12988629e+02, 7.90000000e+01, 7.34391478e+05, 0.00000000e+00,
                                1.09966210e+05, 3.77129846e+04, 0.00000000e+00, 0.00000000e+00,
                                3.44598557e+03, 8.60540203e+04, 4.28306400e+05, 1.03585516e+05,
                                1.98120486e+05, 2.04964963e+04, 1.85883262e+03, 0.00000000e+00],
                            [2.04000000e+03, 2.57895966e+00, 4.71562443e-01, 2.89403954e+01,
                                2.02155592e+02, 5.70000000e+01, 8.78854980e+05, 0.00000000e+00,
                                1.45504167e+05, 2.51423155e+04, 0.00000000e+00, 0.00000000e+00,
                                4.08729014e+03, 1.15354319e+05, 5.14770656e+05, 1.35056064e+05,
                                2.26661893e+05, 2.26874756e+04, 2.53986783e+03, 0.00000000e+00],
                            [2.03900000e+03, 2.74632239e+00, 4.71022599e-01, 2.53930054e+01,
                                1.56019112e+02, 5.00000000e+01, 8.66201494e+05, 0.00000000e+00,
                                1.40804311e+05, 1.75877712e+04, 0.00000000e+00, 0.00000000e+00,
                                4.07228596e+03, 1.09981535e+05, 5.13225181e+05, 1.34053849e+05,
                                2.24140100e+05, 1.53799799e+04, 2.51565908e+03, 0.00000000e+00],
                            [2.03800000e+03, 2.78363861e+00, 4.61429781e-01, 1.80234394e+01,
                                1.18801558e+02, 2.20000000e+01, 8.57873842e+05, 0.00000000e+00,
                                1.37542891e+05, 3.95859884e+03, 0.00000000e+00, 0.00000000e+00,
                                4.06850588e+03, 1.05862234e+05, 5.13145033e+05, 1.33441922e+05,
                                2.13621503e+05, 1.84588149e+04, 2.53555688e+03, 0.00000000e+00]])
        if year == 2046:
            return np.array([[2.04500000e+03, 2.31233927e+00, 4.25659304e-01, 4.81712952e+01,
                                2.88197716e+02, 1.64000000e+02, 7.13119944e+05, 0.00000000e+00,
                                1.13021552e+05, 2.47913981e+05, 0.00000000e+00, 0.00000000e+00,
                                3.45219865e+03, 9.31241699e+04, 4.28377954e+05, 1.04196667e+05,
                                1.50865159e+05, 4.36172300e+04, 1.62230192e+03, 0.00000000e+00],
                            [2.04400000e+03, 2.62632164e+00, 4.54411320e-01, 4.96857376e+01,
                                2.62009095e+02, 1.47000000e+02, 8.72441526e+05, 0.00000000e+00,
                                1.53018703e+05, 1.51518649e+05, 0.00000000e+00, 0.00000000e+00,
                                4.09570771e+03, 1.28078142e+05, 5.15151705e+05, 1.21983589e+05,
                                2.11137424e+05, 4.14406209e+04, 2.37691016e+03, 0.00000000e+00],
                            [2.04300000e+03, 2.58389563e+00, 4.62990877e-01, 3.98406410e+01,
                                2.78579981e+02, 1.25000000e+02, 8.78961837e+05, 0.00000000e+00,
                                1.51405040e+05, 1.02242718e+05, 0.00000000e+00, 0.00000000e+00,
                                4.08047390e+03, 1.24591780e+05, 5.13636087e+05, 1.21003801e+05,
                                2.26384532e+05, 3.66874594e+04, 2.49000359e+03, 0.00000000e+00],
                            [2.04200000e+03, 2.57263752e+00, 4.72272812e-01, 3.68223495e+01,
                                2.10553479e+02, 9.10000000e+01, 8.72950701e+05, 0.00000000e+00,
                                1.48918777e+05, 5.15578012e+04, 0.00000000e+00, 0.00000000e+00,
                                4.07805330e+03, 1.21471955e+05, 5.13607804e+05, 1.20607195e+05,
                                2.35090729e+05, 2.24377506e+04, 2.54719272e+03, 0.00000000e+00]])
        if year == 2050:
            return np.array([[2.04900000e+03, 3.38986915e-01, 8.76037500e-01, 3.50278778e+01,
                                1.99631807e+02, 2.50000000e+02, 2.17411064e+05, 0.00000000e+00,
                                1.12137888e+05, 1.33660876e+06, 0.00000000e+00, 0.00000000e+00,
                                3.43781225e+03, 9.89098627e+04, 1.23143750e+04, 1.04021180e+05,
                                8.15342507e+04, 2.79990000e+04, 4.27541913e+02, 0.00000000e+00],
                            [2.04800000e+03, 3.46683560e-01, 8.92357444e-01, 3.60034904e+01,
                                2.08746014e+02, 3.14000000e+02, 3.05163432e+05, 0.00000000e+00,
                                1.60454461e+05, 1.40176316e+06, 0.00000000e+00, 0.00000000e+00,
                                4.08482101e+03, 1.41685798e+05, 1.48176250e+04, 1.25166469e+05,
                                1.42420750e+05, 3.47348250e+04, 1.51709522e+03, 0.00000000e+00],
                            [2.04700000e+03, 2.34366745e+00, 5.11546374e-01, 3.28702774e+01,
                                2.13498788e+02, 2.94000000e+02, 6.43545034e+05, 0.00000000e+00,
                                1.65956931e+05, 6.09668373e+05, 0.00000000e+00, 0.00000000e+00,
                                4.07592178e+03, 1.43107570e+05, 3.58542750e+05, 1.24683933e+05,
                                1.41424100e+05, 3.42898765e+04, 2.18907393e+03, 0.00000000e+00],
                            [2.04600000e+03, 2.77745798e+00, 4.29516638e-01, 4.84011459e+01,
                                1.92718749e+02, 2.17000000e+02, 7.94162375e+05, 0.00000000e+00,
                                1.61251944e+05, 3.50354532e+05, 0.00000000e+00, 0.00000000e+00,
                                4.07939251e+03, 1.37895790e+05, 5.13806537e+05, 1.19873544e+05,
                                1.48111192e+05, 2.82409743e+04, 2.22677549e+03, 0.00000000e+00]])

    def reward_function(self):

        nominal_inds = np.array(
            [sum([j[i] for j in self.get_default_obs(self.curr_year)])/4 for i in range(1, 6)])

        return sum(np.array([self.pct_diff(i, j) for i, j in zip(nominal_inds, self.curr_inds)]) * self.weights)

    def pct_diff(self, start, end):
        return (end - start) / start if start else 0.0

    def render(self):
        # # print(self.curr_year, self.curr_obs)
        # for i, j in zip(self.last_inds, self.curr_inds):
        #     print(i, j)

        # print()
        # print(np.array([self.pct_diff(i, j) for i, j in zip(
        #     self.last_inds, self.curr_inds)]) * self.weights)
        # print()
        # print(self.curr_year)
        # print(self.curr_obs)
        # for i, j in zip(["GHG", "Renew", "Tariff", "Wholesale", "Unmet"], self.curr_inds):
        #     print(f"{i}: {round(j,4)}", end="\t")
        # print()
        # print(f"Reward: {self.reward}")
        # print()
        # print("#" * 50)
        # print()
        return

    def close(self):
        return

    def category(self, i):
        switcher = {
            0: 'Central',
            1: 'Slow change',
            2: 'Step change',
            3: 'Fast change',
            4: 'High DER',
            5: 'residential',
            6: 'business',
            7: 'both',
            8: 'primary',
            9: 'secondary',
            10: 'none'
        }
        return switcher.get(i, "invalid self.category")

    def applyPercentageChange(self, percentageChange):
        return (100.0 + percentageChange) / 100.0

    def startJVM(self):
        with open('settingsExperiments.json') as f:
            settings = json.load(f)

        jvmpath = settings[settings["jvmPath"]]

        gr4spPath = os.getcwd() + "/.."

        gr4spPath = gr4spPath.replace("\\", "/")

        # Startup Jpype and import the messaging java package
        if jpype.isJVMStarted():
            return

        classpathSeparator = ";"
        if settings["jvmPath"] == "jvmPathUbuntu":
            classpathSeparator = ":"

        classpath = "-Djava.class.path=" \
                    "{0}/{2}{1}" \
                    "{0}/libraries/bsh-2.0b4.jar{1}{0}/libraries/itext-1.2.jar{1}" \
                    "{0}/libraries/j3dcore.jar{1}{0}/libraries/j3dutils.jar{1}" \
                    "{0}/libraries/jcommon-1.0.21.jar{1}" \
                    "{0}/libraries/jfreechart-1.0.17.jar{1}" \
                    "{0}/libraries/jmf.jar{1}" \
                    "{0}/libraries/mason.19.jar{1}" \
                    "{0}/libraries/portfolio.jar{1}" \
                    "{0}/libraries/vecmath.jar{1}" \
                    "{0}/libraries/postgresql-42.2.6.jar{1}" \
                    "{0}/libraries/opencsv-4.6.jar{1}" \
                    "{0}/libraries/yamlbeans-1.13.jar".format(
                        gr4spPath, classpathSeparator, settings["gr4spClasses"])

        # jpype.startJVM(jvmpath, classpath, "-Xmx8192M")  # 8GB
        # jpype.startJVM(jvmpath, classpath, "-Xmx1024M")  # 8GB
        jpype.startJVM(jvmpath, classpath)  # 8GB

    def runGr4sp(self, experimentId, annualCpi=2.33, annualInflation=3.3, consumption=0, energyEfficiency=0, onsiteGeneration=0, solarUptake=0, rooftopPV=7,
                 domesticConsumptionPercentage=30, includePublicallyAnnouncedGen=0, generationRolloutPeriod=1, generatorRetirement=0, technologicalImprovement=1,
                 learningCurve=5, importPriceFactor=29, priceChangePercentageBattery=0, priceChangePercentageBrownCoal=0, priceChangePercentageOcgt=0,
                 priceChangePercentageCcgt=0, priceChangePercentageWind=0, priceChangePercentageWater=0, priceChangePercentageSolar=0,
                 nameplateCapacityChangeBattery=0, nameplateCapacityChangeBrownCoal=0, nameplateCapacityChangeOcgt=0,
                 nameplateCapacityChangeCcgt=0, nameplateCapacityChangeWind=0, nameplateCapacityChangeWater=0, nameplateCapacityChangeSolar=0, wholesaleTariffContribution=28.37,
                 scheduleMinCapMarketGen=30, semiScheduleGenSpotMarket=8, semiScheduleMinCapMarketGen=30, nonScheduleGenSpotMarket=10,
                 nonScheduleMinCapMarketGen=1):

        if not jpype.isJVMStarted():
            print("JVM")
            self.startJVM()
        import java.lang
        try:

            outputID = "_seed_{}".format(experimentId)

            # print(f"experimentId: {experimentId}\n, annualCpi: {annualCpi}\n, annualInflation: {annualInflation}\n, consumption: {category(consumption)}\n, consumption{consumption}\n, energyEfficiency: {category(energyEfficiency)}\n, energyEfficiency{energyEfficiency}\n, onsiteGeneration: {category(onsiteGeneration)}\n, onsiteGeneration{onsiteGeneration}\n, solarUptake: {category(solarUptake)}\n, solarUptake{solarUptake}\n, rooftopPV: {category(rooftopPV)}\n, rooftopPV{rooftopPV}\n")
            # print(f"domesticConsumptionPercentage: {domesticConsumptionPercentage}\n, includePublicallyAnnouncedGen: {includePublicallyAnnouncedGen}\n, generationRolloutPeriod: {generationRolloutPeriod}\n, generatorRetirement: {generatorRetirement}\n, technologicalImprovement:{technologicalImprovement}\n")
            # print(f"learningCurve: {learningCurve}\n, importPriceFactor: {importPriceFactor}\n, priceChangePercentageBattery: {priceChangePercentageBattery}\n, priceChangePercentageBrownCoal: {priceChangePercentageBrownCoal}\n, priceChangePercentageOcgt: {priceChangePercentageOcgt}\n")
            # print(f"priceChangePercentageCcgt: {priceChangePercentageCcgt}\n, priceChangePercentageWind: {priceChangePercentageWind}\n, priceChangePercentageWater: {priceChangePercentageWater}\n, priceChangePercentageSolar: {priceChangePercentageSolar}\n")
            # print(
            #     f"nameplateCapacityChangeBattery: {nameplateCapacityChangeBattery}\n, nameplateCapacityChangeBrownCoal: {nameplateCapacityChangeBrownCoal}\n, nameplateCapacityChangeOcgt: {nameplateCapacityChangeOcgt}\n")
            # print(f"nameplateCapacityChangeCcgt: {nameplateCapacityChangeCcgt}\n, nameplateCapacityChangeWind: {nameplateCapacityChangeWind}\n, nameplateCapacityChangeWater: {nameplateCapacityChangeWater}\n, nameplateCapacityChangeSolar: {nameplateCapacityChangeSolar}\n, wholesaleTariffContribution: {wholesaleTariffContribution}\n")
            # print(f"scheduleMinCapMarketGen: {scheduleMinCapMarketGen}\n, semiScheduleGenSpotMarket: {category(semiScheduleGenSpotMarket)}\n, semiScheduleGenSpotMarket{semiScheduleGenSpotMarket}\n, semiScheduleMinCapMarketGen: {semiScheduleMinCapMarketGen}\n, nonScheduleGenSpotMarket: {category(nonScheduleGenSpotMarket)}\n, nonScheduleGenSpotMarket{nonScheduleGenSpotMarket}\n")
            # print(f"nonScheduleMinCapMarketGen: {nonScheduleMinCapMarketGen}\n")

            # If CSV doesn't exist, then run the simulation. This is usefule to resume failed EMA runs
            #################### REMOVE OR TRUE #######################

            self.gr4sp = jpype.JClass("core.Gr4spSim")
            # to identify each csv created in the simulation with an unique experiment number, instead of using seed = randint(0, 100000), we use the experiment number
            self.gr4spObj = self.gr4sp(experimentId)
            self.gr4spObj.saveDAPP = True

            self.gr4spObj.db_id = self.db_id

            outputID = str(self.gr4spObj.outputID)
            # print(outputID)

            # # UPDATE AFTER BASE YEAR

            # Set Uncertainties
            self.gr4spObj.settingsAfterBaseYear.forecast.annualCpi = annualCpi / 100.0
            self.gr4spObj.settingsAfterBaseYear.policy.annualInflation = annualInflation / 100.0

            self.gr4spObj.settingsAfterBaseYear.forecast.scenario.consumption = self.category(
                consumption)
            self.gr4spObj.settingsAfterBaseYear.forecast.scenario.energyEfficiency = self.category(
                energyEfficiency)
            self.gr4spObj.settingsAfterBaseYear.forecast.scenario.onsiteGeneration = self.category(
                onsiteGeneration)
            self.gr4spObj.settingsAfterBaseYear.forecast.scenario.solarUptake = self.category(
                solarUptake)
            # print(category(solarUptake), solarUptake, technologicalImprovement)
            self.gr4spObj.settingsAfterBaseYear.forecast.rooftopPV = self.category(
                rooftopPV)

            self.gr4spObj.settingsAfterBaseYear.population.domesticConsumptionPercentage = domesticConsumptionPercentage / 100.0

            self.gr4spObj.settingsAfterBaseYear.forecast.includePublicallyAnnouncedGen = jpype.java.lang.Boolean(
                includePublicallyAnnouncedGen)
            self.gr4spObj.settingsAfterBaseYear.forecast.generationRolloutPeriod = generationRolloutPeriod
            self.gr4spObj.settingsAfterBaseYear.forecast.generatorRetirement = generatorRetirement
            self.gr4spObj.settingsAfterBaseYear.forecast.technologicalImprovement = technologicalImprovement / 100.0
            self.gr4spObj.settingsAfterBaseYear.forecast.learningCurve = learningCurve / 100.0
            self.gr4spObj.settingsAfterBaseYear.forecast.importPriceFactor = importPriceFactor / 100.0

            # LCOEs and CFs variations
            brown_coal_base_price = self.gr4spObj.settingsAfterBaseYear.getBasePriceMWh('Brown Coal',
                                                                                        '') * self.applyPercentageChange(
                priceChangePercentageBrownCoal)
            self.gr4spObj.settingsAfterBaseYear.setBasePriceMWh(
                'Brown Coal', '', brown_coal_base_price)

            battery_base_price = self.gr4spObj.settingsAfterBaseYear.getBasePriceMWh('Battery', '') * self.applyPercentageChange(
                priceChangePercentageBattery)
            self.gr4spObj.settingsAfterBaseYear.setBasePriceMWh(
                'Battery', '', battery_base_price)

            ocgt_base_price = self.gr4spObj.settingsAfterBaseYear.getBasePriceMWh('Gas Pipeline Turbine - OCGT',
                                                                                  '') * self.applyPercentageChange(
                priceChangePercentageOcgt)
            self.gr4spObj.settingsAfterBaseYear.setBasePriceMWh(
                'Gas Pipeline Turbine - OCGT', '', ocgt_base_price)

            ccgt_base_price = self.gr4spObj.settingsAfterBaseYear.getBasePriceMWh('Gas Pipeline Turbine - CCGT',
                                                                                  '') * self.applyPercentageChange(
                priceChangePercentageCcgt)
            self.gr4spObj.settingsAfterBaseYear.setBasePriceMWh(
                'Gas Pipeline Turbine - CCGT', '', ccgt_base_price)

            wind_base_price = self.gr4spObj.settingsAfterBaseYear.getBasePriceMWh('Wind', '') * self.applyPercentageChange(
                priceChangePercentageWind)
            self.gr4spObj.settingsAfterBaseYear.setBasePriceMWh(
                'Wind', '', wind_base_price)

            water_base_price = self.gr4spObj.settingsAfterBaseYear.getBasePriceMWh('Water', '') * self.applyPercentageChange(
                priceChangePercentageWater)
            self.gr4spObj.settingsAfterBaseYear.setBasePriceMWh(
                'Water', '', water_base_price)

            solar_base_price = self.gr4spObj.settingsAfterBaseYear.getBasePriceMWh('Solar', '') * self.applyPercentageChange(
                priceChangePercentageSolar)
            self.gr4spObj.settingsAfterBaseYear.setBasePriceMWh(
                'Solar', '', solar_base_price)

            # Nameplate Capacity Change
            brown_coal_nameplate_change = self.applyPercentageChange(
                nameplateCapacityChangeBrownCoal)
            self.gr4spObj.settingsAfterBaseYear.setNameplateCapacityChange(
                'Brown Coal', '', brown_coal_nameplate_change)

            battery_nameplate_change = self.applyPercentageChange(
                nameplateCapacityChangeBattery)
            self.gr4spObj.settingsAfterBaseYear.setNameplateCapacityChange(
                'Battery', '', battery_nameplate_change)

            ocgt_nameplate_change = self.applyPercentageChange(
                nameplateCapacityChangeOcgt)
            self.gr4spObj.settingsAfterBaseYear.setNameplateCapacityChange('Gas Pipeline Turbine - OCGT', '',
                                                                           ocgt_nameplate_change)

            ccgt_nameplate_change = self.applyPercentageChange(
                nameplateCapacityChangeCcgt)
            self.gr4spObj.settingsAfterBaseYear.setNameplateCapacityChange('Gas Pipeline Turbine - CCGT', '',
                                                                           ccgt_nameplate_change)

            wind_nameplate_change = self.applyPercentageChange(
                nameplateCapacityChangeWind)
            self.gr4spObj.settingsAfterBaseYear.setNameplateCapacityChange(
                'Wind', '', wind_nameplate_change)

            water_nameplate_change = self.applyPercentageChange(
                nameplateCapacityChangeWater)
            self.gr4spObj.settingsAfterBaseYear.setNameplateCapacityChange(
                'Water', '', water_nameplate_change)

            solar_nameplate_change = self.applyPercentageChange(
                nameplateCapacityChangeSolar)
            self.gr4spObj.settingsAfterBaseYear.setNameplateCapacityChange(
                'Solar', '', solar_nameplate_change)

            # tariff components
            self.gr4spObj.settingsAfterBaseYear.setUsageTariff('wholesaleContribution',
                                                               (float)(wholesaleTariffContribution / 100.0))

            # # arenas
            self.gr4spObj.settingsAfterBaseYear.setMinCapMarketGen('scheduled',
                                                                   (float)(scheduleMinCapMarketGen))
            self.gr4spObj.settingsAfterBaseYear.setMinCapMarketGen('semiScheduled',
                                                                   (float)(semiScheduleMinCapMarketGen))
            self.gr4spObj.settingsAfterBaseYear.setMinCapMarketGen('nonScheduled',
                                                                   (float)(nonScheduleMinCapMarketGen/10))

            self.gr4spObj.settingsAfterBaseYear.setSpotMarket(
                'semiScheduled', self.category(semiScheduleGenSpotMarket))
            self.gr4spObj.settingsAfterBaseYear.setSpotMarket(
                'nonScheduled', self.category(nonScheduleGenSpotMarket))

        except java.lang.Exception as ex:
            self.broken = True
            print("Exception: " + ex)
