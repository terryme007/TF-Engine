package goway.me.tfengine.core.annotation;

import com.alibaba.fastjson.JSON;
import goway.me.tfengine.core.model.RegistryData;
import goway.me.tfengine.core.utils.*;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.zookeeper.CreateMode;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.context.EnvironmentAware;
import org.springframework.context.ResourceLoaderAware;
import org.springframework.context.annotation.ImportBeanDefinitionRegistrar;
import org.springframework.context.annotation.Lazy;
import org.springframework.core.annotation.AnnotationAttributes;
import org.springframework.core.env.Environment;
import org.springframework.core.io.ResourceLoader;
import org.springframework.core.type.AnnotationMetadata;

import java.util.Date;
import java.util.Map;
import java.util.Set;

@Lazy(value = true)
@Slf4j
public class TEngineServerImportSelector implements ImportBeanDefinitionRegistrar, ResourceLoaderAware, EnvironmentAware {


    private final static String defaultZkAddress="localhost:2181";
    private Environment env;

    public TEngineServerImportSelector() {
    }

    @Override
    public void setEnvironment(Environment environment) {
        this.env=environment;
    }

    @Override
    public void setResourceLoader(ResourceLoader resourceLoader) {

    }

    @Override
    public void registerBeanDefinitions(AnnotationMetadata annotationMetadata, BeanDefinitionRegistry beanDefinitionRegistry) {
        AnnotationAttributes attributes = AnnotationAttributes.fromMap(annotationMetadata.getAnnotationAttributes(TEngineServer.class.getName(), true));
        if(attributes==null){
            log.info("TEngineServerImportSelector.registerBeanDefinitions 未找到注解");
            return;
        }
        boolean enableDubboAPI = attributes.getBoolean("enableDubboAPI");
        if (enableDubboAPI) {
            //加载dubbo远程下载接口
            log.info("TEngineServerImportSelector.registerBeanDefinitions 开始注册dubbo接口jar");
            //获取全部使用了dubbo service注解的类
            try{
                String applicationClassName = annotationMetadata.getClassName();
                String applicationPackage=applicationClassName.substring(0,applicationClassName.lastIndexOf("."));
                String modelName=applicationPackage.substring(applicationPackage.lastIndexOf(".")+1);
                Set<Map<String, String>> dubboInterfaceSet = new ClassScannerUtils().getDubboInterface(applicationPackage);
                dubboInterfaceSet.forEach(dubboInterfaceMap -> {
                    //获取这些类的接口
                    String className=dubboInterfaceMap.get("className");
                    String version=dubboInterfaceMap.get("version");
                    String serviceName=dubboInterfaceMap.get("serviceName");
                    String serviceRegisterName=String.format("%s_%s_%s",modelName,serviceName,version);
                    //根据接口生成jar文件
                    String jarBase64 = JarFileUtils.getJarBase64(className, modelName, version);
                    //注册到zk
                    String registerPath="/tfengine/dubbo_api/"+serviceRegisterName;
                    String zkAddress=env.getProperty("zookeeper.address");
                    if(StringUtils.isBlank(zkAddress)){
                        zkAddress=defaultZkAddress;
                    }
                    RegistryData registryData=new RegistryData();
                    registryData.setJarData(jarBase64);
                    registryData.setVersion(version);
                    registryData.setInterfaceName(serviceName);
                    registryData.setPackageName(className.substring(0,className.lastIndexOf(".")));
                    registryData.setAddress(env.getProperty("dubbo.registry.address"));
                    registryData.setUsername(env.getProperty("dubbo.registry.username"));
                    registryData.setPassword(env.getProperty("dubbo.registry.password"));
                    String registerDataStr= JSON.toJSONString(registryData);
                    log.info("TEngineServerImportSelector.registerBeanDefinitions 接口注册路径据：{}",registerPath);
                    log.info("TEngineServerImportSelector.registerBeanDefinitions 接口注册数据：{}",registerDataStr);
                    ZookeeperUtils.del(zkAddress,registerPath);
                    ZookeeperUtils.create(zkAddress,registerPath,registerDataStr, CreateMode.PERSISTENT);
                    //获取当前IP，将当前服务注册
                    String ipRegisterPath=registerPath+"/"+ IpUtils.getIp();
                    ZookeeperUtils.create(zkAddress,ipRegisterPath,String.valueOf(new Date().getTime()),CreateMode.EPHEMERAL);
                });
            }catch (Exception e){
                e.printStackTrace();
            }
            log.info("TEngineServerImportSelector.registerBeanDefinitions 注册dubbo接口jar完成");
        } else {
            log.info("TEngineServerImportSelector.registerBeanDefinitions 未开启dubbo远程下载接口");
        }
        boolean enableRestAPI = attributes.getBoolean("enableRestAPI");
        if (enableRestAPI) {
            //加载restAPI远程下载接口
            log.info("TEngineServerImportSelector.registerBeanDefinitions 加载restAPI远程下载接口");
//            List<String> importsList = new ArrayList(Arrays.asList(imports));
//            importsList.add("org.springframework.cloud.client.serviceregistry.AutoServiceRegistrationConfiguration");
//            imports = (String[])importsList.toArray(new String[0]);
        } else {
            log.info("TEngineServerImportSelector.registerBeanDefinitions 未开启restAPI远程下载接口");
        }
    }
}
