package exiledsector.ui.decoration;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.ShipHullSpecAPI;
import com.fs.starfarer.api.combat.ShipHullSpecAPI.ShipTypeHints;
import com.fs.starfarer.api.combat.ShipVariantAPI;
import com.fs.starfarer.api.graphics.SpriteAPI;
import com.fs.starfarer.api.loading.WeaponSlotAPI;
import com.fs.starfarer.api.util.Misc;
import exiledsector.skills.ShipFacts;
import exiledsector.skills.skilleffect.render.SpriteCache;
import exiledsector.ui.util.TexturedQuadBatch;
import org.apache.log4j.Logger;
import org.lwjgl.util.vector.Vector2f;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

final class HullModules {

    static final HullModules NONE = new HullModules(List.of(), List.of(), List.of());
    private static final Comparator<HullModule> LARGEST_FIRST = Comparator.comparingDouble(module -> -module.sizeNum());
    private static final Logger LOG = Logger.getLogger(HullModules.class);
    private static final Map<String, HullModules> MODULES_BY_HULL = new HashMap<>();
    private static final SpriteCache MEASURING_CACHE = new SpriteCache(HullModules.class);

    private final List<HullModule> underParent;
    private final List<HullModule> overParent;
    private final List<HullModule> withEngines;

    private HullModules(List<HullModule> underParent, List<HullModule> overParent, List<HullModule> withEngines) {
        this.underParent = underParent;
        this.overParent = overParent;
        this.withEngines = withEngines;
    }

    static HullModules of(List<HullModule> modules) {
        if (modules.isEmpty()) {
            return NONE;
        }
        List<HullModule> under = new ArrayList<>();
        List<HullModule> over = new ArrayList<>();
        for (HullModule module : modules) {
            (module.underParent() ? under : over).add(module);
        }
        under.sort(LARGEST_FIRST);
        over.sort(LARGEST_FIRST);
        List<HullModule> withEngines = modules.stream().filter(module -> module.engines().hasEngines()).toList();
        return new HullModules(List.copyOf(under), List.copyOf(over), withEngines);
    }

    static HullModules of(ShipHullSpecAPI hullSpec) {
        if (hullSpec == null || !ShipFacts.hasModuleSlots(hullSpec)) {
            return NONE;
        }
        return MODULES_BY_HULL.computeIfAbsent(hullSpec.getHullId(), hullId -> of(defaultVariant(hullSpec)));
    }

    static HullModules of(ShipVariantAPI variant) {
        Map<String, String> stationModules = variant == null ? null : variant.getStationModules();
        if (stationModules == null || stationModules.isEmpty() || variant.getHullSpec() == null) {
            return NONE;
        }
        ShipHullSpecAPI parentHull = variant.getHullSpec();
        float parentSizeNum = Misc.getSizeNum(parentHull.getHullSize());
        List<HullModule> modules = new ArrayList<>(stationModules.size());
        for (Map.Entry<String, String> stationModule : stationModules.entrySet()) {
            WeaponSlotAPI slot = parentHull.getWeaponSlotAPI(stationModule.getKey());
            ShipVariantAPI moduleVariant = moduleVariant(variant, stationModule.getKey(), stationModule.getValue());
            HullModule module = slot == null || moduleVariant == null ? null : module(slot, moduleVariant.getHullSpec(), parentSizeNum);
            if (module != null) {
                modules.add(module);
            }
        }
        return of(modules);
    }

    private static HullModule module(WeaponSlotAPI slot, ShipHullSpecAPI moduleHull, float parentSizeNum) {
        String spritePath = moduleHull == null ? null : moduleHull.getSpriteName();
        SpriteAPI texture = spritePath == null ? null : MEASURING_CACHE.texture(spritePath);
        if (texture == null) {
            return null;
        }
        FleetHullVisuals visuals = FleetHullVisuals.of(moduleHull);
        Vector2f slotLocation = slot.getLocation();
        Vector2f anchor = moduleHull.getModuleAnchor();
        float sizeNum = Misc.getSizeNum(moduleHull.getHullSize());
        Set<ShipTypeHints> hints = moduleHull.getHints();
        boolean underParent = (hints != null && hints.contains(ShipTypeHints.UNDER_PARENT)) || sizeNum > parentSizeNum;
        return HullModule.attached(spritePath, texture.getWidth(), texture.getHeight(), visuals.centerX(), visuals.centerY(), slotLocation.x,
                slotLocation.y, slot.getAngle(), anchor == null ? 0f : anchor.x, anchor == null ? 0f : anchor.y, sizeNum, underParent,
                HullEngines.of(visuals));
    }

