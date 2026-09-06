package com.example.los;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
@Disabled("Context loads test disabled in unit test suite without live MySQL database")
class LosApplicationTests {

	@Test
	void contextLoads() {
	}

}
