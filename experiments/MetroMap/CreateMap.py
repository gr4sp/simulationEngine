import pandas as pd
import os
import functools
import networkx as nx
import matplotlib.pyplot as plt
import random
from tqdm import tqdm
from networkx.drawing.nx_agraph import write_dot, graphviz_layout
import matplotlib.colors as colors

folder = "RobustComp100 (OG)"


labels = list(set([i.split("_ea")[0]
                   for i in os.listdir(f"../optimisers/{folder}")]))


reduced_actions = True


def make_graph(metric):

    res = list(filter(lambda a: 'results' in a in a and metric in a,
                      os.listdir(f'../optimisers/{folder}')))
    res.sort(key=lambda a: int(a.split("_")[-2][10:]))

    file = res[-1]
    save_name = f"DAPP Network {folder} {file.split('.')[0]}"
    df = pd.read_csv(f"../optimisers/{folder}/{file}")
    paths = df.loc[:, [f'a{i}' for i in range(7)]]
    paths = [list(i) for i in paths.to_records(index=False)]

    # Prefix
    from collections import defaultdict
    prefix = {}

    for i, j in enumerate(paths):
        #     print("Path",i)
        prefix[i] = defaultdict(list)

        for a, b in enumerate(paths):
            if a == i:
                continue
            for c in range(1, len(j)):
                if j[:c] == b[:c]:
                    prefix[i][c].append(a)

    prefix_list = set()

    for i in paths:
        for j in range(len(i)):
            prefix_list.add(tuple(i[:j+1]))

    prefix_ids = {j: i for i, j in enumerate(prefix_list)}
    G = nx.DiGraph()
    G.add_node(-1)
    labels = {}
    prefix_nodes = {}
    cols = []
    parents = {-1: None}
    for p in paths:
        for i in range(len(p)):
            parent = None
            node_id = prefix_ids[tuple(p[:i+1])]
            G.add_node(node_id)
            if i > 0:
                parent = prefix_ids[tuple(p[:i])]
                G.add_edge(parent, node_id)
            else:
                G.add_edge(-1, node_id)

            labels[node_id] = tuple(p[:i+1])[-1]
            parents[node_id] = parent
            prefix_nodes[tuple(p[:i+1])] = node_id

    samps = ['red', 'yellow', 'orange', 'purple', 'green', 'lime',
             'magenta', 'pink', 'deeppink', 'peru', 'silver', 'cyan', 'blue']

    full_col = {}
    for i in G:
        try:
            full_col[i] = samps[labels[i]]
        except:
            full_col[i] = 'black'

    cols = []
    for i in G:
        cols.append(full_col[i])

    actions = """0. Carbon Tax - gov levels from 2012 with inflation
                1. Secondary Market 30 MW
                2. Learning rate 15% increase
                3. Reduce top 1 - 20%
                4. Reduce top 1 - 10%
                5. Techno rate - 15% increase
                6. Order market by emission merit
                7. Reduce top 1% - 5%
                8. Renewable subsidy - 10%
                9. No action""".split("\n")

    print(metric, file)
    print("Paths:", len(paths))
    print(
        f"Nodes: {len(paths) * 7 } -> {len(G)-1}, {(len(G)-1)/(len(paths)*7):.4f}%")
    print()
    fig = plt.figure(figsize=(80, 40))
    pos = graphviz_layout(G, prog='dot')

    # Color mapping

    # Using a figure to use it as a parameter when calling nx.draw_networkx
    f = plt.figure(1)
    ax = f.add_subplot(1, 1, 1)
    for c, label in zip(samps, actions):
        ax.plot([0], [0], color=c, label=label)

    plt.legend()
    nx.draw(G, pos, with_labels=True, labels=labels,
            arrows=True, node_color=cols)
    plt.savefig(save_name)
    G.clear()
    plt.close()
    del G


for i in labels:
    make_graph(i)
