package com.sunrisedental.servlet;

import com.sunrisedental.model.ReportSummary;
import com.sunrisedental.service.ReportService;
import com.sunrisedental.util.JsonUtil;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * Web Service Servlet providing aggregate analytics, doctor workload reports, and financial summaries.
 */
@WebServlet(name = "ReportServlet", urlPatterns = {"/api/reports/*"})
public class ReportServlet extends HttpServlet {

    private ReportService reportService;

    @Override
    public void init() throws ServletException {
        this.reportService = new ReportService();
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        ReportSummary summary = reportService.generateSummary();
        JsonUtil.sendSuccess(resp, "Management reports summary generated", summary);
    }
}
