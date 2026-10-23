package exiledsector.ui;

import com.fs.starfarer.api.fleet.FleetMemberAPI;
import com.fs.starfarer.api.ui.PositionAPI;
import exiledlib.skills.skilleffect.render.Fade;
import exiledsector.skills.ExoshipNodes;
import exiledsector.skills.SkillNode;
import exiledsector.skills.SkillTier;
import exiledsector.skills.SkillTree;
import exiledsector.skills.tags.SkillTags;
import exiledsector.ui.exoship.ExoshipRenderer;
import exiledsector.ui.exoship.ExoshipVoyage;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Random;

final class ExoshipView {

    static final float ROAMING_SHIP_LENGTH = 90f;
    static final float VIEWED_SHIP_LENGTH = 700f;
    static final float VIEW_FADE_SECONDS = 0.8f;
    static final float DOCK_FADE_SECONDS = 0.6f;
    static final float FIT_SHARE = 0.92f;
    static final float LAYOUT_MARGIN = 96f;
    static final float WARP_STARFIELD_DIM = 0.85f;
    private static final float AUDIBLE_LENGTHS = 12f;
    private static final float AUDIBLE_ALPHA = 0.1f;
    static final float MIN_HIT_PIXELS = 12f;

    private final SkillNode exoshipNode;
    private final float carriedReach;
    private final ExoshipVoyage voyage;
    private final ExoshipRenderer renderer = new ExoshipRenderer();
    private TreeViewport lastViewport;
    private boolean active;
    private boolean hovered;
    private boolean boardable = true;
    private float viewLevel;
    private float dockedLevel;

    ExoshipView() {
        this(ExoshipNodes.exoship(), SkillTree.topology().nonRoots(), new Random());
    }

    ExoshipView(SkillNode exoshipNode, Collection<SkillNode> nodes, Random random) {
        this.exoshipNode = exoshipNode;
        this.carriedReach = carriedReach(exoshipNode, nodes);
        this.voyage = new ExoshipVoyage(exoshipNode == null ? List.of() : orbitAnchors(nodes), random, new VoyageEvents());
    }

    static List<ExoshipVoyage.Anchor> orbitAnchors(Collection<SkillNode> nodes) {
        List<ExoshipVoyage.Anchor> anchors = new ArrayList<>();
        for (SkillNode node : nodes) {
            String region = node.getRegion();
            if (node.getType().getTier() == SkillTier.NOTABLE && !ExoshipNodes.isCarried(node)
                    && !(region != null && SkillTags.ABYSSAL_REGIONS.contains(region)) && !SkillTags.REDACTED_REGION.equals(region)) {
                anchors.add(new ExoshipVoyage.Anchor(node.getX(), node.getY(), SkillTags.CORE_REGION.equals(region)));
            }
        }
        return anchors;
    }

    private static float carriedReach(SkillNode exoshipNode, Collection<SkillNode> nodes) {
        if (exoshipNode == null) {
            return 0f;
        }
        float reach = 0f;
        for (SkillNode node : nodes) {
            if (ExoshipNodes.isCarried(node)) {
                reach = Math.max(reach, (float) Math.hypot(node.getX() - exoshipNode.getX(), node.getY() - exoshipNode.getY()));
            }
        }
        return reach + LAYOUT_MARGIN;
    }

    SkillNode exoshipNode() {
        return exoshipNode;
    }

    boolean isAvailable() {
        return exoshipNode != null && voyage.hasRoute();
    }

    boolean isBoardable() {
        return isAvailable() && boardable;
    }

    void setBoardable(boolean boardable) {
        this.boardable = boardable;
        if (!boardable) {
            active = false;
            viewLevel = 0f;
        }
    }

    boolean isActive() {
        return active;
    }

    void enter() {
        active = isBoardable();
    }

    void leave() {
        active = false;
    }

    void setPlayerShip(FleetMemberAPI member) {
        renderer.setPlayerShip(member == null ? null : member.getHullSpec());
    }

    void advance(float amount, boolean docked) {
        if (!isAvailable()) {
            return;
        }
        voyage.advance(amount);
        renderer.advance(amount, voyage, shipLength());
        viewLevel = Fade.step(viewLevel, active, amount / VIEW_FADE_SECONDS);
        dockedLevel = Fade.step(dockedLevel, docked, amount / DOCK_FADE_SECONDS);
    }

