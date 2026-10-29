package exiledsector.skills;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.HullModEffect;
import com.fs.starfarer.api.combat.ShipAPI.HullSize;
import com.fs.starfarer.api.loading.HullModSpecAPI;
import exiledsector.i18n.Style;
import exiledsector.i18n.StyledText;

import java.util.ArrayList;
import java.util.List;

public final class VanillaHullModLines {

    private static final String PARAMETER = "%s";

    public interface Source {
        String descriptionFormat(String hullModId);

        HullModEffect effect(String hullModId);
    }

    private static final Source GAME_SOURCE = new Source() {
        @Override
        public String descriptionFormat(String hullModId) {
            HullModSpecAPI spec = spec(hullModId);
            return spec == null ? null : spec.getDescriptionFormat();
        }

        @Override
        public HullModEffect effect(String hullModId) {
            HullModSpecAPI spec = spec(hullModId);
            return spec == null ? null : spec.getEffect();
        }

        private HullModSpecAPI spec(String hullModId) {
            return Global.getSettings() == null ? null : Global.getSettings().getHullModSpec(hullModId);
        }
    };

    private static Source source = GAME_SOURCE;

    private VanillaHullModLines() {
    }

    public static void install(Source installedSource) {
        source = installedSource == null ? GAME_SOURCE : installedSource;
    }

    public static List<StyledText> describe(String hullModId, HullSize hullSize) {
        String format = source.descriptionFormat(hullModId);
        HullModEffect effect = source.effect(hullModId);
        if (format == null || effect == null || !effect.shouldAddDescriptionToTooltip(hullSize, null, true)) {
            return List.of();
        }
        List<StyledText> lines = new ArrayList<>();
        int parameterIndex = 0;
        for (String paragraph : format.split("\\n\\s*\\n")) {
            String text = paragraph.trim();
            if (text.isEmpty()) {
                continue;
            }
            StyledText line = StyledText.EMPTY;
            int start = 0;
            for (int at = text.indexOf(PARAMETER); at >= 0; at = text.indexOf(PARAMETER, start)) {
                String parameter = effect.getDescriptionParam(parameterIndex++, hullSize, null);
                line = line.append(text.substring(start, at)).append(StyledText.styled(parameter == null ? "" : parameter, Style.HIGHLIGHT));
                start = at + PARAMETER.length();
            }
            lines.add(line.append(text.substring(start)));
        }
        return lines;
    }
}
