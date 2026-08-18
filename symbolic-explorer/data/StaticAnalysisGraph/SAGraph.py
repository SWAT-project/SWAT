from __future__ import annotations
from dataclasses import dataclass, field
import json

@dataclass
class SANode:
    id: str
    onPathToAssert: bool = False
    prev: list[SANode] = field(default_factory=list, repr=False)
    next: list[SANode] = field(default_factory=list)
    
    def has_fallthrough_child(self):
        return 0 < len(self.next)
    
    def has_branched_child(self):
        return 1 < len(self.next)
    
    def get_fallthrough_child(self):
        return self.next[0] #if 0 < len(self.next) else None
    
    def get_branched_child(self):
        return self.next[1] #if 1 < len(self.next) else None
    
    def is_branch(self):
        return len(self.next) > 1
    
    def walk_till_branch(self) -> SANode | None:
        if self.is_branch():
            return self
        if self.has_fallthrough_child():
            return self.get_fallthrough_child().walk_till_branch()
        return None
    
    def add_fallthrough_child(self, child: SANode):
        self.next.insert(0, child)
    
    def add_branched_child(self, child: SANode):
        self.next.append(child)

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

            if json_edge["type"] == "TRUE_BRANCH":
                source.add_branched_child(target)
            else:
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
