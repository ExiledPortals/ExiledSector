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
import java.util.Random;

public class SkillTreeFleetRenderer {

    static final int LEADING_SHIPS = 8;
    static final int TRAILING_SHIPS = 12;
    static final float DEFAULT_FLEET_RADIUS = 30f;
    static final float MIN_TRAVEL_SPEED = 60f;
    static final float MAX_TRAVEL_SPEED = 400f;
    static final float MIN_SHIP_PIXELS = 1.5f;
    private static final String FLAME_PATH = "graphics/fx/particleline32ln.png";
    private static final String GLOW_PATH = "graphics/fx/hit_glow.png";
    private static final float FLAME_LEAD = 3f;
    private static final float MIN_FLAME_HALF_WIDTH = 0.3f;
    private static final float GLOW_SIZE_MULT = 0.75f;
    private static final float IDLE_FLAME_LENGTH = 0.25f;
    private static final float IDLE_GLOW_GROWTH = 0.75f;
    private static final float SPRITE_ANGLE_OFFSET = 90f;
    private static final float COLOR_CHANNEL_MAX = 255f;

    private record FleetShip(String spritePath, float width, float height, FleetShipDrift drift,
                             List<FleetEngineSlots.EngineFlame> flames) {
    }

    private final SpriteCache spriteCache = new SpriteCache(SkillTreeFleetRenderer.class);
    private final List<FleetShip> ships;
    private final FleetFlight flight;
    private final float cullRadius;
    private final float[] shipScreenX;
    private final float[] shipScreenY;
    private final float[] shipScreenScale;

    private SkillTreeFleetRenderer(List<FleetShip> ships, FleetFlight flight, float cullRadius) {
        this.ships = ships;
        this.flight = flight;
        this.cullRadius = cullRadius;
        this.shipScreenX = new float[ships.size()];
        this.shipScreenY = new float[ships.size()];
        this.shipScreenScale = new float[ships.size()];
    }

    public static SkillTreeFleetRenderer forPlayerFleet() {
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
        return build(shownMembers(playerFleet.getFleetData().getMembersListCopy()), fleetRadius, flight, random);
    }

    static List<FleetMemberAPI> shownMembers(List<FleetMemberAPI> members) {
        List<FleetMemberAPI> ships = members.stream().filter(member -> !member.isFighterWing()).toList();
        if (ships.size() <= LEADING_SHIPS + TRAILING_SHIPS) {
            return ships;
        }
        List<FleetMemberAPI> shown = new ArrayList<>(ships.subList(0, LEADING_SHIPS));
        shown.addAll(ships.subList(ships.size() - TRAILING_SHIPS, ships.size()));
        return shown;
    }

    private static SkillTreeFleetRenderer build(List<FleetMemberAPI> members, float fleetRadius, FleetFlight flight, Random random) {
        SpriteCache measuringCache = new SpriteCache(SkillTreeFleetRenderer.class);
        float largestSizeNum = 0f;
        int largestCount = 0;
        for (FleetMemberAPI member : members) {
            float sizeNum = Misc.getSizeNum(hullSize(member));
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
            HullSize hullSize = hullSize(member);
            float sizeNum = Misc.getSizeNum(hullSize);
            boolean onlyLargest = Float.compare(sizeNum, largestSizeNum) == 0 && largestCount == 1;
            FleetShipDrift.HullMotion motion = FleetShipDrift.HullMotion.of(hullSize);
            float maxOffset = FleetShipDrift.maxOffset(fleetRadius, largestSizeNum, sizeNum, onlyLargest);
            FleetShipDrift drift = new FleetShipDrift(motion, maxOffset, flight.facingDeg(), random);
            ships.add(new FleetShip(spritePath, texture.getWidth(), texture.getHeight(), drift, FleetEngineSlots.of(member.getHullSpec())));
            largestSprite = Math.max(largestSprite, Math.max(texture.getWidth(), texture.getHeight()) * motion.scaleMult());
        }
        ships.sort(Comparator.comparingDouble(ship -> -ship.width() * ship.height()));
        return new SkillTreeFleetRenderer(List.copyOf(ships), flight, fleetRadius + largestSprite);
    }

    private static HullSize hullSize(FleetMemberAPI member) {
        return member.getHullSpec() == null ? HullSize.DEFAULT : member.getHullSpec().getHullSize();
    }

    public void advance(float amount) {
        if (ships.isEmpty()) return;
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
            shipScreenScale[i] = Math.max(ship.width(), ship.height()) * screenScale < MIN_SHIP_PIXELS ? 0f : screenScale;
            if (shipScreenScale[i] > 0f) {
                anyVisible = true;
                SpriteDraw.drawAtCenter(spriteCache, ship.spritePath(), shipScreenX[i], shipScreenY[i], ship.width() * screenScale,
                        ship.height() * screenScale, Color.WHITE, alphaMult, drift.facingDeg() - SPRITE_ANGLE_OFFSET);
            }
        }
        if (anyVisible) {
            renderFlames(zoom, alphaMult);
            renderGlows(alphaMult);
        }
    }

    private void renderFlames(float zoom, float alphaMult) {
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
            double facing = Math.toRadians(ships.get(i).drift().facingDeg());
            float facingCos = (float) Math.cos(facing);
            float facingSin = (float) Math.sin(facing);
            for (FleetEngineSlots.EngineFlame flame : ships.get(i).flames()) {
                float slotX = shipScreenX[i] + (flame.forwardOffset() * facingCos - flame.leftOffset() * facingSin) * screenScale;
                float slotY = shipScreenY[i] + (flame.forwardOffset() * facingSin + flame.leftOffset() * facingCos) * screenScale;
                float halfWidth = Math.max(1f, flame.width() * ships.get(i).drift().motion().scaleMult()) * zoom / 2f;
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
                float alpha = alphaMult * color.getAlpha() / COLOR_CHANNEL_MAX;
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

    private void renderGlows(float alphaMult) {
        float engineLevel = flight.engineLevel();
        float glowFactor = GLOW_SIZE_MULT * (1f + IDLE_GLOW_GROWTH * (1f - engineLevel)) * (0.5f + 0.5f * engineLevel);
        for (int i = 0; i < ships.size(); i++) {
            float screenScale = shipScreenScale[i];
            if (screenScale <= 0f) continue;
            double facing = Math.toRadians(ships.get(i).drift().facingDeg());
            float facingCos = (float) Math.cos(facing);
            float facingSin = (float) Math.sin(facing);
            for (FleetEngineSlots.EngineFlame flame : ships.get(i).flames()) {
                float slotX = shipScreenX[i] + (flame.forwardOffset() * facingCos - flame.leftOffset() * facingSin) * screenScale;
                float slotY = shipScreenY[i] + (flame.forwardOffset() * facingSin + flame.leftOffset() * facingCos) * screenScale;
                float glowSize = flame.length() * screenScale * glowFactor;
                SpriteDraw.drawAdditiveAtCenter(spriteCache, GLOW_PATH, slotX, slotY, glowSize, glowSize, flame.color(), alphaMult);
            }
        }
    }
}
