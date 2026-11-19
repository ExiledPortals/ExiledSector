package exiledsector.ui.atlas;

import com.fs.starfarer.api.campaign.PlanetSpecAPI;
import com.fs.starfarer.api.graphics.SpriteAPI;
import exiledsector.PlanetSpecs;
import exiledsector.charts.tree.AtlasBodyArt;
import exiledsector.skills.skilleffect.render.CategorySprite;
import exiledsector.skills.skilleffect.render.SpriteCache;
import exiledsector.ui.belt.AuroraBeltRenderer;
import exiledsector.ui.belt.RadialBand;
import exiledsector.ui.belt.RingBeltRenderer;
import exiledsector.ui.decoration.AtmosphereRing;
import exiledsector.ui.decoration.PlanetSphere;
import exiledsector.ui.decoration.UnitSphere;
import exiledsector.ui.util.ColorUtil;
import exiledsector.ui.util.GlScope;
import exiledsector.ui.util.HoloShapes;
import exiledsector.ui.util.SpriteDraw;
import org.lwjgl.util.vector.Vector2f;
import org.lwjgl.util.vector.Vector3f;

import java.awt.Color;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

final class AtlasBodyPainter {

    enum Lighting { DULL, AVAILABLE, LIT }

    private static final Color DULL_TINT = new Color(78, 80, 88);
    private static final Color AVAILABLE_TINT = new Color(150, 152, 160);
    private static final String SITE_LIGHT_PATH = AtlasSystemArt.GLOW;
    private static final String CORE_GLOW_PATH = "graphics/fx/hit_glow.png";
    private static final String WHIRL_PATH = "graphics/stations/gate_whirl_glow.png";
    private static final float BREACH_VOID_SHARE = 1.05f;
    private static final float BREACH_VOID_EDGE_SHARE = 1.3f;
    private static final float SITE_LIGHT_REACH = 0.8f;
    private static final float SITE_LIGHT_SIZE = 0.3f;
    private static final float SITE_LIGHT_MIN_PIXELS = 12f;
    private static final float SITE_LIGHT_MAX_PIXELS = 36f;
    private static final int SITE_LIGHT_LAYERS = 2;
    private static final float SITE_LIGHT_MAX_LATITUDE = 45f;
    private static final float SITE_LIGHT_LIMB_FADE = 0.25f;
    private static final float SITE_LIGHT_BLINKS_PER_SECOND = 4f;
    private static final float UNLIT_SITE_LIGHT_SHARE = 0.6f;
    private static final float BINARY_SEPARATION = 0.55f;
    private static final float BINARY_BODY_SHARE = 0.5f;
    private static final float BINARY_DEGREES_PER_SECOND = 2f;
    private static final float PLANET_TURN_DEGREES_PER_SECOND = 5f;
    private static final float ATMOSPHERE_MIN_SHARE = 0.12f;
    private static final float SPRITE_FACING_OFFSET = -90f;
    private static final int SPHERE_DETAIL = 24;
    private static final Color HORIZON_COLOUR = new Color(255, 170, 255);
    private static final Color ACCRETION_COLOUR = new Color(167, 120, 167);
    private static final String HALO_PATH = "graphics/fx/star_halo.png";
    private static final String AURORA_PATH = "graphics/planets/aurorae.png";
    private static final float HOLE_SHARE = 0.42f;
    private static final float RIM_INNER = 0.9f;
    private static final float RIM_OUTER = 1.15f;
    private static final int ACCRETION_BANDS = 4;
    private static final float ACCRETION_INNER_SHARE = 0.46f;
    private static final float ACCRETION_BAND_STEP = 0.12f;
    private static final float ACCRETION_BAND_WIDTH = 0.28f;
    private static final float ACCRETION_SLOT_WIDTH = 256f;
    private static final float ACCRETION_DEGREES_PER_SECOND = 14f;
    private static final float ACCRETION_SLOWING_PER_BAND = 3f;
    private static final float HORIZON_INNER = 1.3f;
    private static final float HORIZON_OUTER = 2.4f;
    private static final float HORIZON_ALPHA = 0.18f;
    private static final float HALO_HALF_SIZE = 5f;
    private static final float HALO_ALPHA = 0.55f;
    private static final float STAR_CORONA_MAX_SHARE = 2.2f;