    float viewLevel() {
        return Fade.smooth(viewLevel);
    }

    float focusX() {
        return voyage.shipX();
    }

    float focusY() {
        return voyage.shipY();
    }

    float shipLength() {
        return ROAMING_SHIP_LENGTH + (VIEWED_SHIP_LENGTH - ROAMING_SHIP_LENGTH) * viewLevel();
    }

    float fitZoom(PositionAPI canvasPosition) {
        if (carriedReach <= 0f) {
            return SmoothZoom.MIN_ZOOM;
        }
        float fitted = Math.min(canvasPosition.getWidth(), canvasPosition.getHeight()) / 2f * FIT_SHARE / carriedReach;
        return Math.max(SmoothZoom.MIN_ZOOM, Math.min(SmoothZoom.MAX_ZOOM, fitted));
    }

    float starfieldAlpha() {
        return 1f - WARP_STARFIELD_DIM * voyage.warpLevel() * viewLevel();
    }

    TreeLayers layers(TreeViewport viewport, float nodeAlpha) {
        float shown = viewLevel();
        if (exoshipNode == null || shown <= 0f) {
            return new TreeLayers(viewport, viewport, nodeAlpha, 0f, false, shipLength() / 2f);
        }
        float zoom = viewport.zoom();
        float shiftX = voyage.shipX() - exoshipNode.getX();
        float shiftY = voyage.shipY() - exoshipNode.getY();
        TreeViewport carriedViewport = new TreeViewport(viewport.centerX() + shiftX * zoom, viewport.centerY() - shiftY * zoom, zoom,
                viewport.left(), viewport.bottom(), viewport.right(), viewport.top());
        return new TreeLayers(viewport, carriedViewport, nodeAlpha * (1f - shown), nodeAlpha * shown, viewLevel >= 0.5f,
                shipLength() / 2f);
    }

    void render(TreeViewport viewport, float alphaMult) {
        lastViewport = alphaMult >= AUDIBLE_ALPHA ? viewport : null;
        if (isAvailable()) {
            renderer.render(viewport, voyage, shipLength(), dockedLevel, hovered && !active, viewLevel(), alphaMult);
        }
    }

    boolean shipContains(TreeViewport viewport, float screenX, float screenY) {
        if (active || !isBoardable() || !voyage.isVisible() || lastViewport == null || viewport.zoom() <= 0f) {
            return false;
        }
        float zoom = viewport.zoom();
        float treeOffsetX = (screenX - viewport.screenX(voyage.shipX())) / zoom;
        float treeOffsetY = (viewport.screenY(voyage.shipY()) - screenY) / zoom;
        double facingRad = Math.toRadians(voyage.facingDeg());
        float cos = (float) Math.cos(facingRad);
        float sin = (float) Math.sin(facingRad);
        float along = treeOffsetX * cos + treeOffsetY * sin;
        float across = -treeOffsetX * sin + treeOffsetY * cos;
        float halfLength = Math.max(shipLength() / 2f, MIN_HIT_PIXELS / zoom);
        float halfWidth = Math.max(shipLength() / 2f * ExoshipRenderer.WIDTH_PER_LENGTH, MIN_HIT_PIXELS / zoom);
        return Math.abs(along) <= halfLength && Math.abs(across) <= halfWidth;
    }

    void setHovered(boolean hovered) {
        this.hovered = hovered;
    }

    private boolean audible() {
        TreeViewport viewport = lastViewport;
        if (!isAvailable()) {
            return false;
        }
        if (active && viewport != null) {
            return true;
        }
        return viewport != null && viewport.isVisible(viewport.screenX(voyage.shipX()), viewport.screenY(voyage.shipY()),
                shipLength() * AUDIBLE_LENGTHS * viewport.zoom());
    }

    private final class VoyageEvents implements ExoshipVoyage.Events {

        @Override
        public void chargingUp(float shipX, float shipY) {
            if (audible()) {
                SkillTreeSounds.exoshipChargingUp();
            }
        }

        @Override
        public void departed(float shipX, float shipY, float headingX, float headingY) {
            renderer.departed(shipX, shipY, headingX, headingY, shipLength());
            if (audible()) {
                SkillTreeSounds.exoshipWarped();
            }
        }

        @Override
        public void arrived(float shipX, float shipY, float headingX, float headingY) {
            renderer.arrived(shipX, shipY, headingX, headingY, shipLength());
        }
    }
}
