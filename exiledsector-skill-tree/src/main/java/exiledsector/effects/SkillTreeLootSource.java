package exiledsector.effects;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.campaign.CargoAPI;
import com.fs.starfarer.api.campaign.SectorAPI;
import com.fs.starfarer.api.campaign.SectorEntityToken;
import com.fs.starfarer.api.campaign.econ.SubmarketAPI;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import com.fs.starfarer.api.impl.campaign.FleetEncounterContext;
import com.fs.starfarer.api.impl.campaign.ids.Submarkets;
import com.fs.starfarer.api.util.Misc;
import exiledsector.compat.NightcrossCompat;
import exiledsector.compat.NightcrossUnlocks;
import exiledsector.compat.RandomAssortmentCompat;
import exiledsector.items.ItemData;
import exiledsector.items.ItemDefinition;
import exiledsector.items.ItemDefinitions;
import exiledsector.items.ItemType;
import exiledsector.items.anchor.AnchorJanusGate;
import exiledsector.items.anchor.FactionlessAnchors;
import exiledsector.items.anchor.WormholeAnchorDefinitions;
import exiledsector.items.drop.FactionFleetDrops;
import exiledsector.items.drop.ItemQuantity;
import exiledsector.items.subroutine.SubroutineKind;
import exiledsector.items.subroutine.doctrine.DoctrineArchives;
import exiledsector.loot.BattleEngagement;
import exiledsector.loot.BattleLoot;
import exiledsector.loot.DropOrigin;
import exiledsector.loot.DropType;
import exiledsector.loot.FleetDifficulty;
import exiledsector.loot.Loot;
import exiledsector.loot.LootDelivery;
import exiledsector.loot.LootSource;
import exiledsector.loot.LootSources;
import exiledsector.loot.MarketStock;
import exiledsector.skills.SkillTreeSetting;
import exiledsector.skills.npc.NpcTreeTag;
import exiledsector.skills.progression.ShipLevelSystem;
import org.apache.log4j.Logger;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.function.Predicate;

public final class SkillTreeLootSource implements LootSource, FleetDifficulty, MarketStock {

    public static final SkillTreeLootSource INSTANCE = new SkillTreeLootSource();
    static final int SHOWCASE_PICKS = 3;
    static final Map<String, Float> ANCHOR_CHANCE_MULT_BY_SUBMARKET = Map.of(Submarkets.SUBMARKET_OPEN, 0.5f, Submarkets.GENERIC_MILITARY, 1f,
            RandomAssortmentCompat.EXOSHIP_SUBMARKET, 1f);
    private static final long ANCHOR_SALVAGE_SEED_SALT = 0x616E63686F7273L;
    private static final String BATTLE_FIND = "battle loot";
    private static final float FULL_DIFFICULTY_STRENGTH = 1f;
    private static final Logger LOG = Logger.getLogger(SkillTreeLootSource.class);

    private static final Object FLEET_FACTION_STATE = new Object();
    private static final Object SUBROUTINE_QUANTITY_FACTION_STATE = new Object();

    private SkillTreeLootSource() {
    }

    private Map<String, Float> defeatedDeploymentPointsByFaction(BattleLoot battle) {
        return battle.state(this, LinkedHashMap::new);
    }

    private static Map<String, Float> defeatedDeploymentPointsByFleetFaction(BattleLoot battle) {
        return battle.state(FLEET_FACTION_STATE, LinkedHashMap::new);
    }

    private static Map<String, Float> subroutineQuantityWeightedDeploymentPointsByFaction(BattleLoot battle) {
        return battle.state(SUBROUTINE_QUANTITY_FACTION_STATE, LinkedHashMap::new);
    }

    private static float subroutineQuantity(CampaignFleetAPI sourceFleet) {
        return LootSources.fleetLootRules(sourceFleet).multipliers().quantity(DropType.SUBROUTINE);
    }

