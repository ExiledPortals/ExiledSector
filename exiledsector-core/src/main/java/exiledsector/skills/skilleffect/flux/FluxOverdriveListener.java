package exiledsector.skills.skilleffect.flux;

import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.listeners.AdvanceableListener;
import exiledsector.i18n.Translation;
import exiledsector.skills.skilleffect.PlayerStatus;
import exiledsector.skills.skilleffect.ScaledBonus;
import exiledsector.skills.skilleffect.ShipCombatListener;
import exiledsector.skills.skilleffect.StatMode;
import exiledsector.skills.skilleffect.weapon.WeaponScope;
import exiledsector.skills.skilleffect.weapon.WeaponStatFamily;

final class FluxOverdriveListener extends ShipCombatListener implements AdvanceableListener {

    static final String DAMAGE_PERCENT_KEY = "exiledSector_fluxOverdriveDamagePercent";
    static final String FLUX_COST_PERCENT_KEY = "exiledSector_fluxOverdriveFluxCostPercent";
    private static final String MOD_ID_PREFIX = "exiledSector_fluxOverdrive_";
    private static final String STATUS_ICON = "graphics/icons/hullsys/ammo_feeder.png";
    private static final ScaledBonus OVERDRIVE_BONUS = new ScaledBonus(
            part(WeaponStatFamily.DAMAGE, WeaponScope.BALLISTIC, DAMAGE_PERCENT_KEY),
            part(WeaponStatFamily.DAMAGE, WeaponScope.ENERGY, DAMAGE_PERCENT_KEY),
            part(WeaponStatFamily.FLUX_COST, WeaponScope.BALLISTIC, FLUX_COST_PERCENT_KEY),
            part(WeaponStatFamily.FLUX_COST, WeaponScope.ENERGY, FLUX_COST_PERCENT_KEY));

    private final ScaledBonus.Tracker overdrive = OVERDRIVE_BONUS.tracker();
    private final PlayerStatus status;
    private int shownDamagePercent = -1;

    FluxOverdriveListener(ShipAPI ownerShip) {
        super(ownerShip, MOD_ID_PREFIX);
        status = new PlayerStatus(modId, STATUS_ICON, "combat.fluxOverdrive");
    }

    private static ScaledBonus.Part part(WeaponStatFamily family, WeaponScope scope, String perUnitKey) {
        return new ScaledBonus.Part(family.target(scope), perUnitKey, StatMode.MULT, 1f);
    }

    @Override
    public void advance(float amount) {
        float fluxLevel = ownerIsAliveNotHulk() ? ownerShip.getFluxTracker().getFluxLevel() : 0f;
        overdrive.apply(ownerShip.getMutableStats(), modId, fluxLevel);
        if (fluxLevel <= 0f) {
            return;
        }
        int damagePercent = Math.round(magnitude(DAMAGE_PERCENT_KEY) * fluxLevel);
        if (damagePercent != shownDamagePercent) {
            shownDamagePercent = damagePercent;
            status.invalidate();
        }
        status.showIfPlayer(ownerShip, false, () -> Translation.msg("combat.fluxOverdrive.status").arg("value", damagePercent).text());
    }
}
