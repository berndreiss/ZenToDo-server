package net.berndreiss.zentodo;

import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.Persistence;
import net.berndreiss.zentodo.data.*;
import net.berndreiss.zentodo.data.Entry;
import net.berndreiss.zentodo.persistence.DbHandler;
import net.berndreiss.zentodo.util.VectorClock;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import net.berndreiss.zentodo.util.ClientStub;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Random;

@SpringBootTest
class ZenToDoServerApplicationTests {

	public static final String mail = "test@test.net";
	public static final String password = "Test1234!?";
	public static final int SYNC_DELAY = 3000;
	public static final int RUNS = 1;


	public static void cleanSlate(TaskRepository taskRepository, QueueRepository queueRepository, MissingQueueUpdatesRepository missingQueueUpdatesRepository, UserRepository userRepository) {
		 taskRepository.deleteById(0L);
		 queueRepository.deleteAll();
		 missingQueueUpdatesRepository.deleteAll();
		 taskRepository.deleteAll();
		 userRepository.deleteAll();

		 Random random = new Random();
		 long userId = random.nextLong();
		 while(userRepository.findById(userId).isPresent())
		     userId++;
		 User user = new User();
		 user.setEmail(mail);
		 user.setId(userId);
		 user.setClock((new VectorClock().jsonify()));
		 PasswordEncoder passwordEncoder = new BCryptPasswordEncoder();
		 user.setPassword(passwordEncoder.encode(password));
		 user.setEnabled(true);
		 userRepository.save(user);
		Optional<User> userCreated = userRepository.findByEmail(mail);
		if (userCreated.isEmpty())
			throw new RuntimeException("User was not registered.");
	}
	@Test
	void contextLoads() {
	}

	public static ClientStub getStub(String userName, String email, String persistenceUnit) throws InvalidUserActionException, IOException, DuplicateUserIdException {

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
		ClientStub.SERVER = "localhost:8080/api/";
		ClientStub.PROTOCOL = "http://";
		ClientStub.WEBSOCKET_PROTOCOL = "ws://";
		stub.init(email, userName, () -> password);
		stub.setMessagePrinter(System.out::println);
		List<Entry> entries = stub.loadEntries();

		for (Entry e: entries)
			stub.removeEntry(e.getId());
		stub.clearQueue();
		return stub;
	}

	public static void cleanUp(){
		Path dir = Paths.get("");

		try (DirectoryStream<Path> stream = Files.newDirectoryStream(dir, "device*")) {
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

	public static void assertEntries(List<ClientStub> stubs, List<Entry> addedEntries, TaskRepository taskRepository, MissingQueueUpdatesRepository missingQueueUpdatesRepository){
		 List<List<Entry>> entries = new ArrayList<>();
		 for (ClientStub stub: stubs)
			 entries.add(stub.loadEntries());
		 entries.add(taskRepository.findAll());
		 Assertions.assertEquals(addedEntries.size(), entries.getFirst().size(),
				 "First stub does not have right amount of entries.");
		 for (int i = 0; i < entries.size()-1; i++){
			 if (i < entries.size() - 2)
			     Assertions.assertEquals(entries.get(i).size(), entries.get(i+1).size(),
						 "Stubs don't have the same amount of entries.");
			 else
				 Assertions.assertEquals(entries.get(i).size(), entries.get(i+1).size(),
						 "Server doesn't have the same amount of entries as stubs.");
		 }
		 for (int i = 0; i <entries.getFirst().size(); i++){
			 for (int j = 0; j < entries.size() - 1; j++) {
				 if (j < entries.size() - 2) {
					 Assertions.assertEquals(entries.get(j).get(i).getTask(), entries.get(j + 1).get(i).getTask(),
							 "Entry " + i  + " does not match all stubs.");
					 Assertions.assertEquals(entries.get(j).get(i).getId(), entries.get(j + 1).get(i).getId(),
							 "Entry " + i  + " does not match all stubs.");
				 } else{
					 Assertions.assertEquals(entries.get(j).get(i).getTask(), entries.get(j + 1).get(i).getTask(),
							 "Entry " + i  + " does not match on server.");
					 Assertions.assertEquals(entries.get(j).get(i).getId(), entries.get(j + 1).get(i).getId(),
							 "Entry " + i  + " does not match on server.");

				 }
			 }
		 }
		 for (int i = 0; i < stubs.size(); i++)
			 Assertions.assertTrue(stubs.get(i).getUser().getQueueItems().isEmpty(),
					 "There are still queue items for stub" + i);
		List<MissingQueueUpdate> updates = missingQueueUpdatesRepository.findAll();
		//Assertions.assertTrue(updates.isEmpty(), "There are still missing updates.");
	}
}
