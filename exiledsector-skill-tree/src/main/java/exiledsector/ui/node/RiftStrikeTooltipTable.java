package exiledsector.ui.node;

import com.fs.starfarer.api.combat.MutableShipStatsAPI;
import com.fs.starfarer.api.combat.ShipVariantAPI;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import com.fs.starfarer.api.loading.WeaponSpecAPI;
import exiledsector.i18n.Translation;
import exiledsector.skills.SkillType;
import exiledsector.skills.SkillTypeEffect;
import exiledsector.skills.skilleffect.onhit.OnHitSkillEffect;
import exiledsector.skills.skilleffect.onhit.RiftStrike;
import exiledsector.skills.skilleffect.weapon.BallisticAsEnergy;
import exiledsector.skills.skilleffect.weapon.RiftExplosions;
import exiledsector.ui.TooltipTable;
import exiledsector.ui.TooltipTable.Row;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

final class RiftStrikeTooltipTable {

    private RiftStrikeTooltipTable() {
    }

    static boolean appliesTo(SkillType type) {
        if (type == null) {
            return false;
        }
        for (SkillTypeEffect effect : type.getEffects()) {
            if (effect.effect() == OnHitSkillEffect.RIFT_STRIKE_CHANCE_PERCENT) {
                return true;
            }
        }
        return false;
    }

    static float increasedRiftDamagePercent(FleetMemberAPI fleetMember) {
        MutableShipStatsAPI stats = fleetMember == null ? null : fleetMember.getStats();
        return stats == null ? 0f : stats.getDynamic().getValue(RiftExplosions.DAMAGE_PERCENT_KEY, 0f);
    }

    static boolean beamsFireRifts(FleetMemberAPI fleetMember) {
        MutableShipStatsAPI stats = fleetMember.getStats();
        return stats != null && stats.getDynamic().getValue(RiftStrike.BEAM_CHANCE_PERCENT_KEY, 0f) > 0f;
    }

    static List<TooltipTable> forType(SkillType type, FleetMemberAPI fleetMember) {
        if (!appliesTo(type) || fleetMember == null || fleetMember.getVariant() == null) {
            return List.of();
        }
        List<WeaponSpecAPI> weapons = riftWeapons(fleetMember.getVariant(), BallisticAsEnergy.active(fleetMember.getStats()),
                beamsFireRifts(fleetMember));
        if (weapons.isEmpty()) {
            return List.of();
        }
        float increasedPercent = increasedRiftDamagePercent(fleetMember);
        weapons.sort(Comparator.comparingDouble((WeaponSpecAPI weapon) -> -RiftStrike.baseRiftDamage(weapon)).thenComparing(WeaponSpecAPI::getWeaponName));
        List<Row> rows = new ArrayList<>();
        for (WeaponSpecAPI weapon : weapons) {
            rows.add(Row.of(false, weapon.getWeaponName(), String.valueOf(Math.round(RiftStrike.riftDamage(weapon, increasedPercent)))));
        }
        return List.of(new TooltipTable(Translation.text("ui.tables.riftExplosionDamage"),
                List.of(Translation.text("ui.tables.weapon"), Translation.text("ui.tables.riftDamage")), rows));
    }

    static List<WeaponSpecAPI> riftWeapons(ShipVariantAPI variant, boolean ballisticCountsAsEnergy, boolean includeBeams) {
        Map<String, WeaponSpecAPI> byId = new LinkedHashMap<>();
        for (String slotId : variant.getFittedWeaponSlots()) {
            WeaponSpecAPI weapon = variant.getWeaponSpec(slotId);
            if (RiftStrike.firesRifts(weapon, ballisticCountsAsEnergy) || includeBeams && RiftStrike.beamFiresRifts(weapon, ballisticCountsAsEnergy)) {
                byId.putIfAbsent(weapon.getWeaponId(), weapon);
            }
        }
        return new ArrayList<>(byId.values());
    }
}
