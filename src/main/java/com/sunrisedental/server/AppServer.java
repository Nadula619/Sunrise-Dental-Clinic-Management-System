package com.sunrisedental.server;

import com.sunrisedental.config.AppConfig;
import com.sunrisedental.servlet.*;
import com.sunrisedental.servlet.filter.CorsFilter;
import com.sunrisedental.util.DatabaseSeeder;
import org.apache.catalina.Context;
import org.apache.catalina.servlets.DefaultServlet;
import org.apache.catalina.startup.Tomcat;
import org.apache.tomcat.util.descriptor.web.FilterDef;
import org.apache.tomcat.util.descriptor.web.FilterMap;

import java.io.File;
import java.util.logging.Logger;

/**
 * Embedded Tomcat Launcher for running the Sunrise Dental Clinic Web Application directly.
 */
public class AppServer {
    private static final Logger LOGGER = Logger.getLogger(AppServer.class.getName());

    public static void main(String[] args) throws Exception {
        int port = AppConfig.getServerPort();

        // 1. Seed initial sample data
        try {
            DatabaseSeeder.seed();
        } catch (Exception e) {
            LOGGER.warning("Seeding warning: " + e.getMessage());
        }

        String webappDirLocation = "src/main/webapp";
        File docBase = new File(webappDirLocation);
        if (!docBase.exists()) {
            docBase = new File("webapp");
        }

        Tomcat tomcat = new Tomcat();
        tomcat.setPort(port);
        tomcat.getConnector(); // Initialize HTTP connector

        String baseDir = new File("target/tomcat").getAbsolutePath();
        tomcat.setBaseDir(baseDir);

        Context context = tomcat.addContext("", docBase.getAbsolutePath());
        // Set parent class loader so context has full access to compiled classes
        context.setParentClassLoader(AppServer.class.getClassLoader());

        // Default Servlet for static resources (HTML, CSS, JS, Images)
        Tomcat.addServlet(context, "default", new DefaultServlet());
        context.addServletMappingDecoded("/", "default");

        // REST Web Service Servlets
        Tomcat.addServlet(context, "AuthServlet", new AuthServlet());
        context.addServletMappingDecoded("/api/auth/*", "AuthServlet");

        Tomcat.addServlet(context, "AppointmentServlet", new AppointmentServlet());
        context.addServletMappingDecoded("/api/appointments/*", "AppointmentServlet");

        Tomcat.addServlet(context, "BillingServlet", new BillingServlet());
        context.addServletMappingDecoded("/api/billing/*", "BillingServlet");

        Tomcat.addServlet(context, "ReportServlet", new ReportServlet());
        context.addServletMappingDecoded("/api/reports/*", "ReportServlet");

        Tomcat.addServlet(context, "TreatmentServlet", new TreatmentServlet());
        context.addServletMappingDecoded("/api/treatments/*", "TreatmentServlet");

        Tomcat.addServlet(context, "DentistServlet", new DentistServlet());
        context.addServletMappingDecoded("/api/dentists/*", "DentistServlet");

        // Register CORS Filter using class instance wrapper
        FilterDef corsFilterDef = new FilterDef();
        corsFilterDef.setFilterName("CorsFilter");
        corsFilterDef.setFilterClass(CorsFilter.class.getName());
        context.addFilterDef(corsFilterDef);

        FilterMap corsFilterMap = new FilterMap();
        corsFilterMap.setFilterName("CorsFilter");
        corsFilterMap.addURLPattern("/*");
        context.addFilterMap(corsFilterMap);

        LOGGER.info("==================================================================");
        LOGGER.info("  SUNRISE DENTAL CLINIC MANAGEMENT SYSTEM STARTED                 ");
        LOGGER.info("  Web URL: http://localhost:" + port + "/login.html                ");
        LOGGER.info("  Default Admin Login: admin / admin123                           ");
        LOGGER.info("  Default Receptionist Login: receptionist / rec123               ");
        LOGGER.info("  Default Dentist Login: dr.roshan / doc123                       ");
        LOGGER.info("==================================================================");

        tomcat.start();
        tomcat.getServer().await();
    }
}
