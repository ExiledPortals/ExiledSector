package exiledsector.i18n;

import org.junit.jupiter.api.Test;

import java.util.Locale;

import static org.junit.jupiter.api.Assertions.assertEquals;

class NumberTextTest {

    @Test
    void wholeNumbersDropTheDecimalPoint() {
        assertEquals("10", NumberText.format(10f));
        assertEquals("-10", NumberText.format(-10f));
        assertEquals("0", NumberText.format(0f));
    }

    @Test
    void fractionsKeepTheirShortestFloatForm() {
        assertEquals("12.5", NumberText.format(12.5f));
        assertEquals("34.21", NumberText.format(34.21f));
    }

    @Test
    void neverUsesScientificNotation() {
        assertEquals("0.0001", NumberText.format(1e-4f));
        assertEquals("3000000000", NumberText.format(3e9f));
        assertEquals("-0.00025", NumberText.format(-2.5e-4f));
    }

    @Test
    void doesNotDependOnTheDefaultLocale() {
        Locale previous = Locale.getDefault();
        try {
            Locale.setDefault(Locale.GERMANY);
            assertEquals("12.5", NumberText.format(12.5f));
        } finally {
            Locale.setDefault(previous);
        }
    }
}
