import gym
from gym import error, spaces, utils
from gym.utils import seeding
import numpy as np

import numpy as np
from random import randint
import os
import json
import gc
import sys
import pandas as pd
import random

DEBUG = False
MAX_ACTIONS = 10


class Gr4spPunishEnv(gym.Env):

    def __init__(self):
        print("Punish")
        self.wrapped = gym.make("gr4sp-v0")
        # TODO: Add in state feature of position on map
        # Example when using discrete actions:
        self.action_space = spaces.Discrete(MAX_ACTIONS,)

        # Example for using image as input:
        self.observation_space = spaces.Box(
            low=0, high=np.inf, shape=(4, 21), dtype=np.float32)

        folder = "RobustComp100"
        file = "90th_ea_checkpoint50_results"

        df = pd.read_csv(f"optimisers/{folder}/{file}.csv")
        self.paths = df.loc[:, [f'a{i}' for i in range(7)]]
        self.paths = [[-1] + list(i)
                      for i in self.paths.to_records(index=False)]
        self.pathway_map = {}
        self.past_actions = [-1]
        self.node_ids = {}
        self.curr_node = None
        self.wrong = False
        self.create_map()
        self.wrong_count = 0

    def create_map(self):

        for path in self.paths:

            for i in range(1, len(path)):
                if tuple(path[:i]) not in self.pathway_map:
                    self.pathway_map[tuple(path[:i])] = []

                self.pathway_map[tuple(path[:i])].append(path[i])

        for i in self.pathway_map:
            self.pathway_map[i] = list(set(self.pathway_map[i]))

        for i, j in enumerate(self.pathway_map.keys()):
            self.node_ids[tuple(j)] = i

    def is_avail_action(self, action):
        return action in self.pathway_map[tuple(
            self.past_actions)]

    def step(self, action):
        if not self.is_avail_action(action):

            self.wrong = True
            self.wrong_count += 1

            # Note this is negative
            reward = (self.wrapped.curr_year - 2054) * 10000
            # reward = (self.wrapped.curr_year - 2054) * 100000
            return self.curr_obs, reward, True, {}
            # raise ValueError(
            #     "Chosen action was not one of the available actions",
            #     action)

        if self.wrong:
            if DEBUG:
                print("Got out of wrong after",
                      self.wrong_count, self.wrapped.curr_year)
            self.wrong_count = 0
        elif DEBUG:
            print("Correct action", self.wrapped.curr_year)

        self.wrong = False
        orig_obs, rew, done, info = self.wrapped.step(action)
        if not done:
            self.past_actions.append(action)
            self.curr_node = self.node_ids[tuple(self.past_actions)]

        self.curr_obs = self.add_node_to_obs(orig_obs)
        if DEBUG and done:
            print("DONE")

        return self.curr_obs, rew, done, info

    def reset(self):
        if DEBUG:
            print("Reset")

        self.wrong_count = 0

        self.wrong = False
        self.past_actions = [-1]
        self.curr_node = self.node_ids[tuple(self.past_actions)]
        orig_obs = self.wrapped.reset()
        self.curr_obs = self.add_node_to_obs(orig_obs)

        return self.curr_obs

    def add_node_to_obs(self, obs):

        obs = np.resize(obs, (4, 21))
        for i in range(len(obs)):
            obs[i][-1] = self.curr_node
        return obs
