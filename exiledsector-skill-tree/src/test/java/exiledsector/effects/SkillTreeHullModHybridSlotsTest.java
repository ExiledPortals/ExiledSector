package exiledsector.effects;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.SectorAPI;
import com.fs.starfarer.api.combat.ShipAPI.HullSize;
import com.fs.starfarer.api.combat.ShipHullSpecAPI;
import com.fs.starfarer.api.combat.ShipVariantAPI;
import com.fs.starfarer.api.combat.WeaponAPI.WeaponType;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import com.fs.starfarer.api.loading.VariantSource;
import exiledlib.skills.SkillTypeEffect;
import exiledlib.skills.skilleffect.weapon.CloneableTestHullSpec;
import exiledlib.skills.skilleffect.weapon.HybridBallisticSlots;
import exiledlib.skills.skilleffect.weapon.RetypableTestSlot;
import exiledlib.skills.skilleffect.weapon.WeaponSkillEffect;
import exiledsector.persistence.ShipSkillDataManager;
import exiledsector.skills.ShipTrees;
import exiledsector.skills.SkillDataResolver;
import exiledsector.skills.SkillNode;
import exiledsector.skills.SkillTier;
import exiledsector.skills.SkillTree;
import exiledsector.skills.SkillType;
import lunalib.lunaSettings.LunaSettings;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class SkillTreeHullModHybridSlotsTest {

    private static final String SHIP_ID = "ship-a";
    private static final String SLOT_ID = "WS0001";

    private MockedStatic<Global> globalMock;
    private MockedStatic<LunaSettings> lunaSettingsMock;

    @BeforeEach
    void setUp() {
        SkillDataResolver.clearCache();
        SectorAPI sector = mock(SectorAPI.class);
        when(sector.getPersistentData()).thenReturn(new HashMap<>());
        lunaSettingsMock = Mockito.mockStatic(LunaSettings.class, invocation -> null);
        globalMock = Mockito.mockStatic(Global.class);
        globalMock.when(Global::getSector).thenReturn(sector);
        SkillTree.clearNodes();
        SkillTree.clearTypes();
    }

    @AfterEach
    void tearDown() {
        globalMock.close();
        lunaSettingsMock.close();
        SkillTree.clearNodes();
        SkillTree.clearTypes();
    }

    private static CloneableTestHullSpec hullSpec(Set<String> tags, RetypableTestSlot slot) {
        CloneableTestHullSpec spec = mock(CloneableTestHullSpec.class);
        when(spec.getAllWeaponSlotsCopy()).thenAnswer(call -> new ArrayList<>(List.of(slot)));
        when(spec.getWeaponSlotAPI(slot.getId())).thenReturn(slot);
        when(spec.getTags()).thenReturn(tags);
        when(spec.hasTag(anyString())).thenAnswer(call -> tags.contains(call.<String>getArgument(0)));
        doAnswer(call -> tags.add(call.getArgument(0))).when(spec).addTag(anyString());
        when(spec.clone()).thenAnswer(call -> hullSpec(new LinkedHashSet<>(tags), slot.copy()));
        return spec;
    }

    private static ShipVariantAPI refitVariant(ShipHullSpecAPI stockHull) {
        ShipVariantAPI variant = mock(ShipVariantAPI.class);
        ShipHullSpecAPI[] currentHull = {stockHull};
        when(variant.getHullSpec()).thenAnswer(call -> currentHull[0]);
        doAnswer(call -> currentHull[0] = call.getArgument(0)).when(variant).setHullSpecAPI(any());
        when(variant.getSource()).thenReturn(VariantSource.REFIT);
        when(variant.getHullSize()).thenReturn(HullSize.CRUISER);
        return variant;
    }

    private static FleetMemberAPI member() {
        FleetMemberAPI member = mock(FleetMemberAPI.class);
        when(member.getId()).thenReturn(SHIP_ID);
        return member;
    }

    private static SkillNode fafnirNode() {
        SkillType type = new SkillType.Builder("fafnir", "Fafnir Breeches", "a.png", SkillTier.NOTABLE)
                .effects(List.of(new SkillTypeEffect(WeaponSkillEffect.BALLISTIC_SLOTS_HYBRID, 0f))).build();
        SkillNode node = new SkillNode("fafnir_1", type, List.of(), 0f, 0f);
        SkillTree.register(node);
        return node;
    }

    private static WeaponType slotType(ShipVariantAPI variant) {
        return variant.getHullSpec().getWeaponSlotAPI(SLOT_ID).getWeaponType();
    }

    @Test
    void allocatingTheNodeMakesTheEditedVariantsBallisticSlotsHybridRightAway() {
        ShipHullSpecAPI stockHull = hullSpec(new LinkedHashSet<>(), new RetypableTestSlot(SLOT_ID, WeaponType.BALLISTIC, false));
        ShipVariantAPI variant = refitVariant(stockHull);
        ShipTrees.allocate(ShipSkillDataManager.get(SHIP_ID), fafnirNode(), 1);

        SkillTreeHullMod.syncHybridBallisticSlots(member(), variant);

        assertNotSame(stockHull, variant.getHullSpec());
        assertTrue(HybridBallisticSlots.converted(variant.getHullSpec()));
        assertEquals(WeaponType.HYBRID, slotType(variant));
    }

    @Test
    void unallocatingTheNodeTurnsTheEditedVariantsSlotsBackToBallisticRightAway() {
        ShipHullSpecAPI stockHull = hullSpec(new LinkedHashSet<>(), new RetypableTestSlot(SLOT_ID, WeaponType.BALLISTIC, false));
        ShipVariantAPI variant = refitVariant(stockHull);
        FleetMemberAPI member = member();
        SkillNode fafnir = fafnirNode();
        ShipTrees.allocate(ShipSkillDataManager.get(SHIP_ID), fafnir, 1);
        SkillTreeHullMod.syncHybridBallisticSlots(member, variant);
        ShipHullSpecAPI hybridHull = variant.getHullSpec();

        ShipTrees.deallocate(ShipSkillDataManager.get(SHIP_ID), fafnir);
        SkillTreeHullMod.syncHybridBallisticSlots(member, variant);

        assertNotSame(hybridHull, variant.getHullSpec());
        assertFalse(HybridBallisticSlots.converted(variant.getHullSpec()));
        assertEquals(WeaponType.BALLISTIC, slotType(variant));
    }

    @Test
    void aShipWithoutTheNodeKeepsItsHullSpec() {
        ShipHullSpecAPI stockHull = hullSpec(new LinkedHashSet<>(), new RetypableTestSlot(SLOT_ID, WeaponType.BALLISTIC, false));
        ShipVariantAPI variant = refitVariant(stockHull);

        SkillTreeHullMod.syncHybridBallisticSlots(member(), variant);
        SkillTreeHullMod.syncHybridBallisticSlots(member(), null);

        assertSame(stockHull, variant.getHullSpec());
    }
}
