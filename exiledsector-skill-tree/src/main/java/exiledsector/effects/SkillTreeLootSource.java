package exiledsector.effects;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.campaign.CargoAPI;
import com.fs.starfarer.api.campaign.CargoStackAPI;
import com.fs.starfarer.api.campaign.SectorAPI;
import com.fs.starfarer.api.campaign.SectorEntityToken;
import com.fs.starfarer.api.campaign.econ.MarketAPI;
import com.fs.starfarer.api.campaign.econ.SubmarketAPI;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import com.fs.starfarer.api.util.Misc;
import exiledsector.items.AnchorJanusGate;
import exiledsector.items.DoctrineArchives;
import exiledsector.items.FactionFleetDrops;
import exiledsector.items.FactionlessAnchors;
import exiledsector.items.ItemData;
import exiledsector.items.ItemDefinition;
import exiledsector.items.ItemDefinitions;
import exiledsector.items.ItemType;
import exiledsector.items.SubroutineKind;
import exiledsector.items.WormholeAnchorDefinitions;
import exiledsector.loot.BattleEngagement;
import exiledsector.loot.BattleLoot;
import exiledsector.loot.FleetDifficulty;
import exiledsector.loot.LootSource;
import exiledsector.skills.SkillTreeSetting;
import exiledsector.skills.npc.NpcTreeTag;
import exiledsector.skills.progression.ShipLevelConfig;
import exiledsector.skills.progression.ShipLevelSystem;
import org.apache.log4j.Logger;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.function.Predicate;

public final class SkillTreeLootSource implements LootSource, FleetDifficulty {

    public static final SkillTreeLootSource INSTANCE = new SkillTreeLootSource();
    static final int SHOWCASE_PICKS = 3;
    private static final long ANCHOR_SALVAGE_SEED_SALT = 0x616E63686F7273L;
    private static final String BATTLE_FIND = "battle loot";
    private static final Logger LOG = Logger.getLogger(SkillTreeLootSource.class);

    private SkillTreeLootSource() {
    }

    private Map<String, Float> defeatedDeploymentPointsByFaction(BattleLoot battle) {
        return battle.state(this, LinkedHashMap::new);
    }

    @Override
    public void recordEngagement(BattleLoot battle, BattleEngagement engagement) {
        holdFactionLosses(defeatedDeploymentPointsByFaction(battle), engagement);
        GateRewardAnchors.recordEngagement(battle, engagement);
    }

    static void holdFactionLosses(Map<String, Float> defeatedDeploymentPointsByFaction, BattleEngagement engagement) {
        for (FleetMemberAPI member : engagement.lostEnemies()) {
            CampaignFleetAPI sourceFleet = member == null ? null : engagement.battle().getSourceFleet(member);
            String factionId = sourceFleet == null || sourceFleet.getFaction() == null ? null : sourceFleet.getFaction().getId();
            if (factionId != null) {
                defeatedDeploymentPointsByFaction.merge(factionId, member.getDeploymentPointsCost(), Float::sum);
            }
        }
    }

    @Override
    public void addEncounterLoot(BattleLoot battle, CargoAPI loot) {
        List<ItemData> drops = battleDrops(battle);
        drops.forEach(item -> loot.addSpecial(item.toSpecialItem(), 1f));
        WormholeAnchorDefinitions.recordFound(drops, Global.getSector(), BATTLE_FIND);
    }

    @Override
    public void deliverBattleLoot(BattleLoot battle, CargoAPI playerCargo) {
        List<ItemData> drops = battleDrops(battle);
        ItemLootListener.deliverItems(playerCargo, drops);
        WormholeAnchorDefinitions.recordFound(drops, Global.getSector(), BATTLE_FIND);
    }

    private List<ItemData> battleDrops(BattleLoot battle) {
        List<ItemData> drops = new ArrayList<>(factionFleetDrops(battle));
        drops.addAll(GateRewardAnchors.claim(battle, drops, Global.getSector()));
        return drops;
    }

    private List<ItemData> factionFleetDrops(BattleLoot battle) {
        if (!battle.playerWon()) {
            return List.of();
        }
        try {
            return FactionFleetDrops.roll(defeatedDeploymentPointsByFaction(battle), battle.random(), FactionFleetDrops::available);
        } catch (RuntimeException e) {
            LOG.error("[ExiledSector] Rolling faction fleet item drops failed", e);
            return List.of();
        }
    }

