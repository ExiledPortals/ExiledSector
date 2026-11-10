package exiledsector.skills;

import exiledsector.ModSettings;

import java.util.List;

public final class IntegrationCostSetting {

    public static final String FIELD_ID = "exiledSector_integrationCostMode";
    public static final String EXPONENTIAL = "Exponential";
    public static final String ADDITIVE = "Additive";
    public static final String STATIC = "Static";
    public static final List<String> OPTIONS = List.of(EXPONENTIAL, ADDITIVE, STATIC);
    public static final String DEFAULT = EXPONENTIAL;
    public static final int STATIC_COST = 2;

    public enum Mode {
        EXPONENTIAL {
            @Override
            int costOf(int integrationNumber) {
                return 1 << Math.min(integrationNumber - 1, 30);
            }
        },
        ADDITIVE {
            @Override
            int costOf(int integrationNumber) {
                return integrationNumber;
            }
        },
        STATIC {
            @Override
            int costOf(int integrationNumber) {
                return STATIC_COST;
            }
        };

        abstract int costOf(int integrationNumber);
    }

    private static volatile Mode mode = parse(DEFAULT);

    private IntegrationCostSetting() {
    }

    public static void reloadSettings() {
        mode = parse(ModSettings.stringOr(FIELD_ID, DEFAULT));
    }

    static Mode parse(String value) {
        String trimmed = value == null ? "" : value.trim();
        if (ADDITIVE.equalsIgnoreCase(trimmed)) {
            return Mode.ADDITIVE;
        }
        return STATIC.equalsIgnoreCase(trimmed) ? Mode.STATIC : Mode.EXPONENTIAL;
    }

    public static Mode mode() {
        return mode;
    }

    public static void setModeForTests(Mode testMode) {
        mode = testMode;
    }
}
