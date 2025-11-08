package org.rights.locker.Repos;

import org.rights.locker.DTOs.MediaMetadata;

public interface MetadataService {
    MediaMetadata extractFromUrl(String url) throws Exception;
}
