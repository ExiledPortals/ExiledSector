package exiledsector.effects;

import com.fs.starfarer.api.combat.MutableShipStatsAPI;
import com.fs.starfarer.api.combat.ShipAPI.HullSize;
import exiledsector.skills.DiminishingReturns;
import exiledsector.skills.EffectContribution;
import exiledsector.skills.StatBaseline;
import exiledsector.skills.ShipSkillData;

import java.util.List;

public final class StatSheetTreeFixture {

    private static final ShipEffectSource TREE_ONLY = new ShipEffectSource() {
        @Override
        public List<EffectContribution> contributions(ShipEffectContext context) {
            return SkillTreeEffectSource.contributions(context.shipData(), context.hullSize());
        }
    };

    private StatSheetTreeFixture() {
    }

    public static ResolvedTree resolve(ShipSkillData shipData, HullSize hullSize) {
        return ResolvedTree.of(new ShipEffectContext(shipData, null, null, hullSize), 1f, StatBaseline.NONE, DiminishingReturns.OFF,
                List.of(TREE_ONLY));
    }

    public static void apply(ResolvedTree resolvedTree, MutableShipStatsAPI stats) {
        for (ResolvedTree.Entry entry : resolvedTree.entries()) {
            if (entry instanceof ResolvedTree.EffectEntry effectEntry) {
                effectEntry.effect().apply(stats, effectEntry.modId(), effectEntry.magnitude());
            }
        }
    }
}
