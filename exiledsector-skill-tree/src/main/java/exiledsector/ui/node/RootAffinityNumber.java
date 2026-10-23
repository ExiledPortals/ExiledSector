package exiledsector.ui.node;

import com.fs.starfarer.api.combat.ShipAPI.HullSize;
import exiledlib.skills.AffinitySteps;
import exiledlib.skills.SkillTypeEffect;
import exiledlib.skills.skilleffect.SkillEffect;
import exiledsector.effects.SkillTreeEffectSource;
import exiledsector.skills.ShipSkillData;
import exiledsector.skills.SkillNode;
import exiledsector.skills.SkillType;
import exiledsector.ui.util.TextLabel;
import org.lazywizard.lazylib.ui.LazyFont;

import java.awt.Color;
import java.util.HashMap;
import java.util.Map;

final class RootAffinityNumber {

    private static final float FONT_SIZE_PER_ROOT_RADIUS = 0.62f;
    private static final float MIN_FONT_SIZE = 14f;
    private static final int FONT_SIZE_STEP = 2;
    private static final float OUTLINE_SHARE_OF_FONT = 0.06f;
    private static final Color NUMBER_COLOUR = Color.WHITE;
    private static final Color OUTLINE_COLOUR = new Color(10, 12, 18);

    private record SizedLabel(int fontSize, TextLabel label, TextLabel outline) {
    }

    private final HullSize hullSize;
    private final Map<String, SizedLabel> labelsByRootId = new HashMap<>();
    private ShipSkillData totalsShip;
    private int totalsRevision = -1;
    private Map<SkillEffect, Float> totals = Map.of();

    RootAffinityNumber(HullSize hullSize) {
        this.hullSize = hullSize;
    }

    void draw(SkillNode root, ShipSkillData shipData, boolean rootPending, float centerX, float centerY, float rootRadius, float alphaMult) {
        SkillTypeEffect rootAffinity = rootAffinity(root.getType());
        if (rootAffinity == null) {
            return;
        }
        float total = rootPending ? rootAffinity.magnitude() : totalsFor(shipData).getOrDefault(rootAffinity.effect(), 0f);
        int fontSize = Math.round(Math.max(MIN_FONT_SIZE, rootRadius * FONT_SIZE_PER_ROOT_RADIUS) / FONT_SIZE_STEP) * FONT_SIZE_STEP;
        SizedLabel sized = labelsByRootId.get(root.getId());
        if (sized == null || sized.fontSize() != fontSize) {
            if (sized != null) {
                sized.label().dispose();
                sized.outline().dispose();
            }
            sized = new SizedLabel(fontSize, new TextLabel(fontSize, NUMBER_COLOUR, LazyFont.TextAnchor.CENTER),
                    new TextLabel(fontSize, OUTLINE_COLOUR, LazyFont.TextAnchor.CENTER));
            labelsByRootId.put(root.getId(), sized);
        }
        String number = Integer.toString(Math.round(total));
        float outlineOffset = Math.max(1f, fontSize * OUTLINE_SHARE_OF_FONT);
        TextLabel outline = sized.outline().set(number).setAlpha(alphaMult);
        for (int offsetX = -1; offsetX <= 1; offsetX++) {
            for (int offsetY = -1; offsetY <= 1; offsetY++) {
                if (offsetX != 0 || offsetY != 0) {
                    outline.draw(centerX + offsetX * outlineOffset, centerY + offsetY * outlineOffset);
                }
            }
        }
        sized.label().set(number).setAlpha(alphaMult).draw(centerX, centerY);
    }

    private Map<SkillEffect, Float> totalsFor(ShipSkillData shipData) {
        if (shipData != totalsShip || shipData.revision() != totalsRevision) {
            totals = AffinitySteps.totals(SkillTreeEffectSource.contributions(shipData, hullSize));
            totalsShip = shipData;
            totalsRevision = shipData.revision();
        }
        return totals;
    }

    static SkillTypeEffect rootAffinity(SkillType rootType) {
        for (SkillTypeEffect effect : rootType.getEffects()) {
            if (AffinitySteps.isAffinity(effect.effect())) {
                return effect;
            }
        }
        return null;
    }
}
