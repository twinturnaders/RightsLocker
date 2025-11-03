package org.rights.locker.Services;

import lombok.RequiredArgsConstructor;
import org.rights.locker.Entities.AppUser;
import org.rights.locker.Entities.CustodyEvent;
import org.rights.locker.Entities.Evidence;
import org.rights.locker.Enums.CustodyEventType;
import org.rights.locker.Repos.CustodyEventRepo;
import org.springframework.stereotype.Service;

import java.util.Map;


@Service
@RequiredArgsConstructor
public class CustodyService {
    private final CustodyEventRepo repo;

    public void record(Evidence ev, AppUser actor, CustodyEventType type, Map<String, Object> metaJson) {
        var e = CustodyEvent.builder()
                .evidence(ev)
                .actor(actor)                   // can be null for anonymous
                .eventType(type)
                .metaJson(metaJson)
                .build();
        repo.save(e);
    }
}