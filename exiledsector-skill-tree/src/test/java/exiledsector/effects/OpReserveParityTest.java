package exiledsector.effects;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.SettingsAPI;
import com.fs.starfarer.api.combat.ShipAPI.HullSize;
import com.fs.starfarer.api.combat.ShipHullSpecAPI;
import com.fs.starfarer.api.combat.ShipVariantAPI;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import com.fs.starfarer.api.loading.HullModSpecAPI;
import exiledlib.MockSector;
import exiledsector.persistence.ShipSkillDataManager;
import exiledsector.skills.ShipTrees;
import exiledsector.skills.SkillNode;
import exiledsector.skills.SkillTier;
import exiledsector.skills.SkillType;
import exiledsector.skills.progression.SkillNodeOpCost;
import lunalib.lunaSettings.LunaSettings;
import org.apache.log4j.Logger;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class OpReserveParityTest {

    private MockSector mockSector;
    private MockedStatic<Logger> loggerMock;
    private SettingsAPI settings;
    private Logger logger;

    @BeforeEach
    void setUp() {
        settings = mock(SettingsAPI.class);
        mockSector = new MockSector();
        mockSector.global.when(Global::getSettings).thenReturn(settings);
        logger = mock(Logger.class);
        loggerMock = Mockito.mockStatic(Logger.class);
        loggerMock.when(() -> Logger.getLogger(OpReserveParity.class)).thenReturn(logger);
    }

    @AfterEach
    void tearDown() {
        loggerMock.close();
        mockSector.close();
    }

    private void reserveCosts(String hullModId, int cost) {
        HullModSpecAPI spec = mock(HullModSpecAPI.class);
        when(spec.getCostFor(HullSize.CRUISER)).thenReturn(cost);
        when(settings.getHullModSpec(hullModId)).thenReturn(spec);
    }

    private static ShipVariantAPI cruiserWith(String... hullMods) {
        ShipVariantAPI variant = mock(ShipVariantAPI.class);
        when(variant.getHullSize()).thenReturn(HullSize.CRUISER);
        when(variant.getHullMods()).thenReturn(List.of(hullMods));
        return variant;
    }

    private static FleetMemberAPI member(String id, ShipVariantAPI variant) {
        FleetMemberAPI member = mock(FleetMemberAPI.class);
        when(member.getId()).thenReturn(id);
        when(member.getShipName()).thenReturn("ISS " + id);
        when(member.getVariant()).thenReturn(variant);
        return member;
    }

    @Test
    void reservedOpAddsUpEveryReserveOnTheVariantAtItsHullSizeCost() {
        reserveCosts("exiledSector_opSpent_0", 9);
        reserveCosts("exiledSector_opSpent_7", 3);
        reserveCosts("heavyarmor", 20);

        assertEquals(12, OpReserveParity.reservedOp(cruiserWith("heavyarmor", "exiledSector_opSpent_0", "exiledSector_opSpent_7")));
        assertEquals(0, OpReserveParity.reservedOp(cruiserWith("heavyarmor", "exiledSector_opSpent_1000")));
    }

    @Test
    void reservedOpAddsUpEveryCoinOnTheVariant() {
        reserveCosts("exiledSector_opReserve_100", 100);
        reserveCosts("exiledSector_opReserve_30", 30);
        reserveCosts("exiledSector_opReserve_7", 7);

        assertEquals(137, OpReserveParity.reservedOp(cruiserWith("exiledSector_opReserve_100", "heavyarmor", "exiledSector_opReserve_30",
                "exiledSector_opReserve_7")));
    }

    @Test
    void aReserveThatMatchesThePaidNodesIsNotReported() {
        ShipVariantAPI variant = cruiserWith("exiledSector_opSpent_0");

        OpReserveParity.warnIfOutOfSync(member("in-sync", variant), variant, 9, 9, "while allocating nodes");

        verify(logger, never()).warn(any());
    }

    @Test
    void aMismatchIsReportedOnceWithTheShipTheCostsAndTheReservesFound() {
        ShipVariantAPI variant = cruiserWith("exiledSector_opSpent_4");
        FleetMemberAPI member = member("missing-reserve", variant);

        OpReserveParity.warnIfOutOfSync(member, variant, 12, 0, "while allocating nodes");
        OpReserveParity.warnIfOutOfSync(member, variant, 12, 0, "while allocating nodes");

        verify(logger, times(1)).warn(contains("OP reserve out of sync while allocating nodes on ISS missing-reserve"));
        verify(logger).warn(contains("its paid nodes cost 12 OP but the variant reserves 0 OP [exiledSector_opSpent_4]."));
    }

    @Test
    void aMismatchOnTheRefitScreensWorkingCopySaysSo() {
        ShipVariantAPI workingCopy = cruiserWith();
        FleetMemberAPI member = member("working-copy", cruiserWith());

        OpReserveParity.warnIfOutOfSync(member, workingCopy, 6, 0, "while allocating nodes");

        verify(logger).warn(contains("on the refit screen's working copy."));
    }

    @Test
    void theShortFormWorksOutThePaidNodeOpFromTheShipsSavedTree() {
        SkillType type = new SkillType.Builder("t", "t", "a.png", SkillTier.SMALL).effects(List.of()).build();
        ShipTrees.allocate(ShipSkillDataManager.get("saved-tree"), new SkillNode("armor_1", type, List.of(), 0f, 0f), 1);
        ShipVariantAPI variant = cruiserWith();
        FleetMemberAPI member = member("saved-tree", variant);
        ShipHullSpecAPI hull = mock(ShipHullSpecAPI.class);
        when(hull.getHullSize()).thenReturn(HullSize.CRUISER);
        when(member.getHullSpec()).thenReturn(hull);

        try (MockedStatic<LunaSettings> ignored = Mockito.mockStatic(LunaSettings.class, invocation -> null)) {
            OpReserveParity.warnIfOutOfSync(member, variant, "before the skill tree re-synced it");
        }

        verify(logger).warn(contains("its paid nodes cost " + SkillNodeOpCost.DEFAULT_CRUISER + " OP but the variant reserves 0 OP"));
    }

    @Test
    void npcShipsAreNeverChecked() {
        ShipVariantAPI variant = cruiserWith("exiledSector_opSpent_2");
        when(variant.getTags()).thenReturn(Set.of("exiledSector_npcTree|bulwark|3|root_low_tech_1"));
        reserveCosts("exiledSector_opSpent_2", 5);

        OpReserveParity.warnIfOutOfSync(member("npc-ship", variant), variant, "before the skill tree re-synced it");

        verify(logger, never()).warn(any());
    }
}
