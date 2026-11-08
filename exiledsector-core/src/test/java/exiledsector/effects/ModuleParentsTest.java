package exiledsector.effects;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.FleetDataAPI;
import com.fs.starfarer.api.combat.CombatEngineAPI;
import com.fs.starfarer.api.combat.ShipAPI.HullSize;
import com.fs.starfarer.api.combat.ShipVariantAPI;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import com.fs.starfarer.api.loading.VariantSource;
import exiledsector.persistence.ShipSkillDataManager;
import exiledsector.skills.DiminishingReturns;
import exiledsector.skills.ShipSkillData;
import lunalib.lunaSettings.LunaSettings;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ModuleParentsTest {

    @AfterEach
    void leaveCombat() {
        Global.setCombatEngine(null);
        DiminishingReturns.reloadSettings();
    }

    private static CombatEngineAPI enterCombat() {
        CombatEngineAPI engine = mock(CombatEngineAPI.class);
        when(engine.getCustomData()).thenReturn(new HashMap<>());
        Global.setCombatEngine(engine);
        return engine;
    }

    private static FleetMemberAPI member(String id, ShipVariantAPI variant, FleetDataAPI fleetData) {
        FleetMemberAPI member = mock(FleetMemberAPI.class);
        when(member.getId()).thenReturn(id);
        when(member.getVariant()).thenReturn(variant);
        when(member.getFleetData()).thenReturn(fleetData);
        return member;
    }

    private static ShipEffectContext parentContext(String parentMemberId) {
        ShipVariantAPI parentVariant = ModuleVariants.parent(VariantSource.REFIT, Map.of("WS0001", ModuleVariants.variant(VariantSource.REFIT)));
        return new ShipEffectContext(new ShipSkillData(), member(parentMemberId, parentVariant, null), parentVariant, HullSize.CRUISER);
    }

    @Test
    void inCombatTheModuleFindsTheParentRegisteredUnderItsTag() {
        enterCombat();
        ShipEffectContext parentContext = parentContext("parent");
        ResolvedTree parentTree = mock(ResolvedTree.class);
        ModuleParents.register(parentContext, parentTree);
        ShipVariantAPI module = ModuleVariants.linkedModule("parent");

        ModuleParent parent = ModuleParents.find(member("temp", module, null), module);

        assertNotNull(parent);
        assertSame(parentContext, parent.context());
        assertSame(parentTree, parent.resolvedTree());
        assertEquals("parent", parent.memberId());
        assertNull(ModuleParents.find(member("temp", ModuleVariants.linkedModule("someone-else"), null), ModuleVariants.linkedModule("someone-else")));
    }

    @Test
    void onlyParentsWithModuleSlotsAreRegistered() {
        CombatEngineAPI engine = enterCombat();
        ShipVariantAPI plainVariant = ModuleVariants.variant(VariantSource.REFIT);

        ModuleParents.register(new ShipEffectContext(new ShipSkillData(), member("plain", plainVariant, null), plainVariant, HullSize.CRUISER),
                mock(ResolvedTree.class));

        assertNull(engine.getCustomData().get(ModuleParents.COMBAT_REGISTRY_KEY));
    }

    @Test
    void inARefitModuleTabTheParentIsTheFleetMemberNamedByTheTag() {
        FleetDataAPI fleetData = mock(FleetDataAPI.class);
        ShipVariantAPI parentVariant = ModuleVariants.parent(VariantSource.REFIT, Map.of("WS0001", ModuleVariants.linkedModule("parent")));
        FleetMemberAPI parentMember = member("parent", parentVariant, fleetData);
        FleetMemberAPI otherMember = member("other", ModuleVariants.variant(VariantSource.REFIT), fleetData);
        when(fleetData.getMembersListCopy()).thenReturn(List.of(otherMember, parentMember));
        ShipVariantAPI module = ModuleVariants.linkedModule("parent");

        ModuleParent parent;
        try (MockedStatic<ShipSkillDataManager> records = Mockito.mockStatic(ShipSkillDataManager.class);
             MockedStatic<LunaSettings> settings = Mockito.mockStatic(LunaSettings.class)) {
            parent = ModuleParents.find(member("stand-in", module, fleetData), module);
        }

        assertNotNull(parent);
        assertSame(parentMember, parent.context().member());
        assertSame(parentVariant, parent.context().variant());
        assertNotNull(parent.resolvedTree());
    }

    @Test
    void noTagNoMemberAFleetMembersOwnShipOrAMissingParentGiveNoParent() {
        enterCombat();
        ModuleParents.register(parentContext("parent"), mock(ResolvedTree.class));
        FleetDataAPI fleetData = mock(FleetDataAPI.class);
        ShipVariantAPI ejectedShip = ModuleVariants.linkedModule("parent");
        FleetMemberAPI ejectedMember = member("ejected", ejectedShip, fleetData);
        when(fleetData.getMembersListCopy()).thenReturn(List.of(ejectedMember));
        ShipVariantAPI untagged = ModuleVariants.variant(VariantSource.REFIT);
        ShipVariantAPI linked = ModuleVariants.linkedModule("parent");

        assertNull(ModuleParents.find(member("temp", untagged, null), untagged));
        assertNull(ModuleParents.find(null, linked));
        assertNull(ModuleParents.find(ejectedMember, ejectedShip));
        assertNull(ModuleParents.find(member("stand-in", linked, fleetData), linked));
    }

    @Test
    void outsideCombatAModuleWithoutFleetDataHasNoParent() {
        ShipVariantAPI module = ModuleVariants.linkedModule("parent");

        assertNull(ModuleParents.find(member("codex", module, null), module));
    }
}
