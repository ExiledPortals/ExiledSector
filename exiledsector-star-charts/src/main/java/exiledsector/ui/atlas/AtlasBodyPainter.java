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
    private static final float PLANET_TURN_DEGREES_PER_SECOND = 5f;
    private static final float ATMOSPHERE_MIN_SHARE = 0.12f;
    private static final float FLEET_LEAD_SHARE = 1.25f;
    private static final float FLEET_ESCORT_SHARE = 0.7f;
    private static final float FLEET_LEAD_AHEAD = 0.25f;
    private static final float FLEET_ESCORT_BEHIND = 0.45f;
    private static final float FLEET_ESCORT_SPREAD = 0.62f;
    private static final float SPRITE_FACING_OFFSET = -90f;
    private static final int SPHERE_DETAIL = 24;

    private final SpriteCache sprites = new SpriteCache(AtlasBodyPainter.class);
    private final UnitSphere sphere = new UnitSphere(SPHERE_DETAIL);
    private final CategorySprite atmosphereTexture = new CategorySprite(AtmosphereRing.TEXTURE_CATEGORY, AtmosphereRing.TEXTURE_ID);

    record Placement(float screenX, float screenY, float screenRadius, float headingDegrees, float spinDegrees) {
    }

    void paint(AtlasBodyArt art, Color colour, Placement placement, Lighting lighting, float seconds, float alpha) {
        if (art.kind() == AtlasBodyArt.Kind.STAR || alpha <= 0f) {
            return;
        }
        if (lighting == Lighting.LIT) {
            float glowSize = placement.screenRadius() * LIT_GLOW_SCALE;
            SpriteDraw.drawAdditiveAtCenter(sprites, GLOW_PATH, placement.screenX(), placement.screenY(), glowSize, glowSize, colour, alpha * LIT_GLOW_ALPHA);
        }
        switch (art.kind()) {
            case PLANET -> paintPlanet(art, colour, placement, lighting, seconds, alpha);
            case SPRITE -> paintSprite(firstPath(art), placement.screenX(), placement.screenY(), placement.screenRadius(), angleOf(art, placement),
                    lighting, alpha);
            case FLEET -> paintFleet(art.spritePaths(), placement, lighting, alpha);
            case BREACH -> paintBreach(colour, placement, lighting, seconds, alpha);
            default -> paintPlain(colour, placement, lighting, alpha);
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

    private void paintFleet(List<String> paths, Placement placement, Lighting lighting, float alpha) {
        if (paths.isEmpty()) {
            return;
        }
        double heading = Math.toRadians(placement.headingDegrees());
        float forwardX = (float) Math.cos(heading);
        float forwardY = (float) Math.sin(heading);
        float radius = placement.screenRadius();
        float angle = placement.headingDegrees() + SPRITE_FACING_OFFSET;
        for (int ship = 1; ship < paths.size(); ship++) {
            float side = ship % 2 == 0 ? -1f : 1f;
            float row = (ship + 1) / 2;
            float escortX = placement.screenX() - forwardX * radius * FLEET_ESCORT_BEHIND * row - forwardY * radius * FLEET_ESCORT_SPREAD * side;
            float escortY = placement.screenY() - forwardY * radius * FLEET_ESCORT_BEHIND * row + forwardX * radius * FLEET_ESCORT_SPREAD * side;
            paintSprite(paths.get(ship), escortX, escortY, radius * FLEET_ESCORT_SHARE, angle,
                    lighting, alpha);
        }
        paintSprite(paths.get(0), placement.screenX() + forwardX * radius * FLEET_LEAD_AHEAD, placement.screenY() + forwardY * radius * FLEET_LEAD_AHEAD,
                radius * FLEET_LEAD_SHARE, angle, lighting, alpha);
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
