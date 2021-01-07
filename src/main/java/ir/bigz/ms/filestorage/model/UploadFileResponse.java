package ir.bigz.ms.filestorage.model;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UploadFileResponse {

    private String fileName;
    private String fileDownloadUrl;
    private String fileType;
    private long size;

    public UploadFileResponse(String fileName, String fileDownloadUri, String fileType, long size) {
        this.fileName = fileName;
        this.fileDownloadUrl = fileDownloadUri;
        this.fileType = fileType;
        this.size = size;
    }
}
