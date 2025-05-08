package net.berndreiss.zentodo;

import net.berndreiss.zentodo.data.ClientOperationHandler;
import net.berndreiss.zentodo.data.UserRepository;
import net.berndreiss.zentodo.util.TestDbHandler;
import net.berndreiss.zentodo.data.User;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import net.berndreiss.zentodo.util.ClientStub;

import java.io.File;
import java.io.IOException;
import java.net.Socket;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Optional;

@SpringBootTest
class ZenToDoServerApplicationTests {

	@Autowired
	private UserRepository userRepository;
	@Test
	void contextLoads() {
		//Optional<User> user = userRepository.findByEmail("bd_reiss@yahoo.de");
        //user.ifPresent(value -> userRepository.deleteById(value.getId()));
	}

	ClientStub getStub(String userName, String email, String persistenceUnit){

		Path path = Paths.get(userName);

		try {
			Files.createDirectory(path);
			System.out.println("Directory created: " + path.toAbsolutePath());
		} catch (Exception e) {
			System.err.println("Failed to create directory: " + e.getMessage());
		}
		TestDbHandler opHandler = new TestDbHandler(persistenceUnit);
		opHandler.tokenPath = userName;
		ClientStub stub = new  ClientStub(email, opHandler);
		stub.setExceptionHandler(e->System.out.println(e.getMessage()));
		stub.setMessagePrinter(System.out::println);
		stub.init(()->"Test123!?");
		return stub;
	}

	void cleanUp(){
		Path dir = Paths.get("");

		try (DirectoryStream<Path> stream = Files.newDirectoryStream(dir, "user*")) {
			for (Path entry : stream) {
				System.out.println(entry);
				if (Files.isDirectory(entry)) {
					try (DirectoryStream<Path> subStream = Files.newDirectoryStream(entry)) {
						for (Path e : subStream)
							Files.delete(e);
					}
					Files.delete(entry);
					System.out.println("Deleted: " + entry);
				}
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	@Test
	void basics(){
		ZenToDoServerApplication.main(new String[]{});

		boolean serverRunning = false;

		//while (!serverRunning){
			//try (Socket socket = new Socket("localhost", 8080)) {
				//serverRunning = true;
			//} catch (IOException e) {
				//System.out.println("Server not running.");
			//}
		//}

		ClientStub stub0 = getStub("user0", "bd_reiss@yahoo.de", "ZenToDoPU");
		ClientStub stub1 = getStub("user1", "bd_reiss@yahoo.de", "ZenToDoPU1");

		stub0.addNewEntry(3, "TEST1", 1);

		cleanUp();
	}


}
