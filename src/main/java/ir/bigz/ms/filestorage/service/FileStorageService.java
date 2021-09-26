package ir.bigz.ms.filestorage.service;

import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.nio.file.Path;

@Service
public interface FileStorageService {

    boolean checkFileValid(MultipartFile file);

    String storeFile(MultipartFile file);

    Resource loadFileAsResource(String fileName);

    byte[] loadFileAsByteFromResources(String category, String fileName);

    File downloadFileAsPDF(String fileName);

    Path getFilePath(String fileName);
}
