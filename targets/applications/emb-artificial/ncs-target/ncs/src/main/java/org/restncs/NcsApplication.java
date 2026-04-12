package org.restncs;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

@SpringBootApplication
public class NcsApplication {

    // http://localhost:8080/v3/api-docs

    public static void main(String[] args) {
        SpringApplication.run(NcsApplication.class, args);
    }
}
