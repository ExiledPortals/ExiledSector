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

    public static void ensure(CampaignFleetAPI fleet) {
        if (!isLevellable(fleet) || !NpcTreeConfig.isEnabled()) {
            return;
        }
        Map<String, String> records = NpcTreeRecords.of(fleet.getMemoryWithoutUpdate());
        String seedPrefix = null;
        int playerLevel = 0;
        String factionRegion = fleet.getFaction() == null ? null : NpcFactionVolumes.regionFor(fleet.getFaction().getId());
        for (FleetMemberAPI member : fleet.getFleetData().getMembersListCopy()) {
            String record = records.get(member.getId());
            if (record == null) {
                if (seedPrefix == null) {
                    playerLevel = Global.getSector().getPlayerStats().getLevel();
                    seedPrefix = Global.getSector().getSeedString() + "|" + fleet.getId() + "|";
                }
                Random socketableRandom = member.isFlagship() && carriesSocketables(fleet)
                        ? new Random((seedPrefix + member.getId() + SOCKETABLE_SEED_SUFFIX).hashCode()) : null;
                record = decide(member, playerLevel, factionRegion, new Random((seedPrefix + member.getId()).hashCode()), socketableRandom,
                        definition -> SocketableUnlock.canDrop(definition, Global.getSector()));
                records.put(member.getId(), record);
                NpcUniqueAlerts.markIfCarrying(fleet, record);
            }
            if (NpcTreeRecords.isLevelled(record)) {
                apply(member, record);
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
        ShipProfile profile = ShipProfile.of(member);
        int nodeCount = Math.min(NpcLevelTable.roll(playerLevel, random), ShipLevelConfig.maxAllocatedNodesBesidesRoot());
        NpcHullMods hullMods = NpcHullMods.of(member.getVariant());
        String designType = designType(member.getHullSpec());
        int socketables = socketableRandom == null ? 0 : NpcSocketables.rollCount(playerLevel, socketableRandom);
        NpcTreeBuild build = NpcSkillTreeBuilder.generate(new NpcBuildRequest(profile, designType, factionRegion, hullMods,
                NpcFreedOp.of(member, hullMods), nodeCount, socketables, type -> SkillTypeUnlockStatus.isLocked(type, null)), random);
        for (String socket : build.claimedSockets()) {
            SocketableDefinition definition = NpcSocketables.pickDefinition(socketableRandom, uniqueAllowed);
            if (definition != null) {
                build.data().socketItem(socket, NpcSocketables.id(SocketableItemData.rolled(definition, socketableRandom.nextLong())));
            }
        }
        return NpcTreeTag.encode(build.data());
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

    static void apply(FleetMemberAPI member, String tag) {
        ShipVariantAPI current = member.getVariant();
        if (current.hasHullMod(SkillTreeHullMod.ID) && tag.equals(NpcTreeTag.find(current))) {
            return;
        }
        ShipVariantAPI variant = SkillTreeInstaller.ownedVariant(member);
        NpcTreeTag.removeAll(variant);
        variant.addTag(tag);
        stripConflictingHullMods(variant, SkillDataResolver.resolve(member, variant));
        if (!variant.hasHullMod(SkillTreeHullMod.ID)) {
            variant.addMod(SkillTreeHullMod.ID);
        }
        member.setStatUpdateNeeded(true);
    }

    private static void stripConflictingHullMods(ShipVariantAPI variant, ShipSkillData data) {
        Set<String> removable = NpcHullMods.of(variant).removable();
        for (AllocatedNode allocated : AllocatedNode.of(data)) {
            for (String hullModId : allocated.exclusiveHullModIds()) {
                if (removable.contains(hullModId)) {
                    variant.removeMod(hullModId);
                }
            }
        }
    }
}
