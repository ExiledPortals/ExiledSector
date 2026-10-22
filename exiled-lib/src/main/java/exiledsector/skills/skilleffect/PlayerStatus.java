package exiledsector.skills.skilleffect;

import com.fs.starfarer.api.combat.CombatEngineAPI;
import exiledsector.i18n.I18n;
import exiledsector.i18n.Translation;

import java.util.function.Supplier;

final class PlayerStatus {

    private final String statusKey;
    private final String iconPath;
    private final String titleKey;
    private String title;
    private String text;

    PlayerStatus(String statusKey, String iconPath, String titleKey) {
        this.statusKey = statusKey;
        this.iconPath = iconPath;
        this.titleKey = titleKey;
    }

    boolean needsText() {
        return text == null;
    }

    void setText(Supplier<String> gameText) {
        text = I18n.forGameText(gameText);
    }

    void invalidate() {
        text = null;
    }

    void show(CombatEngineAPI engine, boolean debuff) {
        if (title == null) {
            title = Translation.gameText(titleKey);
        }
        engine.maintainStatusForPlayerShip(statusKey, iconPath, title, text, debuff);
    }
}
