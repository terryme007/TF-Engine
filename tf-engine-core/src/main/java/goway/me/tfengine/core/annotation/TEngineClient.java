package goway.me.tfengine.core.annotation;

import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Import;
import org.springframework.stereotype.Component;

import java.lang.annotation.*;

@Target({ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Import(TEngineClientImportSelector.class)
public @interface TEngineClient {

    boolean enable() default false;
}
