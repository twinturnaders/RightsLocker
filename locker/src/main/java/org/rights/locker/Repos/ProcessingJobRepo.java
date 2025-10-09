package org.rights.locker.Repos;


import org.rights.locker.Entities.ProcessingJob;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.UUID;


public interface ProcessingJobRepo extends JpaRepository<ProcessingJob, UUID> { }