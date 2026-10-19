package exiledsector.effects;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.combat.ShipHullSpecAPI;
import com.fs.starfarer.api.combat.ShipVariantAPI;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import exiledsector.Seeds;
import exiledsector.charts.ChartIds;
import exiledsector.charts.ChartTiers;
import exiledsector.persistence.ShipSkillDataManager;
import exiledsector.skills.AllocatedNode;
import exiledsector.skills.FittedShipProfile;
import exiledsector.skills.ShipSkillData;
import exiledsector.skills.SkillDataResolver;
import exiledsector.skills.npc.NpcBuildRequest;
import exiledsector.skills.npc.NpcFactionVolumes;
import exiledsector.skills.npc.NpcFreedOp;
import exiledsector.skills.npc.NpcHullMods;
import exiledsector.skills.npc.NpcShipSelector;
import exiledsector.skills.npc.NpcSkillTreeBuilder;
import exiledsector.skills.npc.NpcTreeSize;
import exiledsector.skills.npc.NpcTreeBuild;
import exiledsector.skills.npc.NpcTreeConfig;
import exiledsector.skills.npc.NpcTreeRecords;
import exiledsector.skills.npc.NpcTreeTag;
import exiledsector.skills.progression.ShipLevelConfig;
import exiledsector.skills.tags.ShipProfile;
import exiledsector.skills.unlock.SkillTypeUnlockStatus;
import exiledsector.items.NpcItems;
import exiledsector.items.ItemDefinition;
import exiledsector.items.ItemData;
import exiledsector.items.ItemUnlock;
import exiledsector.items.SubroutineSocketLimits;

import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.function.Predicate;

public final class NpcFleetLeveller {

    private static final String HULL_ITEM_SEED_SUFFIX = "|socketables";
    private static final String CHART_SEED_PREFIX = "chart|";

    private NpcFleetLeveller() {
    }

    public static void ensure(CampaignFleetAPI npcFleet) {
        if (!isLevellable(npcFleet) || !NpcTreeConfig.isEnabled()) {
            return;
        }
        Map<String, String> recordsByMemberId = NpcTreeRecords.of(npcFleet.getMemoryWithoutUpdate());
        String seedPrefix = null;
        int playerLevel = 0;
        List<Integer> playerShipPowers = List.of();
        String fleetFactionId = npcFleet.getFaction() == null ? null : npcFleet.getFaction().getId();
        String factionRegion = fleetFactionId == null ? null : NpcFactionVolumes.regionFor(fleetFactionId);
        NpcTreeSize.PercentRange chartTreeRange = ChartTiers.fleetStrengthRangeFor(npcFleet.getMemoryWithoutUpdate());
        String chartSeed = ChartIds.fleetSeed(npcFleet.getMemoryWithoutUpdate());
        List<FleetMemberAPI> members = npcFleet.getFleetData().getMembersListCopy();
        for (int memberIndex = 0; memberIndex < members.size(); memberIndex++) {
            FleetMemberAPI member = members.get(memberIndex);
            if (isPlayerShip(member)) {
                recordsByMemberId.remove(member.getId());
                ShipTreeSync.dropNpcTree(member);
                continue;
            }
            String memberRecord = recordsByMemberId.get(member.getId());
            if (memberRecord == null) {
                if (seedPrefix == null) {
                    playerLevel = Global.getSector().getPlayerStats().getLevel();
                    playerShipPowers = PlayerTreePower.current();
                    seedPrefix = chartSeed != null ? CHART_SEED_PREFIX + chartSeed + "|"
                            : Global.getSector().getSeedString() + "|" + npcFleet.getId() + "|";
                }
                String memberSeedKey = chartSeed != null ? String.valueOf(memberIndex) : member.getId();
                Random itemRandom = member.isFlagship() && carriesItems(npcFleet)
                        ? Seeds.random(seedPrefix + memberSeedKey + HULL_ITEM_SEED_SUFFIX) : null;
                memberRecord = decide(member, playerLevel, playerShipPowers, chartTreeRange, factionRegion, fleetFactionId,
                        Seeds.random(seedPrefix + memberSeedKey), itemRandom, definition -> ItemUnlock.canDrop(definition, Global.getSector()));
                recordsByMemberId.put(member.getId(), memberRecord);
                NpcUniqueAlerts.markIfCarrying(npcFleet, memberRecord);
            }
            if (NpcTreeRecords.isLevelled(memberRecord)) {
                apply(member, memberRecord);
            }
        }
    }

    static boolean isLevellable(CampaignFleetAPI fleet) {
        return fleet != null && !fleet.isPlayerFleet() && !fleet.isStationMode()
                && fleet.getContainingLocation() != null && fleet.getFleetData() != null;
    }

