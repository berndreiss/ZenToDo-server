package net.berndreiss.zentodo.util;

import net.berndreiss.zentodo.util.PubSubWebSocketHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class EventPublisherController {

    private final PubSubWebSocketHandler webSocketHandler;

    public EventPublisherController(PubSubWebSocketHandler webSocketHandler) {
        this.webSocketHandler = webSocketHandler;
    }

    @GetMapping("/publish")
    public String publish(@RequestParam String message) {
        webSocketHandler.publishEvent(message);
        return "Message published!";
    }
}
