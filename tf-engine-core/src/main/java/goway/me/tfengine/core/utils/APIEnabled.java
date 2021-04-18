package goway.me.tfengine.core.utils;

import lombok.Data;

@Data
public class APIEnabled {

    private static boolean enabled=false;

    public static void enabled(){
        enabled=true;
    }

    public static void disabled(){
        enabled=false;
    }

    public static boolean getEnabled(){
        return enabled;
    }
}
