package com.springcloud.dubbo_consumer.controller;

import com.springcloud.dubbo_api.service.TEngineService;
import com.springcloud.dubbo_consumer.utils.Base64FileUtil;
import com.springcloud.dubbo_consumer.utils.DubboRemoteUtil;
import com.springcloud.dubbo_consumer.utils.SpringContextHolder;
import org.apache.dubbo.config.RegistryConfig;
import org.apache.dubbo.config.annotation.Reference;
import org.apache.dubbo.config.spring.ReferenceBean;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import java.io.File;
import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.*;

/**
 * Created with IntelliJ IDEA.
 *
 * @Date: 2019/9/3
 * @Time: 22:14
 * @email: inwsy@hotmail.com
 * Description:
 */
@RestController
public class HelloController {

//    @Reference(check = false,loadbalance = "demo",url = "dubbo://localhost:9999")
//    private HelloService helloService;
//
//    @GetMapping("/hello")
//    public String hello() {
//        return helloService.hello("Dubbo!");
//    }

    @Reference(check = false,version = "1.0")
    private TEngineService engineService;

    @GetMapping("/{version}/{invokeMethod}/{params}")
    public Object printHello(@PathVariable String version, @PathVariable String invokeMethod, @PathVariable String params) throws  Exception {
        /**
         * [
         *      {
         *          "type":"java.lang.String",
         *          "value":"hello"
         *      },
         *      {
         *          "type":"com.springcloud.dubbo_api.model.UserInfo",
         *          "values":{
         *              "key":"name",
         *              "type":"java.lang.String",
         *              "value":"terry"
         *          }
         *      }
         * ]
         *
         */
        List<String> methodParams = Arrays.asList(params.split(","));
        //调用远程接口
        return DubboRemoteUtil.remoteInvoke(invokeMethod,methodParams,version);
    }

    @GetMapping("/downAndRegister")
    public boolean downAndRegister(String apiReference, String modelName, String version){
        String fileBase64 = engineService.downApiJar(apiReference, modelName, version);
        if(fileBase64!=null){
            try {
                String savePath="apiJar/"+apiReference.substring(apiReference.lastIndexOf(".")+1)+"_"+version+".jar";
                boolean saveSuccess=Base64FileUtil.generateFile(fileBase64,savePath);
                if(saveSuccess){
                    String packagePath="com.springcloud.dubbo_api.service";
                    DubboRemoteUtil.initService(savePath,version,packagePath);
                }else{
                    return false;
                }
                return true;
            } catch (Exception e) {
                e.printStackTrace();
                return false;
            }
        }else{
            return false;
        }
    }

    @GetMapping("/init")
    public String initRemoteService(String jarPath){
        jarPath=jarPath.replaceAll("\\.","/");
        String version="1.0";
        String packagePath="com.springcloud.dubbo_api.service";
        System.out.println("jarPath:"+jarPath);
        int successCount = DubboRemoteUtil.initService(jarPath,version,packagePath);
        if(successCount==0){
            System.out.println("类初始化加载失败！");
        }
        return "成功加载"+successCount+"个类";
    }

    @GetMapping("/getInvokeMap")
    public Map<String,Map<String,String>> getInvokeMap(){
        return DubboRemoteUtil.getInvokeMap();
    }

    @GetMapping("/showClassPath")
    public void main(String[] args) {
        System.out.println(System.getProperty("java.class.path"));
        URL[] urLs = ((URLClassLoader) ClassLoader.getSystemClassLoader()).getURLs();
        for(URL url:urLs){
            System.out.println(url.getPath());
        }
    }


}
