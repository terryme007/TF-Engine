package goway.me.tfengine.core.utils;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.dubbo.config.RegistryConfig;
import org.apache.dubbo.config.spring.ReferenceBean;
import org.springframework.context.ApplicationContext;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Method;
import java.net.URL;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.stream.Collectors;

@Slf4j
public class DubboRemoteUtil {

    private static ConcurrentHashMap<String,ConcurrentHashMap<String,Method>> dubboServiceMethod=new ConcurrentHashMap<>();
    private static ConcurrentHashMap<String,Object> dubboReferenceBean=new ConcurrentHashMap<>();

    public static Object remoteInvoke(String invokeMethod,List methodParams,String version){
        String[] methodSplit = invokeMethod.split("\\.");
        if(methodSplit.length!=2){
            return "调用接口名称不合法";
        }
        String invokeInterfaceName= methodSplit[0]+version;
        String invokeMethodName= methodSplit[1];
        if(dubboServiceMethod.size()==0 || dubboReferenceBean.size()==0 ||
                null==dubboServiceMethod.get(invokeInterfaceName) ||
                null==dubboServiceMethod.get(invokeInterfaceName).get(invokeMethodName)){
            return "未注册方法";
        }
        try {
            //远程调用
            Object res=dubboServiceMethod.get(invokeInterfaceName).get(invokeMethodName).invoke(dubboReferenceBean.get(invokeInterfaceName), methodParams.toArray());
            System.out.println(res);
            //todo 是否支持并发、并发效率有待测试
            return res;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    public static synchronized ConcurrentHashMap<String,ConcurrentHashMap<String,Method>> importService(String classPath, String version, String packagePath, String registryAddress, String username, String password){
        int successCount=0;
        try{
            //从目录中查找全部jar包
            List<File> listFiles = JarFileUtils.getListFiles(classPath,"jar");
            log.info("DubboRemoteUtil.importService 找到{}个文件",listFiles.size());
            List<String> filePathList=listFiles.stream().map(File::getPath).collect(Collectors.toList());
            //加载外部jar包
            ExtClasspathLoader.loadClasspath(filePathList);
            //从jar包加载指定package下的类
            ConcurrentHashMap<String,ConcurrentHashMap<String,Method>> resServiceMethod=new ConcurrentHashMap<>();
            for(File file:listFiles){
                //jar文件目录
                URL jarUrl = file.toURL();
                //加载类
                Set<Class<?>> classSet = JarFileUtils.getPackageService(jarUrl,packagePath);

                for(Class myClazz:classSet) {
                    String mapKey=myClazz.getSimpleName()+version;
                    //获取接口的方法
                    ConcurrentHashMap<String,Method> methodMap=dubboServiceMethod.get(mapKey);
                    if(methodMap==null){
                        methodMap=new ConcurrentHashMap<>();
                    }
                    Method[] methods = myClazz.getMethods();
                    for(Method method:methods){
                        methodMap.put(method.getName(),method);
                    }
                    dubboServiceMethod.put(mapKey,methodMap);
                    resServiceMethod.put(mapKey,methodMap);

                    try{
                        //声明dubbo接口
                        ReferenceBean referenceBean = new ReferenceBean();
                        ApplicationContext applicationContext = SpringContextHolder.getApplicationContext();
                        referenceBean.setApplicationContext(applicationContext);
                        referenceBean.setInterface(myClazz);
                        referenceBean.setVersion(version);
                        referenceBean.setCheck(false);
                        //使用注册中心注册
                        RegistryConfig registryConfig=new RegistryConfig();
                        registryConfig.setAddress(registryAddress);
                        registryConfig.setUsername(username);
                        registryConfig.setPassword(password);
                        referenceBean.setRegistry(registryConfig);
                        referenceBean.afterPropertiesSet();
                        Object object = referenceBean.get();
                        dubboReferenceBean.put(mapKey,object);
                    }catch (Exception e){
                        log.error("DubboRemoteUtil.importService 注册dubbo服务{}失败",myClazz.getCanonicalName(),e);
                        continue;
                    }
                    successCount++;
                }

            }
            return resServiceMethod;
        }catch (Exception e){
            e.printStackTrace();
        }
        return null;
    }

    public static Map<String,Map<String,String>> getInvokeMap(){
        Map<String,Map<String,String>> invokeMap=new HashMap<>();
        for(String interfaceName:dubboServiceMethod.keySet()){
            ConcurrentHashMap<String, Method> dubboMethodMap = dubboServiceMethod.get(interfaceName);
            Map<String, String> methodMap=new HashMap<>();
            for(String methodName:dubboMethodMap.keySet()){
                Method method = dubboMethodMap.get(methodName);
                Class<?>[] parameterTypes = method.getParameterTypes();
                StringBuilder params= new StringBuilder();
                for(Class clazz:parameterTypes){
                    params.append(clazz.getCanonicalName()).append(",");
                }
                if(params.length()>1){
                    params = new StringBuilder(params.substring(0, params.length() - 1));
                }
                methodMap.put(methodName,"("+params+")");
            }
            invokeMap.put(interfaceName,methodMap);
        }
        return invokeMap;
    }
}
