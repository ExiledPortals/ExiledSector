package exiledsector.ui;

import com.fs.starfarer.api.graphics.SpriteAPI;
import com.fs.starfarer.api.ui.PositionAPI;
import com.fs.starfarer.api.util.Misc;
import exiledsector.skills.ResearchStationNodes;
import exiledsector.skills.SkillNode;
import exiledsector.skills.skilleffect.render.Fade;
import exiledsector.skills.skilleffect.render.SpriteCache;
import exiledsector.ui.decoration.ResearchStationOrbit;
import exiledsector.ui.util.SpriteDraw;
import exiledsector.ui.util.TexturedQuadBatch;

import java.awt.Color;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;

final class ResearchStationView {

    static final String SPRITE_PATH = "graphics/stations/station_side00.png";
    static final String PACKET_PATH = "graphics/fx/hit_glow.png";
    static final float STATION_SIZE = 300f;
    static final float HIT_SHARE = 0.45f;
    static final float SPIRAL_SECONDS = 0.9f;
    static final float SPIRAL_TURN_RADIANS = (float) (1.5 * Math.PI);
    static final float FIT_SHARE = 0.92f;
    static final float LAYOUT_MARGIN = 96f;
    static final float SETTLED = 0.001f;
    static final float VIEW_FADE_SECONDS = 0.3f;
    static final float SURROUNDING_DIM = 0.6f;
    static final float HUB_FRACTION = 0.70f;
    static final float SPINE_BOTTOM_FRACTION = 0.01f;
    static final float SPINE_TOP_FRACTION = 0.98f;
    static final float WAKE_SECONDS = 0.6f;
    static final float FLASH_SECONDS = 0.8f;
    private static final float HOVER_GLOW_ALPHA = 0.35f;
    private static final Color HOVER_GLOW = new Color(150, 210, 255);
    private static final Color DORMANT_TINT = new Color(140, 145, 160);
    private static final float HOLD_FILL_ALPHA = 0.5f;
    private static final float FRONT_WIDTH = 0.025f;
    private static final float BREATH_SECONDS = 3f;
    private static final float BREATH_BASE_ALPHA = 0.1f;
    private static final float BREATH_SWING_ALPHA = 0.08f;
    private static final float PULSE_PERIOD_SECONDS = 3f;
    private static final float PULSE_TRAVEL_SECONDS = 1.2f;
    private static final float PULSE_WIDTH = 0.04f;
    private static final float PULSE_ALPHA = 0.8f;
    private static final float FLASH_ALPHA = 0.9f;
    private static final int PACKETS = 3;
    private static final float PACKET_SECONDS = 4f;
    private static final float PACKET_SIZE_SHARE = 0.07f;
    private static final float DISC_HALF_WIDTH_SHARE = 0.27f;
    private static final float DISC_HALF_HEIGHT_SHARE = 0.05f;
    private static final float ANIMATION_LOOP_SECONDS = 600f;

    private final SkillNode stationNode;
    private final List<SkillNode> subsystems;
    private final Map<SkillNode, Integer> slotByNode = new IdentityHashMap<>();
    private final ResearchStationOrbit orbit;
    private final float ringReach;
    private final SpriteCache spriteCache = new SpriteCache(ResearchStationView.class);
    private final TexturedQuadBatch glowQuads = new TexturedQuadBatch();
    private Color circuitColour;
    private Color shownTint = DORMANT_TINT;
    private float shownTintWake;
    private float animationSeconds;
    private float expansion;
    private float viewLevel;
    private boolean active;
    private boolean hovered;
    private boolean integrated;
    private boolean integrationKnown;
    private float holdProgress;
    private float wakeLevel;
    private float flashLevel;
    private final float[] shiftX;
    private final float[] shiftY;
    private final float[] alpha;
    private final boolean[] interactive;
    private final ResearchStationLayer layer;

    ResearchStationView(ResearchStationOrbit orbit) {
        this(orbit, ResearchStationNodes.subsystems());
    }

    ResearchStationView(SkillNode stationNode, List<SkillNode> subsystems) {
        this(new ResearchStationOrbit(stationNode, 0f), subsystems);
    }

    private ResearchStationView(ResearchStationOrbit orbit, List<SkillNode> subsystems) {
        this.orbit = orbit;
        this.stationNode = orbit.stationNode();
        this.subsystems = stationNode == null ? List.of() : subsystems;
        this.ringReach = ringReach(stationNode, this.subsystems);
        int slots = stationNode == null ? 0 : this.subsystems.size() + 1;
        shiftX = new float[slots];
        shiftY = new float[slots];
        alpha = new float[slots];
        interactive = new boolean[slots];
        if (stationNode != null) {
            slotByNode.put(stationNode, 0);
            for (int index = 0; index < this.subsystems.size(); index++) {
                slotByNode.put(this.subsystems.get(index), index + 1);
            }
        }
        layer = stationNode == null ? ResearchStationLayer.NONE
                : new ResearchStationLayer(stationNode, slotByNode, shiftX, shiftY, alpha, interactive, STATION_SIZE * HIT_SHARE, 1f, true);
    }

