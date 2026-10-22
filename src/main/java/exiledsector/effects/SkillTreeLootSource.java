package exiledsector.effects;

import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.campaign.CargoAPI;
import com.fs.starfarer.api.campaign.CargoStackAPI;
import com.fs.starfarer.api.campaign.econ.MarketAPI;
import com.fs.starfarer.api.campaign.econ.SubmarketAPI;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import exiledsector.items.FactionFleetDrops;
import exiledsector.items.ItemData;
import exiledsector.items.ItemDefinition;
import exiledsector.items.WormholeAnchorDefinitions;
import exiledsector.loot.BattleEngagement;
import exiledsector.loot.BattleLoot;
import exiledsector.loot.FleetDifficulty;
import exiledsector.loot.LootSource;
import exiledsector.skills.npc.NpcTreeTag;
import org.apache.log4j.Logger;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

public final class SkillTreeLootSource implements LootSource, FleetDifficulty {

    public static final SkillTreeLootSource INSTANCE = new SkillTreeLootSource();
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
    public int shipLevel(FleetMemberAPI member) {
        String npcTreeTag = NpcTreeTag.find(member.getVariant());
        return npcTreeTag == null ? 0 : NpcTreeTag.allocatedNodeCount(npcTreeTag);
    }
}
