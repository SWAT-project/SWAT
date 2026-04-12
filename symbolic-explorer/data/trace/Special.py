class Special:
    def __init__(self, id, trace_id, has_branched, inst):
        self.id = id
        self.trace_id = trace_id
        self.has_branched = has_branched
        self.inst = inst

    def __str__(self):
        return f'[(S) - {self.id} - ({"T" if self.has_branched else "F"})]'