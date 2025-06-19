package net.berndreiss.zentodo;

import lombok.RequiredArgsConstructor;
import net.berndreiss.zentodo.auth.TokenManager;
import net.berndreiss.zentodo.data.*;
import net.berndreiss.zentodo.util.*;
import org.json.JSONArray;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.*;

/**
 * TODO DESCRIBE
 */
@CrossOrigin(origins = "*")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api")
public class Controller {

    private final UserService userService;
    private final EntryService entryService;
    private final AcknowledgementService acknowledgementService;
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
    //WE ARE USING POST MAPPING BECAUSE FOR SOME REASON ANDROID DOES NOT LIKE GET
    @PostMapping("queue")
    public synchronized ResponseEntity<String> queue(@RequestHeader("Authorization") String auth, @RequestHeader("device") Integer device){

        // TODO SORT BY DATE
        //TODO Authorize queue poll -> add mail to message
        //TODO Filter by mail
        List<ZenMessage> messageList = new ArrayList<>();

        Message message = null;

        List<MissingQueueUpdate> missingQueueUpdates = entryService.missingQueueUpdatesRepository.findAll().stream()
                .filter(u -> u.getDevices().contains(device)).toList();

        if (missingQueueUpdates.isEmpty())
            return ResponseEntity.ok("{ \"message\": []}");

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

/*
        if (message != null) {
            User user = userService.getByMail(tokenManager.getMailFromToken(auth));

            eventPublisherController.publish(
                    String.valueOf(message.getId()),
                    ClientStub.jsonifyList(messageList),
                    user.getEmail(),
                    Collections.singletonList(device));
        }

 */

        String response = "{ \"message\": " + ClientStub.jsonifyList(messageList) + "}";
        return ResponseEntity.ok(response);
    }

    @PostMapping("ackn")
    public synchronized ResponseEntity<String> ackn(@RequestBody Long id, @RequestHeader("Authorization") String auth, @RequestHeader("device") Integer device) {

        acknowledgementService.processAcknowledgement(id, device);
        return ResponseEntity.ok("ackn");
    }

    /**
     * TODO DESCRIBE
     * @param messageListString
     * @return
     */
    @PostMapping("process")
    public synchronized ResponseEntity<String> process(@RequestBody String messageListString, @RequestHeader("Authorization") String auth, @RequestHeader("device") Integer device) throws InterruptedException {

        User user = userService.getByMail(tokenManager.getMailFromToken(auth));

            List<ZenServerMessage> messageList = new ArrayList<>();

            JSONArray array = new JSONArray(messageListString);
            for (int i = 0; i < array.length(); i++) {
                messageList.add(ZenServerMessage.parse(array.get(i).toString()));
            }
            if (messageList.isEmpty())
                return ResponseEntity.ok("");

                List<Integer> devices = userService.getOtherDevices(user, device);

                Message message = new Message();
                messageRepository.save(message);

                if (user == null)
                    return ResponseEntity.status(401).build();



                List<Integer> alreadyAddedPositions = new ArrayList<>();
                for (ZenServerMessage zm : messageList) {
                    VectorClock clock = new VectorClock(user.getClock());

                    clock.increment(device);
                    user.setClock(clock.jsonify());
                    userService.repository.save(user);


                    switch (zm.type) {
                        case ADD_NEW_ENTRY -> {
                            int profile = Integer.parseInt(zm.arguments.getFirst().toString());
                            int originalPosition = Integer.parseInt(zm.arguments.get(3).toString());
                            List<QueueItem> queue = entryService.getQueue(user).stream().sorted(Comparator.comparing(QueueItem::getTimeStamp)).toList();
                            for (QueueItem qi : queue) {

                                List<Integer> missingDevices = entryService.missingQueueUpdatesRepository.findById(qi.getId()).getDevices();

                                if (!missingDevices.contains(device) ||
                                        qi.getType() != OperationType.ADD_NEW_ENTRY ||
                                        Integer.parseInt(qi.getArguments().getFirst()) != profile)
                                    continue;

                                if (Integer.parseInt(qi.getArguments().get(3)) < Integer.parseInt(zm.arguments.get(3).toString()))
                                    continue;

                                if (qi.getTimeStamp().isAfter(zm.timeStamp)) {
                                    qi.getArguments().set(3, String.valueOf(Integer.parseInt(qi.getArguments().get(3)) + 1));
                                    entryService.queueRepository.saveAndFlush(qi);
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

                            while (entryService.entryRepository.findById(id).isPresent())
                                id++;

                            if (id != Long.parseLong(args.get(1).toString())) {
                                List<Object> updateArgs = new ArrayList<>();
                                updateArgs.add(args.get(1));
                                updateArgs.add(id);
                                ZenMessage updatedZM = new ZenMessage(OperationType.UPDATE_ID, updateArgs, null);
                                List<Integer> deviceContainer = new ArrayList<>();
                                deviceContainer.add(device);
                                eventPublisherController.publish(clock.jsonify(), ClientStub.jsonifyMessage(updatedZM), user.getEmail(), deviceContainer);
                                //entryService.addToQueue(ClientStub.jsonifyMessage(zm), Collections.singleton(device));
                            }
                            Entry entry = new Entry(
                                    user.getId(),
                                    profile,
                                    id,
                                    (String) args.get(2),
                                    Integer.parseInt(args.get(3).toString())
                            );
                            entryService.entryRepository.save(entry);
                        }
                        case DELETE -> {
                            //TODO REMOVE FROM QUEUE TOO
                            int profile = Integer.parseInt(zm.arguments.getFirst().toString());
                            long id = Long.parseLong(zm.arguments.get(1).toString());
                            Optional<Entry> entry = entryService.entryRepository.findById(id);

                            //We assume this delete is redundant
                            if (entry.isEmpty())
                                return ResponseEntity.ok("Entry already deleted");

                            //We have to adjust positions and list positions of entries that are queued
                            List<QueueItem> queue = entryService.getQueue(user).stream().sorted(Comparator.comparing(QueueItem::getTimeStamp)).toList();
                            for (QueueItem qi : queue) {

                                List<Integer> missingDevices = entryService.missingQueueUpdatesRepository.findById(qi.getId()).getDevices();

                                if (!missingDevices.contains(device) || Integer.parseInt(qi.getArguments().getFirst()) != profile)
                                    continue;

                                switch (qi.getType()) {
                                    case OperationType.ADD_NEW_ENTRY:
                                        //TODO IMPLEMENT
                                        if (Integer.parseInt(qi.getArguments().get(3)) > entry.get().getPosition()) {
                                            entryService.queueRepository.saveAndFlush(qi);
                                        }
                                        break;
                                    case OperationType.SWAP:
                                        //TODO IMPLEMENT
                                        break;
                                    case OperationType.UPDATE_LIST:
                                        //TODO IMPLEMENT
                                        break;
                                    case OperationType.SWAP_LIST:
                                        //TODO IMPLEMENT
                                        break;
                                    default:
                                }

                            }

                            entryService.entryRepository.delete(entry.get());
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

                eventPublisherController.publish(String.valueOf(message.getId()), ClientStub.jsonifyServerList(messageList), user.getEmail(), devices);
            return ResponseEntity.ok("");
    }
}
