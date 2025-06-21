package net.berndreiss.zentodo.data;

import lombok.RequiredArgsConstructor;
import net.berndreiss.zentodo.operations.OperationType;
import net.berndreiss.zentodo.util.VectorClock;
import net.berndreiss.zentodo.util.ZenServerMessage;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * TODO DESCRIBE
 */
@Service
@RequiredArgsConstructor
public class TaskService {
    public final TaskRepository taskRepository;
    public final QueueRepository queueRepository;
    public final MissingQueueUpdatesRepository missingQueueUpdatesRepository;
    public final AcknowledgementRepository acknowledgementRepository;
    public final MessageRepository messageRepository;

    public List<Task> getAllTasks(long userId){
        return taskRepository.findAllByUserId(userId);
    }

    public void addTask(Task task){
        taskRepository.save(task);
    }

    public void addToQueue(ZenServerMessage zenMessage, User user, List<Integer> devices, Message message) throws InterruptedException {

        QueueItem queueItem = new QueueItem();
        queueItem.setClock(zenMessage.clock.jsonify());
        queueItem.setType(zenMessage.type);
        queueItem.setArguments(zenMessage.arguments);
        queueItem.setUser(user);
        queueItem.setTimeStamp(zenMessage.timeStamp);
        queueRepository.save(queueItem);

        MissingQueueUpdate missingQueueUpdate = new MissingQueueUpdate();
        missingQueueUpdate.setId(queueItem.getId());
        missingQueueUpdate.setDevices(devices);
        missingQueueUpdate.setClock(queueItem.getClock());
        missingQueueUpdatesRepository.save(missingQueueUpdate);
        Acknowledgement ackn = new Acknowledgement();
        ackn.setMessage(message);
        ackn.setMissingQueueUpdateId(missingQueueUpdate.getId());
        acknowledgementRepository.save(ackn);
    }
     public void addAllToQueue(User user, int device) throws InterruptedException {
         if (user == null)
             return;
         List<Task> entries = taskRepository.findAllByUserId(user.getId());
         Message message = new Message();
         messageRepository.save(message);
         for (Task t : entries) {
             List<Object> arguments = new ArrayList<>();
             arguments.add(t.getProfile());
             arguments.add(t.getId());
             arguments.add(t.getTask());
             arguments.add(t.getPosition());
             arguments.add(t.getFocus());
             arguments.add(t.getDropped());
             arguments.add(t.getList());
             arguments.add(t.getListPosition());
             arguments.add(t.getReminderDate());
             arguments.add(t.getRecurrence());
             Instant instant = ZonedDateTime.now().minusYears(100).toInstant();
             ZenServerMessage zm = new ZenServerMessage(OperationType.POST, arguments, new VectorClock(user), instant);
             addToQueue(zm, user, List.of(device), message);
         }
     }

    public List<QueueItem> getQueue(User user){
        return queueRepository.findByUserId(user.getId());
    }
}
