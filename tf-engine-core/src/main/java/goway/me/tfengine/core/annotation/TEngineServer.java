package goway.me.tfengine.core.annotation;

import org.springframework.context.annotation.Import;
import org.springframework.stereotype.Component;

import java.lang.annotation.*;

@Target({ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Import(TEngineServerImportSelector.class)
@Component
public @interface TEngineServer {

    boolean enableDubboAPI() default false;
    boolean enableRestAPI() default false;
    Class<?>[] excludeAPIServer() default {};
}
