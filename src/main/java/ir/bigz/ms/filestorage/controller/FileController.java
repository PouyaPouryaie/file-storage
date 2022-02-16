package ir.bigz.ms.filestorage.controller;

import ir.bigz.ms.filestorage.model.UploadFileResponse;
import ir.bigz.ms.filestorage.service.FileStorageService;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.io.FileUtils;
import org.apache.tika.Tika;
import org.apache.tika.config.TikaConfig;
import org.apache.tika.exception.TikaException;
import org.apache.tika.io.TikaInputStream;
import org.apache.tika.metadata.Metadata;
import org.apache.tomcat.util.codec.binary.Base64;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api")
@Slf4j
public class FileController {

    private final FileStorageService fileStorageService;
    public final static String IF_NONE_MATCH = "if-none-match";
    public final static String ETAG = "etag";
    private final Tika tika = new Tika();

    @Autowired
    public FileController(FileStorageService fileStorageService) {
        this.fileStorageService = fileStorageService;
    }

    @PostMapping("/file/valid")
    public ResponseEntity<?> checkValidFile(@RequestPart MultipartFile file) {
        boolean result = fileStorageService.checkFileValid(file);

        if (result)
            return new ResponseEntity<>("validation success ", HttpStatus.ACCEPTED);
        else {
            return new ResponseEntity<>("validation failed", HttpStatus.EXPECTATION_FAILED);
        }

    }

    @PostMapping("/uploadFile")
    public UploadFileResponse uploadFile(@RequestParam("file") MultipartFile file) {
        String fileName = fileStorageService.storeFile(file);

        String fileDownloadUri = ServletUriComponentsBuilder.fromCurrentContextPath()
                .path("/api/downloadFile/")
                .path(fileName)
                .toUriString();

        return new UploadFileResponse(fileName, fileDownloadUri,
                file.getContentType(), file.getSize());
    }

    @SneakyThrows
    @GetMapping("/downloadFile/file/{category}/{fileName:.+}")
    public ResponseEntity<?> downloadFileFromResourcesOrStaticFolder(@PathVariable(name = "category") String category,
                                                                     @PathVariable(name = "fileName") String fileName) {

        byte[] bytes = fileStorageService.loadFileAsByteFromResources(category, fileName);
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
            FileSystemResource fileSystemResource = new FileSystemResource(fileStorageService.getFilePath(fileName));
           if (fileSystemResource.exists()) {
               return ResponseEntity.ok()
                       .contentType(MediaType.parseMediaType(tika.detect(fileSystemResource.getFile())))
                       .body(fileSystemResource);
           } else {
               return new ResponseEntity<>(HttpStatus.NOT_FOUND);
           }
        }
    }

    @GetMapping("/downloadFile/{fileName:.+}")
    public ResponseEntity<Resource> downloadFile(@PathVariable String fileName, HttpServletRequest request) {
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
        if (contentType == null) {
            contentType = "application/octet-stream";
        }

        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(contentType))
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + resource.getFilename() + "\"")
                .body(resource);
    }

    @GetMapping("/download-pdf-file/{fileName:.+}")
    public void downloadFileAsFile(@PathVariable String fileName, HttpServletRequest request,
                                   HttpServletResponse response) throws IOException {
        String contentType = null;
        File file = null;

        // Load file as File
        file = fileStorageService.downloadFileAsPDF(fileName);

        // Try to determine file's content type
        contentType = request.getServletContext().getMimeType(file.getAbsolutePath());

        // Fallback to the default content type if type could not be determined
        ServletOutputStream os = response.getOutputStream();
        if (!contentType.equals("application/pdf")) {
            os.write("فایل مورد نظر پی دی اف نمیباشد".getBytes());
        } else {
            byte[] bytes = FileUtils.readFileToByteArray(file);
            response.setContentType(contentType);
            response.addHeader(HttpHeaders.CONTENT_TYPE, contentType);
            response.addHeader("Content-Disposition", "attachment; filename=" + fileName + ".pdf");
            os.write(bytes);
        }
        os.flush();
        os.close();
    }

    @GetMapping(path = {"/download-file-object", "/download-file-object/{filename}"},
            produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> downloadFileAsObject(@PathVariable(name = "filename", required = false) String name,
                                                  HttpServletRequest request) throws IOException {
        try {
            final URL resource = getClass().getClassLoader().getResource("images/");
            final File imagesDir = new File(resource.getFile());
            Map<String, Object> data = new HashMap<>();
            if ((name == null || "".equals(name.trim()) || "null".equals(name.trim()))) {
                final String[] list = imagesDir.list();
                if (list == null || list.length == 0)
                    throw new IOException("file not found");
                List<String> fileNames = Arrays.stream(list)
                        .sorted(String::compareTo)
                        .collect(Collectors.toList());
                data.put("imageNames", fileNames);
                final File firstFile = new File(imagesDir, fileNames.get(0));
                data.put("image", Base64.encodeBase64String(FileUtils.readFileToByteArray(firstFile)));
            } else {
                data.put("image", Base64.encodeBase64String(FileUtils.readFileToByteArray(new File(imagesDir, name))));
            }

            ResponseEntity<Map<String, Object>> response = ResponseEntity.ok()
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(data);

            return getAndCacheResponseBody(request, response);

        } catch (Exception e) {
            throw new IOException(e.getMessage());
        }
    }

    /**
     * if request has if_none_match in header, check etag with it and if these are same return not_modified in response and
     * dont send duplicate data for same request.
     *
     * @param request
     * @param response
     * @return ResponseEntity
     */
    protected ResponseEntity<?> getAndCacheResponseBody(HttpServletRequest request, ResponseEntity<?> response) {

        final String etag;
        byte[] imgBytes = Objects.requireNonNull(response.getBody()).toString().getBytes();
        etag = DigestUtils.md5Hex(imgBytes);
        Optional<String> noneMatchHeader = Optional.ofNullable(request.getHeader(IF_NONE_MATCH));

        if (noneMatchHeader.isPresent() && noneMatchHeader.get().equals(etag)) {
            return ResponseEntity.status(HttpStatus.NOT_MODIFIED).build();
        }

        return ResponseEntity.ok()
                .header(ETAG, etag)
                .headers(response.getHeaders())
                .contentType(Objects.requireNonNull(response.getHeaders().getContentType()))
                .body(response.getBody());
    }
}
