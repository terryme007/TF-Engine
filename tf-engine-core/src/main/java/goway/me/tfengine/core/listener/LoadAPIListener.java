package goway.me.tfengine.core.listener;

import com.alibaba.fastjson.JSON;
import goway.me.tfengine.core.dic.InvokeType;
import goway.me.tfengine.core.model.MetHodData;
import goway.me.tfengine.core.model.NodeData;
import goway.me.tfengine.core.model.RegistryData;
import goway.me.tfengine.core.utils.*;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationListener;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.context.event.ContextStartedEvent;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.io.File;
import java.lang.reflect.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Component
public class LoadAPIListener implements ApplicationListener<ContextRefreshedEvent> {

    private final static String defaultZkAddress="localhost:2181";
    @Autowired
    private Environment env;

    @Override
    public void onApplicationEvent(ContextRefreshedEvent contextRefreshedEvent) {
        loadAPI();
    }

    public void loadAPI(){
        if(APIEnabled.getEnabled()){
            //加载dubbo远程下载接口
            log.info("TEngineClientImportSelector.registerBeanDefinitions 开始获取注册的接口");
            try{
                String zkAddress=env.getProperty("zookeeper.address");
                if(StringUtils.isBlank(zkAddress)){
                    zkAddress=defaultZkAddress;
                }
                //获取注册到zk的dubbo接口
                String registerPathDubbo="/tfengine/dubbo_api";
                List<String> childrenPathDubbo = ZookeeperUtils.getChildren(zkAddress, registerPathDubbo);
                //将子节点分类
                List<NodeData> nodeDataList=new ArrayList<>();
                for(String nodePath : childrenPathDubbo){
                    log.info("解析节点：{}",nodePath);
                    NodeData nodeData=new NodeData();
                    String registryPath=nodePath.substring(0,nodePath.lastIndexOf("/"));
                    String registerIp=nodePath.substring(nodePath.lastIndexOf("/")+1);
                    nodeData.setRegistryPath(registryPath);
                    String dataJson = ZookeeperUtils.get(zkAddress, registryPath);
                    nodeData.setRegistryData(JSON.parseObject(dataJson, RegistryData.class));
                    nodeData.getIpList().add(registerIp);
                    nodeDataList.add(nodeData);
                }
                //下载jar包
                nodeDataList.forEach(nodeData -> {
                    RegistryData registryData = nodeData.getRegistryData();
                    System.out.println(registryData.getJarData());
                    String filePath="apiJar/"+nodeData.getRegistryPath().substring(
                            nodeData.getRegistryPath().lastIndexOf("/")+1)+".jar";
                    try{
                        Base64FileUtil.generateFile(registryData.getJarData(),filePath);
                        //注册接口到注册中心，返回注册成功的方法
                        ConcurrentHashMap<String, ConcurrentHashMap<String, Method>> serviceMetHod = DubboRemoteUtil.importService(filePath, registryData.getVersion(), registryData.getPackageName()
                                , registryData.getAddress(), registryData.getUsername(), registryData.getPassword());
                        if(serviceMetHod!=null){
                            List<MetHodData> metHodDataList=new ArrayList<>();
                            serviceMetHod.values().forEach(methodMap->{
                                methodMap.values().forEach(method -> {
                                    MetHodData metHodData=new MetHodData();
                                    metHodData.setInvokeType(InvokeType.DUBBO);
                                    //拼接数据
                                    metHodData.setName(method.getName());
                                    Parameter[] parameters = method.getParameters();
                                    String reqJson = getParamsMap(parameters);
                                    metHodData.setReqParams(reqJson);
                                    Class<?> returnType = method.getReturnType();
                                    String resJson="{}";
                                    if(!returnType.getName().equals(Void.class.getName())){
                                        Map<String, Object> resMap = getTypeNameMap("res", returnType, null);
                                        resJson=JSON.toJSONString(resMap.get("res"));
                                    }

                                    metHodData.setResParams(resJson);
                                    metHodDataList.add(metHodData);
                                });
                            });
                            nodeData.setMetHodDataList(metHodDataList);
                        }
                    }catch (Exception e){
                        log.error("接口注册失败：",e);
                    }
                    nodeData.setTimestamp(new Date().getTime());
                });

                //保存注册信息
                APILib.addNodeDataList(nodeDataList);

                //获取注册到zk的rest接口
                String registerPathRest="/tfengine/rest_api";
                List<String> childrenPathRest = ZookeeperUtils.getChildren(zkAddress, registerPathRest);
                childrenPathRest.forEach(System.out::println);
            }catch (Exception e){
                e.printStackTrace();
            }
            log.info("TEngineClientImportSelector.registerBeanDefinitions 注册dubbo接口jar完成");
        }

    }

