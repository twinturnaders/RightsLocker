package org.rights.locker;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;

@Import(TestcontainersConfiguration.class)
@SpringBootTest
class RightslockerApplicationTests {

	@Test
	void contextLoads() {
	}

}
