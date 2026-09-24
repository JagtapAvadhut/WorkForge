package com.avadhoot.workforgeai.mcp;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication(scanBasePackages = "com.avadhoot.workforgeai")
public class WorkforgeMcpServerApplication {

    public static void main(String[] args) {
        SpringApplication.run(WorkforgeMcpServerApplication.class, args);
    }
}
