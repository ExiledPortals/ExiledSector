package exiledsector.effects;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.combat.ShipHullSpecAPI;
import com.fs.starfarer.api.combat.ShipVariantAPI;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import exiledsector.skills.AllocatedNode;
import exiledsector.skills.ShipSkillData;
import exiledsector.skills.SkillDataResolver;
import exiledsector.skills.npc.NpcBuildRequest;
import exiledsector.skills.npc.NpcFactionVolumes;
import exiledsector.skills.npc.NpcFreedOp;
import exiledsector.skills.npc.NpcHullMods;
import exiledsector.skills.npc.NpcLevelTable;
import exiledsector.skills.npc.NpcShipSelector;
import exiledsector.skills.npc.NpcSkillTreeBuilder;
import exiledsector.skills.npc.NpcTreeBuild;
import exiledsector.skills.npc.NpcTreeConfig;
import exiledsector.skills.npc.NpcTreeRecords;
import exiledsector.skills.npc.NpcTreeTag;
import exiledsector.skills.progression.ShipLevelConfig;
import exiledsector.skills.tags.ShipProfile;
import exiledsector.skills.unlock.SkillTypeUnlockStatus;
import exiledsector.socketables.NpcSocketables;
import exiledsector.socketables.SocketableDefinition;
import exiledsector.socketables.SocketableItemData;
import exiledsector.socketables.SocketableUnlock;

import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.function.Predicate;

public final class NpcFleetLeveller {

    private static final String SOCKETABLE_SEED_SUFFIX = "|socketables";

    private NpcFleetLeveller() {
    }

    public static void ensure(CampaignFleetAPI npcFleet) {
        if (!isLevellable(npcFleet) || !NpcTreeConfig.isEnabled()) {
            return;
        }
        Map<String, String> recordsByMemberId = NpcTreeRecords.of(npcFleet.getMemoryWithoutUpdate());
        String seedPrefix = null;
        int playerLevel = 0;
        String factionRegion = npcFleet.getFaction() == null ? null : NpcFactionVolumes.regionFor(npcFleet.getFaction().getId());
        for (FleetMemberAPI member : npcFleet.getFleetData().getMembersListCopy()) {
            String memberRecord = recordsByMemberId.get(member.getId());
            if (memberRecord == null) {
                if (seedPrefix == null) {
                    playerLevel = Global.getSector().getPlayerStats().getLevel();
                    seedPrefix = Global.getSector().getSeedString() + "|" + npcFleet.getId() + "|";
                }
                Random socketableRandom = member.isFlagship() && carriesSocketables(npcFleet)
                        ? new Random((seedPrefix + member.getId() + SOCKETABLE_SEED_SUFFIX).hashCode()) : null;
                memberRecord = decide(member, playerLevel, factionRegion, new Random((seedPrefix + member.getId()).hashCode()), socketableRandom,
                        definition -> SocketableUnlock.canDrop(definition, Global.getSector()));
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

    static boolean carriesSocketables(CampaignFleetAPI fleet) {
        return fleet.getFaction() != null && !fleet.getFaction().isPlayerFaction();
    }

    static String decide(FleetMemberAPI member, int playerLevel, String factionRegion, Random random, Random socketableRandom,
                         Predicate<SocketableDefinition> uniqueAllowed) {
        if (!NpcShipSelector.isCandidate(member) || !NpcShipSelector.isChosen(member, random)) {
            return NpcTreeRecords.NOT_LEVELLED;
        }
        ShipProfile shipProfile = ShipProfile.of(member);
        int nodeCount = Math.min(NpcLevelTable.roll(playerLevel, random), ShipLevelConfig.maxAllocatedNodesBesidesRoot());
        NpcHullMods hullMods = NpcHullMods.of(member.getVariant());
        String designType = designType(member.getHullSpec());
        int socketableCount = socketableRandom == null ? 0 : NpcSocketables.rollCount(playerLevel, socketableRandom);
        NpcTreeBuild treeBuild = NpcSkillTreeBuilder.generate(new NpcBuildRequest(shipProfile, designType, factionRegion, hullMods,
                NpcFreedOp.of(member, hullMods), nodeCount, socketableCount, type -> SkillTypeUnlockStatus.isLocked(type, null)), random);
        for (String socketNodeId : treeBuild.claimedSockets()) {
            SocketableDefinition definition = NpcSocketables.pickDefinition(socketableRandom, uniqueAllowed);
            if (definition != null) {
                treeBuild.shipData().socketItem(socketNodeId, NpcSocketables.id(SocketableItemData.rolled(definition, socketableRandom.nextLong())));
            }
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
