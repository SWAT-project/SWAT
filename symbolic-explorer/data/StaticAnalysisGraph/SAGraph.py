from __future__ import annotations
from dataclasses import dataclass, field
import json

@dataclass
class SANode:
    id: str
    onPathToAssert: bool = False
    isPhantomGuard: bool = False # a phantom guard is a generated explicit check for an implicit exception, e.g. `if (op2 != 0)` before an IDIV.
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
        if self.is_branch():
            return self
        if self.has_fallthrough_child():
            return self.get_fallthrough_child().walk_till_branch()
        return None
    
    def add_fallthrough_child(self, child: SANode):
        assert self.next_fallthrough is None
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
            self.nodes[id] = SANode(id)
        
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



if __name__ == "__main__":
    import sys
    tree = SAGraph()
    tree.load_json_graph(sys.argv[1])
    print(tree.entry_node)
    print("not onPathToAssert:", [n.id for n in tree.nodes.values() if not n.onPathToAssert])
