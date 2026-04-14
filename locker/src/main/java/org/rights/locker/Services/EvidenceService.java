package org.rights.locker.Services;

import lombok.RequiredArgsConstructor;

import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.Point;
import org.rights.locker.DTOs.EvidenceDetailsDto;
import org.rights.locker.DTOs.EvidenceSummaryDto;
import org.rights.locker.DTOs.OwnerDto;
import org.rights.locker.DTOs.AuthenticityAssessment;
import org.rights.locker.Entities.AppUser;
import org.rights.locker.Entities.Evidence;
import org.rights.locker.Entities.ProcessingJob;
import org.rights.locker.Enums.CustodyEventType;
import org.rights.locker.Enums.EvidenceStatus;
import org.rights.locker.Enums.JobStatus;
import org.rights.locker.Enums.JobType;
import org.rights.locker.Repos.AppUserRepo;
import org.rights.locker.Repos.EvidenceRepo;
import org.rights.locker.Repos.ProcessingJobRepo;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;

import java.io.InputStream;
import java.math.BigDecimal;
import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class EvidenceService {
    private final EvidenceRepo repo;
    private final ProcessingJobRepo jobs;
    private final ProcessorService processor;
    private final StorageService storage;
    private final CustodyService custody;
    private final GeometryFactory gf = new GeometryFactory();
    private final S3Presigner s3Presigner;
    private final AppUserRepo appUserRepo;
    private EvidenceSummaryDto summary;
    private EvidenceDetailsDto details;
    private OwnerDto owner;

    public Evidence upload(MultipartFile file, String title, String description, java.time.Instant capturedAt, Double lat, Double lon, Double accuracy){
        try (InputStream in = file.getInputStream()){
            var md = MessageDigest.getInstance("SHA-256");
            byte[] bytes = in.readAllBytes();
            md.update(bytes);
            String sha = HexFormat.of().formatHex(md.digest());

            String key = java.util.UUID.randomUUID()+"/original-"+file.getOriginalFilename();
            storage.putOriginal(key, new java.io.ByteArrayInputStream(bytes), bytes.length, file.getContentType(), sha);

            Point point = null;
            if (lat != null && lon != null) {
                point = gf.createPoint(new org.locationtech.jts.geom.Coordinate(lon, lat));
            }

            var ev = Evidence.builder()

                    .title(title).description(description)
                    .capturedAt(capturedAt)
                    .captureLatlon(point)
                    .captureAccuracyM(BigDecimal.valueOf(accuracy))
                    .status(EvidenceStatus.RECEIVED)
                    .originalSha256(sha)
                    .originalSizeB((long) bytes.length)
                    .originalKey(key)
                    .legalHold(false)
                    .build();
            ev = repo.save(ev);
            custody.record(ev, null, CustodyEventType.RECEIVED, Map.of("source", "presigned"));

            // Enqueue processing jobs
            var thumb = jobs.save(ProcessingJob.builder()
                    .evidence(ev).type(JobType.THUMBNAIL).status(JobStatus.QUEUED).attempts(0).build());
            processor.publish(thumb);

            var redact = jobs.save(ProcessingJob.builder()
                    .evidence(ev).type(JobType.REDACT).status(JobStatus.QUEUED).attempts(0).build());
            processor.publish(redact);
            return ev;
        } catch (Exception e){
            throw new RuntimeException(e);
        }
    }

    public Page<EvidenceDetailsDto> list(AppUser owner, Pageable pageable){
        UUID ownerId = owner.getId();
        AppUser user = appUserRepo.findById(ownerId).orElse(null);
        if (user != null) {

            Page<Evidence> ev = repo.findByOwner(user, pageable);
            return getPageDetailsDTO(owner, pageable);
        }
        else{
            return null;
        }
    }

    public Page<EvidenceDetailsDto> getPageDetailsDTO(AppUser owner, Pageable pageable) {

      Page<Evidence> ev = repo.findByOwner(owner, pageable);
        return ev.map(evidence -> new EvidenceDetailsDto(
                evidence.getId(), getOwnerDTO(evidence.getOwner()), evidence.getTitle(),

                evidence.getDescription(),
                evidence.getCapturedAt(),
                evidence.getCaptureLatlon(),
                evidence.getCaptureAccuracyM(),
                evidence.getStatus(),
                evidence.getOriginalSha256(),
                evidence.getOriginalSizeB(),
                evidence.getOriginalKey(),
                evidence.getDerivativeKey(),
                evidence.getThumbnailKey(),
                evidence.getRedactedKey(),
                evidence.getRedactedSize(),
                evidence.getLegalHold(),
                evidence.getCreatedAt(),
                evidence.getUpdatedAt(),
                evidence.getExifDateOriginal(),
                evidence.getTzOffsetMinutes(),
                evidence.getCaptureAltitudeM(),
                evidence.getCaptureHeadingDeg(),
                evidence.getCameraMake(),
                evidence.getCameraModel(),
                evidence.getLensModel(),
                evidence.getSoftware(),
                evidence.getWidthPx(),
                evidence.getHeightPx(),
                evidence.getOrientationDeg(),
                evidence.getContainer(),
                evidence.getVideoCodec(),
                evidence.getAudioCodec(),
                evidence.getDurationMs(),
                evidence.getVideoFps(),
                evidence.getVideoRotationDeg(),
                authenticityAssessmentOf(evidence)));

    }
    public EvidenceDetailsDto getDetailsDTO(Evidence ev) {
        return new EvidenceDetailsDto(ev.getId(), getOwnerDTO(ev.getOwner()), ev.getTitle(),

                ev.getDescription(),
                ev.getCapturedAt(),
                ev.getCaptureLatlon(),
                ev.getCaptureAccuracyM(),
                EvidenceStatus.RECEIVED,
                ev.getOriginalSha256(),
                ev.getOriginalSizeB(),
                ev.getOriginalKey(),
                ev.getDerivativeKey(),
                ev.getThumbnailKey(),
                ev.getRedactedKey(),
                ev.getRedactedSize(),
                ev.getLegalHold(),
                ev.getCreatedAt(),
                ev.getUpdatedAt(),
                ev.getExifDateOriginal(),
                ev.getTzOffsetMinutes(),
                ev.getCaptureAltitudeM(),
                ev.getCaptureHeadingDeg(),
                ev.getCameraMake(),
                ev.getCameraModel(),
                ev.getLensModel(),
                ev.getSoftware(),
                ev.getWidthPx(),
                ev.getHeightPx(),
                ev.getOrientationDeg(),
                ev.getContainer(),
                ev.getVideoCodec(),
                ev.getAudioCodec(),
                ev.getDurationMs(),
                ev.getVideoFps(),
                ev.getVideoRotationDeg(),
                authenticityAssessmentOf(ev));
    }

    public OwnerDto getOwnerDTO(AppUser owner) {
        return new OwnerDto(owner.getId(), owner.getEmail(), owner.getDisplayName(), owner.getRole());
    }
  
    public String getKey(Optional<Evidence> item) {
        return item.map(Evidence::getOriginalKey).orElse(null);
    }

    private static AuthenticityAssessment authenticityAssessmentOf(Evidence evidence) {
        return new AuthenticityAssessment(
                evidence.getProvenanceStatus(),
                evidence.getMetadataIntegrity(),
                evidence.getSyntheticMediaRisk(),
                evidence.getManipulationSignals(),
                evidence.getAssessmentSummary()
        );
    }
}
