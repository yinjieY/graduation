package org.hunau.scan;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

@SpringBootApplication
@EnableDiscoveryClient
public class QsScanServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(QsScanServiceApplication.class, args);
    }

}