    private final SpriteCache sprites = new SpriteCache(AtlasBodyPainter.class);
    private final UnitSphere sphere = new UnitSphere(SPHERE_DETAIL);
    private final CategorySprite atmosphereTexture = new CategorySprite(AtmosphereRing.TEXTURE_CATEGORY, AtmosphereRing.TEXTURE_ID);
    private final CategorySprite dustRings = new CategorySprite("misc", "rings_dust0");
    private final Vector2f bandCentre = new Vector2f();
    private final Vector3f siteOffset = new Vector3f();
    private final AtlasStormPainter storms = new AtlasStormPainter();
    private final AtlasFlarePainter flares = new AtlasFlarePainter();
    private final Map<String, Color> tintsByHex = new HashMap<>();

    record Placement(float screenX, float screenY, float screenRadius, float headingDegrees, float spinDegrees, float seedDegrees,
                     float stormSeconds) {
    }

    void paint(AtlasBodyArt art, Color colour, Color siteLight, Placement placement, Lighting lighting, float seconds, float alpha) {
        if (art.kind() == AtlasBodyArt.Kind.STAR || alpha <= 0f) {
            return;
        }
        switch (art.kind()) {
            case PLANET, SWARMED_PLANET, CROSSED_PLANET -> {
                paintPlanet(art, colour, placement, lighting, seconds, alpha);
                storms.paintOnPlanet(art.storm(), colour, placement, lighting, seconds, alpha);
                flares.paint(art.flare(), colour, placement, lighting, alpha);
            }
            case STORM_CELL -> storms.paintCell(colour, placement, lighting, seconds, alpha);
            case SPRITE -> paintSprite(firstPath(art), placement.screenX(), placement.screenY(), placement.screenRadius(), angleOf(art, placement),
                    lighting, alpha);
            case BINARY -> paintBinary(art, placement, lighting, seconds, alpha);
            case BREACH -> paintBreach(colour, placement, lighting, seconds, alpha);
            case FLEET, DERELICTS, SHROUDED, STATION -> {
            }
            default -> paintPlain(colour, placement, lighting, alpha);
        }
        if (siteLight != null) {
            paintSiteLight(art, siteLight, placement, lighting, seconds, alpha);
        }
    }

    private void paintSiteLight(AtlasBodyArt art, Color light, Placement placement, Lighting lighting, float seconds, float alpha) {
        float radius = placement.screenRadius();
        PlanetSpecAPI spec = art.isPlanet() ? PlanetSpecs.byType(art.planetType()) : null;
        float offsetX;
        float offsetY;
        float facing;
        if (spec == null) {
            double siteAngle = Math.toRadians(placement.seedDegrees());
            offsetX = (float) Math.cos(siteAngle) * SITE_LIGHT_REACH;
            offsetY = (float) Math.sin(siteAngle) * SITE_LIGHT_REACH;
            facing = 1f;
        } else {
            siteOnSurface(spec, placement.seedDegrees(), placement.spinDegrees() + seconds * PLANET_TURN_DEGREES_PER_SECOND);
            offsetX = siteOffset.x;
            offsetY = siteOffset.y;
            facing = Math.min(1f, siteOffset.z / SITE_LIGHT_LIMB_FADE);
        }
        if (facing <= 0f) {
            return;
        }
        float blink = (float) Math.sin(seconds * SITE_LIGHT_BLINKS_PER_SECOND + placement.seedDegrees()) * 0.3f + 0.7f;
        float size = Math.min(SITE_LIGHT_MAX_PIXELS, Math.max(SITE_LIGHT_MIN_PIXELS, radius * SITE_LIGHT_SIZE));
        float share = lighting == Lighting.LIT ? 1f : UNLIT_SITE_LIGHT_SHARE;
        for (int layer = 0; layer < SITE_LIGHT_LAYERS; layer++) {
            SpriteDraw.drawAdditiveAtCenter(sprites, SITE_LIGHT_PATH, placement.screenX() + offsetX * radius, placement.screenY() + offsetY * radius, size,
                    size, light, blink * share * facing * alpha);
        }
    }

    private void siteOnSurface(PlanetSpecAPI spec, float seedDegrees, float rotationDegrees) {
        double latitude = Math.toRadians(SITE_LIGHT_MAX_LATITUDE * Math.sin(Math.toRadians(seedDegrees * 3f)));
        double longitude = Math.toRadians(seedDegrees);
        double x = Math.cos(latitude) * Math.cos(longitude);
        double y = Math.sin(latitude);
        double z = Math.cos(latitude) * Math.sin(longitude);
        double spin = Math.toRadians(rotationDegrees);
        double spunX = x * Math.cos(spin) + z * Math.sin(spin);
        double spunZ = -x * Math.sin(spin) + z * Math.cos(spin);
        double pitch = Math.toRadians(spec.getPitch());
        double pitchedY = y * Math.cos(pitch) - spunZ * Math.sin(pitch);
        double pitchedZ = y * Math.sin(pitch) + spunZ * Math.cos(pitch);
        double tilt = Math.toRadians(spec.getTilt());
        siteOffset.set((float) (spunX * Math.cos(tilt) - pitchedY * Math.sin(tilt)), (float) (spunX * Math.sin(tilt) + pitchedY * Math.cos(tilt)),
                (float) -pitchedZ);
    }

