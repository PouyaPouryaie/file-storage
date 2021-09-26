package ir.bigz.ms.filestorage.service;

import ir.bigz.ms.filestorage.config.FileStorageProperties;
import ir.bigz.ms.filestorage.exception.FileStorageException;
import ir.bigz.ms.filestorage.exception.MyFileNotFoundException;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;

@Component
public class FileStorageServiceImpl implements FileStorageService {

    private final Path fileStorageLocation;

    public FileStorageServiceImpl(FileStorageProperties fileStorageProperties) {
        this.fileStorageLocation = Paths.get(fileStorageProperties.getUploadDir()).toAbsolutePath().normalize();
    }

    @Override
    public boolean checkFileValid(MultipartFile file) {
        return false;
    }

    @Override
    public String storeFile(MultipartFile file) {

        String fileName = StringUtils.cleanPath(file.getOriginalFilename());

        try {
            if(fileName.contains("..")) {
                throw new FileStorageException("Sorry! Filename contains invalid path sequence " + fileName);
            }

            Path targetLocation = this.fileStorageLocation.resolve(fileName);
            Files.copy(file.getInputStream(), targetLocation, StandardCopyOption.REPLACE_EXISTING);

            return fileName;
        }catch (IOException ex){
            throw new FileStorageException("Could not store file " + fileName + ". Please try again!", ex);
        }
    }

    @Override
    public Resource loadFileAsResource(String fileName) {
        try {
            Path filePath = getFilePath(fileName);
            Resource resource = new UrlResource(filePath.toUri());
            if(resource.exists()) {
                return resource;
            } else {
                throw new MyFileNotFoundException("File not found " + fileName);
            }
        } catch (MalformedURLException ex) {
            throw new MyFileNotFoundException("File not found " + fileName, ex);
        }
    }

    @Override
    public byte[] loadFileAsByte(String category, String fileName){
        try(InputStream resourceAsStream = getClass().getClassLoader()
        .getResourceAsStream("file" + File.separator + category + File.separator + fileName + ".png")){
            if(resourceAsStream.available() > 0){
                return IOUtils.toByteArray(resourceAsStream);
            }
        }catch(IOException ex){
            throw new MyFileNotFoundException("File not found " + fileName);
        }
        return new byte[0];
    }

    @Override
    public File downloadFileAsPDF(String fileName) {
        Path filePath = null;
        File file = null;

        filePath = getFilePath(fileName);
        file = new File(String.valueOf(Paths.get(filePath.toUri())));

        if (file.length() > 0 ){
            return file;
        }

        throw new MyFileNotFoundException("File not found " + fileName);
    }

    private Path getFilePath(String fileName){
        return this.fileStorageLocation.resolve(fileName).normalize();
    }
}
