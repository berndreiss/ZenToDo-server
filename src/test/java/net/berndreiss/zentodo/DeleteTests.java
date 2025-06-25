package net.berndreiss.zentodo;

import net.berndreiss.zentodo.data.*;
import net.berndreiss.zentodo.exceptions.DuplicateUserIdException;
import net.berndreiss.zentodo.exceptions.InvalidUserActionException;
import net.berndreiss.zentodo.util.ClientStub;
import net.berndreiss.zentodo.util.PubSubWebSocketHandler;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ConfigurableApplicationContext;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import static net.berndreiss.zentodo.ZenToDoServerApplicationTests.*;

@SpringBootTest
public class DeleteTests {
    @Autowired
    private UserService userService;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private TaskRepository taskRepository;
    @Autowired
    private QueueRepository queueRepository;
    @Autowired
    private MissingQueueUpdatesRepository missingQueueUpdatesRepository;
    @Autowired
    private PubSubWebSocketHandler socketHandler;

    @Test
    public void synchronousDelete() throws InterruptedException, InvalidUserActionException, DuplicateUserIdException {
        //TODO check whether tasks are removed from queue too
        //TODO check whether positions are resolved correctly (normal and list)
        for (int i = 0; i < RUNS; i++){

            SpringApplication application = new SpringApplication(ZenToDoServerApplication.class);
            application.setAdditionalProfiles("server.port", "8080");
            try (ConfigurableApplicationContext context = application.run()) {
                cleanSlate(taskRepository, queueRepository, missingQueueUpdatesRepository, userRepository);
                ClientStub stub0 = getStub("device0", mail, "ZenToDoPU");
                ClientStub stub1 = getStub("device1", mail, "ZenToDoPU1");
                ClientStub stub2 = getStub("device2", mail, "ZenToDoPU2");

                List<Task> addedTasks = new ArrayList<>();
                addedTasks.add(stub0.addNewTask("TASK0"));
                addedTasks.add(stub1.addNewTask("TASK1"));
                addedTasks.add(stub2.addNewTask("TASK2"));
                Thread.sleep(SYNC_DELAY);
                Assertions.assertEquals(3, addedTasks.size());
                stub2.removeTask(addedTasks.getFirst().getId());
                stub1.removeTask(addedTasks.get(1).getId());
                stub0.removeTask(addedTasks.get(2).getId());

                addedTasks.clear();
                Thread.sleep(SYNC_DELAY);

                asserTasks(List.of(stub0, stub1, stub2), addedTasks, taskRepository, missingQueueUpdatesRepository);

                stub0.dbHandler.close();
                stub1.dbHandler.close();
                stub2.dbHandler.close();
                cleanUp();
            } catch (IOException | InvalidUserActionException | DuplicateUserIdException e) {
                throw new RuntimeException(e);
            }
        }
    }

    @Test
    public void asynchronousDelete() throws InterruptedException, InvalidUserActionException, DuplicateUserIdException {

        //TODO check whether tasks are removed from queue too
        //TODO check whether positions are resolved correctly (normal and list)
        for (int i = 0; i < RUNS; i++){

            SpringApplication application = new SpringApplication(ZenToDoServerApplication.class);
            application.setAdditionalProfiles("server.port", "8080");
            try (ConfigurableApplicationContext context = application.run()) {
                cleanSlate(taskRepository, queueRepository, missingQueueUpdatesRepository, userRepository);
                ClientStub stub0 = getStub("device0", mail, "ZenToDoPU");
                ClientStub stub1 = getStub("device1", mail, "ZenToDoPU1");
                ClientStub stub2 = getStub("device2", mail, "ZenToDoPU2");

                List<Task> addedEntries = new ArrayList<>();
                addedEntries.add(stub0.addNewTask("TASK0"));
                addedEntries.add(stub1.addNewTask("TASK1"));
                addedEntries.add(stub2.addNewTask("TASK2"));
                context.close();
                Thread.sleep(SYNC_DELAY);
                Assertions.assertEquals(3, addedEntries.size());
                stub2.removeTask(addedEntries.getFirst().getId());
                stub1.removeTask(addedEntries.get(1).getId());
                stub0.removeTask(addedEntries.get(2).getId());

                try (ConfigurableApplicationContext context1 = application.run()) {
                    stub0.reinit();
                    stub1.reinit();
                    stub2.reinit();
                    addedEntries.clear();
                    Thread.sleep(SYNC_DELAY);

                    asserTasks(List.of(stub0, stub1, stub2), addedEntries, taskRepository, missingQueueUpdatesRepository);

                    stub0.dbHandler.close();
                    stub1.dbHandler.close();
                    stub2.dbHandler.close();
                    cleanUp();
                }
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }
}
