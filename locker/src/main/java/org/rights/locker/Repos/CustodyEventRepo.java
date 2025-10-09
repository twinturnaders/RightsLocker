package org.rights.locker.Repos;

import org.rights.locker.Entities.CustodyEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.UUID;


public interface CustodyEventRepo extends JpaRepository<CustodyEvent, UUID> { }