package exiledsector.effects;

import com.fs.starfarer.api.campaign.BaseCampaignEventListener;
import com.fs.starfarer.api.combat.EngagementResultAPI;
import exiledsector.skills.skilleffect.logistics.FleetWideEffects;
import exiledsector.ExiledSector;
import exiledsector.items.SocketLossListener;

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

    public static PlayerEngagementPipeline create(SocketLossListener socketLossListener, ItemLootListener itemLootListener) {
        Map<Stage, Step> stepsByStage = new EnumMap<>(Stage.class);
        stepsByStage.put(Stage.RECORD_LOSSES, engagement -> socketLossListener.reportPlayerEngagement(engagement.result()));
        ExiledSector.domains().forEach(domain -> stepsByStage.putAll(domain.engagementSteps()));
        stepsByStage.put(Stage.SETTLE_CREW, engagement -> FleetCrewLedgerSettlement.schedule());
        stepsByStage.put(Stage.HOLD_LOOT, itemLootListener::holdLoot);
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
}