    static boolean isPlayerShip(FleetMemberAPI member) {
        return member.getVariant().getPermaMods().contains(SkillTreeHullMod.ID) || ShipSkillDataManager.hasProgress(member.getId());
    }

    static boolean carriesItems(CampaignFleetAPI fleet) {
        return fleet.getFaction() != null && !fleet.getFaction().isPlayerFaction();
    }

    static String decide(FleetMemberAPI member, int playerLevel, List<Integer> playerShipPowers, String factionRegion, Random random,
                         Random itemRandom, Predicate<ItemDefinition> uniqueAllowed) {
        return decide(member, playerLevel, playerShipPowers, null, factionRegion, null, random, itemRandom, uniqueAllowed);
    }

    static String decide(FleetMemberAPI member, int playerLevel, List<Integer> playerShipPowers, NpcTreeSize.PercentRange treeRange,
                         String factionRegion, String fleetFactionId, Random random, Random itemRandom, Predicate<ItemDefinition> uniqueAllowed) {
        if (!NpcShipSelector.isCandidate(member) || !NpcShipSelector.isChosen(member, random)) {
            return NpcTreeRecords.NOT_LEVELLED;
        }
        ShipProfile shipProfile = ShipProfile.of(member);
        NpcTreeSize.PercentRange percentRange = treeRange != null ? treeRange : NpcTreeSize.percentRange();
        int nodeCount = Math.min(NpcTreeSize.roll(playerShipPowers, percentRange, random), ShipLevelConfig.maxAllocatedNodesBesidesRoot());
        NpcHullMods hullMods = NpcHullMods.of(member.getVariant());
        String designType = designType(member.getHullSpec());
        int itemCount = itemRandom == null ? 0 : NpcItems.rollCount(playerLevel, itemRandom);
        NpcTreeBuild treeBuild = NpcSkillTreeBuilder.generate(new NpcBuildRequest(shipProfile, designType, factionRegion, hullMods,
                NpcFreedOp.of(member, hullMods), nodeCount, itemCount, SkillTypeUnlockStatus.lockedFor(null),
                member.getHullSpec().getHullId()), random);
        for (String socketNodeId : treeBuild.claimedSockets()) {
            ItemDefinition definition = NpcItems.pickDefinition(itemRandom, fleetFactionId, uniqueAllowed);
            if (definition != null && !SubroutineSocketLimits.limitReached(definition, socketNodeId, treeBuild.shipData().getSocketedItems())) {
                treeBuild.shipData().socketItem(socketNodeId, NpcItems.id(ItemData.rolled(definition, itemRandom.nextLong())));
            }
        }
        if (itemRandom != null) {
            NpcItems.rollSectionItems(treeBuild.shipData(), member.getHullSpec().getHullSize(),
                    () -> FittedShipProfile.profile(member.getHullSpec(), member.getVariant(), treeBuild.shipData()), playerLevel, itemRandom);
        }
        return NpcTreeTag.encode(treeBuild.shipData());
    }

    static String designType(ShipHullSpecAPI hullSpec) {
        if (hullSpec == null) {
            return null;
        }
        String designType = hullSpec.getManufacturer();
        ShipHullSpecAPI baseHull = hullSpec.getBaseHull();
        if (!NpcSkillTreeBuilder.hasRootFor(designType) && baseHull != null && baseHull != hullSpec) {
            return baseHull.getManufacturer();
        }
        return designType;
    }

    static void apply(FleetMemberAPI member, String treeTag) {
        ShipVariantAPI currentVariant = member.getVariant();
        if (currentVariant.hasHullMod(SkillTreeHullMod.ID) && treeTag.equals(NpcTreeTag.find(currentVariant))) {
            return;
        }
        ShipVariantAPI ownedVariant = ShipTreeSync.ownedVariant(member);
        NpcTreeTag.removeAll(ownedVariant);
        ownedVariant.addTag(treeTag);
        stripConflictingHullMods(ownedVariant, SkillDataResolver.resolve(member, ownedVariant));
        if (!ownedVariant.hasHullMod(SkillTreeHullMod.ID)) {
            ownedVariant.addMod(SkillTreeHullMod.ID);
        }
        member.setStatUpdateNeeded(true);
    }

    private static void stripConflictingHullMods(ShipVariantAPI variant, ShipSkillData shipData) {
        Set<String> removableHullModIds = NpcHullMods.of(variant).removable();
        for (AllocatedNode allocated : AllocatedNode.of(shipData)) {
            for (String hullModId : allocated.exclusiveHullModIds()) {
                if (removableHullModIds.contains(hullModId)) {
                    variant.removeMod(hullModId);
                }
            }
        }
    }
}
