package goway.me.tfengine.core.annotation;

import com.alibaba.fastjson.JSON;
import goway.me.tfengine.core.model.NodeData;
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

import java.util.*;

@Lazy(value = true)
@Slf4j
public class TEngineClientImportSelector implements ImportBeanDefinitionRegistrar, ResourceLoaderAware, EnvironmentAware {


    private Environment env;

    public TEngineClientImportSelector() {
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
        AnnotationAttributes attributes = AnnotationAttributes.fromMap(annotationMetadata.getAnnotationAttributes(TEngineClient.class.getName(), true));
        if(attributes==null){
            log.info("TEngineClientImportSelector.registerBeanDefinitions 未找到注解");
            return;
        }
        boolean enable = attributes.getBoolean("enable");
        if (enable) {
            APIEnabled.enabled();

        } else {
            log.info("TEngineClientImportSelector.registerBeanDefinitions 未开启dubbo远程下载接口");
        }
    }
}
