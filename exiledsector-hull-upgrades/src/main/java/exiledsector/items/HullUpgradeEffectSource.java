package exiledsector.items;

import com.fs.starfarer.api.combat.ShipAPI.HullSize;
import exiledsector.effects.ShipEffectContext;
import exiledsector.effects.ShipEffectSource;
import exiledlib.skills.EffectContribution;
import exiledsector.skills.ShipSections;
import exiledlib.skills.SkillTypeEffect;
import exiledlib.skills.skilleffect.special.SpecialSkillEffect;

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
        return List.of(sections, ShipSections.doubledSections(sections), context.variant() == null ? 0 : context.variant().getSMods().size());
    }

    @Override
    public List<EffectContribution> contributions(ShipEffectContext context) {
        int sModCount = context.variant() == null ? 0 : context.variant().getSMods().size();
        return contributions(ShipSections.forVariant(context.shipData(), context.variant()), context.hullSize(), sModCount);
    }

    public static List<EffectContribution> contributions(List<ShipSections.UnlockedSection> sections, HullSize hullSize) {
        return contributions(sections, hullSize, 0);
    }

    public static List<EffectContribution> contributions(List<ShipSections.UnlockedSection> sections, HullSize hullSize, int sModCount) {
        List<EffectContribution> contributions = new ArrayList<>();
        Set<ShipSection> doubled = ShipSections.doubledSections(sections);
        for (ShipSections.UnlockedSection unlocked : sections) {
            List<SkillTypeEffect> effects = perSMod(ShipSections.appliedEffects(unlocked, hullSize, doubled.contains(unlocked.section())), sModCount);
            if (!effects.isEmpty()) {
                contributions.add(EffectContribution.scaledPermanent(unlocked, MOD_ID_PREFIX + unlocked.section().id() + "_",
                        Collections.unmodifiableList(effects)));
            }
        }
        return contributions;
    }

    private static List<SkillTypeEffect> perSMod(List<SkillTypeEffect> effects, int sModCount) {
        return effects.stream().map(effect -> effect.effect() == SpecialSkillEffect.HIGHEST_AFFINITY_FLAT_PER_S_MOD
                ? new SkillTypeEffect(effect.effect(), effect.magnitude() * sModCount) : effect).toList();
    }
}
