package net.berndreiss.zentodo;

import net.berndreiss.zentodo.data.ClientOperationHandler;
import net.berndreiss.zentodo.util.TestDbHandler;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import net.berndreiss.zentodo.util.ClientStub;
@SpringBootTest
class ZenToDoServerApplicationTests {

	@Test
	void contextLoads() {
		ZenToDoServerApplication.main(null);
		System.out.println("HERE");
		ClientOperationHandler opHandler = new TestDbHandler("ZenToDoPU");
		ClientStub stub = new ClientStub("bd_reiss@yahoo.de", opHandler);
		stub.init(()->"Test123!?");

	}



}
