package goway.me.tfengine.core.utils;

import lombok.extern.slf4j.Slf4j;

import java.net.InetAddress;

@Slf4j
public class IpUtils {

    public static String getIp(){
        String ip=null;
        try{
            InetAddress addr = InetAddress.getLocalHost();
            ip=addr.getHostAddress();
            log.info("IpUtils.getIp ip:" + ip);
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
            System.out.println("IpUtils.getHost host:" + host);
        }catch (Exception e){
            e.printStackTrace();
        }
        return host;
    }
}
