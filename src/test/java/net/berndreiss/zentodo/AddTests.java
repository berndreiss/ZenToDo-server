package net.berndreiss.zentodo;

import net.berndreiss.zentodo.data.*;
import net.berndreiss.zentodo.exceptions.DuplicateUserIdException;
import net.berndreiss.zentodo.exceptions.InvalidUserActionException;
import net.berndreiss.zentodo.util.ClientStub;
import net.berndreiss.zentodo.util.PubSubWebSocketHandler;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ConfigurableApplicationContext;

import java.io.IOException;
import java.net.ConnectException;
import java.util.ArrayList;
import java.util.List;

import static net.berndreiss.zentodo.ZenToDoServerApplicationTests.*;

@SpringBootTest
public class AddTests {
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
    void synchronousAdd() throws IOException, InterruptedException, InvalidUserActionException, DuplicateUserIdException {

        for (int n = 0; n < ZenToDoServerApplicationTests.RUNS; n++) {
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
                Thread.sleep(ZenToDoServerApplicationTests.SYNC_DELAY);

                asserTasks(List.of(stub0, stub1, stub2), addedEntries, taskRepository, missingQueueUpdatesRepository);

                stub0.dbHandler.close();
                stub1.dbHandler.close();
                stub2.dbHandler.close();

                cleanUp();
            }
        }
    }

    @Test
    void asynchronousAdd() throws IOException, InterruptedException, InvalidUserActionException, DuplicateUserIdException {

        for (int n = 0; n < ZenToDoServerApplicationTests.RUNS; n++) {
            SpringApplication application = new SpringApplication(ZenToDoServerApplication.class);
            application.setAdditionalProfiles("server.port", "8080");
            try (ConfigurableApplicationContext context = application.run()) {
                cleanSlate(taskRepository, queueRepository, missingQueueUpdatesRepository, userRepository);
                ClientStub stub0 = getStub("device0", mail, "ZenToDoPU");
                ClientStub stub1 = getStub("device1", mail, "ZenToDoPU1");
                ClientStub stub2 = getStub("device2", mail, "ZenToDoPU2");

                List<Task> addedEntries = new ArrayList<>();
                addedEntries.add(stub0.addNewTask("TASK0"));
                context.close();
                addedEntries.add(stub1.addNewTask("TASK1"));
                addedEntries.add(stub2.addNewTask("TASK2"));
                try (ConfigurableApplicationContext context1 = application.run()) {
                    stub0.reinit();
                    stub1.reinit();
                    stub2.reinit();
                    Thread.sleep(SYNC_DELAY);

                    asserTasks(List.of(stub0, stub1, stub2), addedEntries, taskRepository, missingQueueUpdatesRepository);

                    stub0.dbHandler.close();
                    stub1.dbHandler.close();
                    stub2.dbHandler.close();

                    cleanUp();
                }
            }
        }
    }
}
