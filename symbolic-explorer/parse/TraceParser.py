from typing import List

from data.trace.Special import Special
from data.trace.Branch import Branch
from data.trace.Input import Input

from parse.DataTransferObjects import TraceItem, InputItem


class Parser:
    @staticmethod
    def parse_trace(trace: List[TraceItem], trace_id: str) -> List:
        _trace = []
        for branch in trace:
            if branch.type == "Branch":
                sanitized_constraint = ''
                for c in branch.constraint:
                    if ord(c) == 34:
                        sanitized_constraint += '\\"'
                    elif ord(c) == 10:
                        sanitized_constraint += ''
                    else:
                        sanitized_constraint += c
                _trace.append(Branch(id=branch.iid,
                                     trace_id=trace_id,
                                     has_branched=branch.branched,
                                     constraint=sanitized_constraint))
            else:
                _trace.append(Special(id=branch.iid,
                                      trace_id=trace_id,
                                      has_branched=True,
                                      inst=branch.inst))
        return _trace

    @staticmethod
    def parse_inputs(inputs: List[InputItem]) -> List:
        _inputs = []
        for input in inputs:
            _inputs.append(Input(name=input.name,
                                 value=input.value,
                                 type=input.type,
                                 lower_bound=input.lowerBound,
                                 upper_bound=input.upperBound))
        return _inputs
