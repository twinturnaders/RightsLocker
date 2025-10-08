package org.rights.locker;

import org.springframework.boot.SpringApplication;

public class TestRightslockerApplication {

	public static void main(String[] args) {
		SpringApplication.from(RightslockerApplication::main).with(TestcontainersConfiguration.class).run(args);
	}

}
