package org.inurl.feign.proxy.service.impl;

import org.inurl.feign.proxy.service.TestService;
import org.springframework.stereotype.Service;

@Service
public class TestServiceImpl implements TestService {

    @Override
    public String sayHello() {
        return "hello";
    }

}