    private String getParamsMap(Parameter[] parameters){
        if(parameters.length==0){
            return "{}";
        }
        Map<String,Object> paramsMap=new LinkedHashMap<>();
        for(Parameter params:parameters){
            String paramsName = params.getName();
            Class<?> paramsType = params.getType();
            paramsMap.putAll(getTypeNameMap(paramsName,paramsType,null));
        }
        return JSON.toJSONString(paramsMap);
    }

    private Map<String,Object> getTypeNameMap(String paramsName,Class paramsType,Class actualTypeArgument){
        Map<String,Object> paramsMap=new LinkedHashMap<>();
        try{
            String objType = isPrimitive(paramsType);
            switch (objType){
                case BASE:{
                    //基础数据类型
                    String simpleName = paramsType.getSimpleName();
                    System.out.println(simpleName+" "+paramsName);
                    paramsMap.put(paramsName,simpleName);
                    break;
                }
                case OBJECT:{
                    //对象，递归
                    Field[] fields = paramsType.getDeclaredFields();
                    //todo 对接口和超类的处理
                    Map<String, Object> fieldListMap = getFieldListMap(paramsType, fields);
                    paramsMap.put(paramsName,fieldListMap);
                    break;
                }
                case COLLECTIONS:{
                    //集合，循环递归
                    List<Map<String, Object>> fieldListMapList=new ArrayList<>();
                    Field[] fields = actualTypeArgument.getDeclaredFields();
                    Map<String, Object> fieldListMap = getFieldListMap(paramsType, fields);
                    fieldListMapList.add(fieldListMap);
                    paramsMap.put(paramsName,fieldListMapList);
                    break;
                }
            }
        }catch (Exception e){
            log.error("{}实例化失败：",paramsType.getName(),e);
        }
        return paramsMap;
    }

    private Map<String, Object> getFieldListMap(Class paramsType,Field[] fields){
        Map<String, Object> fieldListMap=new LinkedHashMap<>();
        for(Field field:fields){
            String fieldName = field.getName();
            Class<?> fieldType = field.getType();
            if(fieldType.getName().equals(paramsType.getName())){
                continue;
            }
            Class<?> nowActualTypeArgument = null;
            if(isPrimitive(fieldType).equals(COLLECTIONS)){
                Type genericType = field.getGenericType();
                if (null == genericType) {
                    continue;
                }
                if (genericType instanceof ParameterizedType) {
                    ParameterizedType pt = (ParameterizedType) genericType;
                    // 得到泛型里的class类型对象
                    nowActualTypeArgument = (Class<?>)pt.getActualTypeArguments()[0];
                }
            }
            Map<String, Object> fieldMap = getTypeNameMap(fieldName, fieldType,nowActualTypeArgument);
            fieldListMap.putAll(fieldMap);
        }
        return fieldListMap;
    }


    private static final String BASE="base";
    private static final String OBJECT ="obj";
    private static final String COLLECTIONS ="list";

    public static String isPrimitive(Class clazz) {
        if(clazz.isArray()){
            return BASE;
        }
        if(clazz.isInterface()){
            Class[] interfaces = clazz.getInterfaces();
            if(interfaces!=null && interfaces.length>0 && interfaces[0].getName().equals(Collection.class.getName())){
                return COLLECTIONS;
            }
        }
        if(!clazz.isInterface() && clazz.isPrimitive()){
            return BASE;
        }
        Object obj = null;
        try{
            obj = clazz.newInstance();
        }catch (Exception e){
            return BASE;
        }
        String objStr=JSON.toJSONString(obj);
        try{
            JSON.parseObject(objStr);
            return OBJECT;
        }catch (Exception e){
            try{
                JSON.parseArray(objStr);
                return COLLECTIONS;
            }catch (Exception e2){
                return BASE;
            }
        }
    }
}
