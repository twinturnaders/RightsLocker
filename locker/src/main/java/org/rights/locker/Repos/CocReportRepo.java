package org.rights.locker.Repos;

import org.rights.locker.Entities.CocReport;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.UUID;


public interface CocReportRepo extends JpaRepository<CocReport, UUID> { }