package exiledsector.charts.anomaly.excavation;

import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.campaign.InteractionDialogAPI;
import com.fs.starfarer.api.campaign.SectorEntityToken;
import com.fs.starfarer.api.campaign.rules.MemoryAPI;
import com.fs.starfarer.api.combat.BattleCreationContext;
import com.fs.starfarer.api.impl.campaign.FleetInteractionDialogPluginImpl.FIDConfig;
import exiledsector.i18n.Translation;
import exiledsector.charts.anomaly.AnomalyEncounters;

import java.util.function.Consumer;

public final class ShaftEncounters {

    static final String OPPONENT_KEY = "$exiledSector_shaftOpponent";

    private ShaftEncounters() {
    }

    public static FIDConfig config(Consumer<InteractionDialogAPI> onLeave) {
        FIDConfig config = AnomalyEncounters.siteFight("chart.excavation.encounter.leave", true, ShaftEncounters::fightUnderground, onLeave);
        config.afterFirstTimeEngageOptionText = Translation.gameText("chart.excavation.encounter.reengage");
        return config;
    }

    static void fightUnderground(BattleCreationContext battleContext) {
        battleContext.objectivesAllowed = false;
        CampaignFleetAPI opponent = battleContext.getOtherFleet();
        if (opponent != null && opponent.getMemoryWithoutUpdate() != null) {
            opponent.getMemoryWithoutUpdate().set(OPPONENT_KEY, true);
        }
    }

    public static boolean isShaftBattle(SectorEntityToken opponent) {
        MemoryAPI memory = opponent == null ? null : opponent.getMemoryWithoutUpdate();
        return ShaftFleets.isShaftFleet(opponent) || memory != null && memory.getBoolean(OPPONENT_KEY);
    }
}