    private static ShipVariantAPI moduleVariant(ShipVariantAPI variant, String slotId, String variantId) {
        try {
            ShipVariantAPI moduleVariant = variant.getModuleVariant(slotId);
            return moduleVariant != null || variantId == null ? moduleVariant : Global.getSettings().getVariant(variantId);
        } catch (RuntimeException e) {
            LOG.debug("No module variant " + variantId + " in slot " + slotId + " of " + variant.getHullVariantId() + ": " + e.getMessage());
            return null;
        }
    }

    private static ShipVariantAPI defaultVariant(ShipHullSpecAPI hullSpec) {
        for (String hullId : new String[]{hullSpec.getHullId(), hullSpec.getBaseHullId()}) {
            List<String> variantIds = hullId == null ? null : Global.getSettings().getHullIdToVariantListMap().get(hullId);
            if (variantIds == null) {
                continue;
            }
            for (String variantId : variantIds.stream().sorted().toList()) {
                ShipVariantAPI variant = loadedVariant(variantId);
                if (variant != null && variant.getStationModules() != null && !variant.getStationModules().isEmpty()) {
                    return variant;
                }
            }
        }
        return null;
    }

    private static ShipVariantAPI loadedVariant(String variantId) {
        try {
            return Global.getSettings().getVariant(variantId);
        } catch (RuntimeException e) {
            LOG.debug("No variant " + variantId + ": " + e.getMessage());
            return null;
        }
    }

    void drawUnderParent(SpriteCache spriteCache, float screenX, float screenY, float screenScale, float facingDeg, float alpha) {
        draw(underParent, spriteCache, screenX, screenY, screenScale, facingDeg, alpha);
    }

    void drawOverParent(SpriteCache spriteCache, float screenX, float screenY, float screenScale, float facingDeg, float alpha) {
        draw(overParent, spriteCache, screenX, screenY, screenScale, facingDeg, alpha);
    }

    private static void draw(List<HullModule> modules, SpriteCache spriteCache, float screenX, float screenY, float screenScale, float facingDeg,
                             float alpha) {
        if (modules.isEmpty()) return;
        double facingRad = Math.toRadians(facingDeg);
        float facingCos = (float) Math.cos(facingRad);
        float facingSin = (float) Math.sin(facingRad);
        for (HullModule module : modules) {
            HullEngines.drawSprite(spriteCache, module.spritePath(), module.screenX(screenX, facingCos, facingSin, screenScale),
                    module.screenY(screenY, facingCos, facingSin, screenScale), module.spriteWidth() * screenScale,
                    module.spriteHeight() * screenScale, module.centerX(), module.centerY(), screenScale, module.facingDeg(facingDeg), alpha);
        }
    }

    boolean hasEngines() {
        return !withEngines.isEmpty();
    }

    void addFlames(FlameBatch flames, float screenX, float screenY, double facingRad, float screenScale, float detailScale, float lengthFactor,
                   float alpha) {
        if (withEngines.isEmpty()) return;
        float facingCos = (float) Math.cos(facingRad);
        float facingSin = (float) Math.sin(facingRad);
        for (HullModule module : withEngines) {
            module.engines().addFlames(flames, module.screenX(screenX, facingCos, facingSin, screenScale),
                    module.screenY(screenY, facingCos, facingSin, screenScale), facingRad + Math.toRadians(module.angleDeg()), screenScale,
                    detailScale, lengthFactor, alpha);
        }
    }

    void addGlows(TexturedQuadBatch quads, float screenX, float screenY, double facingRad, float screenScale, float glowFactor, float alpha) {
        if (withEngines.isEmpty()) return;
        float facingCos = (float) Math.cos(facingRad);
        float facingSin = (float) Math.sin(facingRad);
        for (HullModule module : withEngines) {
            module.engines().addGlows(quads, module.screenX(screenX, facingCos, facingSin, screenScale),
                    module.screenY(screenY, facingCos, facingSin, screenScale), facingRad + Math.toRadians(module.angleDeg()), screenScale,
                    glowFactor, alpha);
        }
    }
}
