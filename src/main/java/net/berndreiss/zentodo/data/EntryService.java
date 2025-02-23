package net.berndreiss.zentodo.data;

import org.springframework.stereotype.Service;

import java.util.List;

/**
 * TODO DESCRIBE
 */
@Service
public class EntryService {
    private final EntryRepository repository;

    public EntryService (EntryRepository repository){
        this.repository = repository;
    }

    public List<Entry> getAllEntries(){
        return repository.findAll();
    }

    public void addEntry(Entry entry){
        repository.save(entry);
    }
}
