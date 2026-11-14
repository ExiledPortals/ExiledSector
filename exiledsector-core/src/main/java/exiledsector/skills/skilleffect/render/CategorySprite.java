package exiledsector.skills.skilleffect.render;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.SettingsAPI;
import com.fs.starfarer.api.graphics.SpriteAPI;

public final class CategorySprite {

    private final String category;
    private final String spriteId;
    private SpriteAPI sprite;

    public CategorySprite(String category, String spriteId) {
        this.category = category;
        this.spriteId = spriteId;
    }

    public SpriteAPI get() {
        if (sprite == null) {
            SettingsAPI settings = Global.getSettings();
            if (settings != null) {
                sprite = settings.getSprite(category, spriteId);
            }
        }
        return sprite;
    }
}
