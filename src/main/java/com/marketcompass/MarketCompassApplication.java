package com.marketcompass;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class MarketCompassApplication {

	public static void main(String[] args) {
		SpringApplication.run(MarketCompassApplication.class, args);
	}
}
