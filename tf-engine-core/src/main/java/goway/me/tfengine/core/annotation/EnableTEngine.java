package goway.me.tfengine.core.annotation;

import org.springframework.context.annotation.Import;

import java.lang.annotation.*;

@Target({ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Inherited
@Import(EnableTEngineDubboImportSelector.class)
public @interface EnableTEngine {

    boolean enableDubboAPI() default false;
    boolean enableRestAPI() default false;
    Class<?>[] excludeAPIServer() default {};
}
