package goway.me.tfengine.core.annotation;

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

@Lazy()
public class EnableTEngineDubboImportSelector implements ImportBeanDefinitionRegistrar, ResourceLoaderAware, EnvironmentAware {

    private ApplicationContext applicationContext;

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
            System.out.println("加载dubbo远程下载接口");
            //获取全部使用了dubbo service注解的类
            //获取这些类的接口
            //根据接口生成jar文件
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
