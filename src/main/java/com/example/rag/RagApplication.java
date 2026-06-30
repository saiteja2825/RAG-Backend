package com.example.rag;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import java.io.File;
import io.github.cdimascio.dotenv.Dotenv;

@SpringBootApplication
public class RagApplication {

    public static void main(String[] args) {
        String envDir = new File("Backend").exists() ? "Backend" : "../Backend";
        Dotenv dotenv = Dotenv.configure()
                .directory(envDir)
                .ignoreIfMissing()
                .load();
                
        dotenv.entries().forEach(entry -> {
            System.setProperty(entry.getKey(), entry.getValue());
        });
        
        SpringApplication.run(RagApplication.class, args);
    }

}