    @Override
    public void recordEngagement(BattleLoot battle, BattleEngagement engagement) {
        Map<String, Float> defeatedDeploymentPointsByFaction = defeatedDeploymentPointsByFaction(battle);
        engagement.deploymentPointsLostByFaction().forEach((factionId, deploymentPoints) ->
                defeatedDeploymentPointsByFaction.merge(factionId, deploymentPoints, Float::sum));
        Map<String, Float> defeatedDeploymentPointsByFleetFaction = defeatedDeploymentPointsByFleetFaction(battle);
        engagement.deploymentPointsLostByFleetFaction().forEach((factionId, deploymentPoints) ->
                defeatedDeploymentPointsByFleetFaction.merge(factionId, deploymentPoints, Float::sum));
        Map<String, Float> subroutineQuantityWeightedByFaction = subroutineQuantityWeightedDeploymentPointsByFaction(battle);
        engagement.deploymentPointsLostByFaction(SkillTreeLootSource::subroutineQuantity).forEach((factionId, deploymentPoints) ->
                subroutineQuantityWeightedByFaction.merge(factionId, deploymentPoints, Float::sum));
        GateRewardAnchors.recordEngagement(battle, engagement);
        DeserterBountyAnchors.recordEngagement(battle, engagement, Global.getSector());
        if (NightcrossCompat.isModEnabled()) {
            NightcrossUnlocks.recordEngagement(Global.getSector(), engagement);
        }
    }

    @Override
    public void addEncounterLoot(BattleLoot battle, CargoAPI loot) {
        List<ItemData> drops = battleDrops(battle);
        LootDelivery.toLootScreen(loot, new Loot(drops, List.of(), Map.of()), DropOrigin.ANYWHERE);
        WormholeAnchorDefinitions.recordFound(drops, Global.getSector(), BATTLE_FIND);
    }

    @Override
    public void deliverBattleLoot(BattleLoot battle, CargoAPI playerCargo) {
        List<ItemData> drops = battleDrops(battle);
        LootDelivery.deliver(playerCargo, new Loot(drops, List.of(), Map.of()), DropOrigin.ANYWHERE);
        WormholeAnchorDefinitions.recordFound(drops, Global.getSector(), BATTLE_FIND);
    }

    private List<ItemData> battleDrops(BattleLoot battle) {
        List<ItemData> drops = new ArrayList<>(factionFleetDrops(battle));
        drops.addAll(GateRewardAnchors.claim(battle, drops, Global.getSector()));
        drops.addAll(DeserterBountyAnchors.claim(battle, drops));
        return drops;
    }

    private List<ItemData> factionFleetDrops(BattleLoot battle) {
        if (!battle.playerWon()) {
            return List.of();
        }
        try {
            return FactionFleetDrops.roll(defeatedDeploymentPointsByFaction(battle), defeatedDeploymentPointsByFleetFaction(battle),
                    subroutineQuantityWeightedDeploymentPointsByFaction(battle), battle.random(),
                    FactionFleetDrops::available);
        } catch (RuntimeException e) {
            LOG.error("[ExiledSector] Rolling faction fleet item drops failed", e);
            return List.of();
        }
    }

    @Override
    public List<ItemData> bossDrops(int chartTier, String bossFactionId, ItemQuantity quantity, Random random,
                                      Predicate<ItemDefinition> allowed) {
        ItemData doctrine = DoctrineArchives.rollForBoss(chartTier, bossFactionId, quantity, random, allowed);
        return doctrine == null ? List.of() : List.of(doctrine);
    }

    @Override
    public void addSalvageLoot(SectorAPI sector, SectorEntityToken salvageEntity, CargoAPI loot) {
        Random random = new Random(Misc.getSalvageSeed(salvageEntity) ^ ANCHOR_SALVAGE_SEED_SALT);
        ItemData anchor = FactionlessAnchors.roll(ItemSalvageListener.siteId(salvageEntity), random, sector);
        if (anchor != null) {
            LootDelivery.toLootScreen(loot, new Loot(List.of(anchor), List.of(), Map.of()), DropOrigin.site(salvageEntity));
            WormholeAnchorDefinitions.recordFound(List.of(anchor), sector, "salvage");
        }
    }

    @Override
    public void addTechMiningFinds(TechMiningItemListener.Find find) {
        ItemData anchor = FactionlessAnchors.roll(find.dropSite(), find.random(), find.sector(), find.chanceMult());
        if (anchor != null) {
            find.items().add(anchor);
            WormholeAnchorDefinitions.recordFound(List.of(anchor), find.sector(), "tech mining");
        }
    }

