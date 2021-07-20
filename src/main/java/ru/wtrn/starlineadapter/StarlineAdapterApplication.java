package ru.wtrn.starlineadapter;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
public class StarlineAdapterApplication {

	public static void main(String[] args) {
		SpringApplication.run(StarlineAdapterApplication.class, args);
	}

}
