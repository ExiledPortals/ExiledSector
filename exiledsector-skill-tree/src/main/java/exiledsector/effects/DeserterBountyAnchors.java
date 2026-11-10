package exiledsector.effects;

import com.fs.starfarer.api.campaign.FactionAPI;
import com.fs.starfarer.api.campaign.SectorAPI;
import com.fs.starfarer.api.campaign.comm.IntelInfoPlugin;
import com.fs.starfarer.api.campaign.comm.IntelManagerAPI;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import com.fs.starfarer.api.impl.campaign.intel.PersonBountyIntel;
import exiledsector.items.FactionFleetDrops;
import exiledsector.items.ItemData;
import exiledsector.items.ItemDefinition;
import exiledsector.items.WormholeAnchorDefinitions;
import exiledsector.loot.BattleEngagement;
import exiledsector.loot.BattleLoot;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Predicate;

public final class DeserterBountyAnchors {

    private static final Object BATTLE_STATE_KEY = new Object();

    private DeserterBountyAnchors() {
    }

    public static void recordEngagement(BattleLoot battle, BattleEngagement engagement, SectorAPI sector) {
        if (engagement == null || engagement.lostEnemies().isEmpty()) {
            return;
        }
        Set<String> deserterFactionIds = battle.state(BATTLE_STATE_KEY, LinkedHashSet::new);
        deserterFactionIds.addAll(beatenDeserterFactions(deserterBounties(sector), engagement.lostEnemies()));
    }

    static List<PersonBountyIntel> deserterBounties(SectorAPI sector) {
        IntelManagerAPI intelManager = sector == null ? null : sector.getIntelManager();
        if (intelManager == null) {
            return List.of();
        }
        List<PersonBountyIntel> bounties = new ArrayList<>();
        for (IntelInfoPlugin intel : intelManager.getIntel(PersonBountyIntel.class)) {
            if (intel instanceof PersonBountyIntel bounty && bounty.getBountyType() == PersonBountyIntel.BountyType.DESERTER) {
                bounties.add(bounty);
            }
        }
        return bounties;
    }

    static Set<String> beatenDeserterFactions(List<PersonBountyIntel> deserterBounties, List<FleetMemberAPI> lostEnemies) {
        Set<String> lostMemberIds = new HashSet<>();
        for (FleetMemberAPI member : lostEnemies) {
            if (member != null && member.getId() != null) {
                lostMemberIds.add(member.getId());
            }
        }
        Set<String> factionIds = new LinkedHashSet<>();
        for (PersonBountyIntel bounty : deserterBounties) {
            FleetMemberAPI flagship = bounty.getFlagship();
            FactionAPI faction = bounty.getFaction();
            if (flagship != null && faction != null && lostMemberIds.contains(flagship.getId())) {
                factionIds.add(faction.getId());
            }
        }
        return factionIds;
    }

    public static List<ItemData> claim(BattleLoot battle, List<ItemData> drops, Predicate<ItemDefinition> allowed) {
        List<ItemData> rewards = new ArrayList<>();
        if (!battle.playerWon()) {
            return rewards;
        }
        Set<String> deserterFactionIds = battle.state(BATTLE_STATE_KEY, LinkedHashSet::new);
        for (String factionId : deserterFactionIds) {
            List<ItemDefinition> anchors = WormholeAnchorDefinitions.factionAnchors(factionId).stream().filter(allowed).toList();
            if (anchors.isEmpty() || alreadyDropped(anchors, drops) || alreadyDropped(anchors, rewards)) {
                continue;
            }
            rewards.add(ItemData.rolled(anchors.get(battle.random().nextInt(anchors.size())), battle.random().nextLong()));
        }
        return rewards;
    }

    public static List<ItemData> claim(BattleLoot battle, List<ItemData> drops) {
        return claim(battle, drops, FactionFleetDrops::available);
    }

    private static boolean alreadyDropped(List<ItemDefinition> anchors, List<ItemData> drops) {
        return drops.stream().anyMatch(drop -> anchors.contains(drop.definition()));
    }
}
