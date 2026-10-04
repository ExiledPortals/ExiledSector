package exiledsector.socketables;

import exiledsector.skills.SkillTier;
import exiledsector.skills.SkillTree;
import exiledsector.skills.SkillType;
import exiledsector.skills.SkillTypeEffect;
import exiledsector.skills.npc.RealSkillData;
import exiledsector.skills.skilleffect.SkillEffect;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

class EffectThemesTest {

    @AfterEach
    void tearDown() {
        RealSkillData.clear();
    }

    private static SkillType type(String id, List<String> tags, String... effects) {
        return new SkillType.Builder(id, id, "a.png", SkillTier.SMALL)
                .effects(Arrays.stream(effects).map(name -> new SkillTypeEffect(SkillEffect.byName(name), 1f)).toList())
                .tags(tags)
                .build();
    }

    @Test
    void anEffectTakesTheThemesOfTheNodesThatAreMostlyAboutIt() {
        EffectThemes themes = EffectThemes.from(List.of(
                type("speed_notable", List.of("speed"), "TOP_SPEED_PERCENT", "ACCELERATION_PERCENT"),
                type("big_keystone", List.of("armour", "combat_readiness"), "TOP_SPEED_PERCENT", "ARMOR_PERCENT", "HULL_MULT",
                        "FLUX_CAPACITY_MULT", "FLUX_DISSIPATION_MULT"),
                type("beam", List.of("beam", "energy", "req_beam"), "BEAM_WEAPON_DAMAGE_PERCENT")));

        assertEquals(Set.of("speed"), themes.of("TOP_SPEED_PERCENT"));
        assertEquals(Set.of("beam", "energy"), themes.of("BEAM_WEAPON_DAMAGE_PERCENT"));
        assertEquals(Set.of(), themes.of("SHIELD_ARC_FLAT"));
        assertEquals(Set.of("speed", "beam", "energy"), themes.of(List.of(new RolledEffect("ACCELERATION_PERCENT", 1f),
                new RolledEffect("BEAM_WEAPON_DAMAGE_PERCENT", 1f))));
    }

    @Test
    void everyShippedSubroutineEffectHasATheme() throws Exception {
        RealSkillData.load();
        EffectThemes themes = EffectThemes.from(SkillTree.getAllTypes().values());
        String csv = java.nio.file.Files.readString(RealSkillData.projectRoot().resolve("data/config/exiledSector/socketables.csv"));
        SocketableDefinitions.register(org.json.CDL.toJSONArray(csv.replace("\r\n", "\n")));
        try {
            for (SocketableDefinition definition : SocketableDefinitions.all()) {
                for (SocketableDefinition.PoolEntry entry : definition.pool()) {
                    assertFalse(themes.of(entry.effectName()).isEmpty(), entry.effectName() + " has no theme");
                }
            }
        } finally {
            SocketableDefinitions.clear();
        }
    }
}
