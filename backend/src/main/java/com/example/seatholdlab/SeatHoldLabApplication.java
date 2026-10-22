package com.example.seatholdlab;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class SeatHoldLabApplication {

	public static void main(String[] args) {
		SpringApplication.run(SeatHoldLabApplication.class, args);
	}

}
