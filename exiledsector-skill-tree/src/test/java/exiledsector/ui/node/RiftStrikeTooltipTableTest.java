package exiledsector.ui.node;

import com.fs.starfarer.api.combat.MutableShipStatsAPI;
import com.fs.starfarer.api.combat.ShipVariantAPI;
import com.fs.starfarer.api.combat.WeaponAPI.DerivedWeaponStatsAPI;
import com.fs.starfarer.api.combat.WeaponAPI.WeaponType;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import com.fs.starfarer.api.loading.WeaponSpecAPI;
import exiledsector.skills.SkillTier;
import exiledsector.skills.SkillType;
import exiledsector.skills.SkillTypeEffect;
import exiledsector.skills.skilleffect.onhit.OnHitSkillEffect;
import exiledsector.skills.skilleffect.weapon.BallisticAsEnergy;
import exiledsector.skills.skilleffect.weapon.RiftExplosions;
import exiledsector.ui.TooltipTable;
import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class RiftStrikeTooltipTableTest {

    private static final SkillType RIFT_HARMONICS = new SkillType.Builder("rift_harmonics", "Rift Harmonics", "", SkillTier.NOTABLE)
            .effects(List.of(new SkillTypeEffect(OnHitSkillEffect.RIFT_STRIKE_CHANCE_PERCENT, 30f))).build();

    private static WeaponSpecAPI weapon(String id, String name, WeaponType type, boolean beam, float damagePerShot) {
        WeaponSpecAPI spec = mock(WeaponSpecAPI.class);
        DerivedWeaponStatsAPI derived = mock(DerivedWeaponStatsAPI.class);
        when(derived.getDamagePerShot()).thenReturn(damagePerShot);
        when(spec.getDerivedStats()).thenReturn(derived);
        when(spec.getWeaponId()).thenReturn(id);
        when(spec.getWeaponName()).thenReturn(name);
        when(spec.getType()).thenReturn(type);
        when(spec.isBeam()).thenReturn(beam);
        return spec;
    }

    private static FleetMemberAPI member(float increasedRiftDamagePercent, boolean ballisticCountsAsEnergy, WeaponSpecAPI... fitted) {
        Map<String, WeaponSpecAPI> slots = new LinkedHashMap<>();
        for (int index = 0; index < fitted.length; index++) {
            slots.put("WS" + index, fitted[index]);
        }
        ShipVariantAPI variant = mock(ShipVariantAPI.class);
        when(variant.getFittedWeaponSlots()).thenReturn(slots.keySet());
        slots.forEach((slotId, spec) -> when(variant.getWeaponSpec(slotId)).thenReturn(spec));
        MutableShipStatsAPI stats = mock(MutableShipStatsAPI.class, RETURNS_DEEP_STUBS);
        when(stats.getDynamic().getValue(RiftExplosions.DAMAGE_PERCENT_KEY, 0f)).thenReturn(increasedRiftDamagePercent);
        when(stats.getDynamic().getMod(BallisticAsEnergy.KEY).computeEffective(0f)).thenReturn(ballisticCountsAsEnergy ? 1f : 0f);
        FleetMemberAPI member = mock(FleetMemberAPI.class);
        when(member.getVariant()).thenReturn(variant);
        when(member.getStats()).thenReturn(stats);
        return member;
    }

    private static List<List<String>> cells(TooltipTable table) {
        return table.rows().stream().map(TooltipTable.Row::cells).toList();
    }

    @Test
    void listsEachFittedRiftWeaponOnceWithItsRiftDamageIncludingIncreases() {
        WeaponSpecAPI pulse = weapon("pulselaser", "Pulse Laser", WeaponType.ENERGY, false, 100f);
        FleetMemberAPI member = member(20f, false, pulse, weapon("heavyblaster", "Heavy Blaster", WeaponType.ENERGY, false, 500f), pulse,
                weapon("ioncannon", "Ion Cannon", WeaponType.ENERGY, false, 25f), weapon("tachyonlance", "Tachyon Lance", WeaponType.ENERGY, true, 0f),
                weapon("heavymauler", "Heavy Mauler", WeaponType.BALLISTIC, false, 200f));

        List<TooltipTable> tables = RiftStrikeTooltipTable.forType(RIFT_HARMONICS, member);

        assertEquals(1, tables.size());
        assertEquals(List.of(List.of("Heavy Blaster", "300"), List.of("Pulse Laser", "60"), List.of("Ion Cannon", "36")), cells(tables.get(0)));
    }

    @Test
    void ballisticWeaponsAreListedWhileTheyCountAsEnergy() {
        FleetMemberAPI member = member(0f, true, weapon("heavymauler", "Heavy Mauler", WeaponType.BALLISTIC, false, 200f));

        assertEquals(List.of(List.of("Heavy Mauler", "100")), cells(RiftStrikeTooltipTable.forType(RIFT_HARMONICS, member).get(0)));
    }

    @Test
    void otherTypesAndShipsWithoutRiftWeaponsGetNoTable() {
        SkillType other = new SkillType.Builder("other", "Other", "", SkillTier.SMALL).build();
        WeaponSpecAPI pulse = weapon("pulselaser", "Pulse Laser", WeaponType.ENERGY, false, 100f);

        assertTrue(RiftStrikeTooltipTable.forType(other, member(0f, false, pulse)).isEmpty());
        assertTrue(RiftStrikeTooltipTable.forType(RIFT_HARMONICS, member(0f, false)).isEmpty());
        assertTrue(RiftStrikeTooltipTable.forType(null, member(0f, false, pulse)).isEmpty());
    }
}
