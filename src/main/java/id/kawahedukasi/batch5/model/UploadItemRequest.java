package id.kawahedukasi.batch5.model;

import javax.ws.rs.FormParam;

public class UploadItemRequest {

    @FormParam("file")
    public byte[] file;
}
