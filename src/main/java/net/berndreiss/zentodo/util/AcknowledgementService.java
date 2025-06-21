package net.berndreiss.zentodo.util;

import lombok.RequiredArgsConstructor;
import net.berndreiss.zentodo.data.Acknowledgement;
import net.berndreiss.zentodo.data.TaskService;
import net.berndreiss.zentodo.data.MessageRepository;
import net.berndreiss.zentodo.data.MissingQueueUpdate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Objects;

@RequiredArgsConstructor
@Service
public class AcknowledgementService {

    @Autowired
    private final TaskService taskService;
    @Autowired
    private final MessageRepository messageRepository;

    @Async
    public synchronized void processAcknowledgement(Long id, Integer device) {
        try {
            Thread.sleep(200);//PREVENT QUEUE BEING CLEARED BEFORE CONCURRENT ACTIONS ARE COMPLETED
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }

        List<Acknowledgement> acknowledgements = taskService.acknowledgementRepository.findAll().stream().filter(a -> a.getMessage().getId() == id).toList();


        acknowledgements.forEach(a -> {
            MissingQueueUpdate missingQueueUpdate = taskService.missingQueueUpdatesRepository.findById(a.getMissingQueueUpdateId());
            if (missingQueueUpdate != null) {
                for (int i = 0; i < missingQueueUpdate.getDevices().size(); i++) {
                    if (Objects.equals(missingQueueUpdate.getDevices().get(i), device)) {
                        missingQueueUpdate.getDevices().remove(i);
                        break;
                    }
                }
                if (missingQueueUpdate.getDevices().isEmpty()) {
                    taskService.missingQueueUpdatesRepository.delete(missingQueueUpdate);
                    taskService.queueRepository.deleteById(missingQueueUpdate.getId());
                    messageRepository.deleteById(id);
                    taskService.acknowledgementRepository.delete(a);
                } else
                    taskService.missingQueueUpdatesRepository.save(missingQueueUpdate);
            }
        });
    }
}

