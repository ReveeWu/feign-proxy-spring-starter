# Feign Controller 自动生成Starter

> 使用SpringCloud项目需要写Service对应Controller来对外部提供服务，该项目用来简化此步骤

## 使用步骤

1. 启用代理功能

```diff
+ @EnableFeignProxy
public class TestApplication {

}
```

2. 在需要的代理的Service上添加`@FeignProxy`注解

```diff
+ @FeignProxy
public interface TestService {
    @GetMapping("/sayHello")
    String sayHello();
}
```

3. 会自动生成`/sayHello`接口

```shell
curl /sayHello
# return hello
```

## 其他

1. 默认Service必须和ServiceImpl成对应关系，例如Service为`Service`则ServiceImpl必须为`impl.ServiceImpl`，可以通过实现`EnableFeignProxy.ImplClassProvider`自定义查找逻辑

2. 版本号对应SpringBoot版本号
