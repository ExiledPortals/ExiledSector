package exiledsector.ui.decoration;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.campaign.SectorAPI;
import com.fs.starfarer.api.combat.ShipAPI.HullSize;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import com.fs.starfarer.api.graphics.SpriteAPI;
import com.fs.starfarer.api.util.Misc;
import exiledsector.skills.SkillTree;
import exiledsector.ui.TreeViewport;
import exiledsector.ui.util.SpriteCache;
import exiledsector.ui.util.SpriteDraw;
import org.lwjgl.opengl.GL11;

import java.awt.Color;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Random;

public class SkillTreeFleetRenderer {

    static final int LEADING_SHIPS = 8;
    static final int TRAILING_SHIPS = 12;
    static final float DEFAULT_FLEET_RADIUS = 30f;
    static final float MIN_TRAVEL_SPEED = 60f;
    static final float MAX_TRAVEL_SPEED = 400f;
    static final float MIN_SHIP_PIXELS = 1.5f;
    static final float SOLO_FADE_SECONDS = 0.4f;
    private static final String FLAME_PATH = "graphics/fx/particleline32ln.png";
    private static final String GLOW_PATH = "graphics/fx/hit_glow.png";
    private static final float FLAME_LEAD = 3f;
    private static final float MIN_FLAME_HALF_WIDTH = 0.3f;
    private static final float GLOW_SIZE_MULT = 0.75f;
    private static final float IDLE_FLAME_LENGTH = 0.25f;
    private static final float IDLE_GLOW_GROWTH = 0.75f;
    private static final float SPRITE_ANGLE_OFFSET = 90f;
    private static final float COLOR_CHANNEL_MAX = 255f;

    private record FleetShip(String memberId, String spritePath, float width, float height, FleetShipDrift drift,
                             FleetHullVisuals visuals, ShipAnchors anchors) {
    }

    public record InspectedShip(float screenX, float screenY, float screenScale, float longestSidePixels, float facingDeg,
                                ShipAnchors anchors) {

        public float anchorScreenX(ShipAnchors.Anchor anchor) {
            double facing = Math.toRadians(facingDeg);
            return screenX + (anchor.forward() * (float) Math.cos(facing) - anchor.left() * (float) Math.sin(facing)) * screenScale;
        }

        public float anchorScreenY(ShipAnchors.Anchor anchor) {
            double facing = Math.toRadians(facingDeg);
            return screenY + (anchor.forward() * (float) Math.sin(facing) + anchor.left() * (float) Math.cos(facing)) * screenScale;
        }
    }

    private final SpriteCache spriteCache = new SpriteCache(SkillTreeFleetRenderer.class);
    private final List<FleetShip> ships;
    private final FleetFlight flight;
    private final float cullRadius;
    private final float[] shipScreenX;
    private final float[] shipScreenY;
    private final float[] shipScreenScale;
    private final float[] shipAlpha;
    private FleetShip soloShip;
    private boolean soloActive;
    private float soloLevel;

    private SkillTreeFleetRenderer(List<FleetShip> ships, FleetFlight flight, float cullRadius) {
        this.ships = ships;
        this.flight = flight;
        this.cullRadius = cullRadius;
        this.shipScreenX = new float[ships.size()];
        this.shipScreenY = new float[ships.size()];
        this.shipScreenScale = new float[ships.size()];
        this.shipAlpha = new float[ships.size()];
    }

    public static SkillTreeFleetRenderer forPlayerFleet(String viewedMemberId) {
        SectorAPI sector = Global.getSector();
        CampaignFleetAPI playerFleet = sector == null ? null : sector.getPlayerFleet();
        if (playerFleet == null || playerFleet.getFleetData() == null) {
            return new SkillTreeFleetRenderer(List.of(), null, 0f);
        }
        Random random = new Random();
        CoreVolume coreVolume = CoreVolume.of(SkillTree.getStars(), SkillTree.getRingBelts());
        float travelSpeed = Math.max(MIN_TRAVEL_SPEED, Math.min(MAX_TRAVEL_SPEED,
                Misc.getSpeedForBurnLevel(playerFleet.getFleetData().getMinBurnLevel())));
        FleetFlight flight = new FleetFlight(coreVolume, travelSpeed, random);
        float fleetRadius = playerFleet.getRadius() > 0f ? playerFleet.getRadius() : DEFAULT_FLEET_RADIUS;
        SkillTreeFleetRenderer renderer = build(shownMembers(playerFleet.getFleetData().getMembersListCopy(), viewedMemberId),
                fleetRadius, flight, random);
        renderer.soloShip = renderer.ships.stream().filter(ship -> ship.memberId().equals(viewedMemberId)).findFirst().orElse(null);
        return renderer;
    }

