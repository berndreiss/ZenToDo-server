package net.berndreiss.zentodo.util;

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
    public List<Long> publish(@RequestParam String message, String email, List<Long> devices) {
        return webSocketHandler.publishEvent( message, email, devices);
    }

    @PostMapping("ackn")
    public ResponseEntity<String> ackn(@RequestBody Acknowledgement acknowledgement){
        webSocketHandler.handleAcknowledgment(acknowledgement);
        return ResponseEntity.ok("ackn");
    }
}