    private void paintBinary(AtlasBodyArt art, Placement placement, Lighting lighting, float seconds, float alpha) {
        double pairTurn = Math.toRadians(placement.seedDegrees() + seconds * BINARY_DEGREES_PER_SECOND * art.spinRate());
        float offset = placement.screenRadius() * BINARY_SEPARATION;
        float offsetX = offset * (float) Math.cos(pairTurn);
        float offsetY = offset * (float) Math.sin(pairTurn);
        float bodyRadius = placement.screenRadius() * BINARY_BODY_SHARE;
        List<String> paths = art.spritePaths();
        for (int member = 0; member < Math.min(2, paths.size()); member++) {
            float side = member == 0 ? 1f : -1f;
            paintSprite(paths.get(member), placement.screenX() + offsetX * side, placement.screenY() + offsetY * side, bodyRadius,
                    placement.spinDegrees() * side, lighting, alpha);
        }
    }

    private static String firstPath(AtlasBodyArt art) {
        return art.spritePaths().isEmpty() ? null : art.spritePaths().get(0);
    }

    private static float angleOf(AtlasBodyArt art, Placement placement) {
        return art.facing() == AtlasBodyArt.Facing.TRAVEL ? placement.headingDegrees() + SPRITE_FACING_OFFSET : placement.spinDegrees();
    }

    static Color tintFor(Lighting lighting, Color litColour) {
        return switch (lighting) {
            case LIT -> litColour;
            case AVAILABLE -> AVAILABLE_TINT;
            case DULL -> DULL_TINT;
        };
    }

    private void paintPlanet(AtlasBodyArt art, Color colour, Placement placement, Lighting lighting, float seconds, float alpha) {
        PlanetSpecAPI spec = PlanetSpecs.byType(art.planetType());
        String texturePath = spec == null ? null : spec.getTexture();
        SpriteAPI texture = texturePath == null || texturePath.isEmpty() ? null : sprites.texture(texturePath);
        if (texture == null) {
            paintPlain(colour, placement, lighting, alpha);
            return;
        }
        if (spec.isBlackHole()) {
            paintBlackHole(texture, spec, placement, lighting, seconds, alpha);
            return;
        }
        float radius = placement.screenRadius();
        float turn = placement.spinDegrees() + seconds * PLANET_TURN_DEGREES_PER_SECOND;
        Color litTint = art.tintColour() == null ? spec.getPlanetColor()
                : tintsByHex.computeIfAbsent(art.tintColour(), hex -> ColorUtil.parseHexColor(hex, Color.WHITE));
        PlanetSphere.draw(sphere, texture, spec, placement.screenX(), placement.screenY(), radius, turn, tintFor(lighting, litTint), alpha, 0f, 0f);
        if (spec.isStar()) {
            paintStarCorona(spec, placement, lighting, alpha);
        }
        SpriteAPI atmosphere = atmosphereTexture.get();
        if (lighting == Lighting.LIT && atmosphere != null) {
            float thickness = radius * Math.max(spec.getAtmosphereThickness(), ATMOSPHERE_MIN_SHARE);
            float innerRadius = radius - thickness * AtmosphereRing.INNER_INSET_MULT;
            Color atmosphereColour = spec.getAtmosphereThickness() > 0f ? spec.getAtmosphereColor() : colour;
            AtmosphereRing.draw(atmosphere, placement.screenX(), placement.screenY(), innerRadius, innerRadius + thickness, atmosphereColour, alpha);
        }
    }

    private void paintStarCorona(PlanetSpecAPI spec, Placement placement, Lighting lighting, float alpha) {
        String coronaPath = spec.getCoronaTexture();
        if (coronaPath == null || coronaPath.isEmpty()) {
            return;
        }
        float glow = switch (lighting) {
            case LIT -> 1f;
            case AVAILABLE -> 0.5f;
            case DULL -> 0.25f;
        };
        float corona = placement.screenRadius() * Math.min(spec.getCoronaSize(), STAR_CORONA_MAX_SHARE) * 2f;
        SpriteDraw.drawAdditiveAtCenter(sprites, coronaPath, placement.screenX(), placement.screenY(), corona, corona,
                tintFor(lighting, spec.getCoronaColor()), alpha * glow);
    }

