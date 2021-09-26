package ir.bigz.ms.filestorage.controller;

import ir.bigz.ms.filestorage.model.UploadFileResponse;
import ir.bigz.ms.filestorage.service.FileStorageService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.tika.Tika;
import org.apache.tika.config.TikaConfig;
import org.apache.tika.exception.TikaException;
import org.apache.tika.io.TikaInputStream;
import org.apache.tika.metadata.Metadata;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.Collections;
import java.util.HashMap;

@RestController
@RequestMapping("/api")
@Slf4j
public class FileController {

    private final FileStorageService fileStorageService;
    private final Tika tika = new Tika();

    @Autowired
    public FileController(FileStorageService fileStorageService) {
        this.fileStorageService = fileStorageService;
    }

    @PostMapping("/file/valid")
    public ResponseEntity<?> checkValidFile(@RequestPart MultipartFile file){
        boolean result = fileStorageService.checkFileValid(file);

        if(result)
            return new ResponseEntity<>("validation success ", HttpStatus.ACCEPTED);
        else {
            return new ResponseEntity<>("validation failed", HttpStatus.EXPECTATION_FAILED);
        }

    }

    @PostMapping("/uploadFile")
    public UploadFileResponse uploadFile(@RequestParam("file") MultipartFile file){
        String fileName = fileStorageService.storeFile(file);

        String fileDownloadUri = ServletUriComponentsBuilder.fromCurrentContextPath()
                .path("/api/downloadFile/")
                .path(fileName)
                .toUriString();

        return new UploadFileResponse(fileName, fileDownloadUri,
                file.getContentType(), file.getSize());
    }

    @GetMapping("/downladFile/file/{category}/{fileName:.+}")
    public ResponseEntity<?> downloadFileAsByte(@PathVariable(name = "category") String category, @PathVariable(name = "name") String fileName) {
        
        byte[] bytes = fileStorageService.loadFileAsByte(category, fileName);
        if(bytes.length > 1){
            try{
                TikaConfig tc = new TikaConfig();
                Metadata md = new Metadata();
                md.set(Metadata.MIME_TYPE_MAGIC, fileName);
                String mimetype = tc.getDetector().detect(TikaInputStream.get(bytes), md).toString();
                return ResponseEntity.ok().contentType(MediaType.parseMediaType(mimetype)).body(bytes);
            }catch(IOException | TikaException ex){
                return new ResponseEntity<>("not found file", HttpStatus.NOT_FOUND);
            }

        }else{
            FileSystemResource fileSystemResource = new FileSystemResource("file/images/" + (fileName != null ? fileName : "") + ".png");
           if (fileSystemResource.exists()) {
               return new ResponseEntity<>(fileSystemResource, HttpStatus.OK);
           } else {
               return new ResponseEntity<>(HttpStatus.NOT_FOUND);
           }
        }
    }

    @GetMapping("/downloadFile/{fileName:.+}")
    public ResponseEntity<Resource> downloadFile(@PathVariable String fileName, HttpServletRequest request){
        // Load file as Resource
        Resource resource = fileStorageService.loadFileAsResource(fileName);

        // Try to determine file's content type
        String contentType = null;
        try {
            contentType = request.getServletContext().getMimeType(resource.getFile().getAbsolutePath());
        } catch (IOException ex) {
            log.info("Could not determine file type.");
        }

        // Fallback to the default content type if type could not be determined
        if(contentType == null) {
            contentType = "application/octet-stream";
        }

        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(contentType))
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + resource.getFilename() + "\"")
                .body(resource);
    }

    @GetMapping("/download-pdf-file/{fileName:.+}")
    public void downloadFileAsFile(@PathVariable String fileName, HttpServletRequest request, HttpServletResponse response) throws IOException {
        String contentType = null;
        File file = null;

        // Load file as File
        file = fileStorageService.downloadFileAsPDF(fileName);

        // Try to determine file's content type
        contentType = request.getServletContext().getMimeType(file.getAbsolutePath());

        // Fallback to the default content type if type could not be determined
        ServletOutputStream os = response.getOutputStream();
        if(!contentType.equals("application/pdf")) {
            os.write("فایل مورد نظر پی دی اف نمیباشد".getBytes());
        }
        else{
            byte[] bytes = FileUtils.readFileToByteArray(file);
            response.setContentType(contentType);
            response.addHeader(HttpHeaders.CONTENT_TYPE, contentType);
            response.addHeader("Content-Disposition", "attachment; filename=" + fileName + ".pdf");
            os.write(bytes);
        }
        os.flush();
        os.close();
    }
}
