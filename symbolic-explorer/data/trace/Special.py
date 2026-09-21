from typing import Optional

# Fully qualified names of the instructions that delimit a static initializer in the trace. The
# executor emits CLINIT when it enters a <clinit> and INVOKECLINIT_END when it leaves it again.
CLINIT = 'de.uzl.its.swat.symbolic.instruction.CLINIT'
CLINIT_END = 'de.uzl.its.swat.symbolic.instruction.INVOKECLINIT_END'


def short_inst(inst: Optional[str]) -> str:
    """Strips the package from an instruction name for logging, e.g. '...instruction.CLINIT' -> 'CLINIT'."""
    return inst.rsplit('.', 1)[-1] if inst else '?'


class Special:
    def __init__(self, id: int, trace_id: str, has_branched: bool, inst: Optional[str]):
        self.id = id
        self.trace_id = trace_id
        self.has_branched = has_branched
        self.inst = inst

    def __str__(self):
        return f'[(S) - {self.id} - ({"T" if self.has_branched else "F"}) - {short_inst(self.inst)}]'
