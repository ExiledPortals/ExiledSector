package exiledsector.skills.npc;

import com.fs.starfarer.api.combat.ShipHullSpecAPI;
import com.fs.starfarer.api.combat.ShipVariantAPI;
import org.junit.jupiter.api.Test;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class NpcHullModsTest {

    @Test
    void builtInPermaAndSModsArePermanentAndEverythingElseIsRemovable() {
        ShipVariantAPI variant = mock(ShipVariantAPI.class);
        when(variant.getHullMods()).thenReturn(List.of("heavyarmor", "hbi", "degraded_engines", "hardenedshieldemitter", "eccm"));
        ShipHullSpecAPI hullSpec = mock(ShipHullSpecAPI.class);
        when(hullSpec.getBuiltInMods()).thenReturn(List.of("hbi"));
        when(variant.getHullSpec()).thenReturn(hullSpec);
        when(variant.getPermaMods()).thenReturn(Set.of("degraded_engines", "hardenedshieldemitter"));
        when(variant.getSMods()).thenReturn(new LinkedHashSet<>(List.of("hardenedshieldemitter")));

        NpcHullMods hullMods = NpcHullMods.of(variant);

        assertEquals(Set.of("eccm", "heavyarmor"), hullMods.removable());
        assertEquals(Set.of("degraded_engines", "hardenedshieldemitter", "hbi"), hullMods.permanent());
        assertEquals(Set.of("degraded_engines", "eccm", "hardenedshieldemitter", "hbi", "heavyarmor"), hullMods.installed());
    }

    @Test
    void hullModsTheSkillTreePlacedItselfAreNeitherKeptNorStrippable() {
        ShipVariantAPI variant = mock(ShipVariantAPI.class);
        when(variant.getHullMods()).thenReturn(List.of("safetyoverrides", "eccm"));
        when(variant.getPermaMods()).thenReturn(Set.of("safetyoverrides"));
        when(variant.hasTag("exiledSector_installed_safetyoverrides")).thenReturn(true);

        NpcHullMods hullMods = NpcHullMods.of(variant);

        assertEquals(Set.of("eccm"), hullMods.removable());
        assertEquals(Set.of(), hullMods.permanent());
    }

    @Test
    void nullCollectionsAndANullVariantAreEmpty() {
        ShipVariantAPI variant = mock(ShipVariantAPI.class);

        assertEquals(NpcHullMods.NONE, NpcHullMods.of(variant));
        assertEquals(NpcHullMods.NONE, NpcHullMods.of(null));
    }

    @Test
    void installedIsAFreshCopyThatCanBeModified() {
        NpcHullMods hullMods = new NpcHullMods(Set.of("eccm"), Set.of("hbi"));

        hullMods.installed().remove("eccm");

        assertEquals(Set.of("eccm", "hbi"), hullMods.installed());
    }
}
