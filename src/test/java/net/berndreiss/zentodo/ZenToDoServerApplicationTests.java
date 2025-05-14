package net.berndreiss.zentodo;

import net.berndreiss.zentodo.data.*;
import net.berndreiss.zentodo.data.Entry;
import net.berndreiss.zentodo.data.QueueItem;
import net.berndreiss.zentodo.persistence.TestDbHandler;
import net.berndreiss.zentodo.util.PubSubWebSocketHandler;
import net.berndreiss.zentodo.util.WebConfig;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.mockito.BDDMockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.test.context.SpringBootTest;
import net.berndreiss.zentodo.util.ClientStub;
import org.springframework.context.ConfigurableApplicationContext;

import javax.naming.Context;
import java.io.File;
import java.io.IOException;
import java.net.Socket;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Optional;

@SpringBootTest
class ZenToDoServerApplicationTests {

	@Autowired
	private UserRepository userRepository;
	@Autowired
	private EntryRepository entryRepository;
	@Autowired
	private QueueRepository queueRepository;
	@Autowired
	private MissingQueueUpdatesRepository missingQueueUpdatesRepository;
	@Autowired
	private PubSubWebSocketHandler socketHandler;

	 void cleanSlate(){
		List<Entry> entries = entryRepository.findAll();
		for (Entry e: entries)
			entryRepository.delete(e);
		List<QueueItem> queueItems = queueRepository.findAll();
		for (QueueItem q: queueItems)
			queueRepository.delete(q);
		List<MissingQueueUpdate> missingQueueUpdates = missingQueueUpdatesRepository.findAll();
		for (MissingQueueUpdate u: missingQueueUpdates)
			missingQueueUpdatesRepository.delete(u);
	}
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
		Database opHandler = new TestDbHandler(persistenceUnit, userName);
		ClientStub stub = new  ClientStub(email, opHandler);
		stub.setExceptionHandler(e->{
			System.out.println(e.getMessage());
			e.printStackTrace();
		});
		stub.setMessagePrinter(System.out::println);
		stub.init(()->"Test123!?");
		List<Entry> entries = stub.loadEntries();

		for (Entry e: entries)
			stub.removeEntry(e.getId());
		stub.clearQueue();
		stub.id = persistenceUnit;
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
	void synchronousAdd() throws InterruptedException {
		SpringApplication application = new SpringApplication(ZenToDoServerApplication.class);
		application.setAdditionalProfiles("server.port", "8080");
		ConfigurableApplicationContext context = application.run();
		boolean serverRunning = false;

		//while (!serverRunning){
			//try (Socket socket = new Socket("localhost", 8080)) {
				//serverRunning = true;
			//} catch (IOException e) {
				//System.out.println("Server not running.");
			//}
		//}

		cleanSlate();
		ClientStub stub0 = getStub("user0", "bd_reiss@yahoo.de", "ZenToDoPU");
		ClientStub stub1 = getStub("user1", "bd_reiss@yahoo.de", "ZenToDoPU1");
		ClientStub stub2 = getStub("user2", "bd_reiss@yahoo.de", "ZenToDoPU2");

		Thread.sleep(2000);
		System.out.println("OPEN SESSIONS: " + socketHandler.getNumberOfSession());
		System.out.println("SLEEP");
		Entry entry = stub0.addNewEntry("TEST1");

		System.out.println("SLEEP");
		Thread.sleep(2000);
		Optional<Entry> entryReceived1 = stub1.getEntry(entry.getId());
		Optional<Entry> entryReceived2 = stub2.getEntry(entry.getId());

		Assertions.assertTrue(entryReceived1.isPresent());
		Assertions.assertTrue(entryReceived2.isPresent());

		context.close();

		cleanUp();
	}


}
