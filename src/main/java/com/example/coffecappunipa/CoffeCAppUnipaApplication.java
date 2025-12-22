package com.example.coffecappunipa;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.web.servlet.ServletComponentScan;

@SpringBootApplication
@ServletComponentScan(basePackages = "com.example.coffecappunipa")
public class CoffeCAppUnipaApplication {

    public static void main(String[] args) {
        SpringApplication.run(CoffeCAppUnipaApplication.class, args);
    }
}