    static List<FleetMemberAPI> shownMembers(List<FleetMemberAPI> members, String viewedMemberId) {
        List<FleetMemberAPI> ships = members.stream().filter(member -> !member.isFighterWing() && member.getHullSpec() != null).toList();
        if (ships.size() <= LEADING_SHIPS + TRAILING_SHIPS) {
            return ships;
        }
        List<FleetMemberAPI> shown = new ArrayList<>(ships.subList(0, LEADING_SHIPS));
        shown.addAll(ships.subList(ships.size() - TRAILING_SHIPS, ships.size()));
        FleetMemberAPI viewed = ships.stream().filter(member -> Objects.equals(member.getId(), viewedMemberId)).findFirst().orElse(null);
        if (viewed != null && !shown.contains(viewed)) {
            shown.set(shown.size() - 1, viewed);
        }
        return shown;
    }

    private static SkillTreeFleetRenderer build(List<FleetMemberAPI> members, float fleetRadius, FleetFlight flight, Random random) {
        SpriteCache measuringCache = new SpriteCache(SkillTreeFleetRenderer.class);
        float largestSizeNum = 0f;
        int largestCount = 0;
        for (FleetMemberAPI member : members) {
            float sizeNum = Misc.getSizeNum(member.getHullSpec().getHullSize());
            if (sizeNum > largestSizeNum) {
                largestSizeNum = sizeNum;
                largestCount = 1;
            } else if (Float.compare(sizeNum, largestSizeNum) == 0) {
                largestCount++;
            }
        }
        List<FleetShip> ships = new ArrayList<>(members.size());
        float largestSprite = 0f;
        for (FleetMemberAPI member : members) {
            String spritePath = member.getHullSpec().getSpriteName();
            SpriteAPI texture = spritePath == null ? null : measuringCache.texture(spritePath);
            if (texture == null) {
                continue;
            }
            HullSize hullSize = member.getHullSpec().getHullSize();
            float sizeNum = Misc.getSizeNum(hullSize);
            boolean onlyLargest = Float.compare(sizeNum, largestSizeNum) == 0 && largestCount == 1;
            FleetShipDrift.HullMotion motion = FleetShipDrift.HullMotion.of(hullSize);
            float maxOffset = FleetShipDrift.maxOffset(fleetRadius, largestSizeNum, sizeNum, onlyLargest);
            FleetShipDrift drift = new FleetShipDrift(motion, maxOffset, flight.facingDeg(), random);
            FleetHullVisuals visuals = FleetHullVisuals.of(member.getHullSpec());
            ships.add(new FleetShip(member.getId(), spritePath, texture.getWidth(), texture.getHeight(), drift, visuals,
                    ShipAnchors.of(member.getHullSpec(), visuals, texture.getWidth(), texture.getHeight())));
            largestSprite = Math.max(largestSprite, Math.max(texture.getWidth(), texture.getHeight()) * motion.scaleMult());
        }
        ships.sort(Comparator.comparingDouble(ship -> -ship.width() * ship.height()));
        return new SkillTreeFleetRenderer(List.copyOf(ships), flight, fleetRadius + largestSprite);
    }

    public void setSoloActive(boolean soloActive) {
        this.soloActive = soloActive;
    }

    public boolean hasShips() {
        return !ships.isEmpty();
    }

    public float focusX() {
        return flight.positionX() + (soloShip == null ? 0f : soloShip.drift().offsetX());
    }

    public float focusY() {
        return flight.positionY() + (soloShip == null ? 0f : soloShip.drift().offsetY());
    }

