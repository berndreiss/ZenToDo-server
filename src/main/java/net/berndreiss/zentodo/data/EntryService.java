package net.berndreiss.zentodo.data;

import lombok.RequiredArgsConstructor;
import net.berndreiss.zentodo.util.ZenMessage;
import net.berndreiss.zentodo.util.ZenServerMessage;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * TODO DESCRIBE
 */
@Service
@RequiredArgsConstructor
public class EntryService {
    public final EntryRepository repository;
    public final QueueRepository queueRepository;
    public final MissingQueueUpdatesRepository missingQueueUpdatesRepository;
    public final AcknowledgementRepository acknowledgementRepository;

    public List<Entry> getAllEntries(){
        return repository.findAll();
    }

    public void addEntry(Entry entry){
        repository.save(entry);
    }

    public void addToQueue(ZenServerMessage zenMessage, User user, List<Integer> devices, Message message){

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

    public List<QueueItem> getQueue(User user){
        return queueRepository.findByUserId(user.getId());
    }
}
