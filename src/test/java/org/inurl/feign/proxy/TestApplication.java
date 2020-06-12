package org.inurl.feign.proxy;

import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@EnableFeignProxy("org.inurl.feign.proxy.service")
public class TestApplication {

}
