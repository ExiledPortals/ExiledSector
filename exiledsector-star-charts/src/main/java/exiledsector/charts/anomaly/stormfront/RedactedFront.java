package exiledsector.charts.anomaly.stormfront;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.SettingsAPI;
import com.fs.starfarer.api.campaign.AICoreOfficerPlugin;
import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.campaign.FactionAPI;
import com.fs.starfarer.api.campaign.rules.MemoryAPI;
import com.fs.starfarer.api.combat.ShipAPI.HullSize;
import com.fs.starfarer.api.combat.ShipVariantAPI;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import com.fs.starfarer.api.impl.campaign.ids.Factions;
import com.fs.starfarer.api.impl.campaign.ids.ShipRoles;
import com.fs.starfarer.api.util.Misc;
import exiledsector.Seeds;
import exiledsector.charts.anomaly.AnomalyRun;
import exiledsector.charts.anomaly.debris.DebrisRules;
import exiledsector.loot.FleetFactions;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

final class RedactedFront {

    static final String FACTION_ID = Factions.REMNANTS;
    static final String JOINED_MEMORY_KEY = "$exiledSector_stormRedactedFrigatesJoined";
    static final String SEED_PREFIX = "exiledSector_stormRedactedFrigates_";

    private RedactedFront() {
    }

    static void reinforce(AnomalyRun run, CampaignFleetAPI fleet) {
        StormTuning tuning = StormTuning.of(run.run());
        CampaignFleetAPI playerFleet = Global.getSector().getPlayerFleet();
        if (tuning.redactedFrigatesMost() <= 0 || fleet == null || playerFleet == null || fleet == playerFleet || fleet.getFleetData() == null
                || !fleet.isHostileTo(playerFleet) || fleet.getContainingLocation() != run.system()) {
            return;
        }
        MemoryAPI memory = fleet.getMemoryWithoutUpdate();
        if (memory == null || memory.getBoolean(JOINED_MEMORY_KEY)) {
            return;
        }
        List<String> frigates = frigateVariants(Global.getSector().getFaction(FACTION_ID), Global.getSettings());
        if (frigates.isEmpty()) {
            return;
        }
        memory.set(JOINED_MEMORY_KEY, true);
        fleet.inflateIfNeeded();
        Random random = Seeds.random(SEED_PREFIX + run.runId() + "_" + fleet.getId());
        int joining = joiningCount(tuning.redactedFrigatesFewest(), tuning.redactedFrigatesMost(), random);
        Map<FleetMemberAPI, String> factionIdsByMember = recordedFactions(fleet);
        for (int index = 0; index < joining; index++) {
            FleetMemberAPI frigate = fleet.getFleetData().addFleetMember(frigates.get(random.nextInt(frigates.size())));
            if (frigate != null) {
                crew(frigate, run.tier(), random);
                factionIdsByMember.put(frigate, FACTION_ID);
            }
        }
        FleetFactions.recordMemberFactions(fleet, factionIdsByMember);
        fleet.getFleetData().sort();
        fleet.getFleetData().setSyncNeeded();
        fleet.getFleetData().syncIfNeeded();
    }

    static int joiningCount(int fewest, int most, Random random) {
        int low = Math.max(0, Math.min(fewest, most));
        int high = Math.max(low, most);
        return low + random.nextInt(high - low + 1);
    }

    static List<String> frigateVariants(FactionAPI faction, SettingsAPI settings) {
        Set<String> roleVariants = faction == null ? null : faction.getVariantsForRole(ShipRoles.COMBAT_SMALL);
        List<String> frigates = new ArrayList<>();
        for (String variantId : roleVariants == null ? Set.<String>of() : roleVariants) {
            ShipVariantAPI variant = settings.doesVariantExist(variantId) ? settings.getVariant(variantId) : null;
            if (variant != null && variant.getHullSpec() != null && variant.getHullSpec().getHullSize() == HullSize.FRIGATE) {
                frigates.add(variantId);
            }
        }
        return frigates;
    }

    private static Map<FleetMemberAPI, String> recordedFactions(CampaignFleetAPI fleet) {
        Map<FleetMemberAPI, String> factionIdsByMember = new LinkedHashMap<>();
        for (FleetMemberAPI member : fleet.getFleetData().getMembersListCopy()) {
            String factionId = FleetFactions.memberFactionIdOf(member, fleet);
            if (factionId != null) {
                factionIdsByMember.put(member, factionId);
            }
        }
        return factionIdsByMember;
    }

    private static void crew(FleetMemberAPI frigate, int tier, Random random) {
        String coreId = DebrisRules.coreId(DebrisRules.coreLevel(tier, random), 0);
        AICoreOfficerPlugin core = Misc.getAICoreOfficerPlugin(coreId);
        if (core != null) {
            frigate.setCaptain(core.createPerson(coreId, FACTION_ID, random));
        }
        frigate.getRepairTracker().setCR(frigate.getRepairTracker().getMaxCR());
    }
}
