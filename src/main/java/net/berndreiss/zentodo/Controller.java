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
import org.json.JSONObject;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicReference;
import java.util.logging.Logger;

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



            List<Acknowledgement> acknowledgements = entryService.acknowledgementRepository.findAll().stream().filter(a -> a.getMessage().getId() == id).toList();


            acknowledgements.forEach(a -> {
                MissingQueueUpdate missingQueueUpdate = entryService.missingQueueUpdatesRepository.findById(a.getMissingQueueUpdateId());
                if (missingQueueUpdate != null) {
                    missingQueueUpdate.getDevices().remove(device);
                    if (missingQueueUpdate.getDevices().isEmpty()) {
                        entryService.missingQueueUpdatesRepository.delete(missingQueueUpdate);
                        entryService.queueRepository.deleteById(missingQueueUpdate.getId());
                    } else
                        entryService.missingQueueUpdatesRepository.save(missingQueueUpdate);
                }
            });

            for (Acknowledgement a: acknowledgements)
                entryService.acknowledgementRepository.delete(a);

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

        System.out.println("PROCESSING");
        List<ZenServerMessage> messageList = new ArrayList<>();

        JSONArray array = new JSONArray(messageListString);
        for (int i = 0; i< array.length(); i++){
             messageList.add(ZenServerMessage.parse(array.get(i).toString()));
        }


        User user = userService.getByMail(tokenManager.getMailFromToken(auth));
        List<Long> devices = userService.getOtherDevices(user, device);

        Message message = new Message();
        messageRepository.save(message);

        if (user == null)
            return ResponseEntity.status(401).build();


        List<QueueItem> queue = entryService.getQueue(user).stream().sorted(Comparator.comparing(QueueItem::getTimeStamp)).toList();

        List<Integer> alreadyAddedPositions = new ArrayList<>();
        for (ZenServerMessage zm: messageList) {
            VectorClock clock = new VectorClock(user.getClock());

            clock.increment(device);
            user.setClock(clock.jsonify());
            userService.repository.save(user);


            switch (zm.type) {
                case ADD_NEW_ENTRY -> {
                    int originalPosition = Integer.parseInt(zm.arguments.get(3).toString());
                    for (QueueItem qi: queue) {

                        List<Long> missingDevices = entryService.missingQueueUpdatesRepository.findById(qi.getId()).getDevices();

                        if (!missingDevices.contains(device) || qi.getType() != OperationType.ADD_NEW_ENTRY)
                            continue;

                        if (Integer.parseInt(qi.getArguments().get(3)) < Integer.parseInt(zm.arguments.get(3).toString()))
                            continue;

                        if (qi.getTimeStamp().isAfter(zm.timeStamp)){
                            qi.getArguments().set(3, String.valueOf(Integer.parseInt(qi.getArguments().get(3)) + 1));
                            entryService.queueRepository.save(qi);
                        } else
                            zm.arguments.set(3, Integer.parseInt(zm.arguments.get(3).toString()) + 1);

                    }

                    int toAdd = (int) alreadyAddedPositions.stream().filter(i -> i <= Integer.parseInt(zm.arguments.get(3).toString())).count();

                    int finalPosition = Integer.parseInt(zm.arguments.get(3).toString()) + toAdd;
                    zm.arguments.set(3, finalPosition);
                    if (originalPosition != finalPosition)
                        alreadyAddedPositions.add(finalPosition);

                    List<Object> args = zm.arguments;

                    long id = Long.parseLong(args.get(1).toString());

                    while (entryService.repository.findById(id).isPresent())
                        id++;

                    if (id != Long.parseLong(args.get(1).toString())) {
                        List<Object> updateArgs = new ArrayList<>();
                        updateArgs.add(args.get(1));
                        updateArgs.add(id);
                        ZenMessage updatedZM = new ZenMessage(OperationType.UPDATE_ID, updateArgs, null);
                        List<Long> deviceContainer = new ArrayList<>();
                        deviceContainer.add(device);
                        eventPublisherController.publish(clock.jsonify(), ClientStub.jsonifyMessage(updatedZM), user.getEmail(), deviceContainer);
                        //entryService.addToQueue(ClientStub.jsonifyMessage(zm), Collections.singleton(device));
                    }
                    Entry entry = new Entry(
                            Long.parseLong(args.get(0).toString()),
                            id,
                            (String) args.get(2),
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
}
