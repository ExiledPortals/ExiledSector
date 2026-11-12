package exiledsector.charts.officer.scar;

import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.campaign.FleetDataAPI;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import exiledsector.effects.ShipEffectContext;
import exiledsector.effects.ShipEffectSource;
import exiledsector.skills.EffectContribution;
import exiledsector.skills.SkillTypeEffect;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class ScarEffectSource implements ShipEffectSource {

    public static final ScarEffectSource INSTANCE = new ScarEffectSource();
    static final String MOD_ID_PREFIX = "exiledSector_scar_";

    private ScarEffectSource() {
    }

    static Map<String, Integer> scarsOf(FleetMemberAPI member) {
        CampaignFleetAPI fleet = sourceFleetOf(member);
        if (fleet == null || member.getId() == null || !member.getId().equals(ScarMemory.flagshipId(fleet.getMemoryWithoutUpdate()))) {
            return Map.of();
        }
        return ScarMemory.scars(fleet.getMemoryWithoutUpdate());
    }

    static CampaignFleetAPI sourceFleetOf(FleetMemberAPI member) {
        if (member == null) {
            return null;
        }
        FleetDataAPI fleetData = member.getFleetDataForStats() != null ? member.getFleetDataForStats() : member.getFleetData();
        return fleetData == null ? null : fleetData.getFleet();
    }

    @Override
    public Object cacheKey(ShipEffectContext context) {
        Map<String, Integer> scars = scarsOf(context.member());
        if (scars.isEmpty() || context.variant() == null || context.variant().getHullSpec() == null) {
            return List.of();
        }
        return List.of(scars, context.variant().getHullSpec().getHullId(), Set.copyOf(context.variant().getHullMods()));
    }

    @Override
    public List<EffectContribution> contributions(ShipEffectContext context) {
        Map<String, Integer> scars = scarsOf(context.member());
        if (scars.isEmpty()) {
            return List.of();
        }
        return contributions(scars, HullFacts.of(context.variant()));
    }

    static List<EffectContribution> contributions(Map<String, Integer> scars, HullFacts hull) {
        List<EffectContribution> contributions = new ArrayList<>();
        scars.forEach((scarId, stacks) -> {
            List<SkillTypeEffect> effects = effects(ScarTable.row(scarId), stacks, hull);
            if (!effects.isEmpty()) {
                contributions.add(new EffectContribution(new ScarOrigin(scarId), MOD_ID_PREFIX + scarId + "_", effects,
                        null, null, false, false));
            }
        });
        return contributions;
    }

    static List<SkillTypeEffect> effects(ScarRow row, int stacks, HullFacts hull) {
        if (row == null || stacks <= 0 || hull == null) {
            return List.of();
        }
        List<SkillTypeEffect> effects = new ArrayList<>();
        for (ScarRow.Effect entry : row.fittingEffects(hull)) {
            effects.add(new SkillTypeEffect(entry.effect(), entry.magnitudePerStack() * stacks));
        }
        return effects;
    }
}
