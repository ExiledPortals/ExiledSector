package exiledsector.effects;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.campaign.SectorAPI;
import com.fs.starfarer.api.characters.MutableCharacterStatsAPI;
import com.fs.starfarer.api.combat.ShipVariantAPI;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import com.fs.starfarer.api.loading.VariantSource;
import exiledsector.persistence.ShipSkillDataManager;
import exiledsector.skills.AllocatedNode;
import exiledsector.skills.InstalledHullMods;
import exiledsector.skills.ShipSkillData;
import exiledsector.skills.SkillDataResolver;
import exiledsector.skills.SkillTree;
import exiledsector.skills.npc.NpcTreeTag;
import exiledsector.skills.progression.ShipLevelConfig;
import exiledsector.skills.progression.ShipLevelSystem;
import exiledsector.socketables.NpcSocketables;

import java.util.Collection;
import java.util.List;
import java.util.Set;

public final class ShipTreeSync {

    private static boolean playerFleetSyncRequested;

    private ShipTreeSync() {
    }

    public static void requestPlayerFleetSync() {
        playerFleetSyncRequested = true;
    }

    static boolean takePlayerFleetSyncRequest() {
        boolean requested = playerFleetSyncRequested;
        playerFleetSyncRequested = false;
        return requested;
    }

    public static boolean fleetChanged(CampaignFleetAPI fleet) {
        if (fleet == null || fleet.getFleetData() == null) return false;

        boolean changed = false;
        for (FleetMemberAPI member : fleet.getFleetData().getMembersListCopy()) {
            changed |= installTree(member);
        }
        if (changed) {
            fleet.getFleetData().setSyncNeeded();
        }
        return changed;
    }

    public static void afterPlayerEngagement(CampaignFleetAPI playerFleet) {
        if (playerFleet == null) return;

        fleetChanged(playerFleet);
        raiseToLevelFloor(playerFleet);
    }

    public static void levelsChanged(CampaignFleetAPI fleet, Collection<FleetMemberAPI> levelledMembers) {
        if (levelledMembers.isEmpty()) return;

        levelledMembers.forEach(member -> member.setStatUpdateNeeded(true));
        if (fleet != null && fleet.getFleetData() != null) {
            fleet.getFleetData().setSyncNeeded();
        }
    }

    public static boolean memberChanged(FleetMemberAPI member, ShipVariantAPI editedVariant) {
        boolean changed = installTree(member);
        changed |= raiseToLevelFloor(member, currentLevelFloor());
        ShipVariantAPI variantBeingEdited = editedVariant != null ? editedVariant : member.getVariant();
        if (variantBeingEdited != member.getVariant()) {
            changed |= installTree(variantBeingEdited);
        }
        changed |= syncVariant(member, variantBeingEdited);
        if (changed) {
            member.setStatUpdateNeeded(true);
        }
        return changed;
    }

    public static boolean syncVariant(FleetMemberAPI member, ShipVariantAPI variant) {
        ShipSkillData shipData = variant == null ? null : SkillDataResolver.resolve(member, variant);
        if (shipData == null) return false;

        List<AllocatedNode> allocatedNodes = AllocatedNode.of(shipData);
        return syncVariant(member, variant, allocatedNodes, ResolvedTree.phantomHullModIdsOf(allocatedNodes));
    }

    static void syncVariant(FleetMemberAPI member, ShipVariantAPI variant, ResolvedTree resolvedTree) {
        syncVariant(member, variant, resolvedTree.allocated(), resolvedTree.phantomHullModIds());
    }

    private static boolean syncVariant(FleetMemberAPI member, ShipVariantAPI variant, List<AllocatedNode> allocatedNodes,
                                       Set<String> phantomHullModIds) {
        boolean isNpcTree = SkillDataResolver.isNpcTree(variant);
        if (!isNpcTree) {
            SkillDataResolver.syncShipTag(member, variant);
            OpReserveHullMods.sync(member, variant);
        }
        PhantomConflictWatch.inspect(member, variant, phantomHullModIds, !isNpcTree);
        Set<String> placeableHullModIds = PhantomConflictWatch.withoutLearnedConflicts(phantomHullModIds, variant);
        PhantomConflictWatch.queueRevertsForBlocked(member, phantomHullModIds, placeableHullModIds, !isNpcTree);
        boolean phantomsChanged = PhantomInstallSync.sync(placeableHullModIds, variant);
        if (isNpcTree) {
            HullModConflictResolver.removeHullModsThatTriedToStripAPhantom(allocatedNodes, variant, false);
        } else {
            HullModConflictResolver.removeConflicts(allocatedNodes, variant);
        }
        return phantomsChanged;
    }

