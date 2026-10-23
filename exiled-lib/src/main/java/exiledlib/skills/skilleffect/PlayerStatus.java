package exiledlib.skills.skilleffect;

import com.fs.starfarer.api.combat.CombatEngineAPI;
import exiledlib.i18n.I18n;
import exiledlib.i18n.Translation;

import java.util.function.Supplier;

public final class PlayerStatus {

    private final String statusKey;
    private final String iconPath;
    private final String titleKey;
    private String title;
    private String text;

    public PlayerStatus(String statusKey, String iconPath, String titleKey) {
        this.statusKey = statusKey;
        this.iconPath = iconPath;
        this.titleKey = titleKey;
    }

    public boolean needsText() {
        return text == null;
    }

    public void setText(Supplier<String> gameText) {
        text = I18n.forGameText(gameText);
    }

    public void invalidate() {
        text = null;
    }

    public void show(CombatEngineAPI engine, boolean debuff) {
        if (title == null) {
            title = Translation.gameText(titleKey);
        }
        engine.maintainStatusForPlayerShip(statusKey, iconPath, title, text, debuff);
    }
}
