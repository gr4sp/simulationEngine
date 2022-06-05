from gym.envs.registration import register

register(
    id='gr4sp-v0',
    entry_point='gym_gr4sp.envs:Gr4spEnv',
)

register(
    id='gr4sp-v1',
    entry_point='gym_gr4sp.envs:Gr4spMapEnv',
)


register(
    id='gr4sp-v2',
    entry_point='gym_gr4sp.envs:Gr4spPunishEnv',
)

register(
    id='gr4sp-v3',
    entry_point='gym_gr4sp.envs:Gr4spNormEnv',
)

register(
    id='gr4sp-v4',
    entry_point='gym_gr4sp.envs:Gr4spAveEnv',
)

register(
    id='gr4sp-v5',
    entry_point='gym_gr4sp.envs:Gr4spMapAveEnv',
)

register(
    id='gr4sp-v6',
    entry_point='gym_gr4sp.envs:Gr4spAveEvalEnv',
)

register(
    id='gr4sp-v7',
    entry_point='gym_gr4sp.envs:Gr4spMapAveEvalEnv',
)

register(
    id='gr4sp-v8',
    entry_point='gym_gr4sp.envs:Gr4spMapAveNoNodeEvalEnv',
)