    static void raiseToLevelFloor(CampaignFleetAPI fleet) {
        int levelFloor = currentLevelFloor();
        boolean changed = false;
        for (FleetMemberAPI member : fleet.getFleetData().getMembersListCopy()) {
            changed |= raiseToLevelFloor(member, levelFloor);
        }
        if (changed) {
            fleet.getFleetData().setSyncNeeded();
        }
    }

    static int currentLevelFloor() {
        SectorAPI sector = Global.getSector();
        MutableCharacterStatsAPI playerStats = sector == null ? null : sector.getPlayerStats();
        if (playerStats == null) return 0;
        return ShipLevelSystem.levelFloor(playerStats.getLevel(), ShipLevelConfig.levelFloorPercent(), ShipLevelConfig.maxLevel());
    }

    static boolean raiseToLevelFloor(FleetMemberAPI member, int levelFloor) {
        if (levelFloor <= 0) return false;
        ShipSkillData shipData = ShipSkillDataManager.get(member.getId());
        if (!ShipLevelSystem.raiseToLevel(shipData, levelFloor, SkillTree.getAllNodes().values())) return false;
        member.setStatUpdateNeeded(true);
        return true;
    }

    private static boolean installTree(FleetMemberAPI member) {
        boolean changed = adoptNpcTree(member);
        ShipVariantAPI variant = member.getVariant().hasHullMod(SkillTreeHullMod.ID) ? member.getVariant() : ownedVariant(member);
        if (ensureAppliesLast(variant)) {
            changed = true;
        }
        if (changed) {
            member.setStatUpdateNeeded(true);
        }
        return changed;
    }

    private static boolean installTree(ShipVariantAPI variant) {
        boolean changed = false;
        if (NpcTreeTag.find(variant) != null) {
            clearNpcTree(variant);
            changed = true;
        }
        return ensureAppliesLast(variant) || changed;
    }

    private static boolean ensureAppliesLast(ShipVariantAPI variant) {
        if (!variant.hasHullMod(SkillTreeHullMod.ID)) {
            variant.addPermaMod(SkillTreeHullMod.ID);
        } else if (hasHullModsAfterOurs(variant)) {
            variant.removePermaMod(SkillTreeHullMod.ID);
            variant.addPermaMod(SkillTreeHullMod.ID);
        } else {
            return false;
        }
        return true;
    }

    private static boolean hasHullModsAfterOurs(ShipVariantAPI variant) {
        boolean seenOurs = false;
        for (String hullModId : variant.getHullMods()) {
            if (seenOurs && !isPlacedByTheTree(variant, hullModId)) {
                return true;
            }
            seenOurs |= SkillTreeHullMod.ID.equals(hullModId);
        }
        return false;
    }

    private static boolean isPlacedByTheTree(ShipVariantAPI variant, String hullModId) {
        return SkillConflictWarningHullMod.ID.equals(hullModId) || hullModId.startsWith(OpReserveHullMods.ID_PREFIX)
                || InstalledHullMods.isInstalledBySkillTree(variant, hullModId);
    }

    private static boolean adoptNpcTree(FleetMemberAPI member) {
        String npcTreeTag = NpcTreeTag.find(member.getVariant());
        if (npcTreeTag == null) return false;

        ShipSkillData npcTree = NpcTreeTag.decode(npcTreeTag);
        if (npcTree != null && ShipSkillDataManager.get(member.getId()).isBlank()) {
            npcTree.clearNpcBuild();
            NpcSocketables.claimForPlayer(npcTree);
            ShipSkillDataManager.put(member.getId(), npcTree);
        } else if (npcTree != null) {
            NpcSocketables.storeForPlayer(npcTree);
        }

        clearNpcTree(ownedVariant(member));
        return true;
    }

    private static void clearNpcTree(ShipVariantAPI variant) {
        NpcTreeTag.removeAll(variant);
        if (variant.hasHullMod(SkillTreeHullMod.ID) && !variant.getPermaMods().contains(SkillTreeHullMod.ID)) {
            variant.removeMod(SkillTreeHullMod.ID);
        }
    }

    static ShipVariantAPI ownedVariant(FleetMemberAPI member) {
        ShipVariantAPI currentVariant = member.getVariant();
        if (currentVariant.getSource() == VariantSource.REFIT) {
            return currentVariant;
        }
        ShipVariantAPI refitCopy = currentVariant.clone();
        refitCopy.setSource(VariantSource.REFIT);
        member.setVariant(refitCopy, false, true);
        return refitCopy;
    }
}