    public float soloLongestSide() {
        return soloShip == null ? 0f : Math.max(soloShip.width(), soloShip.height()) * soloShip.drift().motion().scaleMult();
    }

    public InspectedShip inspectedShip(TreeViewport viewport) {
        if (soloShip == null) {
            return null;
        }
        float screenScale = soloShip.drift().motion().scaleMult() * viewport.zoom();
        return new InspectedShip(viewport.screenX(focusX()), viewport.screenY(focusY()), screenScale,
                Math.max(soloShip.width(), soloShip.height()) * screenScale, soloShip.drift().facingDeg(), soloShip.anchors());
    }

    public void advance(float amount) {
        if (ships.isEmpty()) return;
        float soloStep = amount / SOLO_FADE_SECONDS;
        soloLevel = Math.max(0f, Math.min(1f, soloLevel + (soloActive && soloShip != null ? soloStep : -soloStep)));
        flight.advance(amount);
        for (FleetShip ship : ships) {
            ship.drift().advance(amount, flight.facingDeg());
        }
    }

    public void render(TreeViewport viewport, float alphaMult) {
        if (ships.isEmpty() || alphaMult <= 0f) return;
        float zoom = viewport.zoom();
        if (!viewport.isVisible(viewport.screenX(flight.positionX()), viewport.screenY(flight.positionY()), cullRadius * zoom)) return;

        boolean anyVisible = false;
        for (int i = 0; i < ships.size(); i++) {
            FleetShip ship = ships.get(i);
            FleetShipDrift drift = ship.drift();
            float screenScale = drift.motion().scaleMult() * zoom;
            shipScreenX[i] = viewport.screenX(flight.positionX() + drift.offsetX());
            shipScreenY[i] = viewport.screenY(flight.positionY() + drift.offsetY());
            shipAlpha[i] = alphaMult * (ship == soloShip ? 1f : 1f - soloLevel);
            boolean tooSmall = Math.max(ship.width(), ship.height()) * screenScale < MIN_SHIP_PIXELS;
            shipScreenScale[i] = tooSmall || shipAlpha[i] <= 0f ? 0f : screenScale;
            if (shipScreenScale[i] > 0f) {
                anyVisible = true;
                drawHull(ship, shipScreenX[i], shipScreenY[i], screenScale, shipAlpha[i]);
            }
        }
        if (anyVisible) {
            renderFlames(zoom);
            renderGlows();
        }
    }

    private void drawHull(FleetShip ship, float screenX, float screenY, float screenScale, float alpha) {
        SpriteAPI sprite = spriteCache.sprite(ship.spritePath());
        if (sprite == null) return;
        sprite.setSize(ship.width() * screenScale, ship.height() * screenScale);
        if (ship.visuals().hasCenter()) {
            sprite.setCenter(ship.visuals().centerX() * screenScale, ship.visuals().centerY() * screenScale);
        }
        sprite.setColor(Color.WHITE);
        sprite.setAlphaMult(alpha);
        sprite.setAngle(ship.drift().facingDeg() - SPRITE_ANGLE_OFFSET);
        sprite.setNormalBlend();
        sprite.renderAtCenter(screenX, screenY);
    }

