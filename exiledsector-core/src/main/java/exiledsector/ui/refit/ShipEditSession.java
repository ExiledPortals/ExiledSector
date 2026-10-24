package exiledsector.ui.refit;

import com.fs.starfarer.api.combat.ShipAPI.HullSize;
import com.fs.starfarer.api.combat.ShipHullSpecAPI;
import com.fs.starfarer.api.combat.ShipVariantAPI;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import exiledlib.PlayerFleet;
import exiledlib.skills.skilleffect.logistics.FleetWideEffects;
import exiledsector.ExiledSector;
import exiledsector.effects.FighterBayOverflow;
import exiledsector.effects.SkillTreeHullMod;
import exiledsector.effects.UnfitWeapons;
import exiledsector.persistence.ShipSkillDataManager;
import exiledsector.skills.FittedShipProfile;
import exiledsector.skills.ShipSkillData;
import exiledsector.skills.tags.ShipProfile;

public final class ShipEditSession {

    private final FleetMemberAPI member;
    private final ShipVariantAPI variant;
    private int revision;

    public ShipEditSession(FleetMemberAPI member, ShipVariantAPI variant) {
        this.member = member;
        this.variant = variant;
    }

    public FleetMemberAPI member() {
        return member;
    }

    public ShipVariantAPI variant() {
        return variant;
    }

    public ShipSkillData data() {
        return ShipSkillDataManager.get(member.getId());
    }

    public HullSize hullSize() {
        return member.getHullSpec().getHullSize();
    }

    public ShipProfile currentFit() {
        return FittedShipProfile.profile(member.getHullSpec(), variant, data());
    }

    public int revision() {
        return revision;
    }

    public boolean changed(boolean applied) {
        if (!applied) {
            return false;
        }
        revision++;
        ShipHullSpecAPI hullSpecBefore = variant.getHullSpec();
        ExiledSector.domains().forEach(domain -> domain.memberChanged(member, variant));
        FleetWideEffects.markPhaseFieldStale();
        member.setStatUpdateNeeded(true);
        member.updateStats();
        SkillTreeHullMod.syncHybridBallisticSlots(member, variant);
        FighterBayOverflow.returnUnhousedWingsAndAnnounce(member, variant, PlayerFleet.cargoOrNull());
        UnfitWeapons.returnUnfitWeaponsAndAnnounce(member, variant, PlayerFleet.cargoOrNull());
        if (variant.getHullSpec() != hullSpecBefore) {
            RefitShipDisplay.rebuildSlots(member, variant);
        }
        return true;
    }
}
