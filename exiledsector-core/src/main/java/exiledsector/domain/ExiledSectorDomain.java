package exiledsector.domain;

import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.ShipAPI.HullSize;
import com.fs.starfarer.api.combat.ShipVariantAPI;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import com.fs.starfarer.api.ui.TooltipMakerAPI;
import exiledsector.effects.PlayerEngagementPipeline;
import exiledsector.effects.ShipEffectSource;
import exiledsector.items.ItemDataRules;
import exiledsector.items.ShipCustody;
import exiledsector.loot.FleetDifficulty;
import exiledsector.loot.LootSource;
import exiledsector.skills.ShipSkillData;
import exiledsector.skills.SkillTypeEffect;
import exiledsector.ui.cargo.ItemIconLooks;
import exiledsector.ui.inspect.ShipSummaryPart;
import exiledsector.ui.refit.RefitButtonColumn;
import exiledsector.ui.socket.BenchModeSupport;

import java.util.List;
import java.util.Map;

public interface ExiledSectorDomain extends Domain {

    default List<BenchModeSupport> benchModes() {
        return List.of();
    }

    default List<ItemDataRules> itemDataRules() {
        return List.of();
    }

    default List<ItemIconLooks> itemIconLooks() {
        return List.of();
    }

    default List<RefitButtonColumn.ButtonSpec> refitButtons() {
        return List.of();
    }

    default Map<PlayerEngagementPipeline.Stage, PlayerEngagementPipeline.Step> engagementSteps() {
        return Map.of();
    }

    default void fleetChanged(CampaignFleetAPI fleet) {
    }

    default void memberChanged(FleetMemberAPI member, ShipVariantAPI editedVariant) {
    }

    default List<SkillTypeEffect> fitEffects(ShipSkillData shipData, HullSize hullSize) {
        return List.of();
    }

    default ShipSkillData shipRecord(ShipVariantAPI variant) {
        return null;
    }

    default void describeShip(TooltipMakerAPI tooltip, HullSize hullSize, ShipAPI ship) {
    }

    default void refitScreenListed() {
    }

    default void playerFleetSynced() {
    }

    default List<ShipCustody> shipCustodies() {
        return List.of();
    }

    default boolean ignoresMemoryFlagUnlocks() {
        return false;
    }

    default List<ShipEffectSource> shipEffectSources() {
        return List.of();
    }

    default List<ShipSummaryPart> shipSummaryParts() {
        return List.of();
    }

    default List<LootSource> lootSources() {
        return List.of();
    }

    default List<FleetDifficulty> fleetDifficulties() {
        return List.of();
    }
}
