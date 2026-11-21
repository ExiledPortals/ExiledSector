package exiledsector.charts.officer.scar;

import com.fs.starfarer.api.combat.ShieldAPI.ShieldType;
import com.fs.starfarer.api.combat.ShipHullSpecAPI;
import com.fs.starfarer.api.combat.ShipVariantAPI;
import com.fs.starfarer.api.combat.WeaponAPI.WeaponType;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import com.fs.starfarer.api.impl.campaign.ids.HullMods;
import com.fs.starfarer.api.loading.WeaponSlotAPI;

import java.util.Collection;
import java.util.EnumSet;
import java.util.Set;

public record HullFacts(boolean shields, boolean frontShield, boolean omniShield, boolean fighterBays, boolean missileSlots,
                        boolean phase, boolean civilian, boolean automated, boolean peakTimeless, boolean carrier) {

    static final float PEAKLESS_SECONDS = 10000f;
    static final String SHIELD_SHUNT = "shield_shunt";
    static final String CONVERTED_HANGAR = "converted_hangar";
    private static final Set<WeaponType> MISSILE_CAPABLE_SLOTS = EnumSet.of(WeaponType.MISSILE, WeaponType.COMPOSITE,
            WeaponType.SYNERGY, WeaponType.UNIVERSAL);

    public static HullFacts of(FleetMemberAPI member) {
        if (member == null) {
            return null;
        }
        return member.getVariant() != null ? of(member.getVariant()) : of(member.getHullSpec());
    }

    public static HullFacts of(ShipVariantAPI variant) {
        if (variant == null || variant.getHullSpec() == null) {
            return null;
        }
        ShipHullSpecAPI hull = variant.getHullSpec();
        Collection<String> hullMods = variant.getHullMods();
        boolean shunted = hullMods.contains(SHIELD_SHUNT);
        ShieldType shieldType = shunted ? ShieldType.NONE : hull.getShieldType();
        return new HullFacts(hasShields(shieldType), shieldType == ShieldType.FRONT, shieldType == ShieldType.OMNI,
                hull.getFighterBays() > 0 || hullMods.contains(CONVERTED_HANGAR), hasMissileSlots(hull), hull.isPhase(),
                variant.isCivilian(), hullMods.contains(HullMods.AUTOMATED) || isAutomated(hull),
                hull.getNoCRLossTime() >= PEAKLESS_SECONDS, variant.isCarrier());
    }

    public static HullFacts of(ShipHullSpecAPI hull) {
        if (hull == null) {
            return null;
        }
        ShieldType shieldType = hull.getShieldType();
        return new HullFacts(hasShields(shieldType), shieldType == ShieldType.FRONT, shieldType == ShieldType.OMNI,
                hull.getFighterBays() > 0, hasMissileSlots(hull), hull.isPhase(), hull.isCivilianNonCarrier(), isAutomated(hull),
                hull.getNoCRLossTime() >= PEAKLESS_SECONDS, hull.isCarrier());
    }

    private static boolean hasShields(ShieldType shieldType) {
        return shieldType == ShieldType.FRONT || shieldType == ShieldType.OMNI;
    }

    private static boolean isAutomated(ShipHullSpecAPI hull) {
        return hull.getBuiltInMods() != null && hull.getBuiltInMods().contains(HullMods.AUTOMATED);
    }

    static boolean hasMissileSlots(ShipHullSpecAPI hull) {
        for (WeaponSlotAPI slot : hull.getAllWeaponSlotsCopy()) {
            if (slot.isWeaponSlot() && !slot.isBuiltIn() && !slot.isDecorative() && !slot.isSystemSlot()
                    && MISSILE_CAPABLE_SLOTS.contains(slot.getWeaponType())) {
                return true;
            }
        }
        return false;
    }
}
