package exiledsector.charts.anomaly;

import com.fs.starfarer.api.campaign.BattleCreationPlugin;
import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.campaign.InteractionDialogPlugin;
import com.fs.starfarer.api.campaign.LocationAPI;
import com.fs.starfarer.api.campaign.SectorAPI;
import com.fs.starfarer.api.campaign.SectorEntityToken;
import com.fs.starfarer.api.campaign.rules.MemoryAPI;
import com.fs.starfarer.api.impl.campaign.procgen.StarSystemGenerator.StarSystemType;
import exiledsector.charts.atlas.AtlasAnomalies;
import exiledsector.charts.atlas.AtlasCatalystUse;
import exiledsector.charts.atlas.AtlasCell;
import exiledsector.charts.atlas.AtlasGenerator;
import exiledsector.charts.atlas.AtlasState;
import exiledsector.charts.atlas.AtlasTraits;
import exiledsector.charts.generation.ChartGenContext;
import exiledsector.charts.run.ChartRun;
import exiledsector.items.ItemChances;
import exiledsector.items.ItemDrops;
import exiledsector.items.UseVerdict;

import java.util.Random;

public interface ChartAnomaly {

    String id();

    default String primaryStar() {
        return null;
    }

    default StarSystemType systemType() {
        return null;
    }

    default boolean replacesStar() {
        return primaryStar() != null || systemType() != null;
    }

    default int minPlanets() {
        return 0;
    }

    default void shape(AnomalyLayoutSite site) {
    }

    default void generate(AnomalyContentSite site) {
    }

    default float atlasWeight(AtlasTraits traits, float weight) {
        return weight;
    }

    default boolean fitsAt(AtlasTraits traits) {
        return true;
    }

    default boolean holdsAt(AtlasGenerator generator, AtlasState state, AtlasCell cell) {
        return true;
    }

    default UseVerdict insertVerdict(AtlasGenerator generator, AtlasState state, AtlasCell cell) {
        return fitsAt(generator.traits(cell)) && holdsAt(generator, state, cell) ? UseVerdict.ALLOWED
                : UseVerdict.refused(AtlasCatalystUse.refusalKey(AtlasAnomalies.InsertCheck.DOES_NOT_FIT));
    }

    default boolean isActiveIn(LocationAPI location) {
        return true;
    }

    default String intelText(ChartRun run, LocationAPI location) {
        return null;
    }

    default void loadData() {
    }

    default void loadItems() {
    }

    default void preloadTextures() {
    }

    default void checkRun(SectorAPI sector) {
    }

    default void fleetGone(SectorAPI sector, CampaignFleetAPI fleet) {
    }

    default void clearFrom(CampaignFleetAPI fleet) {
    }

    default InteractionDialogPlugin dialogFor(SectorEntityToken target) {
        return null;
    }

    default BattleCreationPlugin battleFor(SectorEntityToken opponent) {
        return null;
    }

    default ItemDrops.SiteLoot fleetItems(SectorAPI sector, CampaignFleetAPI fleet, ChartRun run, ChartGenContext context, float share, Random random) {
        return null;
    }

    default ItemDrops.SiteLoot extraFleetLoot(CampaignFleetAPI fleet, ChartRun run, ChartGenContext context, Random random) {
        return ItemDrops.SiteLoot.NONE;
    }

    default boolean holdsBackBossUnique(ChartRun run) {
        return false;
    }

    default boolean holdsPlayerBattle() {
        return false;
    }

    default float battleLootMult(ChartGenContext context) {
        return 1f;
    }

    default ItemDrops.SiteLoot touchBattleLoot(ItemDrops.SiteLoot loot, Random random) {
        return loot;
    }

    default ItemChances siteChances(MemoryAPI siteMemory, ItemChances chances) {
        return chances;
    }

    static StarSystemType systemTypeOr(ChartAnomaly anomaly, StarSystemType fallback) {
        return anomaly != null && anomaly.systemType() != null ? anomaly.systemType() : fallback;
    }
}
