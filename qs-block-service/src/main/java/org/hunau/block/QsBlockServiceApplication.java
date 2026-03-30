package org.hunau.block;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

@SpringBootApplication
@EnableDiscoveryClient
public class QsBlockServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(QsBlockServiceApplication.class, args);
    }

}
