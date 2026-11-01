package exiledsector.ui.decoration;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class HullModuleTest {

    private static final float TOLERANCE = 0.001f;

    @Test
    void aModuleWithoutAnAnchorSitsOnItsSlotAndTurnsByTheSlotAngle() {
        HullModule module = HullModule.attached("graphics/ships/module.png", 64f, 32f, 32f, 16f, 50f, 20f, 90f, 0f, 0f,
                2f, false, HullEngines.NONE);

        assertEquals(50f, module.forwardOffset(), TOLERANCE);
        assertEquals(20f, module.leftOffset(), TOLERANCE);
        assertEquals(200f, module.screenX(100f, 1f, 0f, 2f), TOLERANCE);
        assertEquals(240f, module.screenY(200f, 1f, 0f, 2f), TOLERANCE);
        assertEquals(120f, module.facingDeg(30f), TOLERANCE);
    }

    @Test
    void theModuleAnchorIsTurnedByTheSlotAngleAndPulledBackFromTheSlot() {
        HullModule module = HullModule.attached("graphics/ships/module.png", 64f, 32f, Float.NaN, Float.NaN, 50f, 0f, 90f, 10f, 0f,
                2f, false, HullEngines.NONE);

        assertEquals(50f, module.forwardOffset(), TOLERANCE);
        assertEquals(-10f, module.leftOffset(), TOLERANCE);
    }

    @Test
    void theModuleTurnsWithItsParentAroundTheParentCentre() {
        HullModule module = HullModule.attached("graphics/ships/module.png", 64f, 32f, 32f, 16f, 50f, 0f, 90f, 10f, 0f,
                2f, false, HullEngines.NONE);
        float facingCos = (float) Math.cos(Math.toRadians(90.0));
        float facingSin = (float) Math.sin(Math.toRadians(90.0));

        assertEquals(10f, module.screenX(0f, facingCos, facingSin, 1f), TOLERANCE);
        assertEquals(50f, module.screenY(0f, facingCos, facingSin, 1f), TOLERANCE);
        assertEquals(180f, module.facingDeg(90f), TOLERANCE);
    }

    @Test
    void anAngledSlotMatchesTheEngineFormulaForAnAnchoredModule() {
        float slotForward = 40f;
        float slotLeft = -25f;
        float slotAngleDeg = 30f;
        float anchorForward = 12f;
        float anchorLeft = 5f;
        float parentFacingDeg = 135f;
        HullModule module = HullModule.attached("graphics/ships/module.png", 64f, 32f, 32f, 16f, slotForward, slotLeft, slotAngleDeg,
                anchorForward, anchorLeft, 2f, false, HullEngines.NONE);

        double slotRad = Math.toRadians(slotAngleDeg);
        double anchorX = anchorForward * Math.cos(slotRad) - anchorLeft * Math.sin(slotRad);
        double anchorY = anchorForward * Math.sin(slotRad) + anchorLeft * Math.cos(slotRad);
        double localX = slotForward - anchorX;
        double localY = slotLeft - anchorY;
        double facingRad = Math.toRadians(parentFacingDeg);
        float facingCos = (float) Math.cos(facingRad);
        float facingSin = (float) Math.sin(facingRad);

        assertEquals(300f + (localX * Math.cos(facingRad) - localY * Math.sin(facingRad)) * 3f,
                module.screenX(300f, facingCos, facingSin, 3f), TOLERANCE);
        assertEquals(-80f + (localX * Math.sin(facingRad) + localY * Math.cos(facingRad)) * 3f,
                module.screenY(-80f, facingCos, facingSin, 3f), TOLERANCE);
        assertEquals(165f, module.facingDeg(parentFacingDeg), TOLERANCE);
    }

    @Test
    void onlyModulesWithEngineSlotsCountAsHavingEngines() {
        FleetHullVisuals withFlame = new FleetHullVisuals(32f, 16f,
                List.of(new FleetHullVisuals.EngineFlame(-30f, 0f, 180f, 8f, 20f, FleetHullVisuals.DEFAULT_FLAME_COLOR)), List.of());
        HullModule poweredModule = HullModule.attached("graphics/ships/module.png", 64f, 32f, 32f, 16f, 50f, 0f, 0f, 0f, 0f, 2f, false,
                HullEngines.of(withFlame));
        HullModule bareModule = HullModule.attached("graphics/ships/module.png", 64f, 32f, 32f, 16f, -50f, 0f, 0f, 0f, 0f, 2f, true,
                HullEngines.NONE);

        assertTrue(HullModules.of(List.of(poweredModule, bareModule)).hasEngines());
        assertFalse(HullModules.of(List.of(bareModule)).hasEngines());
        assertFalse(HullModules.NONE.hasEngines());
    }
}
