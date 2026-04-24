package com.riffly;

import com.riffly.config.JwtProperties;
import com.riffly.config.StorageProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@SpringBootApplication
@EnableConfigurationProperties({StorageProperties.class, JwtProperties.class})
public class RifflyApplication {
    public static void main(String[] args) {
        SpringApplication.run(RifflyApplication.class, args);
    }
}
