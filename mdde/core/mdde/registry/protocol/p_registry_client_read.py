from abc import abstractmethod
from typing import Set, Dict, Union

try:
    from typing import Protocol
except ImportError:
    from typing_extensions import Protocol

from mdde.registry.container import RegistryResponse


class PRegistryReadClient(Protocol):

    @abstractmethod
    def read_everything(self) -> RegistryResponse[str]: # TODO: Explicit type
        """
        Retrieve the entire registry state including all of the fragments and tuples for all of the nodes.
        *The function is primarily meant for debugging.*
        :return: Full registry state.
        """
        raise NotImplementedError()

    @abstractmethod
    def read_find_fragment(self, fragment_id: str) -> RegistryResponse[str]:  # TODO: Explicit type
        """
        Get nodes where the fragment is located.
        :param fragment_id: Fragment ID.
        :return: List of nodes where the fragment is allocated.
        """
        raise NotImplementedError()

    @abstractmethod
    def read_get_all_fragments_with_meta(self,
                                         local_meta: Union[Set[str], None],
                                         global_meta: Union[Set[str], None]) -> RegistryResponse[Dict]:
        # TODO: Explicit return type
        """
        A complete catalog of fragments with optional meta values.
        :param local_meta: Exemplar bound meta values for fragments.
        :param global_meta: Global meta values for fragments.
        :return: Full allocation map.
        """
        raise NotImplementedError()

    @abstractmethod
    def read_count_fragment(self, fragment_id: str) -> RegistryResponse[int]:
        """
        Retrieve how many copies of a specific fragment exists in the registry.
        :param fragment_id: Fragment ID.
        :return: Number of fragment exemplars across all nodes.
        """
        raise NotImplementedError()

    @abstractmethod
    def read_nodes(self) -> RegistryResponse[Set[str]]:  # TODO: Explicit type
        """
        Retrieve nodes IDs from the registry.
        :return: Set of Node IDs present in the registry.
        """
        raise NotImplementedError()

    @abstractmethod
    def read_node_unassigned_tuples(self, node_id: str) -> RegistryResponse[Set[str]]:
        """
        Retrieve ids of the tuples located on the nodes that aren't assigned ot a fragment.
        :param node_id: Node ID.
        :return: Set of tuple IDs.
        """
        raise NotImplementedError()

    @abstractmethod
    def read_node_fragments(self, node_id: str) -> RegistryResponse[Set[str]]:
        """
        Get fragments located on the specified node.
        :param node_id: Node ID.
        :return: Set of fragment IDs.
        """
        raise NotImplementedError()

    @abstractmethod
    def read_fragment_meta_on_exemplar(self, fragment_id: str, node_id: str, meta_tag: str) -> RegistryResponse[str]:
        """
        Get value of a meta tag attached to a specific exemplar of the fragment.
        :param fragment_id: Fragment ID.
        :param node_id: Node ID where the fragment is located.
        :param meta_tag: Meta tag.
        :return: Value associated with the meta tag.
        """
        raise NotImplementedError()

    @abstractmethod
    def read_fragment_meta_global(self, fragment_id: str, meta_tag: str) -> RegistryResponse[str]:
        """
        Get value of a meta tag attached to a fragment globally.
        :param fragment_id: Fragment ID.
        :param meta_tag: Meta tag.
        :return: Value associated with the global meta tag.
        """
        raise NotImplementedError()
