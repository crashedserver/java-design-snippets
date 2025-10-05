package system_design.consensus.raft.storage.model;

public class NodeId {
    private final String nodeId;
    public NodeId(String nodeId){
        this.nodeId=nodeId;
    }

    public String getNodeId(){
        return this.nodeId;
    }
}
