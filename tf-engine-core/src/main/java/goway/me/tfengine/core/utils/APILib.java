package goway.me.tfengine.core.utils;

import goway.me.tfengine.core.model.NodeData;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

public class APILib {

    private static Map<String,NodeData> nodeDataMap =new TreeMap<>();

    public static synchronized void addNodeData(NodeData nodeData){
        NodeData node = nodeDataMap.get(nodeData.getRegistryPath());
        if(node==null){
            nodeDataMap.put(nodeData.getRegistryPath(),nodeData);
        }else{
            if(nodeData.getTimestamp()>node.getTimestamp()){
                nodeDataMap.put(nodeData.getRegistryPath(),nodeData);
            }
        }
    }

    public static synchronized void addNodeDataList(List<NodeData> nodeDataList){
        nodeDataList.forEach(APILib::addNodeData);
    }

    public static List<NodeData> getNodeDataMap(){
        return new ArrayList<>(nodeDataMap.values());
    }

}
