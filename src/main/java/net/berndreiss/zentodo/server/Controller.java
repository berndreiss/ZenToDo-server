package net.berndreiss.zentodo.server;

import net.berndreiss.zentodo.AddUserMessage;
import org.apache.coyote.BadRequestException;
import org.springframework.web.bind.annotation.*;
import net.berndreiss.zentodo.util.Message;

import java.util.ArrayList;
import java.util.List;

/**
 * TODO DESCRIBE
 */
@RestController
@RequestMapping("/")
public class Controller {

    List<String> users = new ArrayList<>();

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
    public String process(@RequestBody Message message){

        System.out.println("TYPE: " + message.getType());
        for (Object s: message.getArguments())
            System.out.println(s);
        return "Hello World";
    }
}
