package id.kawahedukasi.batch5.service;

import id.kawahedukasi.batch5.model.Item;
import id.kawahedukasi.batch5.model.UploadItemRequest;
import io.quarkus.narayana.jta.runtime.TransactionConfiguration;
import io.quarkus.scheduler.Scheduled;
import io.vertx.core.json.JsonObject;
import net.sf.jasperreports.engine.*;
import net.sf.jasperreports.engine.data.JRBeanCollectionDataSource;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import javax.enterprise.context.ApplicationScoped;
import javax.transaction.Transactional;
import javax.ws.rs.FormParam;
import javax.ws.rs.PathParam;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Response;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.math.BigDecimal;
import java.time.format.DateTimeFormatter;
import java.util.*;

@ApplicationScoped
public class ItemService implements ExcelService, ReportService, RoutineService {

    public boolean isEmptyVal(JsonObject request, String key) {
        return request.containsKey(key);
    }

    public boolean validRequest(JsonObject request, String... keys) {
        for(String key: keys) {
            if(!request.containsKey(key)) return false;
        }
        return true;
    }

    @Transactional
    @TransactionConfiguration(timeout = 30)
    public void persistListItem(List<Item> itemList){
        Item.persist(itemList);
    }

    @Override
    public Response upload(UploadItemRequest request) throws IOException {
        List<Item> itemList = new ArrayList<>();

        ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(request.file);
        XSSFWorkbook workbook = new XSSFWorkbook(byteArrayInputStream);
        XSSFSheet sheet = workbook.getSheetAt(0);

        //remove header
        sheet.removeRow(sheet.getRow(0));

        //looping setiap baris
        Iterator<Row> rowIterator = sheet.rowIterator();
        while (rowIterator.hasNext()){
            Item item = new Item();
            Row row = rowIterator.next();
            item.name = row.getCell(0).getStringCellValue();
            item.count = Double.valueOf(row.getCell(1).getNumericCellValue()).intValue();
            item.price = BigDecimal.valueOf(Double.valueOf(row.getCell(2).getNumericCellValue()));
            item.type = row.getCell(3).getStringCellValue();
            item.description = row.getCell(4).getStringCellValue();
            itemList.add(item);
        }

        persistListItem(itemList);

        return Response.ok().build();
    }

    @Override
    public Response download() throws IOException {
        List<Item> itemList = Item.listAll();
        XSSFWorkbook workbook = new XSSFWorkbook();
        XSSFSheet sheet = workbook.createSheet("list-item");

        //set header
        int rownum = 0;
        Row row = sheet.createRow(rownum++);
        row.createCell(0).setCellValue("id");
        row.createCell(1).setCellValue("name");
        row.createCell(2).setCellValue("count");
        row.createCell(3).setCellValue("price");
        row.createCell(4).setCellValue("type");
        row.createCell(5).setCellValue("description");
        row.createCell(6).setCellValue("created_at");
        row.createCell(7).setCellValue("updated_at");

        for(Item item : itemList){
            row = sheet.createRow(rownum++);
            row.createCell(0).setCellValue(item.id);
            row.createCell(1).setCellValue(item.name);
            row.createCell(2).setCellValue(item.count);
            row.createCell(3).setCellValue(item.price.doubleValue());
            row.createCell(4).setCellValue(item.type);
            row.createCell(5).setCellValue(item.description);
            row.createCell(6).setCellValue(item.createdAt.format(DateTimeFormatter.ofPattern("dd-MM-yyyy hh:mm:ss")));
            row.createCell(7).setCellValue(item.updatedAt.format(DateTimeFormatter.ofPattern("dd-MM-yyyy hh:mm:ss")));
        }
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        workbook.write(outputStream);

        return Response.ok()
                .type("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")
                .header("Content-Disposition", "attachment; filename=\"item_list_excel.xlsx\"")
                .entity(outputStream.toByteArray()).build();
    }

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

    public Response listItems() {
        return Response.ok().entity(Item.listAll()).build();
    }

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

    public Response getById(@PathParam("id") Long id) {
        Item item = Item.findById(id);
        return item != null ? Response.ok().entity(item).build() : Response.status(Response.Status.BAD_REQUEST)
                .entity(Map.of("message", "item not found"))
                .build();
    }

    public Response getByType(@PathParam("type") String type) {
        List<Item> items = Item.find("UPPER(type) = ?1", type.toUpperCase(Locale.ROOT)).list();
        return Response.ok().entity(items).build();
    }

    @Transactional
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

    @Transactional
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

    public Map<String, Object> jasperParameter(){
        Map<String, Object> parameter = new HashMap<>();
        parameter.put("createBy", "Yudi");
        return parameter;
    }
    @Override
    public Response exportJasper() throws JRException {
        File file = new File("src/main/resources/item.jrxml");
        List<Item> itemList = Item.listAll();
        JasperReport jasperReport = JasperCompileManager.compileReport(file.getAbsolutePath());
        JRBeanCollectionDataSource dataSource = new JRBeanCollectionDataSource(itemList);
        JasperPrint jasperPrint = JasperFillManager.fillReport(jasperReport, jasperParameter(), dataSource);
        byte[] jasperResult = JasperExportManager.exportReportToPdf(jasperPrint);
        return Response.ok().type("application/pdf").entity(jasperResult).build();
    }

    @Override
    @Transactional
    @Scheduled(every = "3600s", identity = "delete-item")
    public void deleteItemZeroCount() {
        Item.deleteIfCountZero();
    }
}
