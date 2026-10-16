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
    void everyToggledAreaIsARealRegion() {
        for (String region : AreaToggles.LOST_SECTOR_REGIONS) {
            assertEquals(true, SkillTags.isRegion(region));
        }
    }
}
