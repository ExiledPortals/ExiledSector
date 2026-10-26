package exiledsector.ui;

import com.fs.starfarer.api.combat.ShipAPI.HullSize;
import com.fs.starfarer.api.combat.ShipVariantAPI;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import exiledlib.skills.skilleffect.SkillEffect;
import exiledsector.effects.SkillTreeHullMod;
import exiledsector.items.HullUpgradePoints;
import exiledsector.items.HullUpgradeRules;
import exiledsector.items.Item;
import exiledsector.items.ItemStore;
import exiledsector.items.RefitSchematic;
import exiledsector.items.ShipSection;
import exiledsector.persistence.ShipSkillDataManager;
import exiledsector.skills.ShipSections;
import exiledsector.skills.ShipSkillData;
import exiledsector.ui.refit.ShipEditSession;
import lunalib.lunaRefit.BaseRefitButton;

import java.util.List;
import java.util.Map;

final class SectionEditor {

    private final ShipEditSession shipEdit;
    private final BaseRefitButton refitButton;
    private int seenDataRevision;
    private int seenEditRevision;

    SectionEditor(FleetMemberAPI member, ShipVariantAPI variant, BaseRefitButton refitButton) {
        this.shipEdit = new ShipEditSession(member, variant);
        this.refitButton = refitButton;
        rememberSeenRevisions();
    }

    int revision() {
        return shipEdit.revision();
    }

    void syncOutsideChanges() {
        if (data().revision() != seenDataRevision && shipEdit.revision() == seenEditRevision) {
            apply(true, false);
        }
        rememberSeenRevisions();
    }

    private void rememberSeenRevisions() {
        seenDataRevision = data().revision();
        seenEditRevision = shipEdit.revision();
    }

    List<ShipSections.UnlockedSection> sections() {
        return ShipSections.forVariant(data(), shipEdit.variant());
    }

    int hullUpgradePoints() {
        return HullUpgradePoints.of(data());
    }

    int unspentHullUpgradePoints() {
        return HullUpgradePoints.unspent(data());
    }

    HullUpgradeRules.UpgradeBlock schematicBlock(RefitSchematic schematic, int schematicsInStorage) {
        return HullUpgradeRules.upgradeBlock(data(), schematic, schematicsInStorage);
    }

    boolean isSectionUnlocked(ShipSection section) {
        return section != null && data().isSocketTypeUnlocked(section.id());
    }

    Map<SkillEffect, Float> affinityTotals() {
        return SkillTreeHullMod.affinityTotals(data(), shipEdit.member(), shipEdit.variant());
    }

    HullSize hullSize() {
        return shipEdit.hullSize();
    }

    HullUpgradeRules.UnlockBlock unlockBlock(ShipSection section) {
        return HullUpgradeRules.unlockBlock(data(), section, shipEdit.currentFit());
    }

    String unmetRequirement(ShipSection section) {
        return ShipSections.unmetRequirement(section, shipEdit.currentFit());
    }

    boolean applySchematic(RefitSchematic schematic) {
        return apply(HullUpgradeRules.applySchematic(data(), schematic, ItemStore.get()), true);
    }

    boolean unlockSection(ShipSection section) {
        return apply(HullUpgradeRules.unlock(data(), section, shipEdit.currentFit()), true);
    }

    boolean lockSection(ShipSection section) {
        return apply(HullUpgradeRules.lock(data(), section), false);
    }

    boolean installInSection(ShipSection section, Item item) {
        return apply(HullUpgradeRules.socket(data(), section, item, ShipSkillDataManager.all(), shipEdit.variant()::hasHullMod), true);
    }

    boolean removeFromSection(ShipSection section) {
        return apply(data().removeFromSection(section.id()) != null, false);
    }

    private ShipSkillData data() {
        return shipEdit.data();
    }

    private boolean apply(boolean applied, boolean socketedSound) {
        if (!shipEdit.changed(applied)) {
            return false;
        }
        if (refitButton != null) {
            refitButton.refreshVariant();
        }
        if (socketedSound) {
            SkillTreeSounds.socketed();
        }
        return true;
    }
}
