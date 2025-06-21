package net.berndreiss.zentodo;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;

import java.util.logging.Logger;

/**
 * TODO DESCRIBE
 */
@SpringBootApplication (scanBasePackages = "net.berndreiss.zentodo")
@EntityScan(basePackages = {"net.berndreiss.zentodo.data"})
public class ZenToDoServerApplication {

	public static final Logger logger = Logger.getLogger(ZenToDoServerApplication.class.getName());
	public static void main(String[] args) {
		SpringApplication.run(ZenToDoServerApplication.class, args);
	}


}
