package exiledsector.ui.socket;

import exiledlib.i18n.StyledText;
import exiledsector.items.Item;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.function.BooleanSupplier;

public final class BenchModes {

    private static final Map<BenchMode, BenchModeSupport> SUPPORT_BY_MODE = Collections.synchronizedMap(new EnumMap<>(BenchMode.class));
    private static final Map<BenchMode, BooleanSupplier> DOMAIN_ENABLED_BY_MODE = Collections.synchronizedMap(new EnumMap<>(BenchMode.class));

    private BenchModes() {
    }

    public static void register(BenchModeSupport support, BooleanSupplier domainEnabled) {
        SUPPORT_BY_MODE.put(support.mode(), support);
        DOMAIN_ENABLED_BY_MODE.put(support.mode(), domainEnabled);
    }

    public static BenchModeSupport of(BenchMode mode) {
        return mode == null ? null : SUPPORT_BY_MODE.get(mode);
    }

    public static boolean enabled(BenchMode mode) {
        BenchModeSupport support = of(mode);
        BooleanSupplier domainEnabled = DOMAIN_ENABLED_BY_MODE.get(mode);
        return support != null && domainEnabled.getAsBoolean() && support.enabled();
    }

    static List<StyledText> detailLines(Item item) {
        List<StyledText> lines = new ArrayList<>();
        synchronized (SUPPORT_BY_MODE) {
            SUPPORT_BY_MODE.values().forEach(support -> lines.addAll(support.detailLines(item)));
        }
        return lines;
    }
}