    private void renderFlames(float zoom) {
        SpriteAPI flameTexture = spriteCache.texture(FLAME_PATH);
        if (flameTexture == null) return;
        float engineLevel = flight.engineLevel();
        float lengthFactor = (IDLE_FLAME_LENGTH + engineLevel) * (0.5f + 0.5f * engineLevel);
        float textureRight = flameTexture.getTextureWidth();
        float textureTop = flameTexture.getTextureHeight();
        GL11.glEnable(GL11.GL_TEXTURE_2D);
        GL11.glEnable(GL11.GL_BLEND);
        GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE);
        flameTexture.bindTexture();
        GL11.glBegin(GL11.GL_QUADS);
        for (int i = 0; i < ships.size(); i++) {
            float screenScale = shipScreenScale[i];
            if (screenScale <= 0f) continue;
            FleetShip ship = ships.get(i);
            double facing = Math.toRadians(ship.drift().facingDeg());
            float facingCos = (float) Math.cos(facing);
            float facingSin = (float) Math.sin(facing);
            for (FleetHullVisuals.EngineFlame flame : ship.visuals().flames()) {
                float slotX = shipScreenX[i] + (flame.forwardOffset() * facingCos - flame.leftOffset() * facingSin) * screenScale;
                float slotY = shipScreenY[i] + (flame.forwardOffset() * facingSin + flame.leftOffset() * facingCos) * screenScale;
                float halfWidth = Math.max(1f, flame.width() * ship.drift().motion().scaleMult()) * zoom / 2f;
                if (halfWidth < MIN_FLAME_HALF_WIDTH) continue;
                double flameAngle = facing + Math.toRadians(flame.angleDeg());
                float directionX = (float) Math.cos(flameAngle);
                float directionY = (float) Math.sin(flameAngle);
                float length = flame.length() * screenScale * lengthFactor;
                float lead = FLAME_LEAD * zoom;
                float backX = slotX - directionX * lead;
                float backY = slotY - directionY * lead;
                float tipX = slotX + directionX * length;
                float tipY = slotY + directionY * length;
                float sideX = -directionY;
                float sideY = directionX;
                Color color = flame.color();
                float alpha = shipAlpha[i] * color.getAlpha() / COLOR_CHANNEL_MAX;
                flameVertex(color, 0f, textureRight, textureTop, backX + sideX * halfWidth / 2f, backY + sideY * halfWidth / 2f);
                flameVertex(color, 0f, textureRight, 0f, backX - sideX * halfWidth / 2f, backY - sideY * halfWidth / 2f);
                flameVertex(color, alpha, 0f, 0f, slotX - sideX * halfWidth, slotY - sideY * halfWidth);
                flameVertex(color, alpha, 0f, textureTop, slotX + sideX * halfWidth, slotY + sideY * halfWidth);
                flameVertex(color, alpha, 0f, 0f, slotX - sideX * halfWidth, slotY - sideY * halfWidth);
                flameVertex(color, alpha, 0f, textureTop, slotX + sideX * halfWidth, slotY + sideY * halfWidth);
                flameVertex(color, 0f, textureRight, textureTop, tipX + sideX * halfWidth / 2f, tipY + sideY * halfWidth / 2f);
                flameVertex(color, 0f, textureRight, 0f, tipX - sideX * halfWidth / 2f, tipY - sideY * halfWidth / 2f);
            }
        }
        GL11.glEnd();
        GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
    }

    private static void flameVertex(Color color, float alpha, float textureX, float textureY, float x, float y) {
        GL11.glColor4f(color.getRed() / COLOR_CHANNEL_MAX, color.getGreen() / COLOR_CHANNEL_MAX, color.getBlue() / COLOR_CHANNEL_MAX, alpha);
        GL11.glTexCoord2f(textureX, textureY);
        GL11.glVertex2f(x, y);
    }

    private void renderGlows() {
        float engineLevel = flight.engineLevel();
        float glowFactor = GLOW_SIZE_MULT * (1f + IDLE_GLOW_GROWTH * (1f - engineLevel)) * (0.5f + 0.5f * engineLevel);
        for (int i = 0; i < ships.size(); i++) {
            float screenScale = shipScreenScale[i];
            if (screenScale <= 0f) continue;
            FleetShip ship = ships.get(i);
            double facing = Math.toRadians(ship.drift().facingDeg());
            float facingCos = (float) Math.cos(facing);
            float facingSin = (float) Math.sin(facing);
            for (FleetHullVisuals.EngineFlame flame : ship.visuals().flames()) {
                float slotX = shipScreenX[i] + (flame.forwardOffset() * facingCos - flame.leftOffset() * facingSin) * screenScale;
                float slotY = shipScreenY[i] + (flame.forwardOffset() * facingSin + flame.leftOffset() * facingCos) * screenScale;
                float glowSize = flame.length() * screenScale * glowFactor;
                SpriteDraw.drawAdditiveAtCenter(spriteCache, GLOW_PATH, slotX, slotY, glowSize, glowSize, flame.color(), shipAlpha[i]);
            }
        }
    }
}
