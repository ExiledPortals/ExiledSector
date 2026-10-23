package exiledsector.items;

import com.fs.starfarer.api.combat.ShipAPI.HullSize;
import exiledsector.effects.ShipEffectContext;
import exiledsector.effects.ShipEffectSource;
import exiledlib.skills.EffectContribution;
import exiledsector.skills.ShipSections;
import exiledlib.skills.SkillTypeEffect;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class HullUpgradeEffectSource implements ShipEffectSource {

    public static final HullUpgradeEffectSource INSTANCE = new HullUpgradeEffectSource();
    static final String MOD_ID_PREFIX = "exiledSector_framework_";

    private HullUpgradeEffectSource() {
    }

    @Override
    public Object cacheKey(ShipEffectContext context) {
        return ShipSections.forVariant(context.shipData(), context.variant());
    }

    @Override
    public List<EffectContribution> contributions(ShipEffectContext context) {
        return contributions(ShipSections.forVariant(context.shipData(), context.variant()), context.hullSize());
    }

    public static List<EffectContribution> contributions(List<ShipSections.UnlockedSection> sections, HullSize hullSize) {
        List<EffectContribution> contributions = new ArrayList<>();
        for (ShipSections.UnlockedSection unlocked : sections) {
            List<SkillTypeEffect> effects = ShipSections.appliedEffects(unlocked, hullSize);
            if (!effects.isEmpty()) {
                contributions.add(EffectContribution.scaledPermanent(unlocked, MOD_ID_PREFIX + unlocked.section().id() + "_",
                        Collections.unmodifiableList(effects)));
            }
        }
        return contributions;
    }
}
