package org.inurl.feign.proxy;

import org.springframework.context.annotation.Import;
import org.springframework.core.annotation.Order;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.function.Function;

/**
 * @author raylax
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
@Documented
@Import({FeignProxyRegistrar.class})
@Order
public @interface EnableFeignProxy {

    String[] value() default {};

    String[] basePackages() default {};

    Class<?>[] basePackageClasses() default {};

    Class<?> impl() default DefaultImplClassProvider.class;

    @FunctionalInterface
    interface ImplClassProvider {

        String get(Class<?> interfaceClass);

    }

    class DefaultImplClassProvider implements ImplClassProvider {

        @Override
        public String get(Class<?> interfaceClass) {
            final String interfaceClassName = interfaceClass.getName();
            return interfaceClassName.substring(0, interfaceClassName.lastIndexOf("."))
                    + ".impl." + interfaceClass.getSimpleName() + "Impl";
        }

    }

}