package exiledsector.effects;

import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.campaign.CargoAPI;
import com.fs.starfarer.api.campaign.CargoStackAPI;
import com.fs.starfarer.api.campaign.econ.MarketAPI;
import com.fs.starfarer.api.campaign.econ.SubmarketAPI;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import exiledsector.items.DoctrineArchives;
import exiledsector.items.FactionFleetDrops;
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
    private static final Logger LOG = Logger.getLogger(SkillTreeLootSource.class);

    private SkillTreeLootSource() {
    }

    private Map<String, Float> defeatedDeploymentPointsByFaction(BattleLoot battle) {
        return battle.state(this, LinkedHashMap::new);
    }

    @Override
    public void recordEngagement(BattleLoot battle, BattleEngagement engagement) {
        holdFactionLosses(defeatedDeploymentPointsByFaction(battle), engagement);
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
        factionFleetDrops(battle).forEach(item -> loot.addSpecial(item.toSpecialItem(), 1f));
    }

    @Override
    public void deliverBattleLoot(BattleLoot battle, CargoAPI playerCargo) {
        ItemLootListener.deliverItems(playerCargo, factionFleetDrops(battle));
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
        addRandomPicks(loot, doctrines, random);
        addRandomPicks(loot, activeAnchors.isEmpty() ? anchors : activeAnchors, random);
    }

    private static void addRandomPicks(CargoAPI loot, List<ItemDefinition> definitions, Random random) {
        List<ItemDefinition> shuffled = new ArrayList<>(definitions);
        Collections.shuffle(shuffled, random);
        for (ItemDefinition definition : shuffled.subList(0, Math.min(SHOWCASE_PICKS, shuffled.size()))) {
            loot.addSpecial(ItemData.rolled(definition, random.nextLong()).toSpecialItem(), 1f);
        }
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
