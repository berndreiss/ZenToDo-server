package net.berndreiss.zentodo;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import lombok.RequiredArgsConstructor;
import net.berndreiss.zentodo.auth.JwtRequestModel;
import net.berndreiss.zentodo.auth.TokenManager;
import net.berndreiss.zentodo.data.*;
import net.berndreiss.zentodo.util.*;
import org.springframework.http.ResponseEntity;
import org.springframework.security.task.DelegatingSecurityContextAsyncTaskExecutor;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * TODO DESCRIBE
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/")
public class Controller {

    private final UserRepository userRepository;
    private final MissingQueueUpdatesRepository missingQueueUpdatesRepository;
    private final EntryRepository entryRepository;
    private final DeviceRepository deviceRepository;
    private final QueueRepository queueRepository;
    private final EventPublisherController eventPublisherController;
    private final TokenManager tokenManager;

    List<String> users = new ArrayList<>();

    @GetMapping ("test")
    public ResponseEntity<String> test(){
        return ResponseEntity.ok("okay");
    }
    /**
     * TODO DESCRIBE
     * @param message
     * @return
     */
    @PostMapping("addUser")
    public int addUser(@RequestBody AddUserMessage message) {
        users.add(message.getMail());
        return users.size();
    }

    /**
     * TODO DESCRIBE
     * @param messageList
     * @return
     */
    @PostMapping("process")
    public ResponseEntity<String> process(@RequestBody List<ZenServerMessage> messageList, @RequestHeader("Authorization") String auth, @RequestHeader("device") Long device){

        System.out.println("PROCESSING");
        System.out.println(auth);
        Claims claims = Jwts.parserBuilder()
                .setSigningKey(tokenManager.getKey())
                .build()
                .parseClaimsJws(auth.substring(7))
                .getBody();

        ServerUser user = userRepository.findByEmail(claims.getSubject()).orElse(null);

        if (user == null)
            return ResponseEntity.status(401).build();

        List<QueueItem> queue = queueRepository.findByUserId(user.getId());


            for (ZenServerMessage message : messageList) {
                switch (message.getType()) {
                    case ADD_NEW_ENTRY -> {

                        System.out.println("ADDING NEW ENTRY");
                        for (ZenServerMessage zm: messageList) {
                            queue.stream()
                                    .sorted(Comparator.comparing(QueueItem::getTimeStamp))
                                    .forEach(qi -> {
                                        if (qi.getType().equals(OperationType.ADD_NEW_ENTRY) &&
                                                missingQueueUpdatesRepository.findById(qi.getId())
                                                        .stream()
                                                        .map(MissingQueueUpdate::getId)
                                                        .toList()
                                                        .contains(device)) {
                                            if (qi.getTimeStamp().isAfter(zm.getTimeStamp()))
                                                qi.getArguments().set(3, String.valueOf(Integer.parseInt(qi.getArguments().get(3) + 1)));
                                            else
                                                zm.getArguments().set(3, Integer.parseInt(zm.getArguments().get(3).toString()) + 1);
                                        }
                                    });
                        }
                    }
                    case DELETE -> {
                    }
                    case SWAP -> {
                    }
                    case SWAP_LIST -> {
                    }
                    case UPDATE_TASK -> {
                    }
                    case UPDATE_FOCUS -> {
                    }
                    case UPDATE_DROPPED -> {
                    }
                    case UPDATE_RECURRENCE -> {
                    }
                    case UPDATE_REMINDER_DATE -> {
                    }
                    case UPDATE_LIST -> {
                    }
                    case UPDATE_LIST_COLOR -> {
                    }
                    case UPDATE_MAIL -> {
                    }
                    case UPDATE_USER_NAME -> {
                    }
                    default -> {
                        return ResponseEntity.badRequest().build();
                    }
                }
                List<Long> devices = new ArrayList<>();

                devices = deviceRepository.findAll().stream()
                        .filter(d -> d.getUser().getId()==user.getId() && d.getId() != device)
                        .map(Device::getId)
                        .toList();



                List<Long> notSent = eventPublisherController.publish(ClientStub.jsonifyServerList(messageList), user.getEmail(), devices);
                if (!notSent.isEmpty()) {
                    QueueItem queueItem = new QueueItem();
                    queueItem.setType(message.getType());
                    queueItem.setArguments(message.getArguments());
                    queueItem.setUserId(user.getId());
                    queueItem.setTimeStamp(message.getTimeStamp());
                    queueRepository.save(queueItem);

                    MissingQueueUpdate missingQueueUpdate = new MissingQueueUpdate();
                    missingQueueUpdate.setId(queueItem.getId());
                    missingQueueUpdate.setDevices(notSent);
                    missingQueueUpdatesRepository.save(missingQueueUpdate);
                }
                System.out.println("MSSING DEVICES:");
                notSent.forEach(System.out::println);
            }

        return ResponseEntity.ok("");
    }
    /**
     * TODO DESCRIBE
     * @param list
     * @return
     */
    @PostMapping("add")
    public ResponseEntity<String> add(@RequestBody List<ZenServerMessage> list, @RequestHeader("device") String device, @RequestHeader("Authorization") String auth){

        Claims claims = Jwts.parserBuilder()
                .setSigningKey(tokenManager.getKey())
                .build()
                .parseClaimsJws(auth)
                .getBody();

        String email = claims.getSubject();

        for (ZenServerMessage message : list) {
            List<Object> arguments = message.getArguments();
            Entry entry = new Entry(Long.parseLong((String) arguments.get(0)), Integer.parseInt((String) arguments.get(1)), (String) arguments.get(2), Long.parseLong((String) arguments.get(3)));

            //entryRepository.save(entry);
            for (Object s : message.getArguments())
                System.out.println(s);
            System.out.println(message.getTimeStamp());
        }


        return ResponseEntity.ok("");
    }


}
