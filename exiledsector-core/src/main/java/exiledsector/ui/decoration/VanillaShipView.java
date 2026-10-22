package exiledsector.ui.decoration;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.ShipVariantAPI;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import exiledsector.ui.util.GlScope;
import org.apache.log4j.Logger;
import org.lwjgl.opengl.GL11;

import java.awt.Color;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;

final class VanillaShipView {

    private record Handles(MethodHandle create, MethodHandle setVariant, MethodHandle updateWithStatus, MethodHandle setScissor,
                           MethodHandle render, MethodHandle ships, MethodHandle cleanup, MethodHandle engine, MethodHandle keepThisFrame) {
    }

    private static final Logger LOG = Logger.getLogger(VanillaShipView.class);
    private static final String VIEW_CLASS = "com.fs.starfarer.title.Object.do";
    private static final String FLEET_MEMBER_CLASS = "com.fs.starfarer.campaign.fleet.FleetMember";
    private static final String MEMBER_STATUS_CLASS = "com.fs.starfarer.campaign.fleet.FleetMemberStatus";
    private static final String VARIANT_CLASS = "com.fs.starfarer.loading.specs.HullVariantSpec";
    private static final String ENGINE_CLASS = "com.fs.starfarer.combat.CombatEngine";
    private static final String SHIP_CLASS = "com.fs.starfarer.combat.entities.Ship";
    private static final float VANILLA_FIT_SCALE = 0.95f;
    private static final float DRAW_BOX = 100000f;
    private static final float VANILLA_FACING_DEG = 90f;
    private static Handles handles;
    private static boolean unavailable;

    private final Object view;
    private final float imageOffsetX;
    private final float imageOffsetY;
    private Object hiddenEngineShips;

    private VanillaShipView(Object view, float imageOffsetX, float imageOffsetY) {
        this.view = view;
        this.imageOffsetX = imageOffsetX;
        this.imageOffsetY = imageOffsetY;
    }

    static VanillaShipView of(FleetMemberAPI member, ShipVariantAPI variant, float imageOffsetX, float imageOffsetY) {
        Handles loaded = handles();
        if (loaded == null || member == null) {
            return null;
        }
        try {
            Object view = loaded.create().invoke(member);
            ShipVariantAPI shown = variant == null ? member.getVariant() : variant;
            if (shown != member.getVariant()) {
                loaded.setVariant().invoke(view, shown, (String) null, (String) null);
            } else {
                loaded.updateWithStatus().invoke(view, member, member.getStatus());
            }
            loaded.setScissor().invoke(view, false);
            return new VanillaShipView(view, imageOffsetX, imageOffsetY);
        } catch (Throwable e) {
            disable("Could not create the vanilla ship view for " + member.getHullId(), e);
            return null;
        }
    }

    static float imageOffsetX(FleetHullVisuals visuals, float spriteWidth) {
        return visuals.hasCenter() ? spriteWidth / 2f - visuals.centerX() : 0f;
    }

    static float imageOffsetY(FleetHullVisuals visuals, float spriteHeight) {
        return visuals.hasCenter() ? spriteHeight / 2f - visuals.centerY() : 0f;
    }

    boolean draw(float pivotX, float pivotY, float screenScale, float facingDeg, float alpha) {
        if (unavailable || handles == null) {
            return false;
        }
        if (alpha <= 0f) {
            return true;
        }
        try (GlScope scope = GlScope.save(GlScope.DRAW_ATTRIBS)) {
            handles.keepThisFrame().invoke(handles.engine().invoke(), view);
            GL11.glPushMatrix();
            try {
                GL11.glTranslatef(pivotX, pivotY, 0f);
                GL11.glRotatef(facingDeg - VANILLA_FACING_DEG, 0f, 0f, 1f);
                float scale = screenScale / VANILLA_FIT_SCALE;
                GL11.glScalef(scale, scale, 1f);
                GL11.glTranslatef(imageOffsetX, imageOffsetY, 0f);
                handles.render().invoke(view, -DRAW_BOX / 2f, -DRAW_BOX / 2f, DRAW_BOX, DRAW_BOX, alpha, Color.WHITE, false);
            } finally {
                GL11.glPopMatrix();
            }
            hideEngines();
            return true;
        } catch (Throwable e) {
            disable("Could not draw the vanilla ship view", e);
            return false;
        }
    }

    void cleanup() {
        if (unavailable || handles == null) {
            return;
        }
        try {
            handles.cleanup().invoke(view);
        } catch (Throwable e) {
            disable("Could not clean up the vanilla ship view", e);
        }
    }

    private void hideEngines() throws Throwable {
        Object ships = handles.ships().invoke(view);
        if (ships == hiddenEngineShips || !(ships instanceof Object[] shipArray)) {
            return;
        }
        for (Object ship : shipArray) {
            if (ship instanceof ShipAPI shipApi) {
                shipApi.setRenderEngines(false);
            }
        }
        hiddenEngineShips = ships;
    }

    private static Handles handles() {
        if (handles == null && !unavailable) {
            try {
                handles = loadHandles();
            } catch (Throwable e) {
                disable("The vanilla ship view is unavailable; ships use the plain hull sprite", e);
            }
        }
        return handles;
    }

    private static Handles loadHandles() throws ReflectiveOperationException {
        ClassLoader gameLoader = Global.class.getClassLoader();
        Class<?> viewClass = Class.forName(VIEW_CLASS, true, gameLoader);
        Class<?> memberClass = Class.forName(FLEET_MEMBER_CLASS, true, gameLoader);
        Class<?> statusClass = Class.forName(MEMBER_STATUS_CLASS, true, gameLoader);
        Class<?> variantClass = Class.forName(VARIANT_CLASS, true, gameLoader);
        Class<?> engineClass = Class.forName(ENGINE_CLASS, true, gameLoader);
        Class<?> shipClass = Class.forName(SHIP_CLASS, true, gameLoader);
        MethodHandles.Lookup lookup = MethodHandles.publicLookup();
        return new Handles(
                lookup.findConstructor(viewClass, MethodType.methodType(void.class, memberClass)),
                lookup.findVirtual(viewClass, "setVariant", MethodType.methodType(void.class, variantClass, String.class, String.class)),
                lookup.findVirtual(viewClass, "updateWithStatus", MethodType.methodType(void.class, memberClass, statusClass)),
                lookup.findVirtual(viewClass, "setScissor", MethodType.methodType(void.class, boolean.class)),
                lookup.findVirtual(viewClass, "render", MethodType.methodType(void.class, float.class, float.class, float.class, float.class,
                        float.class, Color.class, boolean.class)),
                lookup.findVirtual(viewClass, "getShips", MethodType.methodType(shipClass.arrayType())),
                lookup.findVirtual(viewClass, "cleanup", MethodType.methodType(void.class)),
                lookup.findStatic(engineClass, "getInstance", MethodType.methodType(engineClass)),
                lookup.findVirtual(engineClass, "doNotCleanupThisFrame", MethodType.methodType(void.class, viewClass)));
    }

    private static void disable(String message, Throwable cause) {
        if (!unavailable) {
            LOG.warn(message, cause);
        }
        unavailable = true;
    }
}
