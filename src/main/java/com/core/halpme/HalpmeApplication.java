package com.core.halpme;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.web.bind.annotation.RestController;

@EnableJpaAuditing
@EnableCaching
@SpringBootApplication
public class HalpmeApplication {

	public static void main(String[] args) {
		SpringApplication.run(HalpmeApplication.class, args);
	}

}