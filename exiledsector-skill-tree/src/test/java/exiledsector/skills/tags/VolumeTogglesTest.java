package exiledsector.skills.tags;

import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;

class VolumeTogglesTest {

    @Test
    void autoShowsTheLostSectorVolumeOnlyWhenLostSectorIsInstalled() {
        assertEquals(Set.of(), VolumeToggles.disabledVolumes(VolumeToggles.AUTO, true));
        assertEquals(Set.of("enigma", "kesteven"), VolumeToggles.disabledVolumes(VolumeToggles.AUTO, false));
    }

    @Test
    void onAndOffIgnoreWhetherLostSectorIsInstalled() {
        assertEquals(Set.of(), VolumeToggles.disabledVolumes(VolumeToggles.ON, false));
        assertEquals(Set.of("enigma", "kesteven"), VolumeToggles.disabledVolumes(VolumeToggles.OFF, true));
    }

    @Test
    void aMissingOrUnknownSettingBehavesLikeAuto() {
        assertEquals(Set.of("enigma", "kesteven"), VolumeToggles.disabledVolumes(null, false));
        assertEquals(Set.of(), VolumeToggles.disabledVolumes("something else", true));
    }

    @Test
    void theRandomAssortmentVolumesNeedRandomAssortmentInstalledWhateverTheSetting() {
        assertEquals(Set.of(), VolumeToggles.disabledRandomAssortmentVolumes(VolumeToggles.AUTO, true));
        assertEquals(Set.of("exotech", "abyss"), VolumeToggles.disabledRandomAssortmentVolumes(VolumeToggles.AUTO, false));
        assertEquals(Set.of("exotech", "abyss"), VolumeToggles.disabledRandomAssortmentVolumes(VolumeToggles.ON, false));
        assertEquals(Set.of(), VolumeToggles.disabledRandomAssortmentVolumes(VolumeToggles.ON, true));
        assertEquals(Set.of("exotech", "abyss"), VolumeToggles.disabledRandomAssortmentVolumes(VolumeToggles.OFF, true));
        assertEquals(Set.of("exotech", "abyss"), VolumeToggles.disabledRandomAssortmentVolumes(null, false));
    }

    @Test
    void autoShowsTheNightcrossVolumeOnlyWhenNightcrossIsInstalled() {
        assertEquals(Set.of(), VolumeToggles.disabledNightcrossVolumes(VolumeToggles.AUTO, true));
        assertEquals(Set.of("nightcross", "stargazer"), VolumeToggles.disabledNightcrossVolumes(VolumeToggles.AUTO, false));
        assertEquals(Set.of(), VolumeToggles.disabledNightcrossVolumes(VolumeToggles.ON, false));
        assertEquals(Set.of("nightcross", "stargazer"), VolumeToggles.disabledNightcrossVolumes(VolumeToggles.OFF, true));
        assertEquals(Set.of("nightcross", "stargazer"), VolumeToggles.disabledNightcrossVolumes(null, false));
    }

    @Test
    void theTaniaVolumeNeedsNavarchyOfTaniaAndCanStillBeSwitchedOff() {
        assertEquals(Set.of(), VolumeToggles.disabledTaniaVolumes(VolumeToggles.AUTO, true));
        assertEquals(Set.of("tania"), VolumeToggles.disabledTaniaVolumes(VolumeToggles.AUTO, false));
        assertEquals(Set.of("tania"), VolumeToggles.disabledTaniaVolumes(VolumeToggles.ON, false));
        assertEquals(Set.of("tania"), VolumeToggles.disabledTaniaVolumes(VolumeToggles.OFF, true));
        assertEquals(Set.of(), VolumeToggles.disabledTaniaVolumes(null, true));
    }

    @Test
    void everyToggledVolumeIsARealVolume() {
        for (Set<String> toggleGroup : VolumeToggles.TOGGLED_VOLUMES) {
            for (String volume : toggleGroup) {
                assertEquals(true, SkillTags.isVolume(volume));
            }
        }
    }
}
