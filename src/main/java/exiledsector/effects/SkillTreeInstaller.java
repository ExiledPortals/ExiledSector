package exiledsector.effects;

import com.fs.starfarer.api.EveryFrameScript;
import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.campaign.SectorAPI;
import com.fs.starfarer.api.characters.MutableCharacterStatsAPI;
import com.fs.starfarer.api.combat.ShipVariantAPI;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import com.fs.starfarer.api.loading.VariantSource;
import exiledsector.persistence.ShipSkillDataManager;
import exiledsector.skills.ShipSkillData;
import exiledsector.skills.SkillTree;
import exiledsector.skills.progression.ShipLevelConfig;
import exiledsector.skills.progression.ShipLevelSystem;
import exiledsector.skills.npc.NpcTreeTag;
import exiledsector.socketables.NpcSocketables;
import exiledsector.skills.skilleffect.FleetWideEffects;

public class SkillTreeInstaller implements EveryFrameScript {

    private static final float CHECK_INTERVAL_SECONDS = 1f;

    private float timeSinceLastCheck = CHECK_INTERVAL_SECONDS;
    private boolean syncedSinceLoad;

    @Override
    public boolean isDone() {
        return false;
    }

    @Override
    public boolean runWhilePaused() {
        return false;
    }

    @Override
    public void advance(float amount) {
        timeSinceLastCheck += amount;
        if (timeSinceLastCheck >= CHECK_INTERVAL_SECONDS) {
            timeSinceLastCheck = 0f;
            checkPlayerShips();
        }
        FleetWideEffects.recomputeExtendedPhaseFieldIfStale();
    }

    private void checkPlayerShips() {
        CampaignFleetAPI playerFleet = Global.getSector().getPlayerFleet();
        if (playerFleet == null) return;

        boolean changed = !syncedSinceLoad;
        for (FleetMemberAPI member : playerFleet.getFleetData().getMembersListCopy()) {
            changed |= adoptNpcTree(member);
            changed |= ensureHullModAppliesLast(member);
            changed |= restoreInstalledHullMods(member);
        }
        if (changed) {
            syncedSinceLoad = true;
            playerFleet.getFleetData().setSyncNeeded();
        }
    }

    public static void raiseToLevelFloor(CampaignFleetAPI fleet) {
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

    public static void adoptNpcTrees(CampaignFleetAPI fleet) {
        boolean changed = false;
        for (FleetMemberAPI member : fleet.getFleetData().getMembersListCopy()) {
            changed |= adoptNpcTree(member);
        }
        if (changed) {
            fleet.getFleetData().setSyncNeeded();
        }
    }

    public static boolean ensureInstalled(FleetMemberAPI member, ShipVariantAPI editedVariant) {
        boolean changed = adoptNpcTree(member);
        changed |= raiseToLevelFloor(member, currentLevelFloor());
        changed |= ensureHullModAppliesLast(member);
        changed |= restoreInstalledHullMods(member);
        if (editedVariant != null && editedVariant != member.getVariant()) {
            if (NpcTreeTag.find(editedVariant) != null) {
                clearNpcTree(editedVariant);
                changed = true;
            }
            changed |= ensureAppliesLast(editedVariant);
            changed |= PhantomInstallSync.restoreInstalledPermaMods(editedVariant);
        }
        return changed;
    }

    private static boolean ensureHullModAppliesLast(FleetMemberAPI member) {
        ShipVariantAPI variant = member.getVariant().hasHullMod(SkillTreeHullMod.ID) ? member.getVariant() : ownedVariant(member);
        if (!ensureAppliesLast(variant)) {
            return false;
        }
        member.setStatUpdateNeeded(true);
        return true;
    }

    private static boolean restoreInstalledHullMods(FleetMemberAPI member) {
        if (!PhantomInstallSync.restoreInstalledPermaMods(member.getVariant())) {
            return false;
        }
        member.setStatUpdateNeeded(true);
        return true;
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
            if (seenOurs && !SkillConflictWarningHullMod.ID.equals(hullModId)) {
                return true;
            }
            seenOurs |= SkillTreeHullMod.ID.equals(hullModId);
        }
        return false;
    }

    static boolean adoptNpcTree(FleetMemberAPI member) {
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
        member.setStatUpdateNeeded(true);
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
