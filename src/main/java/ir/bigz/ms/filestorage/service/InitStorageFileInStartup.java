package ir.bigz.ms.filestorage.service;

import ir.bigz.ms.filestorage.config.FileStorageProperties;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

@Component
public class InitStorageFileInStartup implements CommandLineRunner {

    private final FileStorageProperties storageProperties;

    public InitStorageFileInStartup(FileStorageProperties storageProperties) {
        this.storageProperties = storageProperties;
    }

    @Override
    public void run(String... args) throws Exception {
        Path normalize = Paths.get(storageProperties.getUploadDir()).toAbsolutePath().normalize();
        Files.createDirectories(normalize);
    }
}
