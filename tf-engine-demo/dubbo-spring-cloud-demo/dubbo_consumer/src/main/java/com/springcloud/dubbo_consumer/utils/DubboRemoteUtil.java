package com.springcloud.dubbo_consumer.utils;

import com.google.common.collect.Table;
import org.apache.commons.lang.StringUtils;
import org.apache.dubbo.config.RegistryConfig;
import org.apache.dubbo.config.spring.ReferenceBean;
import org.springframework.context.ApplicationContext;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.lang.reflect.Method;
import java.net.JarURLConnection;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.stream.Collectors;

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

    public static synchronized int initService(String classPath, String version, String packagePath){
        int successCount=0;
        try{
            //从目录中查找全部jar包
            List<File> listFiles = getListFiles(classPath,"jar");
            System.out.println("找到"+listFiles.size()+"个文件");
            List<String> filePathList=listFiles.stream().map(File::getPath).collect(Collectors.toList());
            //加载外部jar包
            ExtClasspathLoader.loadClasspath(filePathList);
            //从jar包加载指定package下的类
            for(File file:listFiles){
                //jar文件目录
                URL jarUrl = file.toURL();
                //加载类
                Set<Class<?>> classSet = getPackageService(jarUrl,packagePath);

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

                    try{
                        //声明dubbo接口
                        ReferenceBean referenceBean = new ReferenceBean();
                        ApplicationContext applicationContext = SpringContextHolder.getApplicationContext();
                        referenceBean.setApplicationContext(applicationContext);
                        referenceBean.setInterface(myClazz);
                        referenceBean.setVersion(version);
                        referenceBean.setCheck(false);
                        //方法一：利用地址直接访问，不经过注册中心
//                        String url = "dubbo://localhost:9999/"+serviceRelativePath;
//                        referenceBean.setUrl(url);
                        //方法二：利用注册中心访问，可以使用自带的负载均衡策略
                        //使用spring cloud注册中心
                        RegistryConfig registryConfig=new RegistryConfig();
                        registryConfig.setAddress("spring-cloud://localhoost");
                        //使用nacos注册中心
//                        registryConfig.setAddress("nacos://localhost:8848");
//                        registryConfig.setUsername("nacos");
//                        registryConfig.setPassword("nacos");
                        referenceBean.setRegistry(registryConfig);
                        referenceBean.afterPropertiesSet();
                        Object object = referenceBean.get();
                        dubboReferenceBean.put(mapKey,object);
                    }catch (Exception e){
                        System.out.println("连接dubbo服务"+myClazz.getCanonicalName()+"失败");
                        e.printStackTrace();
                    }
                    successCount++;
                }

            }
            return successCount;
        }catch (Exception e){
            e.printStackTrace();
        }
        return successCount;
    }

    //从jar包中加载指定package下的类
    private static Set<Class<?>> getPackageService(URL url,String pack){
        Set<Class<?>> classes = new LinkedHashSet<Class<?>>();// 第一个class类的集合
        boolean recursive = true;// 是否循环迭代
        String packageName = pack;// 获取包的名字 并进行替换
        String packageDirName = packageName.replace('.', '/');
//        ClassLoader systemClassLoader = ClassLoader.getSystemClassLoader();
//        ClassLoader systemClassLoader = Thread.currentThread().getContextClassLoader();
//        ClassLoader classLoader = new URLClassLoader( new URL[] { url }, systemClassLoader );

        JarFile jar;
        try {
            // 获取jar
            jar = new JarFile(url.getFile());
            // 从此jar包 得到一个枚举类
            Enumeration<JarEntry> entries = jar.entries();
            // 同样的进行循环迭代
            while (entries.hasMoreElements()) {
                // 获取jar里的一个实体 可以是目录 和一些jar包里的其他文件 如META-INF等文件
                JarEntry entry = entries.nextElement();
                String name = entry.getName();
                // 如果是以/开头的
                if (name.charAt(0) == '/') {
                    // 获取后面的字符串
                    name = name.substring(1);
                }
                // 如果前半部分和定义的包名相同
                if (name.startsWith(packageDirName)) {
                    int idx = name.lastIndexOf('/');
                    // 如果以"/"结尾 是一个包
                    if (idx != -1) {
                        // 获取包名 把"/"替换成"."
                        packageName = name.substring(0, idx).replace('/', '.');
                    }
                    // 如果可以迭代下去 并且是一个包
                    if ((idx != -1) || recursive) {
                        // 如果是一个.class文件 而且不是目录
                        if (name.endsWith(".class") && !entry.isDirectory()) {
                            // 去掉后面的".class" 获取真正的类名
                            String className = name.substring(packageName.length() + 1, name.length() - 6);
                            try {
                                // 添加到classes
//                                Class<?> extClass = classLoader.loadClass(packageName + '.' + className);
//                                if("TestSSS".equals(extClass.getSimpleName())){
//                                    extClass.getMethod("hello").invoke(extClass.newInstance());
//                                }

                                Class<?> extClass = Class.forName(packageName + '.' + className);
                                classes.add(extClass);
                            } catch (Exception e) {
                                // log.error("添加用户自定义视图类错误 找不到此类的.class文件");
                                e.printStackTrace();
                            }
                        }
                    }
                }
            }
        } catch (IOException e) {
            // log.error("在扫描用户定义视图时从jar包获取文件出错");
            e.printStackTrace();
        }
        return classes;
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

    /**
     * 获取目录下的文件
     * @param obj
     * @return
     */
    private static ArrayList<File> getListFiles(Object obj,String fileType) {
        File directory = null;
        if (obj instanceof File) {
            directory = (File) obj;
        } else {
            directory = new File(obj.toString());
        }
        ArrayList<File> files = new ArrayList<File>();
        if (directory.isFile()) {
            if(StringUtils.isNotBlank(fileType)){
                int lastDot = directory.getName().lastIndexOf(".");
                if(lastDot>0 && fileType.equals(directory.getName().substring(lastDot+1))){
                    files.add(directory);
                }
            }

            return files;
        } else if (directory.isDirectory()) {
            if(!directory.getName().equals(".git") && !directory.getName().equals("targer") && !directory.getName().equals("logs")){
                File[] fileArr = directory.listFiles();
                for (int i = 0; i < fileArr.length; i++) {
                    File fileOne = fileArr[i];
                    files.addAll(getListFiles(fileOne,fileType));
                }
            }
        }
        return files;
    }
}
