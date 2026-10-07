package exiledsector.effects;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.BaseCampaignEventListener;
import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.campaign.SectorAPI;
import com.fs.starfarer.api.combat.EngagementResultAPI;
import exiledsector.skills.skilleffect.FleetWideEffects;
import exiledsector.socketables.SocketLossListener;

import java.util.EnumMap;
import java.util.Map;

public final class PlayerEngagementPipeline extends BaseCampaignEventListener {

    public enum Stage {
        RECORD_LOSSES,
        SYNC_TREES,
        AWARD_XP,
        SETTLE_CREW,
        HOLD_LOOT,
        SALVAGE_BONUS
    }

    @FunctionalInterface
    public interface Step {
        void afterPlayerEngagement(PlayerEngagement engagement);
    }

    private final Map<Stage, Step> stepsByStage;

    PlayerEngagementPipeline(Map<Stage, Step> stepsByStage) {
        super(false);
        this.stepsByStage = new EnumMap<>(stepsByStage);
    }

    public static PlayerEngagementPipeline create(SocketLossListener socketLossListener, SocketableLootListener socketableLootListener) {
        Map<Stage, Step> stepsByStage = new EnumMap<>(Stage.class);
        stepsByStage.put(Stage.RECORD_LOSSES, engagement -> socketLossListener.reportPlayerEngagement(engagement.result()));
        stepsByStage.put(Stage.SYNC_TREES, engagement -> ShipTreeSync.afterPlayerEngagement(playerFleet()));
        stepsByStage.put(Stage.AWARD_XP, CombatXpAward::award);
        stepsByStage.put(Stage.SETTLE_CREW, engagement -> FleetCrewLedgerSettlement.schedule());
        stepsByStage.put(Stage.HOLD_LOOT, socketableLootListener::holdLoot);
        stepsByStage.put(Stage.SALVAGE_BONUS, engagement -> FleetWideEffects.recomputeSalvageBonus(engagement.playerLossIds()));
        return new PlayerEngagementPipeline(stepsByStage);
    }

    @Override
    public void reportPlayerEngagement(EngagementResultAPI engagementResult) {
        if (engagementResult == null) return;

        PlayerEngagement engagement = PlayerEngagement.of(engagementResult, BattleDifficulty.current());
        for (Step step : stepsByStage.values()) {
            step.afterPlayerEngagement(engagement);
        }
    }

    private static CampaignFleetAPI playerFleet() {
        SectorAPI sector = Global.getSector();
        return sector == null ? null : sector.getPlayerFleet();
    }
}
