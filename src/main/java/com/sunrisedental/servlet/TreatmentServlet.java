package com.sunrisedental.servlet;

import com.sunrisedental.dao.ITreatmentDAO;
import com.sunrisedental.dao.TreatmentDAOImpl;
import com.sunrisedental.model.Treatment;
import com.sunrisedental.util.JsonUtil;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;

/**
 * Web Service Servlet exposing dental treatment catalog and base price lookups.
 */
@WebServlet(name = "TreatmentServlet", urlPatterns = {"/api/treatments/*"})
public class TreatmentServlet extends HttpServlet {

    private ITreatmentDAO treatmentDAO;

    @Override
    public void init() throws ServletException {
        this.treatmentDAO = new TreatmentDAOImpl();
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        String pathInfo = req.getPathInfo();
        if (pathInfo == null || "/".equals(pathInfo)) {
            List<Treatment> treatments = treatmentDAO.findAll();
            JsonUtil.sendSuccess(resp, "Treatments retrieved", treatments);
        } else {
            String code = pathInfo.replaceFirst("/", "");
            Treatment t = treatmentDAO.findByCode(code);
            if (t == null) {
                t = treatmentDAO.findByName(code);
            }
            if (t != null) {
                JsonUtil.sendSuccess(resp, "Treatment details", t);
            } else {
                JsonUtil.sendError(resp, HttpServletResponse.SC_NOT_FOUND, "Treatment not found: " + code);
            }
        }
    }
}
