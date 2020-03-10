from typing import Sequence, Tuple
import natsort

from mdde.fragmentation.protocol import PFragmentSorter


class DefaultFragmentSorter(PFragmentSorter):
    """
    Default sorter of fragments. Sorts fragment by IDs in ascending numeric or alphabetic order.
    Suitable for the scenarios where the number of fragments is static (fragmentation is executed once and the generated
    fragments do no change over the course of the scenario run).
    """
    def sort(self, fragments: Sequence[str]) -> Tuple[str, ...]:
        return tuple(natsort.natsorted(fragments, key=lambda fragment: fragment))
