package goway.me.tfengine.core.model;

import lombok.Data;

import java.util.*;

@Data
public class NodeData {

    private RegistryData registryData;
    private String registryPath;
    private Set<String> ipList=new HashSet<>();
    private List<MetHodData> metHodDataList=new ArrayList<>();
    private long timestamp= new Date().getTime();
}