    @Override
    public void itemsBought(List<ItemData> boughtItems) {
        WormholeAnchorDefinitions.recordFound(boughtItems, Global.getSector(), "bought from a market");
    }

    @Override
    public void addShowcaseLoot(CargoAPI loot, SectorEntityToken salvageEntity, Random random) {
        List<ItemData> shown = new ArrayList<>();
        List<ItemDefinition> doctrines = new ArrayList<>();
        for (ItemDefinition definition : ItemDefinitions.ofType(ItemType.SUBROUTINE)) {
            if (SubroutineKind.of(definition) == SubroutineKind.DOCTRINE) {
                if (!DoctrineArchives.infested(definition.id())) {
                    doctrines.add(definition);
                }
            } else if (!definition.unique()) {
                shown.add(ItemData.rolled(definition, random.nextLong()));
            }
        }
        List<ItemDefinition> anchors = List.copyOf(ItemDefinitions.ofType(ItemType.WORMHOLE_ANCHOR));
        List<ItemDefinition> activeAnchors = anchors.stream().filter(anchor -> WormholeAnchorDefinitions.volumeActive(anchor.id())).toList();
        List<ItemDefinition> offeredAnchors = activeAnchors.isEmpty()
                ? anchors.stream().filter(anchor -> !AnchorJanusGate.withholds(anchor.id(), Global.getSector())).toList()
                : activeAnchors.stream().filter(anchor -> WormholeAnchorDefinitions.obtainable(anchor.id(), Global.getSector())).toList();
        shown.addAll(randomPicks(doctrines, random));
        List<ItemData> anchorPicks = randomPicks(offeredAnchors, random);
        shown.addAll(anchorPicks);
        LootDelivery.toLootScreen(loot, new Loot(shown, List.of(), Map.of()), DropOrigin.site(salvageEntity));
        WormholeAnchorDefinitions.recordFound(anchorPicks, Global.getSector(), "showcase salvage");
    }

    private static List<ItemData> randomPicks(List<ItemDefinition> definitions, Random random) {
        List<ItemDefinition> shuffled = new ArrayList<>(definitions);
        Collections.shuffle(shuffled, random);
        List<ItemData> picks = new ArrayList<>();
        for (ItemDefinition definition : shuffled.subList(0, Math.min(SHOWCASE_PICKS, shuffled.size()))) {
            picks.add(ItemData.rolled(definition, random.nextLong()));
        }
        return picks;
    }

    @Override
    public boolean isOwnListing(ItemData listing) {
        ItemDefinition definition = listing == null ? null : listing.definition();
        return definition != null && definition.type() == ItemType.WORMHOLE_ANCHOR;
    }

    @Override
    public List<ItemData> restock(SubmarketAPI submarket, Random random) {
        Float chanceMult = ANCHOR_CHANCE_MULT_BY_SUBMARKET.get(submarket.getSpecId());
        if (chanceMult == null) {
            return List.of();
        }
        List<ItemData> anchors = new ArrayList<>();
        for (ItemDefinition definition : WormholeAnchorDefinitions.rollMarketStock(submarket.getMarket().getFactionId(), chanceMult, random)) {
            anchors.add(ItemData.rolled(definition, random.nextLong()));
        }
        return anchors;
    }

    @Override
    public Float difficultyMultiplier(float battleDifficulty) {
        return ShipLevelSystem.difficultyMultiplier(battleDifficulty, FULL_DIFFICULTY_STRENGTH, FleetEncounterContext.MAX_XP_MULT);
    }

    @Override
    public boolean ratesShipLevels() {
        return SkillTreeSetting.enabled();
    }

    @Override
    public int shipLevel(FleetMemberAPI member) {
        if (!SkillTreeSetting.enabled()) {
            return 0;
        }
        String npcTreeTag = NpcTreeTag.find(member.getVariant());
        return npcTreeTag == null ? 0 : NpcTreeTag.allocatedNodeCount(npcTreeTag);
    }
}
