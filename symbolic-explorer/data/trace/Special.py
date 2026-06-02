from typing import Optional


class Special:
    def __init__(self, id: int, trace_id: str, has_branched: bool, inst: Optional[str]):
        self.id = id
        self.trace_id = trace_id
        self.has_branched = has_branched
        self.inst = inst

    def __str__(self):
        return f'[(S) - {self.id} - ({"T" if self.has_branched else "F"})]'