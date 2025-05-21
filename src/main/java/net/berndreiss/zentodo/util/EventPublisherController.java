package net.berndreiss.zentodo.util;

import net.berndreiss.zentodo.ZenToDoServerApplication;
import net.berndreiss.zentodo.data.Acknowledgement;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
public class EventPublisherController {

    private final PubSubWebSocketHandler webSocketHandler;

    public EventPublisherController(PubSubWebSocketHandler webSocketHandler) {
        this.webSocketHandler = webSocketHandler;
    }

    @GetMapping("/publish")
    public List<Integer> publish(String id, @RequestParam String message, String email, List<Integer> devices) {
        return webSocketHandler.publishEvent(id, message, email, devices);
    }
}
