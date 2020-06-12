package org.inurl.feign.proxy.service;

import org.inurl.feign.proxy.FeignProxy;
import org.springframework.web.bind.annotation.GetMapping;

@FeignProxy
public interface TestService {

    @GetMapping("/sayHello")
    String sayHello();

}
