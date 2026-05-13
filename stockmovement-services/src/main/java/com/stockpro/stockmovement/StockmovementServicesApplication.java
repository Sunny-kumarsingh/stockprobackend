package com.stockpro.stockmovement;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.info.Info;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

@SpringBootApplication
@EnableDiscoveryClient
@OpenAPIDefinition(info = @Info(title = "Stock Movement Service API", version = "1.0", description = "Documentation for Stock Movement Tracking"))
public class StockmovementServicesApplication {

	public static void main(String[] args) {
		SpringApplication.run(StockmovementServicesApplication.class, args);
	}
}
