from pathlib import Path

from mdde.core import Environment

print('Running' if __name__ == '__main__' else 'Importing', Path(__file__).resolve())

import logging
from abc import ABC
from typing import Tuple, Dict

import numpy as np

from mdde.agent.abc import NodeAgentMapping
from mdde.registry.container import BenchmarkStatus


class ABCMDDEHeuristicSample(ABC):
    """Base class containing common methods for the heuristic samples."""
    throughput_all = {}

    def processBenchmarkStatsInEnv(self, bench_response: BenchmarkStatus, env: Environment):
        nodes_sorted = env._scenario.get_ordered_nodes()
        fragments_sorted = env._scenario._actual_fragments

        return self.processBenchmarkStats(bench_response=bench_response,
                                          nodes_sorted=nodes_sorted,
                                          fragments_sorted=fragments_sorted)

    def processBenchmarkStats(self,
                              bench_response: BenchmarkStatus,
                              nodes_sorted: Tuple[NodeAgentMapping, ...],
                              fragments_sorted: Tuple[str, ...]) \
            -> Tuple[np.ndarray, float]:
        """

        :param bench_response: Response of the benchmark runner.
        :param nodes_sorted: Sorted collection of nodes mapped to agents.
        :param fragments_sorted: Sorted collection of fragment IDs.
        :return: 0: Map of fragment reads per node, where rows correspond to nodes_sorted, columns to fragments_sorted.
        1: Throughput.
        """

        if bench_response.result is None:
            raise TypeError("Benchmark results are empty")

        bench_stats = bench_response.result
        nodes = nodes_sorted
        fragments = fragments_sorted

        result_frags_total = np.zeros((len(fragments)), dtype=np.int32)
        fragment_reads_map = np.zeros((len(nodes), len(fragments)), dtype=np.int32)

        for node in nodes:
            node_stats = [d for d in bench_stats.nodes if d['nodeId'] == node.node_id]
            if len(node_stats) == 0:
                continue
            node_idx = nodes.index(node)
            for stats in node_stats:
                frag_stats: Dict = stats['frags']
                for k, v in frag_stats.items():
                    frag_idx = fragments.index(k)
                    frag: Dict = v
                    result_frags_total[frag_idx] = result_frags_total[frag_idx] + frag.get('r', 0)
                    fragment_reads_map[node_idx, frag_idx] = fragment_reads_map[node_idx, frag_idx] + frag.get('r', 0)

        return fragment_reads_map, bench_stats.throughput

    def tune_estimations(self, step_num: int, env: Environment):
        throughput_step: Dict = {}
        self.throughput_all[step_num] = throughput_step
        real_reads, real_throughput = self.processBenchmarkStatsInEnv(env._bench_request_stats(), env)
        throughput_step[-1] = real_throughput

        for idx, real_node_reads in enumerate(real_reads):
            logging.debug("Node[r] {}: {}".format(idx, real_node_reads))

            logging.debug("Node[r] {} sum reads: {}".format(idx, np.sum(real_node_reads)))

        magnitude_variations = [(0, 0.7)]

        for magnitude in magnitude_variations:
            bench_response = env._bench_request_stats_counterfeit(magnitude_start_override=magnitude[0],
                                                                  magnitude_end_override=magnitude[1])
            estimated_reads, estimated_throughput = self.processBenchmarkStatsInEnv(bench_response, env)
            throughput_step[magnitude] = estimated_throughput
            logging.debug(estimated_throughput)

            for idx, estimation_node_reads in enumerate(estimated_reads):
                logging.debug("Node[e] {}: {}".format(idx, estimation_node_reads))

                logging.debug("Node[e] {} sum reads: {}".format(idx, np.sum(estimation_node_reads)))

        logging.info("Step: {}; Throughput: {}".format(step_num, throughput_step))
