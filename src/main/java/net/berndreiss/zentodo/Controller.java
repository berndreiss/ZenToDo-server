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
    private final TaskService taskService;
    private final AcknowledgementService acknowledgementService;
    private final MessageRepository messageRepository;
    private final EventPublisherController eventPublisherController;
    private final TokenManager tokenManager;
    private final MessageProcessor messageProcessor;

    /**
     * TODO
     * TODO for some reason get messages from Android are not authenticated. Therefore a post mapping is used.
     *
     * @return
     */
    @PostMapping("test")
    public ResponseEntity<String> test() {
        return ResponseEntity.ok("okay");
    }

    /**
     * TODO
     * TODO for some reason get messages from Android are not authenticated. Therefore a post mapping is used.
     *
     * @param auth
     * @param device
     * @return
     */
    //WE ARE USING POST MAPPING BECAUSE FOR SOME REASON ANDROID DOES NOT LIKE GET
    @PostMapping("queue")
    public synchronized ResponseEntity<String> queue(@RequestHeader("Authorization") String auth, @RequestHeader("device") Integer device) {

        // TODO SORT BY DATE
        //TODO Authorize queue poll -> add mail to message
        //TODO Filter by mail
        List<ZenMessage> messageList = new ArrayList<>();

        Message message = null;

        List<MissingQueueUpdate> missingQueueUpdates = taskService.missingQueueUpdatesRepository.findAll().stream()
                .filter(u -> u.getDevices().contains(device)).toList();

        if (missingQueueUpdates.isEmpty())
            return ResponseEntity.ok("{ \"message\": []}");

        for (MissingQueueUpdate u : missingQueueUpdates) {
            if (message == null) {
                message = new Message();
                messageRepository.save(message);
            }
            QueueItem queueItem = taskService.queueRepository.findById(u.getId()).orElse(null);
            if (queueItem != null) {

                List<Object> args = new ArrayList<>(queueItem.getArguments());

                VectorClock vectorClock = new VectorClock(queueItem.getClock());
                messageList.add(new ZenMessage(queueItem.getType(), args, vectorClock));

                Acknowledgement acknowledgement = new Acknowledgement();
                acknowledgement.setMissingQueueUpdateId(u.getId());
                acknowledgement.setMessage(message);

                taskService.acknowledgementRepository.save(acknowledgement);

            }
        }

        String response = "{ \"message\": " + ZenMessage.jsonifyList(messageList) + "}";
        return ResponseEntity.ok(response);
    }

    @PostMapping("ackn")
    public synchronized ResponseEntity<String> ackn(@RequestBody Long id, @RequestHeader("Authorization") String auth, @RequestHeader("device") Integer device) {
        acknowledgementService.processAcknowledgement(id, device);
        return ResponseEntity.ok("ackn");
    }

    /**
     * This method processes an operation performed by a device.
     * Available operations are defined in net.berndreiss.zentodo.OperationType.
     *
     * @param messageListString string containing a list of messages
     * @param auth jwt authentication token
     * @param device the device this operation was performed on
     * @return response
     */
    @PostMapping("process_operation")
    public synchronized ResponseEntity<String> process(@RequestBody String messageListString, @RequestHeader("Authorization") String auth, @RequestHeader("device") Integer device) throws InterruptedException {

        //TODO check whether there are items in the queue and send them first
        User user = userService.getByMail(tokenManager.getMailFromToken(auth));

        if (user == null)
            return ResponseEntity.status(401).build();

        List<Integer> devices = userService.getOtherDevices(user, device);

        //Convert the message list string to a list
        List<ZenServerMessage> messageList = new ArrayList<>();
        JSONArray array = new JSONArray(messageListString);
        for (int i = 0; i < array.length(); i++)
            messageList.add(ZenServerMessage.parse(array.get(i).toString()));
        if (messageList.isEmpty())
            return ResponseEntity.ok("");

        //Message to be sent to other devices
        Message message = new Message();
        messageRepository.save(message);

        //Keep track of already added positions to adjust adding new entry accordingly (see MessageProcessor.addNewEntry).
        List<Integer> alreadyAddedPositions = new ArrayList<>();

        //Process every message and refer it to the adequate method in the MessageProcessor.
        for (ZenServerMessage zm : messageList) {

            //Update the vector clock
            VectorClock clock = new VectorClock(user.getClock());
            clock.increment(device);
            user.setClock(clock.jsonify());
            userService.repository.save(user);

            switch (zm.type) {
                case ADD_NEW_ENTRY -> messageProcessor.addNewEntry(zm, user, device, alreadyAddedPositions);
                case DELETE -> messageProcessor.delete(zm, user, device);
                case SWAP -> messageProcessor.swap(zm, user, device);
                case SWAP_LIST -> messageProcessor.swapList(zm, user, device);
                case UPDATE_TASK -> messageProcessor.updateTask(zm, user, device);
                case UPDATE_FOCUS -> messageProcessor.updateFocus(zm, user, device);
                case UPDATE_DROPPED -> messageProcessor.updateDropped(zm, user, device);
                case UPDATE_RECURRENCE -> messageProcessor.updateRecurrence(zm, user, device);
                case UPDATE_REMINDER_DATE -> messageProcessor.updateReminderDate(zm, user, device);
                case UPDATE_LIST -> messageProcessor.updateList(zm, user, device);
                case UPDATE_LIST_COLOR -> messageProcessor.updateListColor(zm, user, device);
                case UPDATE_MAIL -> messageProcessor.updateMail(zm, user, device);
                case UPDATE_USER_NAME -> messageProcessor.updateUserName(zm, user, device);
                default -> {
                    return ResponseEntity.badRequest().build();
                }
            }
            //add the message to the queue for all other devices
            taskService.addToQueue(zm, user, devices, message);
        }

        //publish the message list to all other devices
        eventPublisherController.publish(String.valueOf(message.getId()), ZenServerMessage.jsonifyServerList(messageList), user.getEmail(), devices);
        return ResponseEntity.ok("");
    }
}