    private static float ringReach(SkillNode stationNode, List<SkillNode> subsystems) {
        float reach = STATION_SIZE / 2f;
        if (stationNode == null) {
            return reach + LAYOUT_MARGIN;
        }
        for (SkillNode subsystem : subsystems) {
            reach = Math.max(reach, (float) Math.hypot(subsystem.getX() - stationNode.getX(), subsystem.getY() - stationNode.getY()));
        }
        return reach + LAYOUT_MARGIN;
    }

    boolean isAvailable() {
        return stationNode != null;
    }

    SkillNode stationNode() {
        return stationNode;
    }

    boolean isActive() {
        return active;
    }

    void enter() {
        active = isAvailable();
    }

    void leave() {
        active = false;
    }

    void setHovered(boolean hovered) {
        this.hovered = hovered;
    }

    void setIntegration(boolean stationIntegrated, float stationHoldProgress) {
        if (!integrationKnown) {
            integrationKnown = true;
            wakeLevel = stationIntegrated ? 1f : 0f;
        } else if (stationIntegrated && !integrated) {
            flashLevel = 1f;
        }
        integrated = stationIntegrated;
        holdProgress = stationIntegrated ? 0f : Math.max(0f, Math.min(1f, stationHoldProgress));
    }

    float wakeLevel() {
        return wakeLevel;
    }

    float flashLevel() {
        return flashLevel;
    }

    float expansion() {
        return expansion;
    }

    boolean isSpiralling() {
        return expansion > SETTLED && expansion < 1f - SETTLED;
    }

    float focusX() {
        return orbit.x();
    }

    float focusY() {
        return orbit.y();
    }

    float fitZoom(PositionAPI canvasPosition) {
        float fitted = Math.min(canvasPosition.getWidth(), canvasPosition.getHeight()) / 2f * FIT_SHARE / ringReach;
        return Math.max(SmoothZoom.MIN_ZOOM, Math.min(SmoothZoom.MAX_ZOOM, fitted));
    }

    void advance(float amount, SkillNode pickedSubsystem) {
        if (!isAvailable()) {
            return;
        }
        orbit.advance(amount);
        animationSeconds = (animationSeconds + amount) % ANIMATION_LOOP_SECONDS;
        wakeLevel = Fade.step(wakeLevel, integrated, amount / WAKE_SECONDS);
        flashLevel = Math.max(0f, flashLevel - amount / FLASH_SECONDS);
        expansion = Fade.step(expansion, active && pickedSubsystem == null, amount / SPIRAL_SECONDS);
        viewLevel = Fade.step(viewLevel, active, amount / VIEW_FADE_SECONDS);
        updateLayer(pickedSubsystem);
    }

    ResearchStationLayer layer() {
        return layer;
    }

    private void updateLayer(SkillNode pickedSubsystem) {
        float stationX = focusX();
        float stationY = focusY();
        float shown = Fade.smooth(expansion);
        float pickedPresence = pickedSubsystem == null ? 0f : 1f - shown;
        shiftX[0] = stationX - stationNode.getX();
        shiftY[0] = stationY - stationNode.getY();
        alpha[0] = 1f - pickedPresence;
        interactive[0] = !isSpiralling();
        double turn = SPIRAL_TURN_RADIANS * (1f - shown);
        float cos = (float) Math.cos(turn);
        float sin = (float) Math.sin(turn);
        for (int index = 0; index < subsystems.size(); index++) {
            SkillNode subsystem = subsystems.get(index);
            float offsetX = (subsystem.getX() - stationNode.getX()) * shown;
            float offsetY = (subsystem.getY() - stationNode.getY()) * shown;
            int slot = index + 1;
            shiftX[slot] = stationX + offsetX * cos - offsetY * sin - subsystem.getX();
            shiftY[slot] = stationY + offsetX * sin + offsetY * cos - subsystem.getY();
            boolean picked = subsystem == pickedSubsystem;
            alpha[slot] = picked ? 1f : shown;
            interactive[slot] = active && (picked ? expansion <= SETTLED : expansion >= 1f - SETTLED);
        }
        layer.setSurroundings(1f - SURROUNDING_DIM * Fade.smooth(viewLevel), !active);
    }

    static float holdFillBottom(float progress) {
        return HUB_FRACTION - progress * (HUB_FRACTION - SPINE_BOTTOM_FRACTION);
    }

    static float holdFillTop(float progress) {
        return HUB_FRACTION + progress * (SPINE_TOP_FRACTION - HUB_FRACTION);
    }

