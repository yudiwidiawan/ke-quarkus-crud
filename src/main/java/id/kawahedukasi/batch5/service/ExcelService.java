package id.kawahedukasi.batch5.service;

import id.kawahedukasi.batch5.model.UploadItemRequest;

import javax.ws.rs.FormParam;
import javax.ws.rs.core.Response;
import java.io.IOException;

public interface ExcelService {
    public abstract Response upload(UploadItemRequest request) throws IOException;

    public abstract Response download() throws IOException;
}
