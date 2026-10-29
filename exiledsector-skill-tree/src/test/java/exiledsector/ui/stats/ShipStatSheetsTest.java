package exiledsector.ui.stats;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.SettingsAPI;
import com.fs.starfarer.api.combat.MutableShipStatsAPI;
import com.fs.starfarer.api.combat.MutableStat;
import com.fs.starfarer.api.combat.ShieldAPI;
import com.fs.starfarer.api.combat.ShipAPI.HullSize;
import com.fs.starfarer.api.combat.ShipHullSpecAPI;
import com.fs.starfarer.api.combat.StatBonus;
import exiledlib.skills.SkillTypeEffect;
import exiledlib.skills.skilleffect.flux.FluxSkillEffect;
import exiledlib.skills.skilleffect.weapon.WeaponSkillEffect;
import exiledsector.effects.ResolvedTree;
import exiledsector.effects.StatSheetTreeFixture;
import exiledsector.skills.ShipSkillData;
import exiledsector.skills.ShipTrees;
import exiledsector.skills.SkillNode;
import exiledsector.skills.SkillTier;
import exiledsector.skills.SkillTree;
import exiledsector.skills.SkillType;
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
        assertEquals(List.of(new SourceLine("Capacitor Banks", "+20%", true)), maxFlux.breakdown().sources());
        assertTrue(maxFlux.breakdown().nodeIds().contains("capacitors_1"));
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

        ValueRow armor = row(build(null, Detail.CHANGED), StatGroup.DURABILITY, "Armor");

        assertEquals("1500", armor.value());
        assertEquals("+50%", armor.change());
        assertEquals(List.of(new SourceLine("Heavy Armor", "+500", false)), armor.breakdown().sources());
        assertTrue(armor.breakdown().nodeIds().isEmpty());
    }

    @Test
    void theWeaponGridOnlyAddsTheBeamColumnWhenABeamStatChanges() {
        energyDamage.modifyPercent("energyMod", 10f);

        ShipStatSheet.Grid weapons = section(build(null, Detail.CHANGED), StatGroup.WEAPONS).grids().get(0);
        assertEquals(List.of("Ballistic", "Energy", "Missile"), weapons.columns());
        assertEquals("Damage", weapons.rows().get(0).label());
        assertEquals("+10%", weapons.rows().get(0).cells().get(1).text());
        assertEquals("0%", weapons.rows().get(0).cells().get(0).text());

        beamDamage.modifyPercent("beamMod", 5f);

        ShipStatSheet.Grid withBeams = section(build(null, Detail.CHANGED), StatGroup.WEAPONS).grids().get(0);
        assertEquals(List.of("Ballistic", "Energy", "Missile", "Beam"), withBeams.columns());
        assertEquals("+5%", withBeams.rows().get(0).cells().get(3).text());
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
