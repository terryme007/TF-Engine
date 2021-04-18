package goway.me.tfengine.core.model;

import lombok.Data;

@Data
public class MetHodData {

    private String name;
    private String reqParams;
    private String resParams;
    private String invokeType;
    private String requestMethodType;
    private String contentType;
}
