package exiledsector.skills.npc;

import com.fs.starfarer.api.combat.ShieldAPI.ShieldType;
import com.fs.starfarer.api.combat.ShipAPI.HullSize;
import exiledsector.skills.tags.ShipProfile;
import exiledsector.skills.tags.WeaponKind;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;

class NpcLayoutsTest {

    @AfterEach
    void tearDown() {
        NpcLayouts.register(Map.of());
    }

    private static NpcLayout layout(String id, List<String> requires) {
        return new NpcLayout(id, id, "root", requires, "", List.of(new NpcLayoutEntry("a", null)));
    }

    @Test
    void layoutsAreKeptSortedById() {
        NpcLayouts.register(Map.of("zeta", layout("zeta", List.of()), "alpha", layout("alpha", List.of())));

        assertEquals(List.of("alpha", "zeta"), NpcLayouts.all().stream().map(NpcLayout::id).toList());
    }

    @Test
    void onlyLayoutsWhoseRequirementsTheShipMeetsAreEligible() {
        NpcLayouts.register(Map.of("carrier", layout("carrier", List.of("req_fighter_bays")),
                "bulwark", layout("bulwark", List.of())));
        ShipProfile noBays = new ShipProfile(HullSize.DESTROYER, ShieldType.FRONT, 0, Set.of(WeaponKind.BALLISTIC), false, 0f, false);
        ShipProfile carrier = new ShipProfile(HullSize.DESTROYER, ShieldType.FRONT, 2, Set.of(WeaponKind.BALLISTIC), false, 0f, false);

        assertEquals(List.of("bulwark"), NpcLayouts.eligibleFor(noBays).stream().map(NpcLayout::id).toList());
        assertEquals(List.of("bulwark", "carrier"), NpcLayouts.eligibleFor(carrier).stream().map(NpcLayout::id).toList());
    }

}
