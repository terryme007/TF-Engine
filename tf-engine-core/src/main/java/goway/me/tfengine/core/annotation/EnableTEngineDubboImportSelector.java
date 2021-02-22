package goway.me.tfengine.core.annotation;

import goway.me.tfengine.core.utils.JarFileUtils;
import goway.me.tfengine.core.utils.ScannerUtils;
import goway.me.tfengine.core.utils.ZipUtils;
import goway.me.tfengine.core.utils.ZookeeperUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.context.ApplicationContext;
import org.springframework.context.EnvironmentAware;
import org.springframework.context.ResourceLoaderAware;
import org.springframework.context.annotation.ImportBeanDefinitionRegistrar;
import org.springframework.context.annotation.Lazy;
import org.springframework.core.annotation.AnnotationAttributes;
import org.springframework.core.env.Environment;
import org.springframework.core.io.ResourceLoader;
import org.springframework.core.type.AnnotationMetadata;

import java.util.Map;
import java.util.Set;

@Lazy()
public class EnableTEngineDubboImportSelector implements ImportBeanDefinitionRegistrar, ResourceLoaderAware, EnvironmentAware {

    private ApplicationContext applicationContext;

    @Value("#{ @environment['zookeeper.address']?:'localhost:2181'}")
    private String zkAddress;

    public EnableTEngineDubboImportSelector() {
    }

    @Override
    public void setEnvironment(Environment environment) {

    }

    @Override
    public void setResourceLoader(ResourceLoader resourceLoader) {

    }

    @Override
    public void registerBeanDefinitions(AnnotationMetadata annotationMetadata, BeanDefinitionRegistry beanDefinitionRegistry) {
        AnnotationAttributes attributes = AnnotationAttributes.fromMap(annotationMetadata.getAnnotationAttributes(EnableTEngine.class.getName(), true));
        if(attributes==null){
            System.out.println("未找到注解");
            return;
        }
        boolean enableDubboAPI = attributes.getBoolean("enableDubboAPI");
        if (enableDubboAPI) {
            //加载dubbo远程下载接口
            System.out.println("开始注册dubbo接口jar");
            //获取全部使用了dubbo service注解的类
            try{
                String applicationClassName = annotationMetadata.getClassName();
                String applicationPackage=applicationClassName.substring(0,applicationClassName.lastIndexOf("."));
                String modelName=applicationPackage.substring(applicationPackage.lastIndexOf(".")+1);
                Set<Map<String, String>> dubboInterfaceSet = new ScannerUtils().getDubboInterface(applicationPackage);
                dubboInterfaceSet.forEach(dubboInterfaceMap -> {
                    //获取这些类的接口
                    String className=dubboInterfaceMap.get("className");
                    String version=dubboInterfaceMap.get("version");
                    String serviceName=dubboInterfaceMap.get("serviceName");
                    String serviceRegisterName=String.format("%s_%s_%s",modelName,serviceName,version);
                    System.out.println(className);
                    System.out.println(version);
                    System.out.println(serviceRegisterName);
                    //根据接口生成jar文件
                    String jarBase64 = JarFileUtils.getJarBase64(className, modelName, version);
                    System.out.println(jarBase64);
                    //注册到zk
                    String registerPath="/tfengine/dubbo_api/"+serviceRegisterName;
                    ZookeeperUtils.create(zkAddress,registerPath,jarBase64);
                });
            }catch (Exception e){
                e.printStackTrace();
            }
            System.out.println("注册dubbo接口jar完成");
        } else {
            System.out.println("未开启dubbo远程下载接口");
        }
        boolean enableRestAPI = attributes.getBoolean("enableRestAPI");
        if (enableRestAPI) {
            //加载restAPI远程下载接口
            System.out.println("加载restAPI远程下载接口");
//            List<String> importsList = new ArrayList(Arrays.asList(imports));
//            importsList.add("org.springframework.cloud.client.serviceregistry.AutoServiceRegistrationConfiguration");
//            imports = (String[])importsList.toArray(new String[0]);
        } else {
            System.out.println("未开启restAPI远程下载接口");
        }
    }
}
