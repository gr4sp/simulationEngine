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


from ray.rllib.agents.dqn.distributional_q_tf_model import \
    DistributionalQTFModel
from ray.rllib.agents.dqn.dqn_torch_model import \
    DQNTorchModel
from ray.rllib.models.tf.fcnet import FullyConnectedNetwork
from ray.rllib.models.torch.fcnet import FullyConnectedNetwork as TorchFC
from ray.rllib.utils.framework import try_import_tf, try_import_torch
from ray.rllib.utils.torch_ops import FLOAT_MIN, FLOAT_MAX
from collections import defaultdict


tf1, tf, tfv = try_import_tf()

# from gr4sp_env import Gr4spEnv


MAX_ACTIONS = 10


class Gr4spMapEnv(gym.Env):

    def __init__(self):

        self.action_space = spaces.Discrete(MAX_ACTIONS)
        self.wrapped = gym.make("gr4sp-v0")
        # TODO: Add in state feature of position on map

        # new_wrapped_obs =
        self.observation_space = spaces.Dict({
            "action_mask": spaces.Box(0, 1, shape=(MAX_ACTIONS, )),
            "avail_actions": spaces.Box(0, 1, shape=(MAX_ACTIONS, )),
            "gr4sp": spaces.Box(
                low=0, high=np.inf, shape=(4, 21), dtype=np.float32),
        })

        # self.
        # TODO: Instead of past action, add extra value in each array of id of node in graph
        # self.observation_space = spaces.Dict({
        #     "action_mask": spaces.Box(0, 1, shape=(MAX_ACTIONS, )),
        #     "avail_actions": spaces.Box(0, 1, shape=(MAX_ACTIONS, )),
        #     "gr4sp": spaces.Dict({
        #         "true_model": self.wrapped.observation_space,
        #         "past_actions": spaces.Box(-1, 10, shape=(7,), dtype=np.int_)
        #     })
        # })

        self.action_assignments = np.array([1] * self.action_space.n)

        folder = "RobustComp100"
        file = "90th_ea_checkpoint50_results"

        df = pd.read_csv(f"optimisers/{folder}/{file}.csv")
        self.paths = df.loc[:, [f'a{i}' for i in range(7)]]
        self.paths = [[-1] + list(i)
                      for i in self.paths.to_records(index=False)]
        self.pathway_map = {}
        self.node_ids = {}
        self.curr_node = None
        self.past_actions = [-1]

        self.create_map()

    def create_map(self):

        for path in self.paths:

            # if tuple([-1]) not in self.pathway_map:
            #     self.pathway_map[tuple([-1])] = []

            # self.pathway_map[tuple([-1])].append(path[0])

            for i in range(1, len(path)):
                if tuple(path[:i]) not in self.pathway_map:
                    self.pathway_map[tuple(path[:i])] = []

                self.pathway_map[tuple(path[:i])].append(path[i])

        for i in self.pathway_map:
            self.pathway_map[i] = list(set(self.pathway_map[i]))

        for i, j in enumerate(self.pathway_map.keys()):
            self.node_ids[tuple(j)] = i

        # print(self.node_ids.keys())

    def update_avail_actions(self):

        self.action_mask = np.array([1 if i in self.pathway_map[tuple(
            self.past_actions)] else 0 for i in range(MAX_ACTIONS)])

    def step(self, action):
        if self.action_mask[action] == 0:
            raise ValueError(
                "Chosen action was not one of the non-zero action embeddings",
                action, self.action_mask)

        orig_obs, rew, done, info = self.wrapped.step(action)

        if not done:
            self.past_actions.append(action)
            self.curr_node = self.node_ids[tuple(self.past_actions)]
            self.update_avail_actions()

        obs = {
            "action_mask": self.action_mask,
            "avail_actions": self.action_assignments,
            "gr4sp": orig_obs,
        }

        self.curr_obs = self.add_node_to_obs(obs)

        return self.curr_obs, rew, done, info

    def reset(self):
        self.past_actions = [-1]
        self.curr_node = self.node_ids[tuple(self.past_actions)]
        self.update_avail_actions()

        obs = {
            "action_mask": self.action_mask,
            "avail_actions": self.action_assignments,
            "gr4sp": self.wrapped.reset(),
        }

        self.curr_obs = self.add_node_to_obs(obs)

        return self.curr_obs

    def add_node_to_obs(self, obs):

        obs['gr4sp'] = np.resize(obs['gr4sp'], (4, 21))
        for i in range(len(obs['gr4sp'])):
            obs['gr4sp'][i][-1] = self.curr_node
        # print("HERE", obs['gr4sp'])
        return obs
