package com.krishna.Spendwise;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Spendwise application entry point.
 * {@code @EnableScheduling} activates the daily notification jobs in {@link com.krishna.Spendwise.service.NotificationService}.
 */
@EnableScheduling
@SpringBootApplication
public class SpendwiseApplication {

	public static void main(String[] args) {
		SpringApplication.run(SpendwiseApplication.class, args);
	}

}
