package net.berndreiss.zentodo.util;

import lombok.RequiredArgsConstructor;
import net.berndreiss.zentodo.data.QueueItem;
import net.berndreiss.zentodo.data.User;
import net.berndreiss.zentodo.data.Task;
import net.berndreiss.zentodo.operations.OperationType;
import net.berndreiss.zentodo.data.TaskService;
import org.springframework.stereotype.Component;

import java.util.*;

@Component
@RequiredArgsConstructor
public class MessageProcessor {

    private final TaskService taskService;
    private final EventPublisherController eventPublisherController;

    public void addNewEntry(ZenServerMessage zm, User user, int device, List<Integer> alreadyAddedPositions){
        int profile = Integer.parseInt(zm.arguments.getFirst().toString());
        int originalPosition = Integer.parseInt(zm.arguments.get(3).toString());
        List<QueueItem> queue = taskService.getQueue(user).stream().sorted(Comparator.comparing(QueueItem::getTimeStamp)).toList();
        for (QueueItem qi : queue) {
            List<Integer> missingDevices = taskService.missingQueueUpdatesRepository.findById(qi.getId()).getDevices();

            if (!missingDevices.contains(device) ||
                    qi.getType() != OperationType.ADD_NEW_TASK ||
                    Integer.parseInt(qi.getArguments().getFirst()) != profile)
                continue;

            if (Integer.parseInt(qi.getArguments().get(3)) < Integer.parseInt(zm.arguments.get(3).toString()))
                continue;

            if (qi.getTimeStamp().isAfter(zm.timeStamp)) {
                qi.getArguments().set(3, String.valueOf(Integer.parseInt(qi.getArguments().get(3)) + 1));
                taskService.queueRepository.saveAndFlush(qi);
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

        while (taskService.taskRepository.findById(id).isPresent())
            id++;

        if (id != Long.parseLong(args.get(1).toString())) {
            List<Object> updateArgs = new ArrayList<>();
            updateArgs.add(args.get(1));
            updateArgs.add(id);
            ZenMessage updatedZM = new ZenMessage(OperationType.UPDATE_ID, updateArgs, null);
            List<Integer> deviceContainer = new ArrayList<>();
            deviceContainer.add(device);
            eventPublisherController.publish(user.getClock(), ZenMessage.jsonifyMessage(updatedZM), user.getEmail(), deviceContainer);
            //entryService.addToQueue(ClientStub.jsonifyMessage(zm), Collections.singleton(device));
        }
        Task task = new Task(
                user.getId(),
                profile,
                id,
                (String) args.get(2),
                Integer.parseInt(args.get(3).toString())
        );
        taskService.taskRepository.save(task);
    }
    public void delete(ZenServerMessage zm, User user, int device){
        //TODO REMOVE FROM QUEUE TOO
        int profile = Integer.parseInt(zm.arguments.getFirst().toString());
        long id = Long.parseLong(zm.arguments.get(1).toString());
        Optional<Task> task = taskService.taskRepository.findById(id);

        //We assume this delete is redundant
        if (task.isEmpty())
            return;

        //We have to adjust positions and list positions of entries that are queued
        List<QueueItem> queue = taskService.getQueue(user).stream().sorted(Comparator.comparing(QueueItem::getTimeStamp)).toList();
        for (QueueItem qi : queue) {

            List<Integer> missingDevices = taskService.missingQueueUpdatesRepository.findById(qi.getId()).getDevices();

            if (!missingDevices.contains(device) || Integer.parseInt(qi.getArguments().getFirst()) != profile)
                continue;

            switch (qi.getType()) {
                case OperationType.ADD_NEW_TASK:
                    //TODO IMPLEMENT
                    if (Integer.parseInt(qi.getArguments().get(3)) > task.get().getPosition()) {
                        taskService.queueRepository.saveAndFlush(qi);
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

        taskService.taskRepository.delete(task.get());
    }

    public void swap(ZenServerMessage zm, User user, Integer device) {
    }

    public void swapList(ZenServerMessage zm, User user, Integer device) {
    }

    public void updateTask(ZenServerMessage zm, User user, Integer device) {
    }

    public void updateFocus(ZenServerMessage zm, User user, Integer device) {
    }

    public void updateDropped(ZenServerMessage zm, User user, Integer device) {
    }

    public void updateRecurrence(ZenServerMessage zm, User user, Integer device) {
    }

    public void updateReminderDate(ZenServerMessage zm, User user, Integer device) {
    }

    public void updateList(ZenServerMessage zm, User user, Integer device) {
    }

    public void updateListColor(ZenServerMessage zm, User user, Integer device) {
    }

    public void updateMail(ZenServerMessage zm, User user, Integer device) {
    }

    public void updateUserName(ZenServerMessage zm, User user, Integer device) {
    }

    public void addNewList(ZenServerMessage zm) {
    }
}