    @Override
    public List<ItemData> wardenDrops(int chartTier, String wardenFactionId, float chanceMult, Random random,
                                      Predicate<ItemDefinition> allowed) {
        ItemData doctrine = DoctrineArchives.rollForWarden(chartTier, wardenFactionId, chanceMult, random, allowed);
        return doctrine == null ? List.of() : List.of(doctrine);
    }

    @Override
    public void addSalvageLoot(SectorAPI sector, SectorEntityToken salvageEntity, CargoAPI loot) {
        Random random = new Random(Misc.getSalvageSeed(salvageEntity) ^ ANCHOR_SALVAGE_SEED_SALT);
        ItemData anchor = FactionlessAnchors.roll(ItemSalvageListener.siteId(salvageEntity), random, sector);
        if (anchor != null) {
            loot.addSpecial(anchor.toSpecialItem(), 1f);
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
    public void addShowcaseLoot(CargoAPI loot, Random random) {
        List<ItemDefinition> doctrines = new ArrayList<>();
        for (ItemDefinition definition : ItemDefinitions.ofType(ItemType.SUBROUTINE)) {
            if (SubroutineKind.of(definition) == SubroutineKind.DOCTRINE) {
                doctrines.add(definition);
            } else if (!definition.unique()) {
                loot.addSpecial(ItemData.rolled(definition, random.nextLong()).toSpecialItem(), 1f);
            }
        }
        List<ItemDefinition> anchors = List.copyOf(ItemDefinitions.ofType(ItemType.WORMHOLE_ANCHOR));
        List<ItemDefinition> activeAnchors = anchors.stream().filter(anchor -> WormholeAnchorDefinitions.areaActive(anchor.id())).toList();
        List<ItemDefinition> offeredAnchors = activeAnchors.isEmpty()
                ? anchors.stream().filter(anchor -> !AnchorJanusGate.withholds(anchor.id(), Global.getSector())).toList()
                : activeAnchors.stream().filter(anchor -> WormholeAnchorDefinitions.obtainable(anchor.id(), Global.getSector())).toList();
        addRandomPicks(loot, doctrines, random);
        WormholeAnchorDefinitions.recordFound(addRandomPicks(loot, offeredAnchors, random), Global.getSector(), "showcase salvage");
    }

    private static List<ItemData> addRandomPicks(CargoAPI loot, List<ItemDefinition> definitions, Random random) {
        List<ItemDefinition> shuffled = new ArrayList<>(definitions);
        Collections.shuffle(shuffled, random);
        List<ItemData> picks = new ArrayList<>();
        for (ItemDefinition definition : shuffled.subList(0, Math.min(SHOWCASE_PICKS, shuffled.size()))) {
            ItemData pick = ItemData.rolled(definition, random.nextLong());
            loot.addSpecial(pick.toSpecialItem(), 1f);
            picks.add(pick);
        }
        return picks;
    }

    @Override
    public void stockSubmarket(SubmarketAPI submarket, CargoAPI cargo, Random random) {
        if (MarketItemStock.ANCHOR_SUBMARKETS.contains(submarket.getSpecId())) {
            stockAnchors(submarket.getMarket(), cargo, random);
        }
    }

    private static void stockAnchors(MarketAPI market, CargoAPI cargo, Random random) {
        for (CargoStackAPI stack : cargo.getStacksCopy()) {
            if (stack.isSpecialStack() && ItemData.isMarketListing(stack.getSpecialDataIfSpecial())) {
                cargo.removeStack(stack);
            }
        }
        for (ItemDefinition definition : WormholeAnchorDefinitions.rollMarketStock(market.getFactionId(), random)) {
            cargo.addSpecial(ItemData.rolled(definition, random.nextLong()).toMarketListing(), 1f);
        }
    }

    @Override
    public Float difficultyMultiplier(float battleDifficulty) {
        return ShipLevelSystem.difficultyMultiplier(battleDifficulty, ShipLevelConfig.xpDifficultyStrength(),
                ShipLevelConfig.xpDifficultyMaxMultiplier());
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
