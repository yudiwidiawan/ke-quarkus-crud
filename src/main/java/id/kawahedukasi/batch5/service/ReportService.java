package id.kawahedukasi.batch5.service;

import net.sf.jasperreports.engine.JRException;

import javax.ws.rs.core.Response;

public interface ReportService {
    public abstract Response exportJasper() throws JRException;
}
