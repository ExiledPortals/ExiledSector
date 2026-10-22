package exiledsector.ui.refit;

import com.fs.starfarer.api.combat.ShipAPI.HullSize;
import com.fs.starfarer.api.combat.ShipVariantAPI;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import exiledsector.ExiledSectorModPlugin;
import exiledsector.PlayerFleet;
import exiledsector.effects.FighterBayOverflow;
import exiledsector.persistence.ShipSkillDataManager;
import exiledsector.skills.FittedShipProfile;
import exiledsector.skills.ShipSkillData;
import exiledsector.skills.skilleffect.FleetWideEffects;
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
        ExiledSectorModPlugin.DOMAINS.forEach(domain -> domain.memberChanged(member, variant));
        FleetWideEffects.markPhaseFieldStale();
        member.setStatUpdateNeeded(true);
        member.updateStats();
        FighterBayOverflow.returnUnhousedWingsAndAnnounce(member, variant, PlayerFleet.cargoOrNull());
        return true;
    }
}
