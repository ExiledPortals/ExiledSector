package exiledsector.ui.util;

import org.apache.log4j.Logger;

public final class FallbackSupport {

    private FallbackSupport() {
    }

    @FunctionalInterface
    public interface ThrowingSupplier<T> {
        // deliberately generic: callers wrap unrelated Starsector API calls that each throw different exception types
        @SuppressWarnings("java:S112")
        T get() throws Exception;
    }

    public static <T> T getOrFallback(ThrowingSupplier<T> supplier, T fallback, Logger logger, String context) {
        try {
            T result = supplier.get();
            return result != null ? result : fallback;
        } catch (Exception e) {
            logger.error(context, e);
            return fallback;
        }
    }
}
