from __future__ import annotations
from dataclasses import dataclass, field
import json

@dataclass
class SANode:
    id: str
    onPathToAssert: bool = False
    isPhantomGuard: bool = False # a phantom guard is a generated explicit check for an implicit exception, e.g. `if (op2 != 0)` before an IDIV.
    # The static analysis could not expand something here (a call whose target it could not pin
    # down, or one cut off by its recursion depth limit), so the statements and branches of
    # whatever was left out are missing from the graph. We walk this graph in step with a real
    # execution, matching branch for branch by position, so past this node the execution reports
    # branches the graph has no counterpart for and every later pairing would be shifted by them.
    # walk_till_branch() therefore stops here and reports "no information", which callers already
    # treat as "everything is interesting".
    isIncomplete: bool = False
    prev: list[SANode] = field(default_factory=list, repr=False) # preceding nodes
    next_fallthrough: SANode | None = None
    next_branched: SANode | None = None
    next_exceptional: list[SANode] = field(default_factory=list)
    
    def has_fallthrough_child(self):
        return self.next_fallthrough is not None
    
    def has_branched_child(self):
        return self.next_branched is not None
    
    def get_fallthrough_child(self) -> SANode:
        assert self.next_fallthrough is not None
        return self.next_fallthrough
    
    def get_branched_child(self) -> SANode:
        assert self.next_branched is not None
        return self.next_branched
    
    def is_branch(self):
        if self.has_branched_child():
            assert self.has_fallthrough_child()
            return True
        return False
    
    def walk_till_branch(self) -> SANode | None:
        if self.isIncomplete: # the graph stops being faithful here, so stop trusting it
            return None
        if self.is_branch():
            return self
        if self.has_fallthrough_child():
            return self.get_fallthrough_child().walk_till_branch()
        return None
    
    def add_fallthrough_child(self, child: SANode):
        assert self.next_fallthrough is None, f"SANode {self.id} has multiple fallthrough edges!"
        self.next_fallthrough = child
    
    def add_branched_child(self, child: SANode):
        assert self.next_branched is None
        self.next_branched = child
    
    def add_exceptional_child(self, child: SANode):
        self.next_exceptional.append(child)

def mark_assertion_path(node: SANode):
    if node.onPathToAssert: # already marked, prevent infinite recursion
        return
    
    node.onPathToAssert = True
    for prev in node.prev:
        mark_assertion_path(prev)



class SAGraph:
    def __init__(self):
        self.json_graph = {}
        self.nodes: dict[str, SANode] = {}
        self.entry_node: SANode | None = None


    def load_json_graph(self, path: str):
        with open(path, "r") as f:
            self.json_graph = json.load(f)
        
        for json_node in self.json_graph["nodes"]:
            id = json_node["id"]
            # Older graphs carry no isIncomplete field; absent means the node is faithful.
            self.nodes[id] = SANode(id, isIncomplete=json_node.get("isIncomplete", False))
        
        for json_edge in self.json_graph["edges"]:
            source = self.nodes[json_edge["source"]]
            target = self.nodes[json_edge["target"]]
            
            etype = json_edge["type"]
            if etype == "EXCEPTION":
                source.add_exceptional_child(target)
                
            elif etype == "PHANTOM_TRUE_BRANCH":
                source.add_branched_child(target)
                source.isPhantomGuard = True
                
            elif etype == "TRUE_BRANCH":
                source.add_branched_child(target)
                
            else: # FALSE_BRANCH, PHANTOM_FALSE_BRANCH, NORMAL, CALL, RETURN
                source.add_fallthrough_child(target)

            target.prev.append(source)
        

        self.entry_node = self.nodes[self.json_graph["entryNodeId"]]

        for assertion_point_id in self.json_graph["metadata"]["assertionPointIds"]:
            mark_assertion_path(self.nodes[assertion_point_id])

        # An incomplete node may hide an assert in the part that was not expanded, so the paths
        # leading to it have to stay interesting. The extractor already reports these as assertion
        # points; marking them here too means a graph that ever forgets one half still errs on the
        # safe side rather than pruning a path that leads to something we cannot see.
        for node in self.nodes.values():
            if node.isIncomplete:
                mark_assertion_path(node)



if __name__ == "__main__":
    import sys
    tree = SAGraph()
    tree.load_json_graph(sys.argv[1])
    print(tree.entry_node)
    print("not onPathToAssert:", [n.id for n in tree.nodes.values() if not n.onPathToAssert])
