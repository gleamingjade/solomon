package com.example.solomon;

import java.util.TimeZone;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class SolomonApplication {

	public static void main(String[] args) {
		TimeZone.setDefault(TimeZone.getTimeZone("UTC"));
		SpringApplication.run(SolomonApplication.class, args);

		// docker compose -f ~/Projects/solomon/docker/docker-compose-local.yml down -v --rmi all
		// docker compose -f ~/Projects/solomon/docker/docker-compose-local.yml up -d
		// ./gradlew bootRun --args='--spring.profiles.active=local'
	}

}
