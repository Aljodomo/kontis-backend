package com.aljodomo.kontis;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.scheduling.annotation.EnableScheduling;

import javax.annotation.PostConstruct;
import java.util.TimeZone;

@SpringBootApplication
@EnableScheduling
@EnableConfigurationProperties
public class BerlinKontisBackendApplication {

    public static void main(String[] args) {
        SpringApplication.run(BerlinKontisBackendApplication.class, args);
    }

    @PostConstruct
    public void ini() {
        TimeZone.setDefault(TimeZone.getTimeZone("europe/berlin"));
    }

}
