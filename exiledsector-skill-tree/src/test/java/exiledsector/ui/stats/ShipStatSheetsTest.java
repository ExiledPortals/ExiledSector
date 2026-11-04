package exiledsector.ui.stats;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.SettingsAPI;
import com.fs.starfarer.api.combat.MutableShipStatsAPI;
import com.fs.starfarer.api.combat.MutableStat;
import com.fs.starfarer.api.combat.ShieldAPI;
import com.fs.starfarer.api.combat.ShipAPI.HullSize;
import com.fs.starfarer.api.combat.ShipHullSpecAPI;
import com.fs.starfarer.api.combat.StatBonus;
import exiledsector.skills.SkillTypeEffect;
import exiledsector.skills.skilleffect.flux.FluxSkillEffect;
import exiledsector.skills.skilleffect.weapon.WeaponSkillEffect;
import exiledsector.effects.ResolvedTree;
import exiledsector.effects.StatSheetTreeFixture;
import exiledsector.skills.ShipSkillData;
import exiledsector.skills.ShipTrees;
import exiledsector.skills.SkillNode;
import exiledsector.skills.SkillTier;
import exiledsector.skills.SkillTree;
import exiledsector.skills.SkillType;
import exiledsector.ui.stats.ShipStatSheet.Combine;
import exiledsector.ui.stats.ShipStatSheet.Detail;
import exiledsector.ui.stats.ShipStatSheet.Section;
import exiledsector.ui.stats.ShipStatSheet.SourceLine;
import exiledsector.ui.stats.ShipStatSheet.Trend;
import exiledsector.ui.stats.ShipStatSheet.ValueRow;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ShipStatSheetsTest {

    private MockedStatic<Global> globalMock;
    private MutableShipStatsAPI stats;
    private ShipHullSpecAPI hullSpec;
    private ShipSkillData data;
    private StatBonus armorBonus;
    private MutableStat energyDamage;
    private MutableStat beamDamage;

    @BeforeEach
    void setUp() {
        globalMock = Mockito.mockStatic(Global.class);
        globalMock.when(Global::getSettings).thenReturn(mock(SettingsAPI.class));
        SkillTree.clearNodes();
        SkillTree.clearTypes();
        SkillNode root = new SkillNode("root_1", new SkillType.Builder("root", "Low Tech", "a.png", SkillTier.ROOT).build(), List.of(), 0f, 0f);
        SkillNode capacitors = new SkillNode("capacitors_1", new SkillType.Builder("capacitors", "Capacitor Banks", "a.png", SkillTier.NOTABLE)
                .effects(List.of(new SkillTypeEffect(FluxSkillEffect.FLUX_CAPACITY_PERCENT, 20f),
                        new SkillTypeEffect(WeaponSkillEffect.MISSILE_RELOAD_PERCENT_PER_MINUTE, 10f))).build(), List.of("root_1"), 0f, 0f);
        SkillTree.register(root);
        SkillTree.register(capacitors);
        data = new ShipSkillData();
        ShipTrees.chooseStartingRoot(data, root);
        ShipTrees.allocate(data, capacitors, 0);

        stats = mock(MutableShipStatsAPI.class);
        when(stats.getFluxCapacity()).thenReturn(new MutableStat(1000f));
        armorBonus = new StatBonus();
        when(stats.getArmorBonus()).thenReturn(armorBonus);
        energyDamage = new MutableStat(1f);
        beamDamage = new MutableStat(1f);
        when(stats.getBallisticWeaponDamageMult()).thenReturn(new MutableStat(1f));
        when(stats.getEnergyWeaponDamageMult()).thenReturn(energyDamage);
        when(stats.getMissileWeaponDamageMult()).thenReturn(new MutableStat(1f));
        when(stats.getBeamWeaponDamageMult()).thenReturn(beamDamage);
        hullSpec = mock(ShipHullSpecAPI.class);
        when(hullSpec.getHullSize()).thenReturn(HullSize.CRUISER);
        when(hullSpec.getArmorRating()).thenReturn(1000f);
        when(hullSpec.getShieldType()).thenReturn(ShieldAPI.ShieldType.NONE);
    }

    @AfterEach
    void tearDown() {
        globalMock.close();
        SkillTree.clearNodes();
        SkillTree.clearTypes();
    }

    @Test
    void aStatRowShowsItsValueAndChangeAndNamesTheNodeBehindIt() {
        ShipStatSheet sheet = sheetWithTree(Detail.STANDARD);

        ValueRow maxFlux = row(sheet, StatGroup.FLUX, "Max Flux");
        assertEquals("1200", maxFlux.value());
        assertEquals("+20%", maxFlux.change());
        assertEquals(Trend.BETTER, maxFlux.trend());
        assertEquals(List.of(new SourceLine("Capacitor Banks", Combine.PERCENT, "+20%", true)), maxFlux.breakdown().sources());
        assertTrue(maxFlux.breakdown().nodeIds().contains("capacitors_1"));
    }

    @Test
    void eachSourceIsPlacedInTheColumnOfHowTheEngineCombinesIt() {
        MutableStat fluxCapacity = stats.getFluxCapacity();
        fluxCapacity.modifyMult("overload", 0.9f, "Overload");
        fluxCapacity.modifyFlat("capacitors", 100f, "Capacitors");
        fluxCapacity.modifyPercent("lattice", 20f, "Lattice");
        fluxCapacity.modifyPercent("strain", -5f, "Strain");

        ShipStatSheet.Breakdown breakdown = row(build(null, Detail.STANDARD), StatGroup.FLUX, "Max Flux").breakdown();

        assertEquals(List.of(new SourceLine("Capacitors", Combine.FLAT, "+100", false), new SourceLine("Lattice", Combine.PERCENT, "+20%", false),
                new SourceLine("Strain", Combine.PERCENT, "-5%", false), new SourceLine("Overload", Combine.MULTI, "-10%", false)), breakdown.sources());
        assertEquals("Final value: 1125", breakdown.result());
    }

    @Test
    void theTreesPooledMultiplierIsOneMultiRowNamingEveryNode() {
        SkillNode cells = new SkillNode("cells_1", new SkillType.Builder("cells", "Overcharged Cells", "a.png", SkillTier.SMALL)
                .effects(List.of(new SkillTypeEffect(FluxSkillEffect.FLUX_CAPACITY_MULT, 10f))).build(), List.of("root_1"), 0f, 0f);
        SkillNode lattice = new SkillNode("lattice_1", new SkillType.Builder("lattice", "Hardened Lattice", "a.png", SkillTier.SMALL)
                .effects(List.of(new SkillTypeEffect(FluxSkillEffect.FLUX_CAPACITY_MULT, 5f))).build(), List.of("root_1"), 0f, 0f);
        SkillTree.register(cells);
        SkillTree.register(lattice);
        ShipTrees.allocate(data, cells, 0);
        ShipTrees.allocate(data, lattice, 0);

        ValueRow maxFlux = row(sheetWithTree(Detail.STANDARD), StatGroup.FLUX, "Max Flux");

        List<SourceLine> multi = maxFlux.breakdown().sources().stream().filter(line -> line.combine() == Combine.MULTI).toList();
        assertEquals(List.of(new SourceLine("Overcharged Cells, Hardened Lattice", Combine.MULTI, "+15%", true)), multi);
        assertEquals("1380", maxFlux.value());
    }

    @Test
    void percentsOnASecondScaledStatGoInTheMultiColumn() {
        MutableStat absorption = new MutableStat(1f);
        MutableStat damageTaken = new MutableStat(1f);
        absorption.modifyPercent("absorb", -10f, "Absorb");
        damageTaken.modifyPercent("harden", -20f, "Harden");
        damageTaken.modifyPercent("brace", -10f, "Brace");
        when(stats.getShieldAbsorptionMult()).thenReturn(absorption);
        when(stats.getShieldDamageTakenMult()).thenReturn(damageTaken);
        when(hullSpec.getShieldType()).thenReturn(ShieldAPI.ShieldType.FRONT);
        when(hullSpec.getShieldSpec()).thenReturn(mock(ShipHullSpecAPI.ShieldSpecAPI.class));
        when(hullSpec.getBaseShieldFluxPerDamageAbsorbed()).thenReturn(1f);

        ShipStatSheet.Breakdown breakdown = row(build(null, Detail.STANDARD), StatGroup.SHIELDS, "Shield Flux/Damage").breakdown();

        assertEquals(List.of(new SourceLine("Absorb", Combine.PERCENT, "-10%", false), new SourceLine("Brace, Harden", Combine.MULTI, "-30%", false)),
                breakdown.sources());
    }

    @Test
    void effectsAStatRowAlreadyShowsAreNotRepeatedAsLines() {
        ShipStatSheet sheet = sheetWithTree(Detail.STANDARD);

        Section flux = section(sheet, StatGroup.FLUX);
        String capacityLine = FluxSkillEffect.FLUX_CAPACITY_PERCENT.description(20f).plain();
        assertTrue(flux.effects().stream().noneMatch(row -> row.line().plain().equals(capacityLine)));
    }

    @Test
    void effectsNoStatRowShowsAppearAsLinesInTheirPartsGroup() {
        ShipStatSheet sheet = sheetWithTree(Detail.STANDARD);

        String reloadLine = WeaponSkillEffect.MISSILE_RELOAD_PERCENT_PER_MINUTE.description(10f).plain();
        ShipStatSheet.EffectRow reload = section(sheet, StatGroup.WEAPONS).effects().stream()
                .filter(row -> row.line().plain().equals(reloadLine)).findFirst().orElseThrow();
        assertEquals(List.of("Capacitor Banks"), reload.breakdown().sources().stream().map(SourceLine::source).toList());
        assertTrue(reload.breakdown().nodeIds().contains("capacitors_1"));
    }

    @Test
    void theChangedDetailLeavesOutRowsNothingChanges() {
        assertNull(findSection(build(null, Detail.CHANGED), StatGroup.DURABILITY));

        ValueRow armor = row(build(null, Detail.STANDARD), StatGroup.DURABILITY, "Armor");
        assertEquals("1000", armor.value());
        assertEquals("", armor.change());
        assertEquals(Trend.NEUTRAL, armor.trend());
    }

    @Test
    void modifiersFromOutsideTheTreeAreNamedFromTheirDescription() {
        armorBonus.modifyFlat("heavyarmor", 500f, "Heavy Armor");

        ValueRow armor = row(build(null, Detail.STANDARD), StatGroup.DURABILITY, "Armor");

        assertEquals("1500", armor.value());
        assertEquals("+50%", armor.change());
        assertEquals(List.of(new SourceLine("Heavy Armor", Combine.FLAT, "+500", false)), armor.breakdown().sources());
        assertTrue(armor.breakdown().nodeIds().isEmpty());
    }

    @Test
    void theWeaponGridOnlyAddsTheBeamColumnWhenABeamStatChanges() {
        energyDamage.modifyPercent("energyMod", 10f);

        ShipStatSheet.Grid weapons = section(build(null, Detail.STANDARD), StatGroup.WEAPONS).grids().get(0);
        assertEquals(List.of("Ballistic", "Energy", "Missile"), weapons.columns());
        assertEquals("Damage", weapons.rows().get(0).label());
        assertEquals("+10%", weapons.rows().get(0).cells().get(1).text());
        assertEquals("0%", weapons.rows().get(0).cells().get(0).text());

        beamDamage.modifyPercent("beamMod", 5f);

        ShipStatSheet.Grid withBeams = section(build(null, Detail.STANDARD), StatGroup.WEAPONS).grids().get(0);
        assertEquals(List.of("Ballistic", "Energy", "Missile", "Beam"), withBeams.columns());
        assertEquals("+5%", withBeams.rows().get(0).cells().get(3).text());
    }

    @Test
    void theAllProjectileSpeedMultiplierScalesTheBallisticAndEnergyCellsButNotMissiles() {
        MutableStat allProjectileSpeed = new MutableStat(1f);
        allProjectileSpeed.modifyMult("ugh_magnetobarrels", 2f, "Magneto Turbo Barrels");
        MutableStat ballisticProjectileSpeed = new MutableStat(1f);
        ballisticProjectileSpeed.modifyPercent("rangefinder", 10f, "Rangefinder");
        when(stats.getProjectileSpeedMult()).thenReturn(allProjectileSpeed);
        when(stats.getBallisticProjectileSpeedMult()).thenReturn(ballisticProjectileSpeed);
        when(stats.getEnergyProjectileSpeedMult()).thenReturn(new MutableStat(1f));
        when(stats.getMissileMaxSpeedBonus()).thenReturn(new StatBonus());

        ShipStatSheet.Grid weapons = section(build(null, Detail.STANDARD), StatGroup.WEAPONS).grids().get(0);
        ShipStatSheet.GridRow projectileSpeed = weapons.rows().stream().filter(row -> row.label().equals("Projectile Speed")).findFirst().orElseThrow();

        assertEquals("+120%", projectileSpeed.cells().get(0).text());
        assertEquals("+100%", projectileSpeed.cells().get(1).text());
        assertEquals("0%", projectileSpeed.cells().get(2).text());
        assertEquals(List.of(new SourceLine("Rangefinder", Combine.PERCENT, "+10%", false),
                new SourceLine("Magneto Turbo Barrels", Combine.MULTI, "+100%", false)), projectileSpeed.cells().get(0).breakdown().sources());
    }

    @Test
    void theChangedDetailOnlyShowsWhatTheTreeChanged() {
        armorBonus.modifyFlat("heavyarmor", 500f, "Heavy Armor");
        energyDamage.modifyPercent("energyMod", 10f);

        ShipStatSheet sheet = sheetWithTree(Detail.CHANGED);

        assertNull(findSection(sheet, StatGroup.DURABILITY));
        assertTrue(section(sheet, StatGroup.WEAPONS).grids().isEmpty());
        assertEquals("1200", row(sheet, StatGroup.FLUX, "Max Flux").value());
    }

    @Test
    void temporaryNodesAppliedToTheShipAreNamedAsTreeSources() {
        SkillNode surge = new SkillNode("surge_1", new SkillType.Builder("surge", "Opening Surge", "a.png", SkillTier.SMALL)
                .effects(List.of(new SkillTypeEffect(FluxSkillEffect.FLUX_CAPACITY_FLAT, 100f))).temporaryAfterDeploymentSeconds(30f).build(),
                List.of("root_1"), 0f, 0f);
        SkillTree.register(surge);
        ShipTrees.allocate(data, surge, 0);

        ValueRow maxFlux = row(sheetWithTree(Detail.STANDARD), StatGroup.FLUX, "Max Flux");

        assertTrue(maxFlux.breakdown().sources().contains(new SourceLine("Opening Surge", Combine.FLAT, "+100", true)));
        assertTrue(maxFlux.breakdown().nodeIds().contains("surge_1"));
    }

    @Test
    void sheetsWithoutStatsAreEmpty() {
        ShipStatSheet sheet = ShipStatSheets.build(new ShipStatSheets.Input(null, hullSpec, null, data, null, null, Detail.FULL));

        assertTrue(sheet.isEmpty());
        assertFalse(build(null, Detail.FULL).isEmpty());
    }

    private ShipStatSheet sheetWithTree(Detail detail) {
        ResolvedTree resolvedTree = StatSheetTreeFixture.resolve(data, HullSize.CRUISER);
        StatSheetTreeFixture.apply(resolvedTree, stats);
        return build(resolvedTree, detail);
    }

    private ShipStatSheet build(ResolvedTree resolvedTree, Detail detail) {
        return ShipStatSheets.build(new ShipStatSheets.Input(stats, hullSpec, null, data, resolvedTree, null, detail));
    }

    private static Section findSection(ShipStatSheet sheet, StatGroup group) {
        return sheet.sections().stream().filter(section -> section.group() == group).findFirst().orElse(null);
    }

    private static Section section(ShipStatSheet sheet, StatGroup group) {
        Section section = findSection(sheet, group);
        assertTrue(section != null, "missing section " + group);
        return section;
    }

    private static ValueRow row(ShipStatSheet sheet, StatGroup group, String label) {
        return section(sheet, group).values().stream().filter(row -> row.label().equals(label)).findFirst().orElseThrow();
    }
}
