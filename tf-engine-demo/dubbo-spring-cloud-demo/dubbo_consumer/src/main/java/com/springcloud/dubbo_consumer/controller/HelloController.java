package com.springcloud.dubbo_consumer.controller;

import com.springcloud.dubbo_api.service.TEngineService;
import goway.me.tfengine.core.model.NodeData;
import goway.me.tfengine.core.model.RegistryData;
import goway.me.tfengine.core.utils.APILib;
import goway.me.tfengine.core.utils.Base64FileUtil;
import goway.me.tfengine.core.utils.DubboRemoteUtil;
import org.apache.dubbo.config.annotation.Reference;
import org.apache.dubbo.config.spring.ReferenceBean;
import org.springframework.beans.BeanUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.PostConstruct;
import java.io.File;
import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.*;
import java.util.stream.Collectors;

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

    @GetMapping("/getInvokeMap")
    public Map<String,Map<String,String>> getInvokeMap(){
        return DubboRemoteUtil.getInvokeMap();
    }

    @GetMapping("/getRegistryList")
    public List<NodeData> getRegistryList(){
        return APILib.getNodeDataMap().stream().map(nodeData -> {
            NodeData newNodeData=new NodeData();
            BeanUtils.copyProperties(nodeData,newNodeData);
            newNodeData.getRegistryData().setJarData(null);
            newNodeData.getRegistryData().setUsername(null);
            newNodeData.getRegistryData().setPassword(null);
            return newNodeData;
        }).collect(Collectors.toList());
    }
}
