package org.hunau.alert;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

@SpringBootApplication
@EnableDiscoveryClient
public class QsAlertServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(QsAlertServiceApplication.class, args);
    }

}
