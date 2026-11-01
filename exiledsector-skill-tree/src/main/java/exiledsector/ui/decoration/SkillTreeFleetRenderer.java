package exiledsector.ui.decoration;

import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import com.fs.starfarer.api.util.Misc;
import exiledsector.PlayerFleet;
import exiledsector.skills.skilleffect.render.SpriteCache;
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
    private static final Random FLIGHT_RANDOM = new Random();

    private final SpriteCache spriteCache = new SpriteCache(SkillTreeFleetRenderer.class);
    private final DrawnShips drawnShips = new DrawnShips(spriteCache);
    private final List<RoamingShip> ships;
    private final FleetFlight flight;
    private final float cullRadius;
    private final RoamingShip viewedShip;
    private final FleetMemberAPI viewedMember;
    private VanillaShipView viewedShipView;
    private boolean viewedShipViewTried;

    private SkillTreeFleetRenderer(List<RoamingShip> ships, FleetFlight flight, float cullRadius, RoamingShip viewedShip, FleetMemberAPI viewedMember) {
        this.ships = ships;
        this.flight = flight;
        this.cullRadius = cullRadius;
        this.viewedShip = viewedShip;
        this.viewedMember = viewedMember;
    }

    public static SkillTreeFleetRenderer forPlayerFleet(String viewedMemberId) {
        CampaignFleetAPI playerFleet = PlayerFleet.orNull();
        if (playerFleet == null || playerFleet.getFleetData() == null) {
            return new SkillTreeFleetRenderer(List.of(), null, 0f, null, null);
        }
        RoamVolume roamVolume = TreeRoamVolumes.core(SkillTree.getDecorations().stars(), SkillTree.getDecorations().ringBelts());
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
        RoamingShip viewedShip = null;
        FleetMemberAPI viewedMember = null;
        for (FleetMemberAPI member : members) {
            RoamingShip ship = formation.ship(member.getHullSpec(), member.getVariant(), measuringCache, flight.facingDeg(), random);
            if (ship == null) {
                continue;
            }
            ships.add(ship);
            if (member.getId().equals(viewedMemberId)) {
                viewedShip = ship;
                viewedMember = member;
            }
        }
        ships.sort(FleetFormation.LARGEST_FIRST);
        float largestSprite = (float) ships.stream().mapToDouble(RoamingShip::longestSide).max().orElse(0.0);
        return new SkillTreeFleetRenderer(List.copyOf(ships), flight, fleetRadius + largestSprite, viewedShip, viewedMember);
    }

    public boolean hasShips() {
        return !ships.isEmpty();
    }

    public void advance(float amount) {
        if (ships.isEmpty()) return;
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
            if (drawnShips.add(ship, screenX, screenY, screenScale, flight.engineLevel(), alphaMult)) {
                drawHull(ship, screenX, screenY, screenScale, alphaMult);
            }
        }
        if (!drawnShips.isEmpty()) {
            drawnShips.renderFlames(zoom);
            drawnShips.renderGlows();
        }
    }

    private void drawHull(RoamingShip ship, float screenX, float screenY, float screenScale, float alpha) {
        VanillaShipView view = ship == viewedShip ? viewedShipView() : null;
        if (view != null && view.draw(screenX, screenY, screenScale, ship.drift().facingDeg(), alpha)) {
            return;
        }
        ship.drawHull(spriteCache, screenX, screenY, screenScale, alpha);
    }

    private VanillaShipView viewedShipView() {
        if (!viewedShipViewTried && viewedMember != null && viewedShip != null) {
            viewedShipViewTried = true;
            FleetHullVisuals visuals = viewedShip.engines().visuals();
            viewedShipView = VanillaShipView.of(viewedMember, null, VanillaShipView.imageOffsetX(visuals, viewedShip.width()),
                    VanillaShipView.imageOffsetY(visuals, viewedShip.height()));
        }
        return viewedShipView;
    }
}
