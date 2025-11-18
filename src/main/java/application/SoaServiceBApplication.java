package application;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication(scanBasePackages = {"application", "directory.adapter.rest", "configuration"})
public class SoaServiceBApplication {
    public static void main(String[] args) {
        SpringApplication.run(SoaServiceBApplication.class, args);
    }
}