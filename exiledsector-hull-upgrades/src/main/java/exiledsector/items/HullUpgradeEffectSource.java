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
import java.util.Set;

public final class HullUpgradeEffectSource implements ShipEffectSource {

    public static final HullUpgradeEffectSource INSTANCE = new HullUpgradeEffectSource();
    static final String MOD_ID_PREFIX = "exiledSector_framework_";

    private HullUpgradeEffectSource() {
    }

    @Override
    public Object cacheKey(ShipEffectContext context) {
        List<ShipSections.UnlockedSection> sections = ShipSections.forVariant(context.shipData(), context.variant());
        return List.of(sections, ShipSections.doubledSections(sections));
    }

    @Override
    public List<EffectContribution> contributions(ShipEffectContext context) {
        return contributions(ShipSections.forVariant(context.shipData(), context.variant()), context.hullSize());
    }

    public static List<EffectContribution> contributions(List<ShipSections.UnlockedSection> sections, HullSize hullSize) {
        List<EffectContribution> contributions = new ArrayList<>();
        Set<ShipSection> doubled = ShipSections.doubledSections(sections);
        for (ShipSections.UnlockedSection unlocked : sections) {
            List<SkillTypeEffect> effects = ShipSections.appliedEffects(unlocked, hullSize, doubled.contains(unlocked.section()));
            if (!effects.isEmpty()) {
                contributions.add(EffectContribution.scaledPermanent(unlocked, MOD_ID_PREFIX + unlocked.section().id() + "_",
                        Collections.unmodifiableList(effects)));
            }
        }
        return contributions;
    }
}
