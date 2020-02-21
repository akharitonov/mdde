from mdde.core.exception import EnvironmentInitializationError
from mdde.registry.container import RegistryResponseHelper
from mdde.registry.protocol import PRegistryControlClient, PRegistryWriteClient, PRegistryReadClient
from mdde.scenario.abc import ABCScenario
from mdde.registry.enums import ERegistryMode
import numpy as np
import logging


class Environment:
    """
    Entry point to MDDE. Reinforcement learning frameworks should be wrapped around this class to function
    """
    def __init__(self,
                 scenario: ABCScenario,
                 registry_ctrl: PRegistryControlClient,
                 registry_write: PRegistryWriteClient,
                 registry_read: PRegistryReadClient):
        """
        Environment constructor
        :param scenario: Scenario object implementing ABCScenario.
        :param registry_ctrl: Control commands for the MDDE registry implementation
        :param registry_write: Write commands for the MDDE registry implementation
        :param registry_read: Read commands for the MDDE registry implementation
        """
        if not isinstance(scenario, ABCScenario):
            raise TypeError("scenario must extend ABCScenario")

        if registry_ctrl is None:
            raise TypeError("registry control client can't be None")
        if registry_write is None:
            raise TypeError("registry write client can't be None")
        if registry_read is None:
            raise TypeError("registry read client can't be None")

        self._logger = logging.getLogger('Environment')

        self._scenario = scenario
        self._registry_ctrl = registry_ctrl
        self._registry_write = registry_write
        self._registry_read = registry_read

    def initialize_registry(self):
        """
        Initialize or re-initialize the registry. All existing data will be removed, all data generated anew.
        """
        self._logger.info("Environment initialization starting")
        # Registry must be in the 'shuffle' mode
        self._set_registry_mode(ERegistryMode.shuffle)
        # Flush existing data
        flush_result = self._registry_ctrl.ctrl_flush()
        if flush_result.failed:
            raise EnvironmentInitializationError(flush_result.error)
        # Re-initialize nodes
        nodes_populate_res = self._registry_ctrl.ctrl_populate_default_nodes()
        if nodes_populate_res.failed:
            raise EnvironmentInitializationError(nodes_populate_res.error)
        # Registry must be in the 'benchmark' mode, meaning not accepting any modification (write) commands
        self._set_registry_mode(ERegistryMode.benchmark)
        # Generate data
        data_gen_result = self._registry_ctrl.ctrl_generate_data(self._scenario.get_datagenerator_workload())
        if data_gen_result.failed:
            raise EnvironmentInitializationError(data_gen_result.error)
        if not data_gen_result.result:
            raise EnvironmentInitializationError("Initial data was not generated, "
                                                 "check the registry logs for more information")
        # Run initial fragmentation
        fragmenter = self._scenario.get_fragmenter()
        fragmentation_requires_shuffle = fragmenter.run_fragmentation(self._registry_read, self._registry_write)
        # Switch to shuffle
        self._set_registry_mode(ERegistryMode.shuffle)
        # Shuffle tuples if fragmentation introduced any changes in the registry
        if fragmentation_requires_shuffle:
            registry_to_data_sync_result = self._registry_ctrl.ctrl_sync_registry_to_data()
            if registry_to_data_sync_result.failed:
                raise EnvironmentInitializationError(data_gen_result.error)
        # Create an initial default snapshot (Environment will roll back to this snapshot at reset)
        snapshot_create_result = self._registry_ctrl.ctrl_snapshot_create(True)
        if snapshot_create_result.failed:
            raise EnvironmentInitializationError(snapshot_create_result.error)
        default_snapshot_id = snapshot_create_result.result
        if default_snapshot_id:
            self._logger.info("Default snapshot created with ID: %s", default_snapshot_id)
        else:
            raise EnvironmentInitializationError("Failed to create a new default snapshot, "
                                                 "no ID returned from the registry")

        self._logger.info("Environment initialization is complete")

    def _set_registry_mode(self, target_mode: ERegistryMode):
        """
        Switch current registry mode to a target mode if needed (if it's not already in that specific mode of execution)
        :param target_mode: ERegistryMode value
        """
        get_mode_result = self._registry_ctrl.ctrl_get_mode()  # Verify that the environment is in benchmark mode
        if get_mode_result.failed:
            raise EnvironmentInitializationError(get_mode_result.error)
        if get_mode_result.result == ERegistryMode.unknown:
            raise RuntimeError("Registry is in unknown mode")
        if get_mode_result.result != target_mode:
            self._logger.info("Switching registry to %s mode, current mode: %s",
                              target_mode.name, get_mode_result.result.name)
            if target_mode == ERegistryMode.benchmark:
                set_bench_result = self._registry_ctrl.ctrl_set_benchmark_mode()
            elif target_mode == ERegistryMode.shuffle:
                set_bench_result = self._registry_ctrl.ctrl_set_shuffle_mode()
            else:
                raise RuntimeError("Illegal registry mode switch attempt")
            RegistryResponseHelper.raise_on_error(set_bench_result)

    def reset(self):
        self._logger.info("Resetting the environment")
        reset_call_response = self._registry_ctrl.ctrl_reset()
        RegistryResponseHelper.raise_on_error(reset_call_response)
        # TODO: Return observation space
        obs_n = []
        agents = self._scenario.get_agents()
        for agent in agents:
            obs_n.append(agent.get_observation())
        return obs_n

    def step(self, action_n):
        """

        :param action_n:
        :return:
        """
        # TODO: Return observations per agent
        # TODO: Return reward per agent
        obs_n = []
        reward_n = []
        done_n = []
        info_n = {'n': []}

        agents = self._scenario.get_agents()
        for i, agent in enumerate(agents):
            self._set_action(action_n[i], agent, self.action_space[i])


        return obs_n, reward_n, done_n, info_n
