package com.example.los;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;

@SpringBootApplication
@EnableAsync
public class LosApplication {

	public static void main(String[] args) {
		SpringApplication.run(LosApplication.class, args);
	}

}
