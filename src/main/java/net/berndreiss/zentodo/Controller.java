package net.berndreiss.zentodo;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import jakarta.persistence.criteria.CriteriaBuilder;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import net.berndreiss.zentodo.auth.TokenManager;
import net.berndreiss.zentodo.data.*;
import net.berndreiss.zentodo.util.*;
import org.json.JSONArray;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicReference;

/**
 * TODO DESCRIBE
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/")
public class Controller {

    private final UserService userService;
    private final EntryService entryService;
    private final MessageRepository messageRepository;
    private final EventPublisherController eventPublisherController;
    private final TokenManager tokenManager;

    /**
     * TODO
     * TODO for some reason get messages from Android are not authenticated. Therefore a post mapping is used.
     * @return
     */
    @PostMapping ("test")
    public ResponseEntity<String> test(){
        System.out.println("TEST");
        return ResponseEntity.ok("okay");
    }

    /**
     * TODO
     * TODO for some reason get messages from Android are not authenticated. Therefore a post mapping is used.
     * @param auth
     * @param device
     * @return
     */
    @PostMapping("queue")
    public synchronized ResponseEntity<String> queue(@RequestHeader("Authorization") String auth, @RequestHeader("device") Long device){

        System.out.println("QUEUE");
        //TODO Authorize queue poll -> add mail to message

        List<ZenMessage> messageList = new ArrayList<>();

        Message message = null;

        List<MissingQueueUpdate> missingQueueUpdates = entryService.missingQueueUpdatesRepository.findAll().stream()
                .filter(u -> u.getDevices().contains(device)).toList();

        for (MissingQueueUpdate u: missingQueueUpdates){
            if (message == null){
                message = new Message();
                messageRepository.save(message);
            }
                    QueueItem queueItem = entryService.queueRepository.findById(u.getId()).orElse(null);
                    if (queueItem != null){

                        List<Object> args = new ArrayList<>(queueItem.getArguments());

                        VectorClock vectorClock = new VectorClock(queueItem.getClock());
                        messageList.add(new ZenMessage(queueItem.getType(), args, vectorClock));

                        Acknowledgement acknowledgement = new Acknowledgement();
                        acknowledgement.setMissingQueueUpdateId(u.getId());
                        acknowledgement.setMessage(message);

                        entryService.acknowledgementRepository.save(acknowledgement);

                    }
        }


        if (message != null) {
            User user = userService.getByMail(tokenManager.getMailFromToken(auth));

            eventPublisherController.publish(
                    String.valueOf(message.getId()),
                    ClientStub.jsonifyList(messageList),
                    user.getEmail(),
                    Collections.singletonList(device));
        }

        return ResponseEntity.ok("");
    }

    @PostMapping("ackn")
    public synchronized ResponseEntity<String> ackn(@RequestBody Long id, @RequestHeader("Authorization") String auth, @RequestHeader("device") Long device){


        System.out.println("ACKN");

            System.out.println(tokenManager.getMailFromToken(auth) + id);

            List<Acknowledgement> acknowledgements = entryService.acknowledgementRepository.findAll().stream().filter(a -> a.getMessage().getId() == id).toList();


            acknowledgements.forEach(a -> {
                MissingQueueUpdate missingQueueUpdate = entryService.missingQueueUpdatesRepository.findById(a.getMissingQueueUpdateId());
                missingQueueUpdate.getDevices().remove(device);
                System.out.println(missingQueueUpdate.getId());
                System.out.println(missingQueueUpdate.getDevices().size());
                if (missingQueueUpdate.getDevices().isEmpty()) {
                    entryService.missingQueueUpdatesRepository.delete(missingQueueUpdate);
                    entryService.queueRepository.deleteById(missingQueueUpdate.getId());
                } else
                    entryService.missingQueueUpdatesRepository.save(missingQueueUpdate);

            });


            messageRepository.deleteById(id);
            return ResponseEntity.ok("ackn");
    }

    /**
     * TODO DESCRIBE
     * @param messageListString
     * @return
     */
    @PostMapping("process")
    public synchronized ResponseEntity<String> process(@RequestBody String messageListString, @RequestHeader("Authorization") String auth, @RequestHeader("device") Long device){

        List<ZenServerMessage> messageList = new ArrayList<>();

        JSONArray array = new JSONArray(messageListString);
        array.forEach(o -> messageList.add(ZenServerMessage.parse(o.toString())));

        System.out.println("PROCESSING");

        User user = userService.getByMail(tokenManager.getMailFromToken(auth));

        List<Long> devices = userService.getOtherDevices(user, device);

        Message message = new Message();
        messageRepository.save(message);

        if (user == null)
            return ResponseEntity.status(401).build();


        List<QueueItem> queue = entryService.getQueue(user).stream().sorted(Comparator.comparing(QueueItem::getTimeStamp)).toList();

        for (ZenServerMessage zm: messageList) {
            VectorClock clock = new VectorClock(user.getClock());

            clock.increment(device);
            user.setClock(clock.jsonify());
            userService.repository.save(user);


            switch (zm.type) {
                case ADD_NEW_ENTRY -> {
                    for (QueueItem qi: queue) {

                        List<Long> missingDevices = entryService.missingQueueUpdatesRepository.findById(qi.getId()).getDevices();

                        if (!missingDevices.contains(device) || qi.getType() != OperationType.ADD_NEW_ENTRY)
                            continue;

                        if (Integer.parseInt(qi.getArguments().get(3)) < Integer.parseInt(zm.arguments.get(3).toString()))
                            continue;

                        if (qi.getTimeStamp().isBefore(zm.timeStamp)){
                            qi.getArguments().set(3, String.valueOf(Integer.parseInt(qi.getArguments().get(3)) + 1));
                            entryService.queueRepository.save(qi);
                        } else
                            zm.arguments.set(3, Integer.parseInt(zm.arguments.get(3).toString()) + 1);

                    }
                    List<Object> args = zm.arguments;

                    long id = Long.parseLong(args.getFirst().toString());

                    while (entryService.repository.findById(id).isPresent())
                        id++;

                    if (id != Long.parseLong(args.getFirst().toString())) {
                        List<Object> updateArgs = new ArrayList<>();
                        updateArgs.add(args.getFirst());
                        updateArgs.add(id);
                        ZenMessage updatedZM = new ZenMessage(OperationType.UPDATE_ID, updateArgs, null);
                        List<Long> deviceContainer = new ArrayList<>();
                        deviceContainer.add(device);
                        eventPublisherController.publish(clock.jsonify(), ClientStub.jsonifyMessage(updatedZM), user.getEmail(), deviceContainer);
                        //entryService.addToQueue(ClientStub.jsonifyMessage(zm), Collections.singleton(device));
                    }
                    Entry entry = new Entry(
                            id,
                            (String) args.get(1),
                            Long.parseLong(args.get(2).toString()),
                            Integer.parseInt(args.get(3).toString())
                    );
                    entryService.repository.save(entry);

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
            entryService.addToQueue(zm, user, devices, message);

        }

        System.out.println(ClientStub.jsonifyServerList(messageList));
        eventPublisherController.publish(String.valueOf(message.getId()), ClientStub.jsonifyServerList(messageList), user.getEmail(), devices);

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
            List<Object> arguments = message.arguments;
            Entry entry = new Entry(Long.parseLong((String) arguments.get(0)), (String) arguments.get(1), Long.parseLong((String) arguments.get(2)), Integer.parseInt((String) arguments.get(3)));

            //entryRepository.save(entry);
            for (Object s : message.arguments)
                System.out.println(s);
            System.out.println(message.timeStamp);
        }


        return ResponseEntity.ok("");
    }


}
