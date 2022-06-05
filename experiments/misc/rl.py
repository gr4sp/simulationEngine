import time
import json
from ray.tune.registry import register_env
import ray
import ray.rllib.agents.dqn as dqn
import os
from gym_gr4sp.envs.gr4sp_env import Gr4spEnv

from discord_webhook import DiscordWebhook

discord_url = ''


# Prepare pre-trained RL Agent
select_env = "gr4sp-v0"
chkpt_root = "tmp/exa"
ray_results = "{}/ray_results/".format(os.getenv("HOME"))
# Register custom env with Ray
register_env(select_env, lambda config: Gr4spEnv())

info = ray.init(num_cpus=4, ignore_reinit_error=True)
try:
    print("Dashboard URL: http://{}".format(info["webui_url"]))
except:
    pass


config = {'num_workers': 3}
agent = dqn.DQNTrainer(env='gr4sp-v0', config=config)

results = []
episode_data = []
episode_json = []
n = 0
while True:
    # for n in range(5):
    start = time.time()
    result = agent.train()
    results.append(result)

    episode = {'n': n,
               'episode_reward_min': result['episode_reward_min'],
               'episode_reward_mean': result['episode_reward_mean'],
               'episode_reward_max': result['episode_reward_max'],
               'episode_len_mean': result['episode_len_mean']}

    episode_data.append(episode)
    episode_json.append(json.dumps(episode))
    file_name = agent.save()
    # file_name = agent.save("tmp/exa")
    if n % 20 == 0:
        # if n % 10 == 0:
        msg = f'{n:3d}: Min/Mean/Max reward: {result["episode_reward_min"]:8.4f}/{result["episode_reward_mean"]:8.4f}/{result["episode_reward_max"]:8.4f}. Checkpoint saved to {file_name}'
        print(msg)
        webhook = DiscordWebhook(
            url=discord_url, content=msg)
        response = webhook.execute()

        webhook = DiscordWebhook(
            url=discord_url, content=f"Time elapsed: {time.time() - start}")
        response = webhook.execute()
        print("Took", (time.time() - start), "\n")
    n += 1
