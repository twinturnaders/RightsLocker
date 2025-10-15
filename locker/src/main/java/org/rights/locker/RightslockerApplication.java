package org.rights.locker;

import org.rights.locker.Security.JwtProps;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@SpringBootApplication
@EnableConfigurationProperties(JwtProps.class)
public class RightslockerApplication {

	public static void main(String[] args) {
		SpringApplication.run(RightslockerApplication.class, args);
	}

}
