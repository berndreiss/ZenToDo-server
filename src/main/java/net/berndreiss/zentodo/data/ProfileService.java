package net.berndreiss.zentodo.data;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ProfileService {
    @Autowired
    ProfileRepository profileRepository;
    public void addAllToQueue(){
        //TODO IMPLEMENT
    }
}
