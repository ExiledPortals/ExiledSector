package exiledsector.ui;

import com.fs.starfarer.api.ui.PositionAPI;
import exiledsector.skills.ResearchStationNodes;
import exiledsector.skills.SkillNode;
import exiledsector.skills.decoration.Orbit;
import exiledsector.skills.skilleffect.render.Fade;
import exiledsector.skills.skilleffect.render.SpriteCache;
import exiledsector.ui.util.SpriteDraw;

import java.awt.Color;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;

final class ResearchStationView {

    static final String SPRITE_PATH = "graphics/stations/station_side00.png";
    static final float STATION_SIZE = 300f;
    static final float HIT_SHARE = 0.45f;
    static final float ORBIT_SECONDS = 60f;
    static final float SPIRAL_SECONDS = 0.9f;
    static final float SPIRAL_TURN_RADIANS = (float) (1.5 * Math.PI);
    static final float FIT_SHARE = 0.92f;
    static final float LAYOUT_MARGIN = 96f;
    static final float SETTLED = 0.001f;
    static final float VIEW_FADE_SECONDS = 0.3f;
    static final float SURROUNDING_DIM = 0.6f;
    private static final float HOVER_GLOW_ALPHA = 0.35f;
    private static final Color HOVER_GLOW = new Color(150, 210, 255);

    private final SkillNode stationNode;
    private final List<SkillNode> subsystems;
    private final Map<SkillNode, Integer> slotByNode = new IdentityHashMap<>();
    private final Orbit orbit;
    private final float ringReach;
    private final SpriteCache spriteCache = new SpriteCache(ResearchStationView.class);
    private float elapsedSeconds;
    private float expansion;
    private float viewLevel;
    private boolean active;
    private boolean hovered;
    private final float[] shiftX;
    private final float[] shiftY;
    private final float[] alpha;
    private final boolean[] interactive;
    private final ResearchStationLayer layer;

    ResearchStationView() {
        this(ResearchStationNodes.station(), ResearchStationNodes.subsystems());
    }

    ResearchStationView(SkillNode stationNode, List<SkillNode> subsystems) {
        this.stationNode = stationNode;
        this.subsystems = stationNode == null ? List.of() : subsystems;
        this.orbit = stationNode == null ? null : orbitOf(stationNode);
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

    private static Orbit orbitOf(SkillNode stationNode) {
        float radius = (float) Math.hypot(stationNode.getX(), stationNode.getY());
        float startAngle = (float) Math.toDegrees(Math.atan2(stationNode.getY(), stationNode.getX()));
        return new Orbit(radius, ORBIT_SECONDS, false, startAngle);
    }

    private static float ringReach(SkillNode stationNode, List<SkillNode> subsystems) {
        float reach = STATION_SIZE / 2f;
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

    float expansion() {
        return expansion;
    }

    boolean isSpiralling() {
        return expansion > SETTLED && expansion < 1f - SETTLED;
    }

    float focusX() {
        return orbit == null ? 0f : orbit.radius() * (float) Math.cos(Math.toRadians(orbit.angleDegAt(elapsedSeconds)));
    }

    float focusY() {
        return orbit == null ? 0f : orbit.radius() * (float) Math.sin(Math.toRadians(orbit.angleDegAt(elapsedSeconds)));
    }

    float fitZoom(PositionAPI canvasPosition) {
        float fitted = Math.min(canvasPosition.getWidth(), canvasPosition.getHeight()) / 2f * FIT_SHARE / ringReach;
        return Math.max(SmoothZoom.MIN_ZOOM, Math.min(SmoothZoom.MAX_ZOOM, fitted));
    }

    void advance(float amount, SkillNode pickedSubsystem) {
        if (!isAvailable()) {
            return;
        }
        elapsedSeconds += amount;
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
        float angleDeg = -(orbit.angleDegAt(elapsedSeconds) + 90f);
        SpriteDraw.drawAtCenter(spriteCache, SPRITE_PATH, screenX, screenY, screenSize, screenSize, Color.WHITE, alphaMult, angleDeg);
        if (hovered && !active) {
            SpriteDraw.drawAdditiveAtCenter(spriteCache, SPRITE_PATH, screenX, screenY, screenSize, screenSize, HOVER_GLOW,
                    alphaMult * HOVER_GLOW_ALPHA, angleDeg);
        }
    }
}
