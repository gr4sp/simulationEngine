
import ray
from ray.rllib import agents
from ray import tune
from ray.rllib.models import ModelCatalog
from ray.rllib.models.tf.tf_modelv2 import TFModelV2
from ray.rllib.models.tf.fcnet import FullyConnectedNetwork
from ray.rllib.utils import try_import_tf
from ray.rllib.agents.dqn.distributional_q_tf_model import \
    DistributionalQTFModel
from gym import spaces

tf1, tf, tfv = try_import_tf()

MAX_ACTIONS = 10


class ParametricActionsModel(DistributionalQTFModel):

    def __init__(self, obs_space, action_space, num_outputs,
                 model_config, name, true_obs_shape=(4, 21,),
                 action_embed_size=MAX_ACTIONS, *args, **kwargs):

        super(ParametricActionsModel, self).__init__(obs_space,
                                                     action_space, num_outputs, model_config, name,
                                                     *args, **kwargs)
        self.action_embed_model = FullyConnectedNetwork(
            spaces.Box(-1, 1, shape=true_obs_shape),
            action_space, action_embed_size,
            model_config, name + "_action_embedding")

    def forward(self, input_dict, state, seq_lens):
        avail_actions = input_dict["obs"]["avail_actions"]
        action_mask = input_dict["obs"]["action_mask"]

        action_embedding, _ = self.action_embed_model({
            "obs": input_dict["obs"]["gr4sp"]})
        intent_vector = tf.expand_dims(action_embedding, 1)
        action_logits = tf.reduce_sum(avail_actions * intent_vector,
                                      axis=1)
        inf_mask = tf.maximum(tf.log(action_mask), tf.float32.min)
        return action_logits + inf_mask, state

    def value_function(self):
        return self.action_embed_model.value_function()
