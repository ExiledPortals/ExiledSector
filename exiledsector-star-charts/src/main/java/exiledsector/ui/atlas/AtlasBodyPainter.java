package exiledsector.ui.atlas;

import com.fs.starfarer.api.campaign.PlanetSpecAPI;
import com.fs.starfarer.api.graphics.SpriteAPI;
import exiledsector.PlanetSpecs;
import exiledsector.charts.tree.AtlasBodyArt;
import exiledsector.skills.skilleffect.render.CategorySprite;
import exiledsector.skills.skilleffect.render.SpriteCache;
import exiledsector.ui.decoration.AtmosphereRing;
import exiledsector.ui.decoration.PlanetSphere;
import exiledsector.ui.decoration.UnitSphere;
import exiledsector.ui.util.SpriteDraw;

import java.awt.Color;
import java.util.List;

final class AtlasBodyPainter {

    enum Lighting { DULL, AVAILABLE, LIT }

    private static final Color DULL_TINT = new Color(78, 80, 88);
    private static final Color AVAILABLE_TINT = new Color(150, 152, 160);
    private static final String GLOW_PATH = "graphics/fx/glow64.png";
    private static final String CORE_GLOW_PATH = "graphics/fx/hit_glow.png";
    private static final String WHIRL_PATH = "graphics/stations/gate_whirl_glow.png";
    private static final float LIT_GLOW_SCALE = 3.4f;
    private static final float LIT_GLOW_ALPHA = 0.45f;
    private static final float UNLIT_GLOW_SHARE = 0.5f;
    private static final float SITE_LIGHT_REACH = 0.8f;
    private static final float SITE_LIGHT_SIZE = 0.7f;
    private static final float SITE_LIGHT_MIN_PIXELS = 6f;
    private static final float SITE_LIGHT_BLINKS_PER_SECOND = 4f;
    private static final float UNLIT_SITE_LIGHT_SHARE = 0.6f;
    private static final float BINARY_SEPARATION = 0.55f;
    private static final float BINARY_BODY_SHARE = 0.5f;
    private static final float BINARY_DEGREES_PER_SECOND = 10f;
    private static final float PLANET_TURN_DEGREES_PER_SECOND = 5f;
    private static final float ATMOSPHERE_MIN_SHARE = 0.12f;
    private static final float SPRITE_FACING_OFFSET = -90f;
    private static final int SPHERE_DETAIL = 24;

    private final SpriteCache sprites = new SpriteCache(AtlasBodyPainter.class);
    private final UnitSphere sphere = new UnitSphere(SPHERE_DETAIL);
    private final CategorySprite atmosphereTexture = new CategorySprite(AtmosphereRing.TEXTURE_CATEGORY, AtmosphereRing.TEXTURE_ID);

    record Placement(float screenX, float screenY, float screenRadius, float headingDegrees, float spinDegrees, float seedDegrees) {
    }

    void paint(AtlasBodyArt art, Color colour, Color siteLight, Placement placement, Lighting lighting, float seconds, float alpha) {
        if (art.kind() == AtlasBodyArt.Kind.STAR || alpha <= 0f) {
            return;
        }
        float glowSize = placement.screenRadius() * LIT_GLOW_SCALE;
        float glowAlpha = LIT_GLOW_ALPHA * (lighting == Lighting.LIT ? 1f : UNLIT_GLOW_SHARE);
        SpriteDraw.drawAdditiveAtCenter(sprites, GLOW_PATH, placement.screenX(), placement.screenY(), glowSize, glowSize, colour, alpha * glowAlpha);
        switch (art.kind()) {
            case PLANET -> paintPlanet(art, colour, placement, lighting, seconds, alpha);
            case SPRITE -> paintSprite(firstPath(art), placement.screenX(), placement.screenY(), placement.screenRadius(), angleOf(art, placement),
                    lighting, alpha);
            case BINARY -> paintBinary(art, placement, lighting, seconds, alpha);
            case BREACH -> paintBreach(colour, placement, lighting, seconds, alpha);
            case FLEET, DERELICTS, STATION -> {
            }
            default -> paintPlain(colour, placement, lighting, alpha);
        }
        if (siteLight != null) {
            paintSiteLight(siteLight, placement, lighting, seconds, alpha);
        }
    }

    private void paintSiteLight(Color light, Placement placement, Lighting lighting, float seconds, float alpha) {
        double siteAngle = Math.toRadians(placement.seedDegrees());
        float radius = placement.screenRadius();
        float blink = (float) Math.sin(seconds * SITE_LIGHT_BLINKS_PER_SECOND + placement.seedDegrees()) * 0.3f + 0.7f;
        float size = Math.max(SITE_LIGHT_MIN_PIXELS, radius * SITE_LIGHT_SIZE);
        float share = lighting == Lighting.LIT ? 1f : UNLIT_SITE_LIGHT_SHARE;
        SpriteDraw.drawAdditiveAtCenter(sprites, GLOW_PATH, placement.screenX() + (float) Math.cos(siteAngle) * radius * SITE_LIGHT_REACH,
                placement.screenY() + (float) Math.sin(siteAngle) * radius * SITE_LIGHT_REACH, size, size, light, blink * share * alpha);
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

    private static Color tintFor(Lighting lighting, Color litColour) {
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
        float radius = placement.screenRadius();
        float turn = placement.spinDegrees() + seconds * PLANET_TURN_DEGREES_PER_SECOND;
        PlanetSphere.draw(sphere, texture, spec, placement.screenX(), placement.screenY(), radius, turn, tintFor(lighting, spec.getPlanetColor()),
                alpha, 0f, 0f);
        SpriteAPI atmosphere = atmosphereTexture.get();
        if (lighting == Lighting.LIT && atmosphere != null) {
            float thickness = radius * Math.max(spec.getAtmosphereThickness(), ATMOSPHERE_MIN_SHARE);
            float innerRadius = radius - thickness * AtmosphereRing.INNER_INSET_MULT;
            Color atmosphereColour = spec.getAtmosphereThickness() > 0f ? spec.getAtmosphereColor() : colour;
            AtmosphereRing.draw(atmosphere, placement.screenX(), placement.screenY(), innerRadius, innerRadius + thickness, atmosphereColour, alpha);
        }
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
