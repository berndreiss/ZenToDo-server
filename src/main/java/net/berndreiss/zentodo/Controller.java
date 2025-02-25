package net.berndreiss.zentodo;

import lombok.RequiredArgsConstructor;
import net.berndreiss.zentodo.data.Entry;
import net.berndreiss.zentodo.data.EntryRepository;
import net.berndreiss.zentodo.util.*;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;

/**
 * TODO DESCRIBE
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/")
public class Controller {

    private final EntryRepository entryRepository;
    private final EventPublisherController eventPublisherController;

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
    @PostMapping("/process")
    public ResponseEntity<String> process(@RequestBody List<ZenServerMessage> messageList){

        for (ZenServerMessage message: messageList) {
            switch (message.getType()){
                case ADD_NEW_ENTRY -> {}
                case DELETE -> {}
                case SWAP -> {}
                case SWAP_LIST -> {}
                case UPDATE_TASK -> {}
                case UPDATE_FOCUS -> {}
                case UPDATE_DROPPED -> {}
                case UPDATE_RECURRENCE -> {}
                case UPDATE_REMINDER_DATE -> {}
                case UPDATE_LIST -> {}
                case UPDATE_LIST_COLOR -> {}
                case UPDATE_MAIL -> {}
                case UPDATE_USER_NAME -> {}
                default -> {return ResponseEntity.badRequest().build();}
            }
            System.out.println("TYPE: " + message.getType());
            for (Object s : message.getArguments())
                System.out.println(s);
        }
        return ResponseEntity.ok("");
    }
    /**
     * TODO DESCRIBE
     * @param list
     * @return
     */
    @PostMapping("/add")
    public void add(@RequestBody List<ZenServerMessage> list){

        System.out.println("ADD CALLED");
        for (ZenServerMessage message : list) {
            List<Object> arguments = message.getArguments();
            Entry entry = new Entry(Long.parseLong((String) arguments.get(0)), Integer.parseInt((String) arguments.get(1)), (String) arguments.get(2), Long.parseLong((String) arguments.get(3)));

            //entryRepository.save(entry);
            for (Object s : message.getArguments())
                System.out.println(s);
            System.out.println(message.getTimeStamp());
        }


        System.out.println(eventPublisherController.publish(ClientStub.jsonifyServerList(list)));

    }
}
