package exiledsector.ui;

import com.fs.starfarer.api.combat.ShipAPI.HullSize;
import com.fs.starfarer.api.combat.ShipVariantAPI;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import exiledsector.items.HullUpgradeRules;
import exiledlib.items.Item;
import exiledlib.items.ItemStore;
import exiledsector.items.ShipSection;
import exiledsector.persistence.ShipSkillDataManager;
import exiledsector.skills.ShipSections;
import exiledsector.skills.ShipSkillData;
import exiledsector.ui.refit.ShipEditSession;
import lunalib.lunaRefit.BaseRefitButton;

import java.util.List;

final class SectionEditor {

    private final ShipEditSession shipEdit;
    private final BaseRefitButton refitButton;

    SectionEditor(FleetMemberAPI member, ShipVariantAPI variant, BaseRefitButton refitButton) {
        this.shipEdit = new ShipEditSession(member, variant);
        this.refitButton = refitButton;
    }

    int revision() {
        return shipEdit.revision();
    }

    List<ShipSections.UnlockedSection> sections() {
        return ShipSections.forVariant(data(), shipEdit.variant());
    }

    int hullUpgradePoints() {
        return data().getHullUpgradePoints();
    }

    int unspentHullUpgradePoints() {
        return data().getUnspentHullUpgradePoints();
    }

    boolean isSectionUnlocked(ShipSection section) {
        return data().isSocketTypeUnlocked(section.id());
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

    boolean installUpgrade() {
        return apply(HullUpgradeRules.installUpgrade(data(), hullSize(), ItemStore.get()), true);
    }

    boolean unlockSection(ShipSection section) {
        return apply(HullUpgradeRules.unlock(data(), section, shipEdit.currentFit()), true);
    }

    boolean lockSection(ShipSection section) {
        return apply(HullUpgradeRules.lock(data(), section), false);
    }

    boolean installInSection(ShipSection section, Item item) {
        return apply(HullUpgradeRules.socket(data(), section, item, ShipSkillDataManager.all()), true);
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
