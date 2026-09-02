package com.sunrisedental.service;

import com.sunrisedental.dao.IBillingDAO;
import com.sunrisedental.dao.ITreatmentDAO;
import com.sunrisedental.model.Bill;
import com.sunrisedental.model.Treatment;
import com.sunrisedental.service.factory.BillingCalculatorFactory;
import com.sunrisedental.service.strategy.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

/**
 * TDD Test Suite for BillingService, Factory Pattern, and Strategy Pattern.
 */
public class BillingServiceTest {

    private IBillingDAO mockBillingDAO;
    private ITreatmentDAO mockTreatmentDAO;
    private BillingService billingService;

    @BeforeEach
    void setUp() {
        mockBillingDAO = Mockito.mock(IBillingDAO.class);
        mockTreatmentDAO = Mockito.mock(ITreatmentDAO.class);
        billingService = new BillingService(mockBillingDAO, mockTreatmentDAO);

        Treatment rct = new Treatment("TRT-004", "Root Canal Treatment", "Endodontics", 18000.00, 75, "RCT");
        when(mockTreatmentDAO.findByName("Root Canal Treatment")).thenReturn(rct);
    }

    @Test
    @DisplayName("Factory Pattern: Should return appropriate BillingStrategy instances")
    void testBillingCalculatorFactory() {
        BillingStrategy standard = BillingCalculatorFactory.getStrategy("STANDARD");
        assertTrue(standard instanceof StandardBillingStrategy);
        assertEquals(0.0, standard.getDiscountRate(), 0.001);

        BillingStrategy senior = BillingCalculatorFactory.getStrategy("SENIOR_CITIZEN");
        assertTrue(senior instanceof SeniorCitizenBillingStrategy);
        assertEquals(0.10, senior.getDiscountRate(), 0.001);

        BillingStrategy insurance = BillingCalculatorFactory.getStrategy("INSURANCE");
        assertTrue(insurance instanceof InsuranceCoveredBillingStrategy);
        assertEquals(0.15, insurance.getDiscountRate(), 0.001);

        BillingStrategy promo = BillingCalculatorFactory.getStrategy("PROMOTIONAL");
        assertTrue(promo instanceof PromotionalBillingStrategy);
        assertEquals(0.05, promo.getDiscountRate(), 0.001);
    }

    @Test
    @DisplayName("Standard Strategy: Should calculate total without discount")
    void testStandardCalculation() {
        Bill bill = billingService.calculateBill(
                "APT-2026-0001", "Kamal Silva", "Dr. Sarah Perera",
                "Root Canal Treatment", 2500.00, 500.00,
                "STANDARD", "CASH"
        );

        // Subtotal = 2500 + 18000 + 500 = 21000
        assertEquals(21000.00, bill.getTotalAmount(), 0.001);
        assertEquals(0.00, bill.getDiscountAmount(), 0.001);
        assertEquals("STANDARD", bill.getDiscountType());
    }

    @Test
    @DisplayName("Senior Citizen Strategy: Should apply 10% discount on total subtotal")
    void testSeniorCitizenCalculation() {
        Bill bill = billingService.calculateBill(
                "APT-2026-0001", "Kamal Silva", "Dr. Sarah Perera",
                "Root Canal Treatment", 2500.00, 500.00,
                "SENIOR_CITIZEN", "CARD"
        );

        // Subtotal = 21000. 10% Discount = 2100. Total = 18900
        assertEquals(2100.00, bill.getDiscountAmount(), 0.001);
        assertEquals(18900.00, bill.getTotalAmount(), 0.001);
        assertEquals(0.10, bill.getDiscountRate(), 0.001);
    }

    @Test
    @DisplayName("Insurance Strategy: Should apply 15% discount on total subtotal")
    void testInsuranceCalculation() {
        Bill bill = billingService.calculateBill(
                "APT-2026-0001", "Kamal Silva", "Dr. Sarah Perera",
                "Root Canal Treatment", 2500.00, 500.00,
                "INSURANCE", "ONLINE_TRANSFER"
        );

        // Subtotal = 21000. 15% Discount = 3150. Total = 17850
        assertEquals(3150.00, bill.getDiscountAmount(), 0.001);
        assertEquals(17850.00, bill.getTotalAmount(), 0.001);
    }

    @Test
    @DisplayName("Should successfully persist generated bill via DAO")
    void testGenerateAndSaveBill() {
        when(mockBillingDAO.createBill(any(Bill.class))).thenReturn(true);

        Bill bill = new Bill();
        bill.setAppointmentNumber("APT-2026-0002");
        bill.setPatientName("Nimali Perera");
        bill.setConsultationFee(2500.00);
        bill.setTreatmentFee(5000.00);
        bill.setExtraCharges(0.00);
        bill.setDiscountType("STANDARD");

        Bill saved = billingService.generateAndSaveBill(bill);
        assertNotNull(saved);
        assertEquals(7500.00, saved.getTotalAmount(), 0.001);
    }
}
