package exiledsector.skills.skilleffect;

import com.fs.starfarer.api.combat.AmmoTrackerAPI;
import com.fs.starfarer.api.combat.MutableShipStatsAPI;
import com.fs.starfarer.api.combat.MutableStat;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.StatBonus;
import com.fs.starfarer.api.combat.WeaponAPI;
import com.fs.starfarer.api.loading.WeaponSpecAPI;
import com.fs.starfarer.api.util.DynamicStatsAPI;
import exiledsector.skills.SkillTypeEffect;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyFloat;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class WeaponScopeHierarchyTest {

    private static final float EPSILON = 1e-4f;

    @Test
    void everyGeneratedEffectFollowsTheNamingSchemeAndResolvesByName() {
        for (ScopedWeaponEffect effect : ScopedWeaponEffect.all()) {
            String expected = effect.scope().namePrefix() + "WEAPON_" + effect.family().name() + "_" + effect.mode().name();
            assertEquals(expected, effect.name());
            assertSame(effect, SkillEffect.byName(effect.name()));
        }
    }

    @Test
    void childrenAreTheScopesThatNameItAsTheirParentInDeclarationOrder() {
        assertEquals(List.of(WeaponScope.BALLISTIC, WeaponScope.MISSILE, WeaponScope.ENERGY), WeaponScope.ALL.children());
        assertEquals(List.of(WeaponScope.NON_BEAM_ENERGY, WeaponScope.BEAM), WeaponScope.ENERGY.children());
        assertTrue(WeaponScope.BEAM.children().isEmpty());
    }

    @Test
    void theRequestedParentAndChildDamageEffectsExist() {
        assertSame(ScopedWeaponEffect.find(WeaponStatFamily.DAMAGE, WeaponScope.ALL, StatMode.PERCENT),
                SkillEffect.byName("WEAPON_DAMAGE_PERCENT"));
        assertSame(ScopedWeaponEffect.find(WeaponStatFamily.DAMAGE, WeaponScope.BEAM, StatMode.PERCENT),
                SkillEffect.byName("BEAM_WEAPON_DAMAGE_PERCENT"));
    }

    @Test
    void combinationsTheEngineCannotSupportCleanlyAreNotGenerated() {
        for (String name : List.of("NON_BEAM_ENERGY_WEAPON_FIRE_RATE_PERCENT", "BEAM_WEAPON_FIRE_RATE_PERCENT",
                "BALLISTIC_WEAPON_TURN_RATE_PERCENT", "ENERGY_WEAPON_TURN_RATE_PERCENT",
                "NON_BEAM_ENERGY_WEAPON_FLUX_COST_PERCENT", "WEAPON_AUTOFIRE_ACCURACY_FLAT",
                "MISSILE_WEAPON_ECCM_CHANCE_MULT", "BEAM_WEAPON_AMMO_REGEN_FLAT")) {
            assertThrows(IllegalArgumentException.class, () -> SkillEffect.byName(name), name);
        }
        SkillEffect.byName("NON_BEAM_ENERGY_WEAPON_FLUX_COST_MULT");
    }

    private static MutableShipStatsAPI realWeaponStats() {
        MutableShipStatsAPI stats = mock(MutableShipStatsAPI.class);
        MutableStat ballistic = new MutableStat(1f);
        MutableStat missile = new MutableStat(1f);
        MutableStat energy = new MutableStat(1f);
        MutableStat beam = new MutableStat(1f);
        when(stats.getBallisticWeaponDamageMult()).thenReturn(ballistic);
        when(stats.getMissileWeaponDamageMult()).thenReturn(missile);
        when(stats.getEnergyWeaponDamageMult()).thenReturn(energy);
        when(stats.getBeamWeaponDamageMult()).thenReturn(beam);
        return stats;
    }

    private static float beamDamagePercentPool(MutableShipStatsAPI stats) {
        return stats.getEnergyWeaponDamageMult().getPercentMod() + stats.getBeamWeaponDamageMult().getPercentMod();
    }

    private static float nonBeamEnergyDamagePercentPool(MutableShipStatsAPI stats) {
        return stats.getEnergyWeaponDamageMult().getPercentMod();
    }

    @Test
    void allAndBeamIncreasesPoolIntoASingleBeamBonus() {
        MutableShipStatsAPI stats = realWeaponStats();

        SkillEffect.byName("WEAPON_DAMAGE_PERCENT").apply(stats, "all", 10f);
        SkillEffect.byName("BEAM_WEAPON_DAMAGE_PERCENT").apply(stats, "beam", 10f);

        assertEquals(20f, beamDamagePercentPool(stats), EPSILON);
        assertEquals(10f, nonBeamEnergyDamagePercentPool(stats), EPSILON);
        assertEquals(10f, stats.getBallisticWeaponDamageMult().getPercentMod(), EPSILON);
    }

    @Test
    void allAndNonBeamIncreasesPoolWithoutLeakingOntoBeams() {
        MutableShipStatsAPI stats = realWeaponStats();

        SkillEffect.byName("WEAPON_DAMAGE_PERCENT").apply(stats, "all", 10f);
        SkillEffect.byName("NON_BEAM_ENERGY_WEAPON_DAMAGE_PERCENT").apply(stats, "nonBeam", 10f);

        assertEquals(20f, nonBeamEnergyDamagePercentPool(stats), EPSILON);
        assertEquals(10f, beamDamagePercentPool(stats), EPSILON);
    }

    @Test
    void energyAndBeamIncreasesPoolForBeams() {
        MutableShipStatsAPI stats = realWeaponStats();

        SkillEffect.byName("ENERGY_WEAPON_DAMAGE_PERCENT").apply(stats, "energy", 10f);
        SkillEffect.byName("BEAM_WEAPON_DAMAGE_PERCENT").apply(stats, "beam", 5f);

        assertEquals(15f, beamDamagePercentPool(stats), EPSILON);
        assertEquals(10f, nonBeamEnergyDamagePercentPool(stats), EPSILON);
    }

    @Test
    void nonBeamMoreMultiplierIsExactlyCancelledOnBeams() {
        MutableShipStatsAPI stats = realWeaponStats();

        SkillEffect.byName("NON_BEAM_ENERGY_WEAPON_DAMAGE_MULT").apply(stats, "nonBeam", 25f);

        assertEquals(1.25f, stats.getEnergyWeaponDamageMult().getMult(), EPSILON);
        assertEquals(1f, stats.getEnergyWeaponDamageMult().getMult() * stats.getBeamWeaponDamageMult().getMult(), EPSILON);
    }

    @Test
    void chanceStatsAddPercentagePoints() {
        MutableShipStatsAPI stats = mock(MutableShipStatsAPI.class);
        MutableStat eccm = mock(MutableStat.class);
        when(stats.getEccmChance()).thenReturn(eccm);

        SkillEffect.byName("MISSILE_WEAPON_ECCM_CHANCE_PERCENT").apply(stats, "mod_id", 50f);

        verify(eccm).modifyFlat("mod_id", 0.5f);
    }

    @Test
    void descriptionsNameTheScopeAndNeverExposeTheBeamOffset() {
        assertEquals("10% increased non-beam energy weapon damage.",
                SkillEffect.byName("NON_BEAM_ENERGY_WEAPON_DAMAGE_PERCENT").description(10f).plain());
        assertEquals("Increases weapon range by 50.", SkillEffect.byName("WEAPON_RANGE_FLAT").description(50f).plain());
        assertEquals("Significantly improved missile guidance algorithm.",
                SkillEffect.byName("MISSILE_WEAPON_GUIDANCE_FLAT").description(1f).plain());
        assertTrue(SkillEffect.byName("ENERGY_WEAPON_FIRE_RATE_PERCENT").description(10f).plain().contains("Burst beams"));
        assertFalse(SkillEffect.byName("BALLISTIC_WEAPON_FIRE_RATE_PERCENT").description(10f).plain().contains("Burst beams"));
    }

    @Test
    void beamAndNonBeamAmmoDescriptionsWarnThatRefitTooltipsWontShowThem() {
        String note = " This is set directly on each weapon: it's correct in combat, "
                + "but the refit screen's weapon tooltips won't show it.";
        for (String name : List.of("BEAM_WEAPON_AMMO_PERCENT", "BEAM_WEAPON_AMMO_FLAT", "BEAM_WEAPON_AMMO_REGEN_PERCENT",
                "NON_BEAM_ENERGY_WEAPON_AMMO_MULT", "NON_BEAM_ENERGY_WEAPON_AMMO_REGEN_MULT")) {
            assertTrue(SkillEffect.byName(name).description(10f).plain().endsWith(note), name);
        }
        assertEquals("10% increased beam weapon ammo capacity." + note,
                SkillEffect.byName("BEAM_WEAPON_AMMO_PERCENT").description(10f).plain());
        assertFalse(SkillEffect.byName("ENERGY_WEAPON_AMMO_PERCENT").description(10f).plain().contains("refit"));
        assertFalse(SkillEffect.byName("WEAPON_AMMO_REGEN_PERCENT").description(10f).plain().contains("refit"));
    }

    private static WeaponAPI weapon(boolean beam, int specMaxAmmo) {
        WeaponAPI weapon = mock(WeaponAPI.class);
        WeaponSpecAPI spec = mock(WeaponSpecAPI.class);
        when(weapon.getType()).thenReturn(WeaponAPI.WeaponType.ENERGY);
        when(weapon.isBeam()).thenReturn(beam);
        when(weapon.usesAmmo()).thenReturn(true);
        when(weapon.getSpec()).thenReturn(spec);
        when(spec.getMaxAmmo()).thenReturn(specMaxAmmo);
        return weapon;
    }

    @Test
    void beamAmmoPoolsWithTheEnergyAmmoBonusOnBeamsOnly() {
        MutableShipStatsAPI stats = mock(MutableShipStatsAPI.class);
        DynamicStatsAPI dynamic = mock(DynamicStatsAPI.class);
        StatBonus beamAmmo = new StatBonus();
        StatBonus energyAmmo = new StatBonus();
        energyAmmo.modifyPercent("energy", 20f);
        when(stats.getDynamic()).thenReturn(dynamic);
        when(dynamic.getMod("exiledSector_BEAM_AMMO")).thenReturn(beamAmmo);
        when(stats.getEnergyAmmoBonus()).thenReturn(energyAmmo);
        WeaponAPI beam = weapon(true, 10);
        WeaponAPI pulse = weapon(false, 10);
        ShipAPI ship = mock(ShipAPI.class);
        when(ship.getMutableStats()).thenReturn(stats);
        when(ship.getAllWeapons()).thenReturn(List.of(beam, pulse));

        SkillEffect effect = SkillEffect.byName("BEAM_WEAPON_AMMO_PERCENT");
        effect.apply(stats, "mod_id", 30f);
        effect.applyAfterShipCreation(ship, "mod_id", 30f);

        verify(beam).setMaxAmmo(15);
        verify(pulse, never()).setMaxAmmo(anyInt());
        assertFalse(effect.supportsTemporaryGating());
    }

    private static ShipAPI shipWith(MutableShipStatsAPI stats, WeaponAPI... weapons) {
        ShipAPI ship = mock(ShipAPI.class);
        when(ship.getMutableStats()).thenReturn(stats);
        when(ship.getAllWeapons()).thenReturn(List.of(weapons));
        return ship;
    }

    private static MutableShipStatsAPI statsWithDynamicMod(String key, StatBonus child) {
        MutableShipStatsAPI stats = mock(MutableShipStatsAPI.class);
        DynamicStatsAPI dynamic = mock(DynamicStatsAPI.class);
        when(stats.getDynamic()).thenReturn(dynamic);
        when(dynamic.getMod(key)).thenReturn(child);
        return stats;
    }

    @Test
    void nonBeamAmmoAddsFlatBonusesAfterThePooledPercentAndRefillsTheWeapon() {
        StatBonus energyAmmo = new StatBonus();
        energyAmmo.modifyPercent("energy", 50f);
        energyAmmo.modifyFlat("energy", 2f);
        MutableShipStatsAPI stats = statsWithDynamicMod("exiledSector_NON_BEAM_ENERGY_AMMO", new StatBonus());
        when(stats.getEnergyAmmoBonus()).thenReturn(energyAmmo);
        WeaponAPI pulse = weapon(false, 10);
        WeaponAPI beam = weapon(true, 10);
        WeaponAPI noAmmo = weapon(false, 10);
        when(noAmmo.usesAmmo()).thenReturn(false);

        SkillEffect effect = SkillEffect.byName("NON_BEAM_ENERGY_WEAPON_AMMO_FLAT");
        effect.apply(stats, "mod_id", 3f);
        effect.applyAfterShipCreation(shipWith(stats, pulse, beam, noAmmo), "mod_id", 3f);

        verify(pulse).setMaxAmmo(20);
        verify(pulse).resetAmmo();
        verify(beam, never()).setMaxAmmo(anyInt());
        verify(noAmmo, never()).setMaxAmmo(anyInt());
    }

    private static WeaponAPI regeneratingWeapon(boolean beam, float specAmmoPerSecond) {
        WeaponAPI weapon = weapon(beam, 10);
        AmmoTrackerAPI tracker = mock(AmmoTrackerAPI.class);
        when(weapon.getAmmoTracker()).thenReturn(tracker);
        when(weapon.getSpec().getAmmoPerSecond()).thenReturn(specAmmoPerSecond);
        return weapon;
    }

    @Test
    void beamAmmoRegenScalesByThePooledRegenOverTheShipWideEnergyRegenTheEngineAlreadyApplies() {
        MutableStat energyRegen = new MutableStat(1f);
        energyRegen.modifyPercent("energy", 20f);
        MutableShipStatsAPI stats = statsWithDynamicMod("exiledSector_BEAM_AMMO_REGEN", new StatBonus());
        when(stats.getEnergyAmmoRegenMult()).thenReturn(energyRegen);
        WeaponAPI beam = regeneratingWeapon(true, 2f);
        WeaponAPI pulse = regeneratingWeapon(false, 2f);

        SkillEffect effect = SkillEffect.byName("BEAM_WEAPON_AMMO_REGEN_PERCENT");
        effect.apply(stats, "mod_id", 30f);
        effect.applyAfterShipCreation(shipWith(stats, beam, pulse), "mod_id", 30f);

        verify(beam.getAmmoTracker()).setAmmoPerSecond(2f * 1.5f / 1.2f);
        verify(pulse.getAmmoTracker(), never()).setAmmoPerSecond(anyFloat());
    }

    @Test
    void ammoRegenLeavesWeaponsAloneWhenTheShipWideEnergyRegenIsZero() {
        MutableStat energyRegen = new MutableStat(1f);
        energyRegen.modifyMult("energy", 0f);
        MutableShipStatsAPI stats = statsWithDynamicMod("exiledSector_NON_BEAM_ENERGY_AMMO_REGEN", new StatBonus());
        when(stats.getEnergyAmmoRegenMult()).thenReturn(energyRegen);
        WeaponAPI pulse = regeneratingWeapon(false, 2f);

        SkillEffect effect = SkillEffect.byName("NON_BEAM_ENERGY_WEAPON_AMMO_REGEN_MULT");
        effect.apply(stats, "mod_id", 50f);
        effect.applyAfterShipCreation(shipWith(stats, pulse), "mod_id", 50f);

        verify(pulse.getAmmoTracker(), never()).setAmmoPerSecond(anyFloat());
    }

    private static WeaponAPI typed(WeaponAPI.WeaponType type, boolean beam) {
        WeaponAPI weapon = mock(WeaponAPI.class);
        when(weapon.getType()).thenReturn(type);
        when(weapon.isBeam()).thenReturn(beam);
        return weapon;
    }

    @Test
    void eachScopeMatchesTheWeaponsItNames() {
        WeaponAPI ballistic = typed(WeaponAPI.WeaponType.BALLISTIC, false);
        WeaponAPI missile = typed(WeaponAPI.WeaponType.MISSILE, false);
        WeaponAPI pulse = typed(WeaponAPI.WeaponType.ENERGY, false);
        WeaponAPI beam = typed(WeaponAPI.WeaponType.ENERGY, true);
        List<WeaponAPI> weapons = List.of(ballistic, missile, pulse, beam);

        assertEquals(weapons, weapons.stream().filter(WeaponScope.ALL::matches).toList());
        assertEquals(List.of(ballistic), weapons.stream().filter(WeaponScope.BALLISTIC::matches).toList());
        assertEquals(List.of(missile), weapons.stream().filter(WeaponScope.MISSILE::matches).toList());
        assertEquals(List.of(pulse, beam), weapons.stream().filter(WeaponScope.ENERGY::matches).toList());
        assertEquals(List.of(pulse), weapons.stream().filter(WeaponScope.NON_BEAM_ENERGY::matches).toList());
        assertEquals(List.of(beam), weapons.stream().filter(WeaponScope.BEAM::matches).toList());
    }

    private static SkillTypeEffect entry(String name, float magnitude) {
        return new SkillTypeEffect(SkillEffect.byName(name), magnitude);
    }

    private static List<String> names(List<SkillTypeEffect> effects) {
        List<String> names = new ArrayList<>();
        for (SkillTypeEffect effect : effects) {
            names.add(effect.effect().name() + "=" + effect.magnitude());
        }
        return names;
    }

    @Test
    void tooltipCollapsesMatchingChildrenIntoTheirParentRecursively() {
        List<SkillTypeEffect> effects = List.of(entry("BALLISTIC_WEAPON_DAMAGE_PERCENT", 10f),
                entry("HULL_PERCENT", 5f), entry("MISSILE_WEAPON_DAMAGE_PERCENT", 10f),
                entry("NON_BEAM_ENERGY_WEAPON_DAMAGE_PERCENT", 10f), entry("BEAM_WEAPON_DAMAGE_PERCENT", 10f));

        assertEquals(List.of("WEAPON_DAMAGE_PERCENT=10.0", "HULL_PERCENT=5.0"),
                names(WeaponEffectTooltipAggregator.collapse(effects)));
    }

    @Test
    void tooltipKeepsDifferentMagnitudesAndParentChildPairsSeparate() {
        List<SkillTypeEffect> effects = List.of(entry("NON_BEAM_ENERGY_WEAPON_DAMAGE_PERCENT", 10f),
                entry("BEAM_WEAPON_DAMAGE_PERCENT", 5f), entry("ENERGY_WEAPON_RANGE_PERCENT", 10f),
                entry("BEAM_WEAPON_RANGE_PERCENT", 10f));

        assertEquals(names(effects), names(WeaponEffectTooltipAggregator.collapse(effects)));
    }

    @Test
    void everyEffectInTheRealSkillTypesFileResolves() throws IOException {
        String json = Files.readString(Path.of("data/skilltrees/skill_types.json"), StandardCharsets.UTF_8);
        Matcher matcher = Pattern.compile("\"effect\"\\s*:\\s*\"([A-Z0-9_]+)\"").matcher(json);
        while (matcher.find()) {
            String name = matcher.group(1);
            assertTrue(SkillEffectRegistry.names().contains(name), "Unknown effect in skill_types.json: " + name);
        }
    }
}
