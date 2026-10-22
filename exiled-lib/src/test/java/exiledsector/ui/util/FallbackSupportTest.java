package exiledsector.ui.util;

import org.apache.log4j.Logger;
import org.junit.jupiter.api.Test;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.assertEquals;

class FallbackSupportTest {

    private static final Logger LOGGER = Logger.getLogger(FallbackSupportTest.class);

    @Test
    void returnsTheSuppliersResultWhenItSucceeds() {
        String result = FallbackSupport.getOrFallback(() -> "value", "fallback", LOGGER, "context");

        assertEquals("value", result);
    }

    @Test
    void returnsFallbackWhenSupplierThrowsARuntimeException() {
        String result = FallbackSupport.getOrFallback(() -> {
            throw new RuntimeException("boom");
        }, "fallback", LOGGER, "context");

        assertEquals("fallback", result);
    }

    @Test
    void returnsFallbackWhenSupplierThrowsACheckedException() {
        String result = FallbackSupport.getOrFallback(() -> {
            throw new IOException("boom");
        }, "fallback", LOGGER, "context");

        assertEquals("fallback", result);
    }

    @Test
    void returnsFallbackWhenSupplierReturnsNull() {
        String result = FallbackSupport.getOrFallback(() -> null, "fallback", LOGGER, "context");

        assertEquals("fallback", result);
    }
}
