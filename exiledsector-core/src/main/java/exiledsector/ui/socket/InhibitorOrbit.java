package exiledsector.ui.socket;

import exiledsector.items.CraftingInhibitor;
import exiledsector.skills.skilleffect.render.SpriteCache;
import exiledsector.ui.util.SpriteDraw;

import java.awt.Color;
import java.util.Collection;

final class InhibitorOrbit {

    private static final float RADIANS_PER_SECOND = 0.6f;
    private static final float ORBIT_X_SHARE = 1.5f;
    private static final float ORBIT_Y_SHARE = 0.95f;
    private static final float ICON_SHARE = 0.34f;
    private static final float FULL_TURN = (float) (Math.PI * 2.0);
    private static final SpriteCache ICONS = new SpriteCache(InhibitorOrbit.class);

    private float phase;

    void advance(float amount) {
        phase = (phase + amount * RADIANS_PER_SECOND) % FULL_TURN;
    }

    void render(float centreX, float centreY, float slotRadius, Collection<CraftingInhibitor> active, float alpha) {
        if (active.isEmpty() || alpha <= 0f) {
            return;
        }
        float iconSize = slotRadius * ICON_SHARE;
        float spacing = FULL_TURN / active.size();
        int index = 0;
        for (CraftingInhibitor inhibitor : active) {
            float angle = phase + index * spacing;
            float iconX = centreX + slotRadius * ORBIT_X_SHARE * (float) Math.cos(angle);
            float iconY = centreY + slotRadius * ORBIT_Y_SHARE * (float) Math.sin(angle);
            SpriteDraw.drawAtCenter(ICONS, CraftingItemTrayEntry.iconPath(inhibitor.itemId()), iconX, iconY, iconSize, iconSize, Color.WHITE, alpha);
            index++;
        }
    }
}
