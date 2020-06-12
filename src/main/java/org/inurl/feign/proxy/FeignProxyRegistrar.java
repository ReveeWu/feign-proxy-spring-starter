package org.inurl.feign.proxy;

import javassist.ClassPool;
import javassist.CtClass;
import javassist.LoaderClassPath;
import org.springframework.beans.factory.annotation.AnnotatedBeanDefinition;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.BeanDefinitionHolder;
import org.springframework.beans.factory.support.AbstractBeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.support.BeanDefinitionReaderUtils;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.context.EnvironmentAware;
import org.springframework.context.ResourceLoaderAware;
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider;
import org.springframework.context.annotation.ImportBeanDefinitionRegistrar;
import org.springframework.core.env.Environment;
import org.springframework.core.io.ResourceLoader;
import org.springframework.core.type.AnnotationMetadata;
import org.springframework.core.type.filter.AnnotationTypeFilter;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;
import org.springframework.util.StringUtils;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

class FeignProxyRegistrar implements ImportBeanDefinitionRegistrar, ResourceLoaderAware, EnvironmentAware {

    private ResourceLoader resourceLoader;

    private Environment environment;

    private EnableFeignProxy.ImplClassProvider implClassProvider;

    public FeignProxyRegistrar() {
    }

    @Override
    public void setResourceLoader(ResourceLoader resourceLoader) {
        this.resourceLoader = resourceLoader;
    }

    @Override
    public void setEnvironment(Environment environment) {
        this.environment = environment;
    }

    @Override
    public void registerBeanDefinitions(AnnotationMetadata metadata, BeanDefinitionRegistry registry) {
        registerFeignProxyControllers(metadata, registry);
    }

    public void registerFeignProxyControllers(AnnotationMetadata metadata, BeanDefinitionRegistry registry) {
        ClassPathScanningCandidateComponentProvider scanner = getScanner();
        scanner.setResourceLoader(resourceLoader);
        AnnotationTypeFilter annotationTypeFilter = new AnnotationTypeFilter(FeignProxy.class);
        scanner.addIncludeFilter(annotationTypeFilter);
        Set<String> basePackages = getBasePackages(metadata);

        final ClassPool classPool = getClassPool();
        final EnableFeignProxy.ImplClassProvider implClassProvider = getImplClassProvider(metadata);
        for (String basePackage : basePackages) {
            Set<BeanDefinition> candidateComponents = scanner.findCandidateComponents(basePackage);
            for (BeanDefinition candidateComponent : candidateComponents) {
                if (candidateComponent instanceof AnnotatedBeanDefinition) {
                    AnnotatedBeanDefinition beanDefinition = (AnnotatedBeanDefinition) candidateComponent;
                    AnnotationMetadata annotationMetadata = beanDefinition.getMetadata();
                    Assert.isTrue(annotationMetadata.isInterface(),
                            "@FeignProxy can only be specified on an interface");
                    registerFeignProxyController(classPool, implClassProvider, registry, beanDefinition);
                }
            }
        }
    }

    public void registerFeignProxyController(ClassPool classPool,
                                             EnableFeignProxy.ImplClassProvider implClassProvider,
                                             BeanDefinitionRegistry registry,
                                             AnnotatedBeanDefinition beanDefinition) {
        try {
            Class<?> interfaceClass = Class.forName(beanDefinition.getBeanClassName());
            final String interfaceClassName = interfaceClass.getName();
            String implClassName = implClassProvider.get(interfaceClass);
            final String className = interfaceClass + "$FeignProxyController";
            final CtClass implClass = classPool.get(implClassName);
            final CtClass controller = classPool.makeClass(className, implClass);
            controller.addInterface(classPool.get(interfaceClassName));
            controller.addInterface(classPool.get(FeignProxyController.class.getName()));
            final Class<?> clazz = controller.toClass();
            final BeanDefinitionBuilder beanDefinitionBuilder = BeanDefinitionBuilder.genericBeanDefinition(clazz);
            final AbstractBeanDefinition definition = beanDefinitionBuilder.getBeanDefinition();
            definition.setAutowireCandidate(false);
            BeanDefinitionHolder holder = new BeanDefinitionHolder(definition, className);
            BeanDefinitionReaderUtils.registerBeanDefinition(holder, registry);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private ClassPathScanningCandidateComponentProvider getScanner() {
        return new ClassPathScanningCandidateComponentProvider(false, environment) {
            @Override
            protected boolean isCandidateComponent(
                    AnnotatedBeanDefinition beanDefinition) {
                boolean isCandidate = false;
                if (beanDefinition.getMetadata().isIndependent()) {
                    if (!beanDefinition.getMetadata().isAnnotation()) {
                        isCandidate = true;
                    }
                }
                return isCandidate;
            }
        };
    }

    private Set<String> getBasePackages(AnnotationMetadata importingClassMetadata) {
        Map<String, Object> attributes = importingClassMetadata
                .getAnnotationAttributes(EnableFeignProxy.class.getCanonicalName());
        Set<String> basePackages = new HashSet<>();
        for (String pkg : (String[]) attributes.get("value")) {
            if (StringUtils.hasText(pkg)) {
                basePackages.add(pkg);
            }
        }
        for (String pkg : (String[]) attributes.get("basePackages")) {
            if (StringUtils.hasText(pkg)) {
                basePackages.add(pkg);
            }
        }
        for (Class<?> clazz : (Class<?>[]) attributes.get("basePackageClasses")) {
            basePackages.add(ClassUtils.getPackageName(clazz));
        }
        if (basePackages.isEmpty()) {
            basePackages.add(
                    ClassUtils.getPackageName(importingClassMetadata.getClassName()));
        }
        return basePackages;
    }

    private ClassPool getClassPool() {
        ClassPool classPool = ClassPool.getDefault();
        classPool.appendClassPath(new LoaderClassPath(Thread.currentThread().getContextClassLoader()));
        return classPool;
    }

    private EnableFeignProxy.ImplClassProvider getImplClassProvider(AnnotationMetadata importingClassMetadata) {
        Map<String, Object> attributes = importingClassMetadata
                .getAnnotationAttributes(EnableFeignProxy.class.getCanonicalName());
        @SuppressWarnings("unchecked")
        final Class<EnableFeignProxy.ImplClassProvider> implClass =
                (Class<EnableFeignProxy.ImplClassProvider>) attributes.get("impl");
        try {
            return implClass.newInstance();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

}
