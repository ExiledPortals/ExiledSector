package exiledsector.ui.decoration;

import com.fs.starfarer.api.combat.ShipHullSpecAPI;
import com.fs.starfarer.api.combat.ShipVariantAPI;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import com.fs.starfarer.api.graphics.SpriteAPI;
import exiledsector.skills.skilleffect.render.SpriteCache;

public final class RefitShipPortrait {

    static final float REFIT_FACING_DEG = 90f;

    private final SpriteCache spriteCache = new SpriteCache(RefitShipPortrait.class);
    private final String spritePath;
    private final float spriteWidth;
    private final float spriteHeight;
    private final FleetHullVisuals visuals;
    private final HullEngines engines;
    private final ShipHullSpecAPI hullSpec;
    private FleetMemberAPI member;
    private ShipVariantAPI variant;
    private VanillaShipView vanillaView;
    private boolean vanillaViewTried;

    private RefitShipPortrait(String spritePath, float spriteWidth, float spriteHeight, FleetHullVisuals visuals, HullEngines engines,
                              ShipHullSpecAPI hullSpec) {
        this.spritePath = spritePath;
        this.spriteWidth = spriteWidth;
        this.spriteHeight = spriteHeight;
        this.visuals = visuals;
        this.engines = engines;
        this.hullSpec = hullSpec;
    }

    public static RefitShipPortrait of(FleetMemberAPI member, ShipVariantAPI variant) {
        RefitShipPortrait portrait = of(variant);
        if (portrait != null) {
            portrait.member = member;
            portrait.variant = variant;
        }
        return portrait;
    }

    private static RefitShipPortrait of(ShipVariantAPI variant) {
        ShipHullSpecAPI hullSpec = variant == null ? null : variant.getHullSpec();
        String spritePath = hullSpec == null ? null : hullSpec.getSpriteName();
        if (spritePath == null) {
            return null;
        }
        SpriteAPI texture = new SpriteCache(RefitShipPortrait.class).texture(spritePath);
        if (texture == null) {
            return null;
        }
        FleetHullVisuals visuals = FleetHullVisuals.of(hullSpec);
        return new RefitShipPortrait(spritePath, texture.getWidth(), texture.getHeight(), visuals, HullEngines.of(variant), hullSpec);
    }

    ShipHullSpecAPI hullSpec() {
        return hullSpec;
    }

    FleetHullVisuals visuals() {
        return visuals;
    }

    float spriteWidth() {
        return spriteWidth;
    }

    float spriteHeight() {
        return spriteHeight;
    }

    public float halfWidthPixels(float zoom) {
        return spriteWidth * zoom / 2f;
    }

    public float halfLengthPixels(float zoom) {
        return spriteHeight * zoom / 2f;
    }

    public InspectedShip at(float imageCentreX, float imageCentreY, float zoom) {
        return new InspectedShip(pivotX(imageCentreX, zoom), pivotY(imageCentreY, zoom), zoom,
                Math.max(spriteWidth, spriteHeight) * zoom, REFIT_FACING_DEG);
    }

    public void draw(float imageCentreX, float imageCentreY, float zoom, float alpha) {
        if (alpha <= 0f) {
            return;
        }
        VanillaShipView view = vanillaView();
        if (view != null && view.draw(pivotX(imageCentreX, zoom), pivotY(imageCentreY, zoom), zoom, REFIT_FACING_DEG, alpha)) {
            return;
        }
        engines.drawHull(spriteCache, spritePath, pivotX(imageCentreX, zoom), pivotY(imageCentreY, zoom), spriteWidth * zoom, spriteHeight * zoom,
                zoom, REFIT_FACING_DEG, alpha);
    }

    public void cleanup() {
        if (vanillaView != null) {
            vanillaView.cleanup();
        }
    }

    private VanillaShipView vanillaView() {
        if (!vanillaViewTried && member != null) {
            vanillaViewTried = true;
            vanillaView = VanillaShipView.of(member, variant, VanillaShipView.imageOffsetX(visuals, spriteWidth),
                    VanillaShipView.imageOffsetY(visuals, spriteHeight));
        }
        return vanillaView;
    }

    float pivotX(float imageCentreX, float zoom) {
        return visuals.hasCenter() ? imageCentreX + (visuals.centerX() - spriteWidth / 2f) * zoom : imageCentreX;
    }

    float pivotY(float imageCentreY, float zoom) {
        return visuals.hasCenter() ? imageCentreY + (visuals.centerY() - spriteHeight / 2f) * zoom : imageCentreY;
    }
}
