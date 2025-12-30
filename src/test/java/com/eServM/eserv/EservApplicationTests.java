package com.eServM.eserv;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

@SpringBootTest
@TestPropertySource(properties = {
        "jwt.secret=MDEyMzQ1Njc4OUFCQ0RFRjAxMjM0NTY3ODlBQkNERUY=",
        "jwt.exp.minutes=60",
        "spring.datasource.url=jdbc:sqlite:target/test-app.db"
})
class EservApplicationTests {

	@Test
	void contextLoads() {
	}

}
