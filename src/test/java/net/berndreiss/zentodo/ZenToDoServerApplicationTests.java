package net.berndreiss.zentodo;

import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.Persistence;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import net.berndreiss.zentodo.data.*;
import net.berndreiss.zentodo.data.Entry;
import net.berndreiss.zentodo.data.QueueItem;
import net.berndreiss.zentodo.persistence.DbHandler;
import net.berndreiss.zentodo.util.PubSubWebSocketHandler;
import net.berndreiss.zentodo.util.VectorClock;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.test.context.SpringBootTest;
import net.berndreiss.zentodo.util.ClientStub;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Optional;
import java.util.Random;

@SpringBootTest
class ZenToDoServerApplicationTests {

	private static final String mail = "test@test.net";
	private static final String password = "Test1234!?";
	private static User user;

	@Autowired
	private UserService userService;
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
	private final PasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

	 void cleanSlate() throws Exception {
		 entryRepository.deleteById(0L);
		 queueRepository.deleteAll();
		 missingQueueUpdatesRepository.deleteAll();
		 entryRepository.deleteAll();
		 userRepository.deleteAll();

		 Random random = new Random();
		 long userId = random.nextLong();
		 while(userRepository.findById(userId).isPresent())
		     userId++;
		 User user = new User();
		 user.setEmail(mail);
		 user.setId(userId);
		 user.setClock((new VectorClock().jsonify()));
		 user.setPassword(passwordEncoder.encode("Test1234!?"));
		 user.setEnabled(true);
		 userRepository.save(user);
		Optional<User> userCreated = userRepository.findByEmail(mail);
		if (userCreated.isEmpty())
			throw new RuntimeException("User was not registered.");
		ZenToDoServerApplicationTests.user = userCreated.get();
	}
	@Test
	void contextLoads() {
	}

	ClientStub getStub(String userName, String email, String persistenceUnit){

		Path path = Paths.get(userName);

		try {
			Files.createDirectory(path);
			System.out.println("Directory created: " + path.toAbsolutePath());
		} catch (Exception e) {
			System.err.println("Failed to create directory: " + e.getMessage());
		}
		EntityManagerFactory emf = Persistence.createEntityManagerFactory(persistenceUnit);
		Database opHandler = new DbHandler(emf, userName);
		ClientStub stub = new  ClientStub(opHandler);
		stub.setExceptionHandler(e->{
			System.out.println(e.getMessage());
			e.printStackTrace();
		});
		stub.init(email, userName, () -> password);
		stub.setMessagePrinter(System.out::println);
		List<Entry> entries = stub.loadEntries();

		for (Entry e: entries)
			stub.removeEntry(e.getId());
		stub.clearQueue();
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
	void synchronousAdd() throws Exception {
		SpringApplication application = new SpringApplication(ZenToDoServerApplication.class);
		application.setAdditionalProfiles("server.port", "8080");
		ConfigurableApplicationContext context = application.run();

		cleanSlate();
		ClientStub stub0 = getStub("user0", mail, "ZenToDoPU");
		ClientStub stub1 = getStub("user1", mail, "ZenToDoPU1");
		ClientStub stub2 = getStub("user2", mail, "ZenToDoPU2");

		Thread.sleep(2000);
		Entry entry = stub0.addNewEntry("TEST1");

		Thread.sleep(2000);
		Optional<Entry> entryReceived1 = stub1.getEntry(entry.getId());
		Optional<Entry> entryReceived2 = stub2.getEntry(entry.getId());

		Assertions.assertTrue(entryReceived1.isPresent());
		Assertions.assertTrue(entryReceived2.isPresent());

		context.close();

		stub0.dbHandler.close();
		stub1.dbHandler.close();
		stub2.dbHandler.close();
		cleanUp();
	}


}
