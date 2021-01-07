package ir.bigz.ms.filestorage.service;

import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Service
public interface FileStorageService {

    boolean checkFileValid(MultipartFile file);

    public String storeFile(MultipartFile file);

    public Resource loadFileAsResource(String fileName);
}
