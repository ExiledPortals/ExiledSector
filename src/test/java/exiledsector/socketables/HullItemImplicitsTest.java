package exiledsector.socketables;

import exiledsector.skills.npc.RealSkillData;
import exiledsector.skills.SkillTypeEffect;
import exiledsector.skills.skilleffect.EffectAliases;
import org.json.CDL;
import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class HullItemImplicitsTest {

    @BeforeEach
    void loadRealData() throws Exception {
        EffectAliases.register(csvRows(EffectAliases.DATA_PATH));
        SocketableDefinitions.register(csvRows(SocketableDefinitions.DATA_PATH));
        HullItemImplicits.register(csvRows(HullItemImplicits.DATA_PATH));
    }

    @AfterEach
    void clear() {
        HullItemImplicits.clear();
        SocketableDefinitions.clear();
    }

    @Test
    void everyHullSocketTypeHasImplicitsThatRollRangesOfKnownEffects() {
        for (SocketType type : SocketType.frameworkTypes()) {
            List<PoolEntry> implicits = HullItemImplicits.forType(type);
            assertFalse(implicits.isEmpty(), type.id() + " has no implicits");
            for (PoolEntry implicit : implicits) {
                assertFalse(implicit.scalesWithHullSize(), implicit.effectName());
                assertTrue(implicit.canVary(), implicit.effectName() + " should roll a range");
            }
        }
        assertTrue(HullItemImplicits.forType(SocketType.SUBROUTINE).isEmpty());
    }

    @Test
    void subroutinesCannotBeGivenImplicits() {
        assertThrows(IllegalArgumentException.class, () -> HullItemImplicits.parseImplicits(SocketType.SUBROUTINE, "HULL_PERCENT:5:10"));
        assertThrows(IllegalArgumentException.class,
                () -> HullItemImplicits.parseImplicits(SocketType.ARMOR_PLATING, "ARMOR_FLAT:10/15/20/30"));
    }

    @Test
    void anItemRollsTheSameImplicitsFromItsSeedAndKeepsThemFrozen() throws Exception {
        Socketable shieldItem = new Socketable("item", "framework_shield_generator_emitter", 42L, List.of());
        List<RolledEffect> firstRoll = shieldItem.implicits();
        Socketable sameSeed = new Socketable("other", "framework_shield_generator_emitter", 42L, List.of());

        assertEquals(2, firstRoll.size());
        assertEquals(magnitudes(firstRoll), magnitudes(sameSeed.implicits()));
        HullItemImplicits.register(new JSONArray().put(new JSONObject().put("type", "shield_generator").put("implicits", "HULL_PERCENT:50:60")));
        assertEquals(magnitudes(firstRoll), magnitudes(shieldItem.implicits()), "already-rolled implicits stay frozen");
    }

    @Test
    void uniquesGetTheirTypeImplicitsAndSubroutinesGetNone() {
        SocketableDefinition unique = SocketableDefinitions.all().stream()
                .filter(definition -> definition.unique() && definition.kind() == SocketType.WEAPON_MOUNT).findFirst().orElseThrow();
        SocketableDefinition subroutine = SocketableDefinitions.all().stream()
                .filter(definition -> definition.kind() == SocketType.SUBROUTINE).findFirst().orElseThrow();

        assertEquals(1, new Socketable("u", unique.id(), 7L, List.of()).implicits().size());
        assertTrue(new Socketable("s", subroutine.id(), 7L, List.of()).implicits().isEmpty());
    }

    @Test
    void implicitsApplyAlongsideExplicitsWithoutChangingRarity() {
        Socketable plate = new Socketable("plate", "framework_armor_plating_plate", 9L, List.of(new RolledEffect("HULL_PERCENT", 5f)));
        List<SkillTypeEffect> applied = plate.skillEffects();

        assertEquals(2, applied.size());
        assertEquals("ARMOR_PERCENT", applied.get(0).effect().name());
        assertEquals(SocketableRarity.COMMON, plate.rarity());
    }

    private static List<Float> magnitudes(List<RolledEffect> effects) {
        return effects.stream().map(RolledEffect::magnitude).toList();
    }

    private static JSONArray csvRows(String dataPath) throws Exception {
        String csv = Files.readString(RealSkillData.projectRoot().resolve(dataPath), StandardCharsets.UTF_8);
        return CDL.toJSONArray(csv.replace("\r\n", "\n"));
    }
}
