package com.cargosetu.catalogue;

import com.cargosetu.utils.CapacityUnit;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

class CapacityUnitTest {

    @Test
    void testUnitConversions() {
        // 1 TONNE = 1000 KG
        assertThat(CapacityUnit.TONNE.toKg(1.0)).isEqualTo(1000.0);
        assertThat(CapacityUnit.TONNE.fromKg(1000.0)).isEqualTo(1.0);

        // 1 KG = 1 KG
        assertThat(CapacityUnit.KG.toKg(500.0)).isEqualTo(500.0);
        assertThat(CapacityUnit.KG.fromKg(500.0)).isEqualTo(500.0);

        // 1 POUND = 0.453592 KG
        assertThat(CapacityUnit.POUND.toKg(1.0)).isEqualTo(0.453592);
        assertThat(CapacityUnit.POUND.fromKg(0.453592)).isCloseTo(1.0, within(0.0001));

        // Direct conversion: TONNE to POUND
        double poundFromTonne = CapacityUnit.convert(1.0, CapacityUnit.TONNE, CapacityUnit.POUND);
        assertThat(poundFromTonne).isCloseTo(2204.62, within(0.1));
    }

    @Test
    void testFromString() {
        assertThat(CapacityUnit.fromString("tonne")).isEqualTo(CapacityUnit.TONNE);
        assertThat(CapacityUnit.fromString("TON")).isEqualTo(CapacityUnit.TONNE);
        assertThat(CapacityUnit.fromString("pound")).isEqualTo(CapacityUnit.POUND);
        assertThat(CapacityUnit.fromString("lbs")).isEqualTo(CapacityUnit.POUND);
        assertThat(CapacityUnit.fromString("kg")).isEqualTo(CapacityUnit.KG);
        assertThat(CapacityUnit.fromString(null)).isEqualTo(CapacityUnit.KG);
        assertThat(CapacityUnit.fromString("")).isEqualTo(CapacityUnit.KG);
        assertThat(CapacityUnit.fromString("unknown")).isEqualTo(CapacityUnit.KG);
    }
}