    void render(TreeViewport viewport, float alphaMult) {
        if (!isAvailable() || alphaMult <= 0f) {
            return;
        }
        float screenX = viewport.screenX(focusX());
        float screenY = viewport.screenY(focusY());
        float screenSize = STATION_SIZE * viewport.zoom();
        if (!viewport.isVisible(screenX, screenY, screenSize)) {
            return;
        }
        float angleDeg = -(orbit.angleDeg() + 90f);
        if (circuitColour == null) {
            circuitColour = Misc.getStoryOptionColor();
        }
        SpriteDraw.drawAtCenter(spriteCache, SPRITE_PATH, screenX, screenY, screenSize, screenSize, tintFor(wakeLevel), alphaMult, angleDeg);
        SpriteAPI glowTexture = spriteCache.texture(SPRITE_PATH);
        if (glowTexture != null && (holdProgress > 0f || wakeLevel > 0f || flashLevel > 0f || hovered && !active)) {
            try (TexturedQuadBatch quads = glowQuads.beginAdditive(glowTexture)) {
                drawStationGlow(quads, screenX, screenY, screenSize, angleDeg, alphaMult);
            }
        }
        float packetAlpha = Math.max(wakeLevel, holdProgress) * alphaMult;
        SpriteAPI packetTexture = packetAlpha > 0f ? spriteCache.texture(PACKET_PATH) : null;
        if (packetTexture != null) {
            try (TexturedQuadBatch quads = glowQuads.beginAdditive(packetTexture)) {
                drawPackets(quads, screenX, screenY, screenSize, angleDeg, packetAlpha);
            }
        }
    }

    private Color tintFor(float wake) {
        if (Float.compare(wake, shownTintWake) != 0) {
            shownTintWake = wake;
            shownTint = new Color(lerp(DORMANT_TINT.getRed(), wake), lerp(DORMANT_TINT.getGreen(), wake), lerp(DORMANT_TINT.getBlue(), wake));
        }
        return shownTint;
    }

    private static int lerp(int dormantChannel, float wake) {
        return Math.round(dormantChannel + (255 - dormantChannel) * wake);
    }

    private void drawStationGlow(TexturedQuadBatch quads, float screenX, float screenY, float screenSize, float angleDeg, float alphaMult) {
        if (hovered && !active) {
            quads.quadAtCenter(screenX, screenY, screenSize, screenSize, angleDeg, HOVER_GLOW, alphaMult * HOVER_GLOW_ALPHA);
        }
        if (holdProgress > 0f) {
            float bottom = holdFillBottom(holdProgress);
            float top = holdFillTop(holdProgress);
            quads.bandAtCenter(bottom, top, screenX, screenY, screenSize, screenSize, angleDeg, circuitColour, alphaMult * HOLD_FILL_ALPHA);
            quads.bandAtCenter(bottom, bottom + FRONT_WIDTH, screenX, screenY, screenSize, screenSize, angleDeg, circuitColour, alphaMult);
            quads.bandAtCenter(top - FRONT_WIDTH, top, screenX, screenY, screenSize, screenSize, angleDeg, circuitColour, alphaMult);
        }
        if (wakeLevel > 0f) {
            float breath = (float) Math.sin(2 * Math.PI * animationSeconds / BREATH_SECONDS);
            quads.quadAtCenter(screenX, screenY, screenSize, screenSize, angleDeg, circuitColour,
                    alphaMult * wakeLevel * (BREATH_BASE_ALPHA + BREATH_SWING_ALPHA * breath));
            float travel = (animationSeconds % PULSE_PERIOD_SECONDS) / PULSE_TRAVEL_SECONDS;
            if (travel < 1f) {
                float reach = Fade.smooth(travel);
                float pulseAlpha = alphaMult * wakeLevel * PULSE_ALPHA * (1f - travel);
                float down = holdFillBottom(reach);
                float up = holdFillTop(reach);
                quads.bandAtCenter(down, down + PULSE_WIDTH, screenX, screenY, screenSize, screenSize, angleDeg, circuitColour, pulseAlpha);
                quads.bandAtCenter(up - PULSE_WIDTH, up, screenX, screenY, screenSize, screenSize, angleDeg, circuitColour, pulseAlpha);
            }
        }
        if (flashLevel > 0f) {
            quads.quadAtCenter(screenX, screenY, screenSize, screenSize, angleDeg, circuitColour, alphaMult * FLASH_ALPHA * flashLevel * flashLevel);
        }
    }

    private void drawPackets(TexturedQuadBatch quads, float screenX, float screenY, float screenSize, float angleDeg, float packetAlpha) {
        double angle = Math.toRadians(angleDeg);
        float cos = (float) Math.cos(angle);
        float sin = (float) Math.sin(angle);
        float hubOffsetY = (HUB_FRACTION - 0.5f) * screenSize;
        float halfWidth = DISC_HALF_WIDTH_SHARE * screenSize;
        float halfHeight = DISC_HALF_HEIGHT_SHARE * screenSize;
        float packetSize = PACKET_SIZE_SHARE * screenSize;
        double spin = 2 * Math.PI * animationSeconds / PACKET_SECONDS;
        for (int packet = 0; packet < PACKETS; packet++) {
            double phase = spin + packet * 2 * Math.PI / PACKETS;
            float depth = (float) Math.sin(phase);
            float localX = halfWidth * (float) Math.cos(phase);
            float localY = hubOffsetY + halfHeight * depth;
            float packetX = screenX + localX * cos - localY * sin;
            float packetY = screenY + localX * sin + localY * cos;
            float nearness = 0.55f - 0.45f * depth;
            quads.quadAtCenter(packetX, packetY, packetSize, packetSize, 0f, circuitColour, packetAlpha * nearness);
        }
    }
}
