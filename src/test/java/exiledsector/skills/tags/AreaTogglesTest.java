package exiledsector.skills.tags;

import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;

class AreaTogglesTest {

    @Test
    void autoShowsTheLostSectorAreaOnlyWhenLostSectorIsInstalled() {
        assertEquals(Set.of(), AreaToggles.disabledRegions(AreaToggles.AUTO, true));
        assertEquals(Set.of("enigma", "kesteven"), AreaToggles.disabledRegions(AreaToggles.AUTO, false));
    }

    @Test
    void onAndOffIgnoreWhetherLostSectorIsInstalled() {
        assertEquals(Set.of(), AreaToggles.disabledRegions(AreaToggles.ON, false));
        assertEquals(Set.of("enigma", "kesteven"), AreaToggles.disabledRegions(AreaToggles.OFF, true));
    }

    @Test
    void aMissingOrUnknownSettingBehavesLikeAuto() {
        assertEquals(Set.of("enigma", "kesteven"), AreaToggles.disabledRegions(null, false));
        assertEquals(Set.of(), AreaToggles.disabledRegions("something else", true));
    }

    @Test
    void autoShowsTheNightcrossAreaOnlyWhenNightcrossIsInstalled() {
        assertEquals(Set.of(), AreaToggles.disabledNightcrossRegions(AreaToggles.AUTO, true));
        assertEquals(Set.of("nightcross"), AreaToggles.disabledNightcrossRegions(AreaToggles.AUTO, false));
        assertEquals(Set.of(), AreaToggles.disabledNightcrossRegions(AreaToggles.ON, false));
        assertEquals(Set.of("nightcross"), AreaToggles.disabledNightcrossRegions(AreaToggles.OFF, true));
        assertEquals(Set.of("nightcross"), AreaToggles.disabledNightcrossRegions(null, false));
    }

    @Test
    void everyToggledAreaIsARealRegion() {
        for (Set<String> area : AreaToggles.TOGGLED_AREAS) {
            for (String region : area) {
                assertEquals(true, SkillTags.isRegion(region));
            }
        }
    }
}
