package exiledsector.effects;

import com.fs.starfarer.api.combat.ShipAPI.HullSize;
import exiledsector.items.HullUpgradeEffectSource;
import exiledlib.skills.DiminishingReturns;
import exiledlib.skills.EffectContribution;
import exiledsector.skills.ShipSections;
import exiledsector.skills.ShipSkillData;
import exiledlib.skills.StatBaseline;

import java.util.List;

final class ResolvedTrees {

    record FixedSections(List<ShipSections.UnlockedSection> sections) implements ShipEffectSource {

        @Override
        public Object cacheKey(ShipEffectContext context) {
            return sections;
        }

        @Override
        public List<EffectContribution> contributions(ShipEffectContext context) {
            return HullUpgradeEffectSource.contributions(context.shipData(), sections, context.hullSize());
        }
    }

    private ResolvedTrees() {
    }

    static ResolvedTree of(ShipSkillData shipData, HullSize hullSize) {
        return of(shipData, hullSize, 1f);
    }

    static ResolvedTree of(ShipSkillData shipData, HullSize hullSize, float bonusScale) {
        return of(shipData, hullSize, bonusScale, List.of());
    }

    static ResolvedTree of(ShipSkillData shipData, HullSize hullSize, float bonusScale, List<ShipSections.UnlockedSection> sections) {
        return of(shipData, hullSize, bonusScale, sections, StatBaseline.NONE, DiminishingReturns.OFF);
    }

    static ResolvedTree of(ShipSkillData shipData, HullSize hullSize, float bonusScale, List<ShipSections.UnlockedSection> sections,
                           StatBaseline statBaseline, DiminishingReturns.Config diminishing) {
        return ResolvedTree.of(new ShipEffectContext(shipData, null, null, hullSize), bonusScale, statBaseline, diminishing,
                List.of(SkillTreeEffectSource.INSTANCE, new FixedSections(sections)));
    }
}
