package com.sunrisedental.util;

import com.sunrisedental.dao.*;
import com.sunrisedental.model.*;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.logging.Logger;

/**
 * Utility to populate initial demo data for quick testing and demonstration.
 */
public class DatabaseSeeder {
    private static final Logger LOGGER = Logger.getLogger(DatabaseSeeder.class.getName());

    public static void seed() {
        LOGGER.info("Starting database seeding for Sunrise Dental Clinic...");

        IUserDAO userDAO = new UserDAOImpl();
        IAppointmentDAO appointmentDAO = new AppointmentDAOImpl();
        IBillingDAO billingDAO = new BillingDAOImpl();
        ITreatmentDAO treatmentDAO = new TreatmentDAOImpl();
        IDentistDAO dentistDAO = new DentistDAOImpl();

        // 1. Seed Users
        if (userDAO.findByUsername("admin") == null) {
            userDAO.createUser(new User("admin", PasswordUtil.hashPassword("admin123"), "Dr. Sarah Perera (Admin)", "ADMIN", "admin@sunrisedental.lk"));
        }
        if (userDAO.findByUsername("receptionist") == null) {
            userDAO.createUser(new User("receptionist", PasswordUtil.hashPassword("rec123"), "Anoma Fernando", "RECEPTIONIST", "anoma@sunrisedental.lk"));
        }
        if (userDAO.findByUsername("dr.roshan") == null) {
            userDAO.createUser(new User("dr.roshan", PasswordUtil.hashPassword("doc123"), "Dr. Roshan Fernando", "DENTIST", "roshan@sunrisedental.lk"));
        }

        // 2. Seed Appointments if empty
        if (appointmentDAO.countAppointments() == 0) {
            SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd");
            Calendar cal = Calendar.getInstance();

            // Today
            String todayStr = df.format(cal.getTime());

            // Tomorrow
            cal.add(Calendar.DAY_OF_YEAR, 1);
            String tomorrowStr = df.format(cal.getTime());

            // In 2 days
            cal.add(Calendar.DAY_OF_YEAR, 1);
            String dayAfterStr = df.format(cal.getTime());

            Appointment a1 = new Appointment(
                    "APT-2026-0001", "Kamal Silva", "45 Galle Road, Colombo 03",
                    "0771234567", "Dr. Sarah Perera", "Routine Dental Checkup & Consultation",
                    todayStr, "09:30", "Patient reports mild sensitivity"
            );
            a1.setStatus("COMPLETED");
            appointmentDAO.createAppointment(a1);

            Appointment a2 = new Appointment(
                    "APT-2026-0002", "Nimali Perera", "12 Kandy Road, Kiribathgoda",
                    "0719876543", "Dr. Roshan Fernando", "Teeth Cleaning & Polishing (Scaling)",
                    todayStr, "11:00", "Six-month routine hygiene cleaning"
            );
            a2.setStatus("IN_PROGRESS");
            appointmentDAO.createAppointment(a2);

            Appointment a3 = new Appointment(
                    "APT-2026-0003", "Sunil Jayawardena", "78 High Level Rd, Nugegoda",
                    "0755551234", "Dr. Anusha Jayawardena", "Root Canal Treatment (RCT)",
                    tomorrowStr, "14:00", "Severe lower molar ache during night"
            );
            a3.setStatus("SCHEDULED");
            appointmentDAO.createAppointment(a3);

            Appointment a4 = new Appointment(
                    "APT-2026-0004", "Dilani Wickramasinghe", "33 Havelock Rd, Colombo 05",
                    "0784433221", "Dr. Dinesh Wickramasinghe", "Composite Dental Filling",
                    dayAfterStr, "15:30", "Upper premolar cavity filling"
            );
            a4.setStatus("SCHEDULED");
            appointmentDAO.createAppointment(a4);

            // 3. Seed Sample Bills for completed appointments
            Bill b1 = new Bill(
                    "INV-2026-0001", "APT-2026-0001", "Kamal Silva",
                    "Dr. Sarah Perera", "Routine Dental Checkup & Consultation",
                    2500.00, 2500.00, 500.00, 0.10, "SENIOR_CITIZEN", "CASH"
            );
            billingDAO.createBill(b1);

            Bill b2 = new Bill(
                    "INV-2026-0002", "APT-2026-0002", "Nimali Perera",
                    "Dr. Roshan Fernando", "Teeth Cleaning & Polishing (Scaling)",
                    3000.00, 4500.00, 0.00, 0.0, "STANDARD", "CARD"
            );
            billingDAO.createBill(b2);
        }

        LOGGER.info("Database seeding complete.");
    }
}
