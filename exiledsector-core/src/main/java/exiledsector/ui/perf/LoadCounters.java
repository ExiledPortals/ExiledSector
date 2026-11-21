package exiledsector.ui.perf;

public final class LoadCounters {

    public record Totals(int textureLoads, long textureNanos, int hullFileReads, long hullFileNanos) {

        public static final Totals NONE = new Totals(0, 0L, 0, 0L);

        Totals minus(Totals earlier) {
            return new Totals(textureLoads - earlier.textureLoads, textureNanos - earlier.textureNanos, hullFileReads - earlier.hullFileReads,
                    hullFileNanos - earlier.hullFileNanos);
        }

        boolean isEmpty() {
            return textureLoads == 0 && hullFileReads == 0;
        }
    }

    private static int textureLoads;
    private static long textureNanos;
    private static int hullFileReads;
    private static long hullFileNanos;

    private LoadCounters() {
    }

    public static void textureLoaded(long nanos) {
        textureLoads++;
        textureNanos += nanos;
    }

    public static void hullFileRead(long nanos) {
        hullFileReads++;
        hullFileNanos += nanos;
    }

    public static Totals totals() {
        return new Totals(textureLoads, textureNanos, hullFileReads, hullFileNanos);
    }
}
