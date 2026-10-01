package exiledsector.effects;

import com.fs.starfarer.api.EveryFrameScript;
import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.combat.ShipVariantAPI;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import com.fs.starfarer.api.loading.VariantSource;
import exiledsector.persistence.ShipSkillDataManager;
import exiledsector.skills.ShipSkillData;
import exiledsector.skills.npc.NpcTreeTag;
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
        }
        if (changed) {
            syncedSinceLoad = true;
            playerFleet.getFleetData().setSyncNeeded();
        }
    }

    public static boolean ensureInstalled(FleetMemberAPI member, ShipVariantAPI editedVariant) {
        boolean changed = adoptNpcTree(member);
        changed |= ensureHullModAppliesLast(member);
        if (editedVariant != null && editedVariant != member.getVariant()) {
            if (NpcTreeTag.find(editedVariant) != null) {
                clearNpcTree(editedVariant);
                changed = true;
            }
            changed |= ensureAppliesLast(editedVariant);
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
            if (seenOurs) {
                return true;
            }
            seenOurs = SkillTreeHullMod.ID.equals(hullModId);
        }
        return false;
    }

    static boolean adoptNpcTree(FleetMemberAPI member) {
        String tag = NpcTreeTag.find(member.getVariant());
        if (tag == null) return false;

        ShipSkillData npcTree = NpcTreeTag.decode(tag);
        if (npcTree != null && ShipSkillDataManager.get(member.getId()).isBlank()) {
            npcTree.clearNpcBuild();
            ShipSkillDataManager.put(member.getId(), npcTree);
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
        ShipVariantAPI variant = member.getVariant();
        if (variant.getSource() == VariantSource.REFIT) {
            return variant;
        }
        ShipVariantAPI copy = variant.clone();
        copy.setSource(VariantSource.REFIT);
        member.setVariant(copy, false, true);
        return copy;
    }
}
