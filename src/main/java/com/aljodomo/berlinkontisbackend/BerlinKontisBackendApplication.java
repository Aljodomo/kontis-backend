package com.aljodomo.berlinkontisbackend;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class BerlinKontisBackendApplication {

	public static void main(String[] args) {
		SpringApplication.run(BerlinKontisBackendApplication.class, args);
	}

}
