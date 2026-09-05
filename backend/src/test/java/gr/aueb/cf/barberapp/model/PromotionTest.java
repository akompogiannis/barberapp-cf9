package gr.aueb.cf.barberapp.model;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

class PromotionTest {

    private static final LocalDate FROM = LocalDate.of(2026, 6, 1);
    private static final LocalDate TO = LocalDate.of(2026, 8, 31);

    private Promotion promotion;

    @BeforeEach
    void setUp() {
        promotion = new Promotion();
        promotion.setDiscountPercent(20);
        promotion.setValidFrom(FROM);
        promotion.setValidTo(TO);
        promotion.setActive(true);
    }

    @Test
    @DisplayName("applies the discount and rounds to the cent")
    void appliesDiscount() {
        assertThat(promotion.applyTo(new BigDecimal("22.00"))).isEqualByComparingTo("17.60");
        assertThat(promotion.applyTo(new BigDecimal("15.00"))).isEqualByComparingTo("12.00");
    }

    @Test
    @DisplayName("rounds half up, so a customer is never quoted a third of a cent")
    void roundsHalfUp() {
        promotion.setDiscountPercent(33);
        // 10.00 * 0.67 = 6.70 exactly
        assertThat(promotion.applyTo(new BigDecimal("10.00"))).isEqualByComparingTo("6.70");

        // 15.55 * 0.67 = 10.4185 -> 10.42
        assertThat(promotion.applyTo(new BigDecimal("15.55"))).isEqualByComparingTo("10.42");
    }

    @Test
    @DisplayName("always returns a price scaled to two decimal places")
    void alwaysScaledToCents() {
        assertThat(promotion.applyTo(new BigDecimal("15.00")).scale()).isEqualTo(2);
    }

    @Test
    @DisplayName("a hundred percent off is free, not negative")
    void fullDiscountIsFree() {
        promotion.setDiscountPercent(100);
        assertThat(promotion.applyTo(new BigDecimal("22.00"))).isEqualByComparingTo("0.00");
    }

    @Test
    @DisplayName("is live on both boundary days")
    void validityIsInclusive() {
        assertThat(promotion.isValidOn(FROM)).isTrue();
        assertThat(promotion.isValidOn(TO)).isTrue();
        assertThat(promotion.isValidOn(FROM.plusDays(10))).isTrue();
    }

    @Test
    @DisplayName("is not live outside its window")
    void notValidOutsideWindow() {
        assertThat(promotion.isValidOn(FROM.minusDays(1))).isFalse();
        assertThat(promotion.isValidOn(TO.plusDays(1))).isFalse();
    }

    @Test
    @DisplayName("a switched-off promotion is never live")
    void inactiveIsNeverValid() {
        promotion.setActive(false);
        assertThat(promotion.isValidOn(FROM.plusDays(10))).isFalse();
    }

    @Test
    @DisplayName("a withdrawn promotion is never live")
    void softDeletedIsNeverValid() {
        promotion.softDelete();
        assertThat(promotion.isValidOn(FROM.plusDays(10))).isFalse();
    }

    @Test
    @DisplayName("applies only to the services it was attached to")
    void appliesOnlyToItsServices() {
        BarberService haircut = serviceNamed("Haircut");
        BarberService beard = serviceNamed("Beard Trim");

        promotion.addService(haircut);

        assertThat(promotion.appliesTo(haircut)).isTrue();
        assertThat(promotion.appliesTo(beard)).isFalse();
    }

    @Test
    @DisplayName("removing a service detaches both sides of the association")
    void removeServiceKeepsBothSidesInStep() {
        BarberService haircut = serviceNamed("Haircut");

        promotion.addService(haircut);
        assertThat(haircut.getPromotions()).contains(promotion);

        promotion.removeService(haircut);
        assertThat(promotion.appliesTo(haircut)).isFalse();
        assertThat(haircut.getPromotions()).doesNotContain(promotion);
    }

    private BarberService serviceNamed(String name) {
        BarberService service = new BarberService();
        service.setUuid(java.util.UUID.randomUUID());
        service.setName(name);
        service.setPrice(new BigDecimal("15.00"));
        service.setDurationMinutes(30);
        return service;
    }
}
