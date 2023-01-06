package id.kawahedukasi.batch5.controller;

import id.kawahedukasi.batch5.model.Item;

import io.vertx.core.json.Json;
import io.vertx.core.json.JsonObject;
import org.hibernate.PropertyValueException;
import org.postgresql.util.PSQLException;

import javax.management.Query;
import javax.transaction.Transactional;
import javax.ws.rs.*;
import javax.ws.rs.core.Response;
import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Path("/item")
public class ItemController {

    public boolean isEmptyVal(JsonObject request, String key) {
        return request.containsKey(key);
    }

    public boolean validRequest(JsonObject request, String... keys) {
        for(String key: keys) {
            if(!request.containsKey(key)) return false;
        }
        return true;
    }


    @POST
    @Transactional
    public Response create(JsonObject request) {
        Item item = new Item();

        if(!validRequest(request, "name", "type")) {
            return Response.status(Response.Status.BAD_REQUEST)
                .entity(Map.of("message", "name and type cannot be null."))
                .build();
        }
        String inputName = request.getString("name");
        if(Item.find("UPPER(name) = ?1", inputName.toUpperCase(Locale.ROOT)).list().size() > 0) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(Map.of("message", "Name already used."))
                    .build();
        }

        item.name = inputName;
        item.count = isEmptyVal(request, "count") ? request.getInteger("count") : 0;
        item.price = BigDecimal.valueOf(isEmptyVal(request, "price") ? request.getDouble("price") : 0.0);
        item.type = request.getString("type");
        item.description = request.getString("description");
        if(item.price.compareTo(BigDecimal.ZERO) < 0) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(Map.of("message", "price can't be negative."))
                    .build();
        }
        item.persist();

        return Response.ok().entity(Map.of("message", "Success added a new data.")).build();
    }

    @GET
    public Response listItems() {
        return Response.ok().entity(Item.listAll()).build();
    }

    @GET
    @Path("/search")
    public Response searchItems(@QueryParam("q") String query,
                                @QueryParam("min") Double min,
                                @QueryParam("max") Double max) {
        String findQuery = "";
        if(query != null) {
            findQuery+="UPPER(name) LIKE '%"+query.toUpperCase(Locale.ROOT)+"%'";
            if(min != null && max != null) {
                findQuery += " AND price BETWEEN " + min + " AND " + max;
            } else {
                findQuery += " AND price " + (min != null?">=":"<=") + " " +(min != null?min:max);
            }
            List<Item> items = Item.find(findQuery).list();
            return Response.ok().entity(Map.of("query", findQuery, "data", items)).build();
        }
        return Response.status(Response.Status.BAD_REQUEST)
                .entity(Map.of("message", "keyword unprovided."))
                .build();
    }

    @GET
    @Path("/{id}")
    public Response getById(@PathParam("id") Long id) {
        Item item = Item.findById(id);
        return item != null ? Response.ok().entity(item).build() : Response.status(Response.Status.BAD_REQUEST)
                .entity(Map.of("message", "item not found"))
                .build();
    }

    @GET
    @Path("/type/{type}")
    public Response getByType(@PathParam("type") String type) {
        List<Item> items = Item.find("UPPER(type) = ?1", type.toUpperCase(Locale.ROOT)).list();
        return Response.ok().entity(items).build();
    }

    @PUT
    @Transactional
    @Path("/{id}")
    public Response update(@PathParam("id") Long id, JsonObject request) {
        Item item = Item.findById(id);
        if(!validRequest(request, "name", "type")) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(Map.of("message", "name and type cannot be null."))
                    .build();
        } else if(item == null) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(Map.of("message", "item not found."))
                    .build();
        }
        String inputName = request.getString("name");
        if(Item.find("UPPER(name) = ?1", inputName.toUpperCase(Locale.ROOT)).list().size() > 0) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(Map.of("message", "Name already used."))
                    .build();
        }

        item.name = inputName;
        item.count = request.getInteger("count");
        item.price = BigDecimal.valueOf(request.getDouble("price"));
        item.type = request.getString("type");
        item.description = request.getString("description");
        if(item.price.compareTo(BigDecimal.ZERO) < 0) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(Map.of("message", "price can't be negative."))
                    .build();
        }
        item.persist();

        return Response.ok().entity(Map.of("message", "Success modified a data.", "data", item)).build();
    }

    @DELETE
    @Transactional
    @Path("/{id}")
    public Response delete(@PathParam("id") Long id){
        Item item = Item.findById(id);
        if(item != null) {
            Item.deleteById(id);
            return Response.ok().entity(Map.of("message", "Success deleted a data.")).build();
        } else{
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(Map.of("message", "item not found."))
                    .build();
        }
    }
}
