package exiledsector.charts.phenomenon;

import com.fs.starfarer.api.impl.campaign.procgen.StarSystemGenerator.StarSystemType;

import java.util.Locale;

public interface ChartPhenomenon {

    enum Kind {
        STAR,
        OVERLAY;

        public static Kind parse(String text) {
            if (text == null) {
                return null;
            }
            for (Kind kind : values()) {
                if (kind.name().equals(text.trim().toUpperCase(Locale.ROOT))) {
                    return kind;
                }
            }
            return null;
        }
    }

    String id();

    Kind kind();

    default String primaryStar() {
        return null;
    }

    default StarSystemType systemType() {
        return null;
    }

    static StarSystemType systemTypeOr(ChartPhenomenon phenomenon, StarSystemType fallback) {
        return phenomenon != null && phenomenon.systemType() != null ? phenomenon.systemType() : fallback;
    }

    default void shape(PhenomenonSite site) {
    }
}
