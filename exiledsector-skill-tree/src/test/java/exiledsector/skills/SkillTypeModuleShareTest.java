package exiledsector.skills;

import com.fs.starfarer.api.combat.ShipAPI.HullSize;
import exiledlib.ProjectFiles;
import exiledlib.skills.SkillTypeEffect;
import exiledlib.skills.skilleffect.defense.DefenseSkillEffect;
import exiledlib.skills.skilleffect.movement.MovementSkillEffect;
import exiledlib.skills.skilleffect.shield.ShieldSkillEffect;
import exiledlib.skills.skilleffect.hullmodule.ModuleShare;
import exiledlib.skills.skilleffect.hullmodule.StationModuleSkillEffect;
import exiledlib.skills.skilleffect.special.SpecialSkillEffect;
import exiledsector.skills.loader.SkillTypeLoader;
import exiledsector.skills.npc.RealSkillData;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SkillTypeModuleShareTest {

    private static final List<HullSize> HULL_SIZES = List.of(HullSize.FRIGATE, HullSize.DESTROYER, HullSize.CRUISER, HullSize.CAPITAL_SHIP);

    private static Map<String, SkillType> realTypes;

    @BeforeAll
    static void loadRealTypes() throws Exception {
        realTypes = SkillTypeLoader.parseSkillTypes(ProjectFiles.readJson(ProjectFiles.projectRoot().resolve(RealSkillData.TYPES_FILE)));
    }

    private static List<String> sharedNames(String typeId, HullSize hullSize) {
        return realTypes.get(typeId).moduleSharedEffects(hullSize).stream().map(effect -> effect.effect().name()).toList();
    }

    private static SkillType.Builder builder(SkillTypeEffect... effects) {
        return new SkillType.Builder("test", "Test", "icon.png", SkillTier.SMALL).effects(List.of(effects));
    }

    @Test
    void autoNodesWithAnExcludedEffectShareNothing() {
        for (SkillType type : realTypes.values()) {
            if (type.getModuleShareOverride() != ModuleShareOverride.AUTO) continue;
            for (HullSize hullSize : HULL_SIZES) {
                boolean hasExcluded = type.effectsFor(hullSize).stream().anyMatch(effect -> effect.effect().moduleShare() == ModuleShare.EXCLUDE);
                if (hasExcluded) {
                    assertTrue(type.moduleSharedEffects(hullSize).isEmpty(), type.getId());
                }
            }
        }
    }

    @Test
    void nodesWhoseUpsideIsExcludedNeverShareTheirDownside() {
        for (String typeId : List.of("shared_fate", "safety_overrides", "adaptiveshields", "frontemitter", "high_scatter_amp",
                "unstable_injector", "nskr_volatile", "frontshield", "shield_shunt", "dynamic_grid")) {
            for (HullSize hullSize : HULL_SIZES) {
                assertEquals(List.of(), sharedNames(typeId, hullSize), typeId);
            }
        }
    }

    @Test
    void penaltiesOnSharedEffectsTravelWithTheNode() {
        assertEquals(List.of("MISSILE_WEAPON_AMMO_PERCENT", "MISSILE_WEAPON_FIRE_RATE_PERCENT"), sharedNames("missleracks", HullSize.CRUISER));
        assertEquals(List.of("REPAIR_TIME_PERCENT", "SHIELD_DAMAGE_TAKEN_MULT"), sharedNames("seraphs_grace", HullSize.CRUISER));
        assertTrue(sharedNames("reckless_assault", HullSize.CRUISER).containsAll(List.of("WEAPON_DAMAGE_PERCENT", "ARMOR_PERCENT")));
    }

    @Test
    void allSharesTheShareablePartAndLeavesTheRestOnTheParent() {
        assertEquals(List.of("ARMOR_PERCENT", "WEAPON_RECOIL_MULT"), sharedNames("armoredweapons", HullSize.CRUISER));
        assertTrue(sharedNames("heavyarmor", HullSize.CAPITAL_SHIP).containsAll(List.of("ARMOR_PERCENT", "HULL_FLAT")));
        assertTrue(!sharedNames("heavyarmor", HullSize.CAPITAL_SHIP).contains("MANEUVERABILITY_PERCENT"));
    }

    @Test
    void theOverrideDecidesMixedNodes() {
        SkillTypeEffect armor = new SkillTypeEffect(DefenseSkillEffect.ARMOR_PERCENT, 10f);
        SkillTypeEffect sharedShieldDamage = new SkillTypeEffect(ShieldSkillEffect.SHIELD_DAMAGE_SHARED_PERCENT, 20f);

        assertEquals(List.of(), builder(armor, sharedShieldDamage).build().moduleSharedEffects(HullSize.CRUISER));
        assertEquals(List.of(armor), builder(armor, sharedShieldDamage).moduleShareOverride(ModuleShareOverride.ALL).build()
                .moduleSharedEffects(HullSize.CRUISER));
        assertEquals(List.of(), builder(armor).moduleShareOverride(ModuleShareOverride.NONE).build().moduleSharedEffects(HullSize.CRUISER));
        assertEquals(List.of(armor), builder(armor).build().moduleSharedEffects(HullSize.CRUISER));
    }

    @Test
    void temporaryNodesAndVanillaPassthroughsShareNothing() {
        SkillTypeEffect armor = new SkillTypeEffect(DefenseSkillEffect.ARMOR_PERCENT, 10f);

        assertEquals(List.of(), builder(armor).temporaryAfterDeploymentSeconds(30f).build().moduleSharedEffects(HullSize.CRUISER));
        assertEquals(List.of(), builder(armor).vanillaHullModId("missile_autoloader").build().moduleSharedEffects(HullSize.CRUISER));
    }

    @Test
    void theShareExtentSaysWhetherAllSomeOrNoneOfTheEffectsReachModules() {
        SkillTypeEffect armor = new SkillTypeEffect(DefenseSkillEffect.ARMOR_PERCENT, 10f);
        SkillTypeEffect topSpeed = new SkillTypeEffect(MovementSkillEffect.TOP_SPEED_PERCENT, 10f);
        SkillTypeEffect sharedShieldDamage = new SkillTypeEffect(ShieldSkillEffect.SHIELD_DAMAGE_SHARED_PERCENT, 20f);

        assertEquals(ModuleShareExtent.ALL, builder(armor).build().moduleShareExtent(HullSize.CRUISER));
        assertEquals(ModuleShareExtent.SOME, builder(armor, topSpeed).build().moduleShareExtent(HullSize.CRUISER));
        assertEquals(ModuleShareExtent.NONE, builder(topSpeed).build().moduleShareExtent(HullSize.CRUISER));
        assertEquals(ModuleShareExtent.NONE, builder(armor, sharedShieldDamage).build().moduleShareExtent(HullSize.CRUISER));
        assertEquals(ModuleShareExtent.NONE, builder(armor).temporaryAfterDeploymentSeconds(30f).build().moduleShareExtent(HullSize.CRUISER));
    }

    @Test
    void affinityCountsAsSharedAndModuleOnlyEffectsAreLeftOut() {
        SkillTypeEffect affinity = new SkillTypeEffect(SpecialSkillEffect.HIGH_TECH_AFFINITY_FLAT, 1f);
        SkillTypeEffect moduleHull = new SkillTypeEffect(StationModuleSkillEffect.MODULE_HULL_PERCENT, 10f);
        SkillTypeEffect armor = new SkillTypeEffect(DefenseSkillEffect.ARMOR_PERCENT, 10f);

        assertEquals(ModuleShareExtent.ALL, builder(affinity).build().moduleShareExtent(HullSize.CRUISER));
        assertEquals(ModuleShareExtent.ALL, builder(armor, moduleHull).build().moduleShareExtent(HullSize.CRUISER));
        assertNull(builder(moduleHull).build().moduleShareExtent(HullSize.CRUISER));
        assertNull(builder().build().moduleShareExtent(HullSize.CRUISER));
        assertEquals(ModuleShareExtent.NONE, builder().vanillaHullModId("missile_autoloader").build().moduleShareExtent(HullSize.CRUISER));
    }
}
