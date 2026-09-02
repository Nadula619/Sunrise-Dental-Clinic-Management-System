package com.sunrisedental.servlet;

import com.sunrisedental.dao.DentistDAOImpl;
import com.sunrisedental.dao.IDentistDAO;
import com.sunrisedental.model.Dentist;
import com.sunrisedental.util.JsonUtil;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.BufferedReader;
import java.io.IOException;
import java.util.List;

/**
 * Web Service Servlet exposing dental practitioners and consultation schedules.
 */
@WebServlet(name = "DentistServlet", urlPatterns = {"/api/dentists/*"})
public class DentistServlet extends HttpServlet {

    private IDentistDAO dentistDAO;

    @Override
    public void init() throws ServletException {
        this.dentistDAO = new DentistDAOImpl();
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        String pathInfo = req.getPathInfo();
        if (pathInfo == null || "/".equals(pathInfo)) {
            List<Dentist> dentists = dentistDAO.findAll();
            JsonUtil.sendSuccess(resp, "Dentists retrieved", dentists);
        } else {
            String name = pathInfo.replaceFirst("/", "");
            Dentist d = dentistDAO.findByName(name);
            if (d != null) {
                JsonUtil.sendSuccess(resp, "Dentist details", d);
            } else {
                JsonUtil.sendError(resp, HttpServletResponse.SC_NOT_FOUND, "Dentist not found: " + name);
            }
        }
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        StringBuilder sb = new StringBuilder();
        try (BufferedReader reader = req.getReader()) {
            String line;
            while ((line = reader.readLine()) != null) {
                sb.append(line);
            }
        }

        Dentist dentist = JsonUtil.fromJson(sb.toString(), Dentist.class);
        if (dentist == null || dentist.getName() == null || dentist.getName().trim().isEmpty()) {
            JsonUtil.sendError(resp, HttpServletResponse.SC_BAD_REQUEST, "Dentist name is required.");
            return;
        }

        boolean saved = dentistDAO.save(dentist);
        if (saved) {
            JsonUtil.sendSuccess(resp, "Doctor / Dentist profile saved successfully!", dentist);
        } else {
            JsonUtil.sendError(resp, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Failed to save dentist profile.");
        }
    }
}
