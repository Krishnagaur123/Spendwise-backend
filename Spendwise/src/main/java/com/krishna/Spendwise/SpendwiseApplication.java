package com.krishna.Spendwise;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@EnableScheduling
@SpringBootApplication
public class SpendwiseApplication {

	public static void main(String[] args) {
		SpringApplication.run(SpendwiseApplication.class, args);
	}

}
