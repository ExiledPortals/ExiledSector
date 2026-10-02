package exiledsector.effects;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.SettingsAPI;
import com.fs.starfarer.api.campaign.CampaignUIAPI.CoreUITradeMode;
import com.fs.starfarer.api.combat.HullModEffect;
import com.fs.starfarer.api.combat.MutableShipStatsAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.ShipAPI.HullSize;
import com.fs.starfarer.api.combat.ShipVariantAPI;
import com.fs.starfarer.api.loading.HullModSpecAPI;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class PhantomHullModsTest {

    private static final String HULL_MOD = "safetyoverrides";

    private MockedStatic<Global> globalMock;
    private SettingsAPI settings;
    private HullModSpecAPI spec;
    private String effectClass;
    private HullModEffect createdEffect;

    @BeforeEach
    void setUp() {
        PhantomHullMods.clearForTests();
        RecordingHullModEffect.CALLS.clear();
        settings = mock(SettingsAPI.class);
        when(settings.getScriptClassLoader()).thenReturn(PhantomHullModsTest.class.getClassLoader());
        globalMock = Mockito.mockStatic(Global.class);
        globalMock.when(Global::getSettings).thenReturn(settings);
        spec = mock(HullModSpecAPI.class);
        when(spec.getId()).thenReturn(HULL_MOD);
        when(settings.getHullModSpec(HULL_MOD)).thenReturn(spec);
        effectClass = RecordingHullModEffect.class.getName();
        when(spec.getEffectClass()).thenAnswer(invocation -> effectClass);
        doAnswer(invocation -> {
            effectClass = invocation.getArgument(0);
            return null;
        }).when(spec).setEffectClass(anyString());
    }

    @AfterEach
    void tearDown() {
        globalMock.close();
        PhantomHullMods.clearForTests();
    }

    private void engineCreatesEffectsLazily() {
        when(spec.getEffect()).thenAnswer(invocation -> {
            if (createdEffect == null) {
                createdEffect = effectClass.equals(PhantomHullModEffect.class.getName())
                        ? new PhantomHullModEffect() : new RecordingHullModEffect();
                createdEffect.init(spec);
            }
            return createdEffect;
        });
    }

    private static ShipAPI shipWith(boolean phantom) {
        ShipAPI ship = mock(ShipAPI.class);
        ShipVariantAPI variant = mock(ShipVariantAPI.class);
        when(ship.getVariant()).thenReturn(variant);
        when(variant.hasTag("exiledSector_installed_" + HULL_MOD)).thenReturn(phantom);
        return ship;
    }

    @Test
    void aHullModWhoseEffectTheGameHasNotCreatedYetBecomesAPhantomWrappingTheVanillaEffect() {
        engineCreatesEffectsLazily();

        PhantomHullMods.install(List.of(HULL_MOD));

        assertTrue(PhantomHullMods.isActive(HULL_MOD));
        assertInstanceOf(PhantomHullModEffect.class, createdEffect);
    }

    @Test
    void aHullModWhoseEffectAlreadyExistsIsLeftAloneAndNodesWillNotPlaceIt() {
        createdEffect = new RecordingHullModEffect();
        when(spec.getEffect()).thenReturn(createdEffect);

        PhantomHullMods.install(List.of(HULL_MOD));

        assertFalse(PhantomHullMods.isActive(HULL_MOD));
        assertEquals(RecordingHullModEffect.class.getName(), effectClass);
    }

    @Test
    void aWrapperThatCouldNotRecreateTheVanillaEffectIsNeverTreatedAsActive() {
        when(spec.getEffect()).thenReturn(new PhantomHullModEffect());

        PhantomHullMods.install(List.of(HULL_MOD));

        assertFalse(PhantomHullMods.isActive(HULL_MOD));
    }

    @Test
    void aFailureWhileTheGameCreatesTheEffectRevertsToTheVanillaClass() {
        when(spec.getEffect()).thenThrow(new IllegalStateException("script failed"));

        PhantomHullMods.install(List.of(HULL_MOD));

        assertFalse(PhantomHullMods.isActive(HULL_MOD));
        assertEquals(RecordingHullModEffect.class.getName(), effectClass);
    }

    @Test
    void aMissingHullModOrEffectClassIsNeverSwapped() {
        when(settings.getHullModSpec(HULL_MOD)).thenReturn(null);
        PhantomHullMods.install(List.of(HULL_MOD));
        assertFalse(PhantomHullMods.isActive(HULL_MOD));

        when(settings.getHullModSpec(HULL_MOD)).thenReturn(spec);
        effectClass = "no.such.EffectClass";
        PhantomHullMods.install(List.of(HULL_MOD));
        assertFalse(PhantomHullMods.isActive(HULL_MOD));
        verify(spec, never()).getEffect();
    }

    @Test
    void realCopiesKeepEveryVanillaBehaviour() {
        engineCreatesEffectsLazily();
        PhantomHullMods.install(List.of(HULL_MOD));
        ShipAPI ship = shipWith(false);
        MutableShipStatsAPI stats = mock(MutableShipStatsAPI.class);
        ShipVariantAPI variant = ship.getVariant();
        when(stats.getVariant()).thenReturn(variant);

        createdEffect.applyEffectsBeforeShipCreation(HullSize.CRUISER, stats, HULL_MOD);
        createdEffect.advanceInCombat(ship, 0.1f);

        assertEquals(List.of("before:" + HULL_MOD, "advance"), RecordingHullModEffect.CALLS);
        assertFalse(createdEffect.isApplicableToShip(ship));
        assertTrue(createdEffect.shouldAddDescriptionToTooltip(HullSize.CRUISER, ship, false));
    }

    @Test
    void phantomCopiesPlacedByTheSkillTreeDoNothingAndCannotBeRemoved() {
        engineCreatesEffectsLazily();
        PhantomHullMods.install(List.of(HULL_MOD));
        ShipAPI ship = shipWith(true);
        MutableShipStatsAPI stats = mock(MutableShipStatsAPI.class);
        ShipVariantAPI variant = ship.getVariant();
        when(stats.getVariant()).thenReturn(variant);

        createdEffect.applyEffectsBeforeShipCreation(HullSize.CRUISER, stats, HULL_MOD);
        createdEffect.advanceInCombat(ship, 0.1f);

        assertTrue(RecordingHullModEffect.CALLS.isEmpty());
        assertTrue(createdEffect.isApplicableToShip(ship));
        assertFalse(createdEffect.shouldAddDescriptionToTooltip(HullSize.CRUISER, ship, false));
        assertFalse(createdEffect.canBeAddedOrRemovedNow(ship, null, CoreUITradeMode.OPEN));
        assertFalse(createdEffect.hasSModEffectSection(HullSize.CRUISER, ship, false));
    }
}
