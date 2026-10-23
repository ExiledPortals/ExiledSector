package exiledsector.effects;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.combat.ShipHullSpecAPI;
import com.fs.starfarer.api.combat.ShipVariantAPI;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import exiledlib.Seeds;
import exiledsector.persistence.ShipSkillDataManager;
import exiledsector.skills.AllocatedNode;
import exiledsector.skills.ShipSkillData;
import exiledsector.skills.SkillDataResolver;
import exiledsector.skills.npc.NpcBuildRequest;
import exiledsector.skills.npc.NpcFactionVolumes;
import exiledsector.skills.npc.NpcFocus;
import exiledsector.skills.npc.NpcFreedOp;
import exiledsector.skills.npc.NpcHullMods;
import exiledsector.skills.npc.NpcLevelCaps;
import exiledsector.skills.npc.NpcLevelLimit;
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
import exiledsector.loot.LootSources;
import exiledsector.skills.npc.NpcNodeRange;

import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

public final class NpcFleetLeveller {

    private static final String CHART_SEED_PREFIX = "chart|";

    private NpcFleetLeveller() {
    }

    public static void ensure(CampaignFleetAPI npcFleet) {
        if (!isLevellable(npcFleet) || !NpcTreeConfig.isEnabled()) {
            return;
        }
        Map<String, String> recordsByMemberId = NpcTreeRecords.of(npcFleet.getMemoryWithoutUpdate());
        String seedPrefix = null;
        List<Integer> playerShipPowers = List.of();
        String fleetFactionId = npcFleet.getFaction() == null ? null : npcFleet.getFaction().getId();
        String factionRegion = fleetFactionId == null ? null : NpcFactionVolumes.regionFor(fleetFactionId);
        NpcNodeRange chartLevels = NpcNodeRange.of(LootSources.levelRange(npcFleet.getMemoryWithoutUpdate()), ShipLevelConfig.maxLevel(),
                ShipLevelConfig.maxAllocatedNodesBesidesRoot());
        NpcLevelLimit levelLimit = null;
        String chartSeed = LootSources.levelSeed(npcFleet.getMemoryWithoutUpdate());
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
                    playerShipPowers = chartLevels != null ? List.of() : PlayerTreePower.current();
                    seedPrefix = chartSeed != null ? CHART_SEED_PREFIX + chartSeed + "|"
                            : Global.getSector().getSeedString() + "|" + npcFleet.getId() + "|";
                }
                if (levelLimit == null) {
                    levelLimit = NpcLevelCaps.limitFor(npcFleet);
                }
                String memberSeedKey = chartSeed != null ? String.valueOf(memberIndex) : member.getId();
                memberRecord = decide(member, playerShipPowers, chartLevels, levelLimit, fleetFactionId, factionRegion,
                        Seeds.random(seedPrefix + memberSeedKey));
                recordsByMemberId.put(member.getId(), memberRecord);
            }
            if (NpcTreeRecords.isLevelled(memberRecord)) {
                apply(member, memberRecord);
                if (!NpcLevelCaps.hasTag(member.getVariant())) {
                    if (levelLimit == null) {
                        levelLimit = NpcLevelCaps.limitFor(npcFleet);
                    }
                    NpcLevelCaps.tag(member.getVariant(), levelLimit.capFor(member.isFlagship()));
                }
            }
        }
    }

    static boolean isLevellable(CampaignFleetAPI fleet) {
        return fleet != null && !fleet.isPlayerFleet() && (!fleet.isStationMode() || NpcTreeConfig.levelsStations())
                && fleet.getContainingLocation() != null && fleet.getFleetData() != null;
    }

    static boolean isPlayerShip(FleetMemberAPI member) {
        return member.getVariant().getPermaMods().contains(SkillTreeHullMod.ID) || ShipSkillDataManager.hasProgress(member.getId());
    }

    static String decide(FleetMemberAPI member, List<Integer> playerShipPowers, NpcNodeRange chartLevels, NpcLevelLimit levelLimit,
                         String fleetFactionId, String factionRegion, Random random) {
        if (!NpcShipSelector.isCandidate(member) || !NpcShipSelector.isChosen(member, random)) {
            return NpcTreeRecords.NOT_LEVELLED;
        }
        ShipProfile shipProfile = ShipProfile.of(member);
        int nodeCount = chartLevels != null ? chartLevels.roll(random)
                : levelLimit.nodeCount(NpcTreeSize.roll(playerShipPowers, random), ShipLevelConfig.maxAllocatedNodesBesidesRoot(),
                        member.isFlagship());
        NpcHullMods hullMods = NpcHullMods.of(member.getVariant());
        String designType = designType(member.getHullSpec());
        NpcTreeBuild treeBuild = NpcSkillTreeBuilder.generate(new NpcBuildRequest(shipProfile, designType, factionRegion, hullMods,
                NpcFreedOp.of(member, hullMods), nodeCount, NpcItems.anchorFor(factionRegion), SkillTypeUnlockStatus.lockedFor(null),
                member.getHullSpec().getHullId(), NpcFocus.of(fleetFactionId, member.getHullSpec().getShipSystemId(),
                member.getHullSpec().getShipDefenseId())), random);
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
