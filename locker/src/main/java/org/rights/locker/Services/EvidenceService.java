package org.rights.locker.Services;


import lombok.RequiredArgsConstructor;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.Point;
import org.rights.locker.Entities.Evidence;
import org.rights.locker.Enums.CustodyEventType;
import org.rights.locker.Enums.EvidenceStatus;
import org.rights.locker.Repos.EvidenceRepo;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;


import java.io.InputStream;
import java.security.MessageDigest;
import java.util.HexFormat;


@Service
@RequiredArgsConstructor
public class EvidenceService {
    private final EvidenceRepo repo;
    private final StorageService storage;
    private final CustodyService custody;
    private final GeometryFactory gf = new GeometryFactory();


    public Evidence upload(MultipartFile file, String title, String description, java.time.Instant capturedAt, Double lat, Double lon, Double accuracy){
        try (InputStream in = file.getInputStream()){
// hash stream (simple: read all — swap for streaming chunked hash later)
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
                    .captureAccuracyM(accuracy)
                    .status(EvidenceStatus.RECEIVED)
                    .originalSha256(sha)
                    .originalSizeB((long) bytes.length)
                    .originalKey(key)
                    .legalHold(false)
                    .build();
            ev = repo.save(ev);
            custody.record(ev, null, CustodyEventType.RECEIVED, "{}");
            return ev;
        } catch (Exception e){
            throw new RuntimeException(e);
        }
    }


    public Page<Evidence> list(EvidenceStatus status, Pageable pageable){
        return (status == null) ? repo.findAll(pageable) : repo.findByStatus(status, pageable);
    }
}