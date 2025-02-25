package net.berndreiss.zentodo;

import lombok.RequiredArgsConstructor;
import net.berndreiss.zentodo.data.Entry;
import net.berndreiss.zentodo.data.EntryRepository;
import net.berndreiss.zentodo.util.ZenMessage;
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
     * @param message
     * @return
     */
    @PostMapping("/process")
    public String process(@RequestBody ZenMessage message){

        System.out.println("TYPE: " + message.getType());
        for (Object s: message.getArguments())
            System.out.println(s);
        return "Hello World";
    }
    /**
     * TODO DESCRIBE
     * @param message
     * @return
     */
    @PostMapping("/add")
    public void add(@RequestBody ZenMessage message){

        List<Object> arguments = message.getArguments();
        Entry entry = new Entry(Long.parseLong((String) arguments.get(0)), Integer.parseInt((String) arguments.get(1)), (String) arguments.get(2), Long.parseLong((String) arguments.get(3)));

        entryRepository.save(entry);
        for (Object s: message.getArguments())
            System.out.println(s);
    }
}
