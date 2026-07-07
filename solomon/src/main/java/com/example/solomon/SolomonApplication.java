package com.example.solomon;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class SolomonApplication {

	public static void main(String[] args) {
		SpringApplication.run(SolomonApplication.class, args);

		// docker compose -f /workspaces/codespaces-blank/docker/docker-compose-local.yml down -v --rmi all
		// docker compose -f /workspaces/codespaces-blank/docker/docker-compose-local.yml up -d 
		// ./gradlew bootRun --args='--spring.profiles.active=local'
		
	}

}
