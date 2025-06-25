package net.berndreiss.zentodo;

import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.Persistence;
import net.berndreiss.zentodo.data.*;
import net.berndreiss.zentodo.exceptions.DuplicateUserIdException;
import net.berndreiss.zentodo.exceptions.InvalidUserActionException;
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
		 user.setPasswordHash(passwordEncoder.encode(password));
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
		ClientStub.SERVER = "localhost:8080/api";
		ClientStub.PROTOCOL = "http";
		ClientStub.WEBSOCKET_PROTOCOL = "ws";
		stub.setMessagePrinter(System.out::println);
		stub.init(email, userName, () -> password);
		List<Task> tasks = stub.loadTasks();

		for (Task t: tasks)
			stub.removeTask(t.getId());
		stub.clearQueue();
		return stub;
	}

	public static void cleanUp(){
		Path dir = Paths.get("");

		try (DirectoryStream<Path> stream = Files.newDirectoryStream(dir, "device*")) {
			for (Path task : stream) {
				System.out.println(task);
				if (Files.isDirectory(task)) {
					try (DirectoryStream<Path> subStream = Files.newDirectoryStream(task)) {
						for (Path e : subStream)
							Files.delete(e);
					}
					Files.delete(task);
					System.out.println("Deleted: " + task);
				}
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public static void asserTasks(List<ClientStub> stubs, List<Task> addedTasks, TaskRepository taskRepository, MissingQueueUpdatesRepository missingQueueUpdatesRepository){
		 List<List<Task>> tasks = new ArrayList<>();
		 for (ClientStub stub: stubs)
			 tasks.add(stub.loadTasks());
		 tasks.add(taskRepository.findAll());
		 Assertions.assertEquals(addedTasks.size(), tasks.getFirst().size(),
				 "First stub does not have right amount of tasks.");
		 for (int i = 0; i < tasks.size()-1; i++){
			 if (i < tasks.size() - 2)
			     Assertions.assertEquals(tasks.get(i).size(), tasks.get(i+1).size(),
						 "Stubs don't have the same amount of tasks.");
			 else
				 Assertions.assertEquals(tasks.get(i).size(), tasks.get(i+1).size(),
						 "Server doesn't have the same amount of tasks as stubs.");
		 }
		 for (int i = 0; i <tasks.getFirst().size(); i++){
			 for (int j = 0; j < tasks.size() - 1; j++) {
				 if (j < tasks.size() - 2) {
					 Assertions.assertEquals(tasks.get(j).get(i).getTask(), tasks.get(j + 1).get(i).getTask(),
							 "Entry " + i  + " does not match all stubs.");
					 Assertions.assertEquals(tasks.get(j).get(i).getId(), tasks.get(j + 1).get(i).getId(),
							 "Entry " + i  + " does not match all stubs.");
				 } else{
					 Assertions.assertEquals(tasks.get(j).get(i).getTask(), tasks.get(j + 1).get(i).getTask(),
							 "Entry " + i  + " does not match on server.");
					 Assertions.assertEquals(tasks.get(j).get(i).getId(), tasks.get(j + 1).get(i).getId(),
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
