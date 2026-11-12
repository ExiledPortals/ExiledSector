package exiledsector.charts.run;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.BattleAPI;
import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.campaign.FactionAPI;
import com.fs.starfarer.api.campaign.SectorAPI;
import com.fs.starfarer.api.campaign.rules.MemoryAPI;
import com.fs.starfarer.api.impl.campaign.CoreReputationPlugin.RepActionEnvelope;
import com.fs.starfarer.api.impl.campaign.CoreReputationPlugin.RepActions;
import com.fs.starfarer.api.impl.campaign.ids.Factions;
import exiledsector.charts.ChartIds;
import exiledsector.charts.anomaly.RunFactionDress;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Predicate;

public final class ChartTransponderReputation {

    private static final Set<String> NEVER_PENALISED = Set.of(Factions.PLAYER, Factions.NEUTRAL, RunFactionDress.ALLY_FACTION_ID);

    private ChartTransponderReputation() {
    }

    public static void battleOccurred(BattleAPI battle) {
        SectorAPI sector = Global.getSector();
        CampaignFleetAPI playerFleet = sector == null ? null : sector.getPlayerFleet();
        if (battle == null || playerFleet == null || !battle.isPlayerInvolved()) {
            return;
        }
        Set<String> penalised = factionsToPenalise(battle.getNonPlayerSideSnapshot(), playerFleet.isTransponderOn(),
                factionId -> hostileToPlayer(sector, factionId));
        for (String factionId : penalised) {
            sector.adjustPlayerReputation(new RepActionEnvelope(RepActions.COMBAT_AGGRESSIVE, null, null), factionId);
        }
    }

    static Set<String> factionsToPenalise(List<CampaignFleetAPI> enemyFleets, boolean transponderOn, Predicate<String> hostileToPlayer) {
        Set<String> penalised = new LinkedHashSet<>();
        if (!transponderOn || enemyFleets == null) {
            return penalised;
        }
        for (CampaignFleetAPI fleet : enemyFleets) {
            String factionId = isChartFleet(fleet) ? fleet.getFaction().getId() : null;
            if (factionId != null && !NEVER_PENALISED.contains(factionId) && !hostileToPlayer.test(factionId)) {
                penalised.add(factionId);
            }
        }
        return penalised;
    }

    private static boolean isChartFleet(CampaignFleetAPI fleet) {
        MemoryAPI memory = fleet == null ? null : fleet.getMemoryWithoutUpdate();
        return memory != null && memory.contains(ChartIds.TIER_MEMORY_KEY) && fleet.getFaction() != null;
    }

    private static boolean hostileToPlayer(SectorAPI sector, String factionId) {
        FactionAPI faction = sector.getFaction(factionId);
        return faction == null || faction.isHostileTo(Factions.PLAYER);
    }
}
