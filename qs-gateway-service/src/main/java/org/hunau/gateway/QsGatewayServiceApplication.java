package org.hunau.gateway;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class QsGatewayServiceApplication {

    public static void main(String[] args) {
        //查看是否获取到nacos配置

        SpringApplication.run(QsGatewayServiceApplication.class, args);
    }

}
