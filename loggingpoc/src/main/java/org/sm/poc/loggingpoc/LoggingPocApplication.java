package org.sm.poc.loggingpoc;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class LoggingPocApplication {

	public static void main(String[] args) {
		SpringApplication.run(LoggingPocApplication.class, args);
	}

}
