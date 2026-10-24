package exiledsector.ui.decoration;

import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import com.fs.starfarer.api.util.Misc;
import exiledlib.PlayerFleet;
import exiledlib.skills.skilleffect.render.Fade;
import exiledlib.skills.skilleffect.render.SpriteCache;
import exiledsector.skills.SkillTree;
import exiledsector.ui.TreeViewport;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Random;

public class SkillTreeFleetRenderer {

    static final int LEADING_SHIPS = 8;
    static final int TRAILING_SHIPS = 12;
    static final float DEFAULT_FLEET_RADIUS = 30f;
    static final float MIN_TRAVEL_SPEED = 60f;
    static final float MAX_TRAVEL_SPEED = 400f;
    static final float SOLO_FADE_SECONDS = 0.4f;
    private static final Random FLIGHT_RANDOM = new Random();

    private final SpriteCache spriteCache = new SpriteCache(SkillTreeFleetRenderer.class);
    private final DrawnShips drawnShips = new DrawnShips(spriteCache);
    private final List<RoamingShip> ships;
    private final FleetFlight flight;
    private final float cullRadius;
    private final RoamingShip soloShip;
    private final FleetMemberAPI soloMember;
    private VanillaShipView soloView;
    private boolean soloViewTried;
    private boolean soloActive;
    private float soloLevel;

    private SkillTreeFleetRenderer(List<RoamingShip> ships, FleetFlight flight, float cullRadius, RoamingShip soloShip, FleetMemberAPI soloMember) {
        this.ships = ships;
        this.flight = flight;
        this.cullRadius = cullRadius;
        this.soloShip = soloShip;
        this.soloMember = soloMember;
    }

    public static SkillTreeFleetRenderer forPlayerFleet(String viewedMemberId) {
        CampaignFleetAPI playerFleet = PlayerFleet.orNull();
        if (playerFleet == null || playerFleet.getFleetData() == null) {
            return new SkillTreeFleetRenderer(List.of(), null, 0f, null, null);
        }
        RoamVolume roamVolume = RoamVolume.core(SkillTree.getStars(), SkillTree.getRingBelts());
        float travelSpeed = Math.max(MIN_TRAVEL_SPEED, Math.min(MAX_TRAVEL_SPEED,
                Misc.getSpeedForBurnLevel(playerFleet.getFleetData().getMinBurnLevel())));
        FleetFlight flight = new FleetFlight(roamVolume, travelSpeed, FLIGHT_RANDOM);
        float fleetRadius = playerFleet.getRadius() > 0f ? playerFleet.getRadius() : DEFAULT_FLEET_RADIUS;
        return build(shownMembers(playerFleet.getFleetData().getMembersListCopy(), viewedMemberId), viewedMemberId, fleetRadius, flight,
                FLIGHT_RANDOM);
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

    private static SkillTreeFleetRenderer build(List<FleetMemberAPI> members, String viewedMemberId, float fleetRadius, FleetFlight flight,
                                                Random random) {
        SpriteCache measuringCache = new SpriteCache(SkillTreeFleetRenderer.class);
        FleetFormation formation = FleetFormation.of(members.stream().map(FleetMemberAPI::getHullSpec).toList(), fleetRadius);
        List<RoamingShip> ships = new ArrayList<>(members.size());
        RoamingShip soloShip = null;
        FleetMemberAPI soloMember = null;
        for (FleetMemberAPI member : members) {
            RoamingShip ship = formation.ship(member.getHullSpec(), measuringCache, flight.facingDeg(), random);
            if (ship == null) {
                continue;
            }
            ships.add(ship);
            if (member.getId().equals(viewedMemberId)) {
                soloShip = ship;
                soloMember = member;
            }
        }
        ships.sort(FleetFormation.LARGEST_FIRST);
        float largestSprite = (float) ships.stream().mapToDouble(RoamingShip::longestSide).max().orElse(0.0);
        return new SkillTreeFleetRenderer(List.copyOf(ships), flight, fleetRadius + largestSprite, soloShip, soloMember);
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
        return soloShip == null ? 0f : soloShip.longestSide();
    }

    public void advance(float amount) {
        if (ships.isEmpty()) return;
        float soloStep = amount / SOLO_FADE_SECONDS;
        soloLevel = Fade.step(soloLevel, soloActive && soloShip != null, soloStep);
        flight.advance(amount);
        for (RoamingShip ship : ships) {
            ship.drift().advance(amount, flight.facingDeg());
        }
    }

    public void render(TreeViewport viewport, float alphaMult) {
        if (ships.isEmpty() || alphaMult <= 0f) return;
        float zoom = viewport.zoom();
        if (!viewport.isVisible(viewport.screenX(flight.positionX()), viewport.screenY(flight.positionY()), cullRadius * zoom)) return;

        drawnShips.begin(ships.size());
        for (RoamingShip ship : ships) {
            FleetShipDrift drift = ship.drift();
            float screenScale = drift.motion().scaleMult() * zoom;
            float screenX = viewport.screenX(flight.positionX() + drift.offsetX());
            float screenY = viewport.screenY(flight.positionY() + drift.offsetY());
            float alpha = alphaMult * (ship == soloShip ? 1f : 1f - soloLevel);
            if (drawnShips.add(ship, screenX, screenY, screenScale, flight.engineLevel(), alpha)) {
                drawHull(ship, screenX, screenY, screenScale, alpha);
            }
        }
        if (!drawnShips.isEmpty()) {
            drawnShips.renderFlames(zoom);
            drawnShips.renderGlows();
        }
    }

    private void drawHull(RoamingShip ship, float screenX, float screenY, float screenScale, float alpha) {
        VanillaShipView view = ship == soloShip ? soloView() : null;
        if (view != null && view.draw(screenX, screenY, screenScale, ship.drift().facingDeg(), alpha)) {
            return;
        }
        ship.drawHull(spriteCache, screenX, screenY, screenScale, alpha);
    }

    private VanillaShipView soloView() {
        if (!soloViewTried && soloMember != null && soloShip != null) {
            soloViewTried = true;
            FleetHullVisuals visuals = soloShip.engines().visuals();
            soloView = VanillaShipView.of(soloMember, null, VanillaShipView.imageOffsetX(visuals, soloShip.width()),
                    VanillaShipView.imageOffsetY(visuals, soloShip.height()));
        }
        return soloView;
    }
}