    private void paintBlackHole(SpriteAPI texture, PlanetSpecAPI spec, Placement placement, Lighting lighting, float seconds, float alpha) {
        float glow = alpha * switch (lighting) {
            case LIT -> 1f;
            case AVAILABLE -> 0.75f;
            case DULL -> 0.45f;
        };
        float x = placement.screenX();
        float y = placement.screenY();
        float hole = placement.screenRadius() * HOLE_SHARE;
        bandCentre.set(x, y);
        SpriteAPI dust = dustRings.get();
        if (dust != null) {
            for (int band = 0; band < ACCRETION_BANDS; band++) {
                float inner = placement.screenRadius() * (ACCRETION_INNER_SHARE + ACCRETION_BAND_STEP * band);
                float turn = placement.seedDegrees() * (band + 1) + seconds * (ACCRETION_DEGREES_PER_SECOND - ACCRETION_SLOWING_PER_BAND * band);
                RingBeltRenderer.renderSlot(dust, new RadialBand(bandCentre, inner, inner + placement.screenRadius() * ACCRETION_BAND_WIDTH), band % 2,
                        ACCRETION_SLOT_WIDTH, ACCRETION_COLOUR, glow * (1f - band * 0.2f), turn);
            }
        }
        PlanetSphere.draw(sphere, texture, spec, x, y, hole, 0f, Color.BLACK, alpha, 0f, 0f);
        SpriteAPI atmosphere = atmosphereTexture.get();
        if (atmosphere != null) {
            AtmosphereRing.draw(atmosphere, x, y, hole * RIM_INNER, hole * RIM_OUTER, HORIZON_COLOUR, glow);
        }
        SpriteAPI aurora = sprites.texture(AURORA_PATH);
        if (aurora != null) {
            AuroraBeltRenderer.render(aurora, new RadialBand(bandCentre, hole * HORIZON_INNER, hole * HORIZON_OUTER), HORIZON_COLOUR, glow * HORIZON_ALPHA,
                    seconds, 1f);
        }
        float halo = hole * HALO_HALF_SIZE * 2f;
        SpriteDraw.drawAdditiveAtCenter(sprites, HALO_PATH, x, y, halo, halo, HORIZON_COLOUR, glow * HALO_ALPHA);
    }

    private void paintSprite(String path, float screenX, float screenY, float screenRadius, float angleDegrees, Lighting lighting, float alpha) {
        SpriteAPI sprite = path == null ? null : sprites.sprite(path);
        if (sprite == null) {
            return;
        }
        float longest = Math.max(sprite.getWidth(), sprite.getHeight());
        if (longest <= 0f) {
            return;
        }
        float scale = screenRadius * 2f / longest;
        SpriteDraw.drawAtCenter(sprite, screenX, screenY, sprite.getWidth() * scale, sprite.getHeight() * scale, tintFor(lighting, Color.WHITE), alpha,
                angleDegrees);
    }

    private void paintBreach(Color colour, Placement placement, Lighting lighting, float seconds, float alpha) {
        float strength = switch (lighting) {
            case LIT -> 0.9f;
            case AVAILABLE -> 0.4f;
            case DULL -> 0.2f;
        };
        float radius = placement.screenRadius();
        try (GlScope scope = GlScope.flat()) {
            HoloShapes.disc(placement.screenX(), placement.screenY(), radius * BREACH_VOID_SHARE, Color.BLACK, alpha);
            HoloShapes.radialGlow(placement.screenX(), placement.screenY(), radius * BREACH_VOID_SHARE, radius * BREACH_VOID_EDGE_SHARE, Color.BLACK,
                    alpha);
        }
        float outer = radius * 2.4f;
        float inner = radius * 1.7f;
        SpriteDraw.drawAdditiveAtCenter(sprites, WHIRL_PATH, placement.screenX(), placement.screenY(), outer, outer, colour, alpha * strength,
                placement.spinDegrees() + seconds * 40f);
        SpriteDraw.drawAdditiveAtCenter(sprites, WHIRL_PATH, placement.screenX(), placement.screenY(), inner, inner, colour, alpha * strength,
                -placement.spinDegrees() - seconds * 25f);
        float core = radius * 1.1f;
        SpriteDraw.drawAdditiveAtCenter(sprites, CORE_GLOW_PATH, placement.screenX(), placement.screenY(), core, core, colour, alpha * strength);
    }

    private void paintPlain(Color colour, Placement placement, Lighting lighting, float alpha) {
        float size = placement.screenRadius() * 2f;
        SpriteDraw.drawAdditiveAtCenter(sprites, CORE_GLOW_PATH, placement.screenX(), placement.screenY(), size, size, tintFor(lighting, colour), alpha);
    }
}
