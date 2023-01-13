package id.kawahedukasi.batch5.controller;

import id.kawahedukasi.batch5.model.Item;

import id.kawahedukasi.batch5.model.UploadItemRequest;
import id.kawahedukasi.batch5.service.ItemService;
import io.vertx.core.json.Json;
import io.vertx.core.json.JsonObject;
import net.sf.jasperreports.engine.JRException;
import org.hibernate.PropertyValueException;
import org.jboss.resteasy.annotations.providers.multipart.MultipartForm;
import org.postgresql.util.PSQLException;

import javax.inject.Inject;
import javax.management.Query;
import javax.transaction.Transactional;
import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.io.IOException;
import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Path("/item")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class ItemController {

    @Inject
    ItemService itemService;

    @GET
    @Path("/report")
    @Produces("application/pdf")
    public Response createReport() throws JRException {
        return itemService.exportJasper();
    }

    @POST
    @Path("/upload")
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    public Response upload(@MultipartForm UploadItemRequest request) throws IOException {
        return itemService.upload(request);
    }

    @GET
    @Path("/download")
    @Produces("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")
    public Response download() throws IOException {
        return itemService.download();
    }

    @POST
    public Response create(JsonObject request) {
        return itemService.create(request);
    }

    @GET
    public Response listItems() {
        return itemService.listItems();
    }

    @GET
    @Path("/search")
    public Response searchItems(@QueryParam("q") String query,
                                @QueryParam("min") Double min,
                                @QueryParam("max") Double max) {
        return itemService.searchItems(query, min, max);
    }

    @GET
    @Path("/{id}")
    public Response getById(@PathParam("id") Long id) {
        return itemService.getById(id);
    }

    @GET
    @Path("/type/{type}")
    public Response getByType(@PathParam("type") String type) {
        return itemService.getByType(type);
    }

    @PUT
    @Path("/{id}")
    public Response update(@PathParam("id") Long id, JsonObject request) {
        return itemService.update(id, request);
    }

    @DELETE
    @Path("/{id}")
    public Response delete(@PathParam("id") Long id){
        return itemService.delete(id);
    }

}
