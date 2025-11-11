package org.rights.locker.Repos;


import org.rights.locker.Entities.Evidence;
import org.rights.locker.Entities.ProcessingJob;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.UUID;


public interface ProcessingJobRepo extends JpaRepository<ProcessingJob, UUID> {
    @org.springframework.transaction.annotation.Transactional
    @org.springframework.data.jpa.repository.Modifying
    @org.springframework.data.jpa.repository.Query("delete from ProcessingJob j where j.evidence.id = :eId")
    void deleteByEvidenceId(@org.springframework.data.repository.query.Param("eId") UUID evidenceId);
}