package exiledsector.ui.decoration;

import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.combat.ShipAPI.HullSize;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import com.fs.starfarer.api.graphics.SpriteAPI;
import com.fs.starfarer.api.util.Misc;
import exiledsector.PlayerFleet;
import exiledsector.skills.SkillTree;
import exiledsector.ui.TreeViewport;
import exiledsector.ui.util.Fade;
import exiledsector.ui.util.SpriteCache;
import exiledsector.ui.util.TexturedQuadBatch;

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
    private static final Random FLIGHT_RANDOM = new Random();
    private record FleetShip(String memberId, String spritePath, float width, float height, FleetShipDrift drift,
                             FleetHullVisuals visuals, HullEngines engines, ShipAnchors anchors) {
    }

    public record InspectedShip(float screenX, float screenY, float screenScale, float longestSidePixels, float facingDeg,
                                ShipAnchors anchors) {

        public float anchorScreenX(ShipAnchors.Anchor anchor) {
            double facing = Math.toRadians(facingDeg);
            return screenX + HullFrame.rotatedX(anchor.forward(), anchor.left(), (float) Math.cos(facing), (float) Math.sin(facing)) * screenScale;
        }

        public float anchorScreenY(ShipAnchors.Anchor anchor) {
            double facing = Math.toRadians(facingDeg);
            return screenY + HullFrame.rotatedY(anchor.forward(), anchor.left(), (float) Math.cos(facing), (float) Math.sin(facing)) * screenScale;
        }
    }

    private final SpriteCache spriteCache = new SpriteCache(SkillTreeFleetRenderer.class);
    private final TexturedQuadBatch glowQuads = new TexturedQuadBatch();
    private final FlameBatch flameBatch = new FlameBatch();
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
        CampaignFleetAPI playerFleet = PlayerFleet.orNull();
        if (playerFleet == null || playerFleet.getFleetData() == null) {
            return new SkillTreeFleetRenderer(List.of(), null, 0f);
        }
        CoreVolume coreVolume = CoreVolume.of(SkillTree.getStars(), SkillTree.getRingBelts());
        float travelSpeed = Math.max(MIN_TRAVEL_SPEED, Math.min(MAX_TRAVEL_SPEED,
                Misc.getSpeedForBurnLevel(playerFleet.getFleetData().getMinBurnLevel())));
        FleetFlight flight = new FleetFlight(coreVolume, travelSpeed, FLIGHT_RANDOM);
        float fleetRadius = playerFleet.getRadius() > 0f ? playerFleet.getRadius() : DEFAULT_FLEET_RADIUS;
        SkillTreeFleetRenderer renderer = build(shownMembers(playerFleet.getFleetData().getMembersListCopy(), viewedMemberId),
                fleetRadius, flight, FLIGHT_RANDOM);
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
            ships.add(new FleetShip(member.getId(), spritePath, texture.getWidth(), texture.getHeight(), drift, visuals, HullEngines.of(visuals),
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
        soloLevel = Fade.step(soloLevel, soloActive && soloShip != null, soloStep);
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
        ship.engines().drawHull(spriteCache, ship.spritePath(), screenX, screenY, ship.width() * screenScale, ship.height() * screenScale,
                screenScale, ship.drift().facingDeg(), alpha);
    }

    private void renderFlames(float zoom) {
        SpriteAPI flameTexture = spriteCache.texture(HullEngines.FLAME_PATH);
        if (flameTexture == null) return;
        float lengthFactor = HullEngines.flameLengthFactor(flight.engineLevel());
        try (FlameBatch flames = flameBatch.begin(flameTexture)) {
            for (int i = 0; i < ships.size(); i++) {
                if (shipScreenScale[i] > 0f) {
                    FleetShip ship = ships.get(i);
                    ship.engines().addFlames(flames, shipScreenX[i], shipScreenY[i], Math.toRadians(ship.drift().facingDeg()), shipScreenScale[i],
                            zoom, lengthFactor, shipAlpha[i]);
                }
            }
        }
    }

    private void renderGlows() {
        SpriteAPI glowTexture = spriteCache.texture(HullEngines.GLOW_PATH);
        if (glowTexture == null) return;
        float glowFactor = HullEngines.glowFactor(flight.engineLevel());
        try (TexturedQuadBatch quads = glowQuads.beginAdditive(glowTexture)) {
            for (int i = 0; i < ships.size(); i++) {
                if (shipScreenScale[i] > 0f) {
                    FleetShip ship = ships.get(i);
                    ship.engines().addGlows(quads, shipScreenX[i], shipScreenY[i], Math.toRadians(ship.drift().facingDeg()), shipScreenScale[i],
                            glowFactor, shipAlpha[i]);
                }
            }
        }
    }
}
