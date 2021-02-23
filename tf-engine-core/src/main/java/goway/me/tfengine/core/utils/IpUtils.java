package goway.me.tfengine.core.utils;

import java.net.InetAddress;

public class IpUtils {

    public static String getIp(){
        String ip=null;
        try{
            InetAddress addr = InetAddress.getLocalHost();
            ip=addr.getHostAddress();
            System.out.println("ip:" + ip);
        }catch (Exception e){
            e.printStackTrace();
        }
        return ip;
    }

    public static String getHost(){
        String host=null;
        try{
            InetAddress addr = InetAddress.getLocalHost();
            host=addr.getHostName();
            System.out.println("host:" + host);
        }catch (Exception e){
            e.printStackTrace();
        }
        return host;
    }
}
