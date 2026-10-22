package exiledsector.ui.decoration;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.FactionAPI;
import com.fs.starfarer.api.campaign.RepLevel;
import com.fs.starfarer.api.combat.ShipAPI.HullSize;
import com.fs.starfarer.api.combat.ShipHullSpecAPI;
import com.fs.starfarer.api.combat.ShipHullSpecAPI.ShipTypeHints;
import com.fs.starfarer.api.graphics.SpriteAPI;
import com.fs.starfarer.api.util.Misc;
import exiledsector.compat.NexerelinCompat;
import exiledsector.skills.SkillNode;
import exiledsector.skills.SkillTree;
import exiledsector.skills.npc.NpcFactionVolumes;
import exiledsector.skills.tags.SkillTags;
import exiledsector.ui.TreeViewport;
import exiledsector.ui.hyperspace.HyperspaceAnchor;
import exiledlib.ui.util.SpriteCache;
import exiledlib.ui.util.TexturedQuadBatch;
import org.apache.log4j.Logger;

import java.awt.Color;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.function.Predicate;

public final class FactionFleetRenderer {

    static final int MIN_FLEETS_PER_VOLUME = 2;
    static final int MAX_FLEETS_PER_VOLUME = 3;
    static final int MIN_SHIPS_PER_FLEET = 3;
    static final int MAX_SHIPS_PER_FLEET = 5;
    static final float FLEET_RADIUS = 40f;
    static final float MIN_TRAVEL_SPEED = 90f;
    static final float MAX_TRAVEL_SPEED = 180f;
    static final float VOLUME_TRAVEL_CHANCE = 0.15f;
    static final float ENCOUNTER_CHANCE = 0.15f;
    static final int MAX_ACTIVE_BATTLES = 3;
    static final float MIN_RESPAWN_SECONDS = 30f;
    static final float MAX_RESPAWN_SECONDS = 60f;
    static final RepLevel ALLY_LEVEL = RepLevel.COOPERATIVE;
    private static final Logger LOG = Logger.getLogger(FactionFleetRenderer.class);

    private static final class Respawn {
        private final String region;
        private float secondsLeft;

        private Respawn(String region, float secondsLeft) {
            this.region = region;
            this.secondsLeft = secondsLeft;
        }
    }

    private final SpriteCache spriteCache = new SpriteCache(FactionFleetRenderer.class);
    private final TexturedQuadBatch glowQuads = new TexturedQuadBatch();
    private final FlameBatch flameBatch = new FlameBatch();
    private final Random random = new Random();
    private final Map<String, RoamVolume> volumesByRegion = new LinkedHashMap<>();
    private final Map<String, List<ShipHullSpecAPI>> hullsByFaction = new HashMap<>();
    private final List<FactionFleet> fleets = new ArrayList<>();
    private final List<FleetBattle> battles = new ArrayList<>();
    private final List<Respawn> respawns = new ArrayList<>();
    private final BattleEffects effects = new BattleEffects();
    private RoamingShip[] drawnShips = new RoamingShip[0];
    private float[] drawnEngineLevel = new float[0];
    private float[] drawnAlpha = new float[0];
    private float[] drawnScreenX = new float[0];
    private float[] drawnScreenY = new float[0];
    private float[] drawnScreenScale = new float[0];

    private FactionFleetRenderer() {
    }

    public static FactionFleetRenderer forActiveTree(Predicate<SkillNode> nodeHidden) {
        FactionFleetRenderer renderer = new FactionFleetRenderer();
        if (Global.getSector() == null) {
            return renderer;
        }
        Collection<SkillNode> nodes = SkillTree.getAllNodes().values();
        Set<String> lockedRegions = HyperspaceAnchor.lockedRegions(nodes, SkillNode::getRegion, nodeHidden);
        for (String region : SkillTags.FACTION_VOLUMES) {
            RoamVolume volume = lockedRegions.contains(region) ? null : RoamVolume.region(region, SkillTree.getStars(), nodes);
            if (volume != null) {
                renderer.volumesByRegion.put(region, volume);
            }
        }
        for (String region : renderer.volumesByRegion.keySet()) {
            int fleetCount = MIN_FLEETS_PER_VOLUME + renderer.random.nextInt(MAX_FLEETS_PER_VOLUME - MIN_FLEETS_PER_VOLUME + 1);
            for (int i = 0; i < fleetCount; i++) {
                FactionFleet fleet = renderer.spawn(region);
                if (fleet != null) {
                    fleet.showFullySpawned();
                }
            }
        }
        return renderer;
    }

    static boolean roams(ShipHullSpecAPI hullSpec) {
        if (hullSpec == null || hullSpec.getSpriteName() == null || hullSpec.isCivilianNonCarrier()) {
            return false;
        }
        HullSize hullSize = hullSpec.getHullSize();
        if (hullSize != HullSize.FRIGATE && hullSize != HullSize.DESTROYER && hullSize != HullSize.CRUISER && hullSize != HullSize.CAPITAL_SHIP) {
            return false;
        }
        Set<ShipTypeHints> hints = hullSpec.getHints();
        return hints == null || !(hints.contains(ShipTypeHints.STATION) || hints.contains(ShipTypeHints.MODULE)
                || hints.contains(ShipTypeHints.HIDE_IN_CODEX));
    }

    static List<ShipHullSpecAPI> pickHulls(List<ShipHullSpecAPI> hulls, Random random) {
        List<ShipHullSpecAPI> flagships = hulls.stream().filter(hull -> hull.getHullSize() != HullSize.FRIGATE).toList();
        List<ShipHullSpecAPI> escorts = hulls.stream()
                .filter(hull -> hull.getHullSize() == HullSize.FRIGATE || hull.getHullSize() == HullSize.DESTROYER).toList();
        int shipCount = MIN_SHIPS_PER_FLEET + random.nextInt(MAX_SHIPS_PER_FLEET - MIN_SHIPS_PER_FLEET + 1);
        List<ShipHullSpecAPI> picked = new ArrayList<>(shipCount);
        picked.add(randomHull(flagships.isEmpty() ? hulls : flagships, random));
        for (int i = 1; i < shipCount; i++) {
            picked.add(randomHull(escorts.isEmpty() ? hulls : escorts, random));
        }
        return picked;
    }

    static RoamVolume nextVolume(RoamVolume current, RoamVolume home, List<RoamVolume> alliedVolumes, Random random) {
        if (alliedVolumes.isEmpty() || random.nextFloat() >= VOLUME_TRAVEL_CHANCE) {
            return current;
        }
        List<RoamVolume> destinations = new ArrayList<>(alliedVolumes);
        destinations.add(home);
        destinations.remove(current);
        return destinations.get(random.nextInt(destinations.size()));
    }

    private static ShipHullSpecAPI randomHull(List<ShipHullSpecAPI> hulls, Random random) {
        return hulls.get(random.nextInt(hulls.size()));
    }

    private static List<ShipHullSpecAPI> roamingHulls(String factionId) {
        FactionAPI faction = Global.getSector().getFaction(factionId);
        if (faction == null || faction.getKnownShips() == null) {
            return List.of();
        }
        List<ShipHullSpecAPI> hulls = new ArrayList<>();
        for (String hullId : faction.getKnownShips()) {
            try {
                ShipHullSpecAPI hullSpec = Global.getSettings().getHullSpec(hullId);
                if (roams(hullSpec)) {
                    hulls.add(hullSpec);
                }
            } catch (RuntimeException e) {
                LOG.debug("Faction " + factionId + " knows hull " + hullId + " but it has no spec: " + e.getMessage());
            }
        }
        hulls.sort(Comparator.comparing(ShipHullSpecAPI::getHullId));
        return hulls;
    }

    private static boolean allied(String factionId, String otherFactionId) {
        if (factionId.equals(otherFactionId)) {
            return false;
        }
        FactionAPI faction = Global.getSector().getFaction(factionId);
        return (faction != null && faction.isAtWorst(otherFactionId, ALLY_LEVEL)) || NexerelinCompat.areAllied(factionId, otherFactionId);
    }

    private static boolean hostile(String factionId, String otherFactionId) {
        FactionAPI faction = Global.getSector().getFaction(factionId);
        return faction != null && faction.isHostileTo(otherFactionId);
    }

    private FactionFleet spawn(String region) {
        RoamVolume home = volumesByRegion.get(region);
        List<String> factionIds = NpcFactionVolumes.factionsIn(region).stream()
                .filter(factionId -> !hullsByFaction.computeIfAbsent(factionId, FactionFleetRenderer::roamingHulls).isEmpty()).toList();
        if (home == null || factionIds.isEmpty()) {
            return null;
        }
        String factionId = factionIds.get(random.nextInt(factionIds.size()));
        FactionAPI faction = Global.getSector().getFaction(factionId);
        Color factionColor = faction.getColor() == null ? Color.WHITE : faction.getColor();
        float travelSpeed = MIN_TRAVEL_SPEED + random.nextFloat() * (MAX_TRAVEL_SPEED - MIN_TRAVEL_SPEED);
        FactionFleet fleet = new FactionFleet(factionId, factionColor, region, home, alliedVolumes(factionId, region), travelSpeed, random,
                this::arrive);
        List<RoamingShip> ships = buildShips(pickHulls(hullsByFaction.get(factionId), random), fleet.flight().facingDeg());
        if (ships.isEmpty()) {
            return null;
        }
        fleet.addShips(ships, FLEET_RADIUS);
        fleets.add(fleet);
        return fleet;
    }

    private List<RoamVolume> alliedVolumes(String factionId, String homeRegion) {
        return volumesByRegion.entrySet().stream()
                .filter(entry -> !entry.getKey().equals(homeRegion)
                        && NpcFactionVolumes.factionsIn(entry.getKey()).stream().anyMatch(other -> allied(factionId, other)))
                .map(Map.Entry::getValue).toList();
    }

    private List<RoamingShip> buildShips(List<ShipHullSpecAPI> hulls, float startFacingDeg) {
        float largestSizeNum = 0f;
        int largestCount = 0;
        for (ShipHullSpecAPI hull : hulls) {
            float sizeNum = Misc.getSizeNum(hull.getHullSize());
            if (sizeNum > largestSizeNum) {
                largestSizeNum = sizeNum;
                largestCount = 1;
            } else if (Float.compare(sizeNum, largestSizeNum) == 0) {
                largestCount++;
            }
        }
        List<RoamingShip> ships = new ArrayList<>(hulls.size());
        for (ShipHullSpecAPI hull : hulls) {
            SpriteAPI texture = spriteCache.texture(hull.getSpriteName());
            if (texture == null) {
                continue;
            }
            float sizeNum = Misc.getSizeNum(hull.getHullSize());
            boolean onlyLargest = Float.compare(sizeNum, largestSizeNum) == 0 && largestCount == 1;
            FleetShipDrift.HullMotion motion = FleetShipDrift.HullMotion.of(hull.getHullSize());
            float maxOffset = FleetShipDrift.maxOffset(FLEET_RADIUS, largestSizeNum, sizeNum, onlyLargest);
            FleetShipDrift drift = new FleetShipDrift(motion, maxOffset, startFacingDeg, random);
            ships.add(new RoamingShip(hull.getSpriteName(), texture.getWidth(), texture.getHeight(), hull.getHullSize(), drift,
                    HullEngines.of(FleetHullVisuals.of(hull))));
        }
        ships.sort(Comparator.comparingDouble(ship -> -ship.width() * ship.height()));
        return ships;
    }

    private RoamVolume arrive(FactionFleet fleet, RoamVolume current) {
        if (random.nextFloat() < ENCOUNTER_CHANCE && startBattle(fleet)) {
            return current;
        }
        return nextVolume(current, fleet.home(), fleet.alliedVolumes(), random);
    }

    private boolean startBattle(FactionFleet fleet) {
        if (battles.size() >= MAX_ACTIVE_BATTLES || !fleet.isFree()) {
            return false;
        }
        RoamVolume fleetVolume = fleet.flight().roamVolume();
        List<FactionFleet> enemies = fleets.stream()
                .filter(other -> other != fleet && other.isFree() && other.flight().roamVolume() != fleetVolume
                        && hostile(fleet.factionId(), other.factionId()))
                .toList();
        if (enemies.isEmpty()) {
            return false;
        }
        battles.add(FleetBattle.start(fleet, enemies.get(random.nextInt(enemies.size())), random));
        return true;
    }

    public void advance(float amount) {
        for (FactionFleet fleet : fleets) {
            fleet.advance(amount);
        }
        Iterator<FleetBattle> battleIterator = battles.iterator();
        while (battleIterator.hasNext()) {
            FleetBattle battle = battleIterator.next();
            battle.advance(amount, random, effects);
            if (battle.isOver()) {
                battleIterator.remove();
            }
        }
        Iterator<FactionFleet> fleetIterator = fleets.iterator();
        while (fleetIterator.hasNext()) {
            FactionFleet fleet = fleetIterator.next();
            if (fleet.isDestroyed()) {
                fleetIterator.remove();
                respawns.add(new Respawn(fleet.homeRegion(), MIN_RESPAWN_SECONDS + random.nextFloat() * (MAX_RESPAWN_SECONDS - MIN_RESPAWN_SECONDS)));
            }
        }
        effects.advance(amount);
        for (int index = respawns.size() - 1; index >= 0; index--) {
            Respawn respawn = respawns.get(index);
            respawn.secondsLeft -= amount;
            if (respawn.secondsLeft <= 0f) {
                respawns.remove(index);
                spawn(respawn.region);
            }
        }
    }

    public void render(TreeViewport viewport, float alphaMult) {
        if ((fleets.isEmpty() && effects.isEmpty()) || alphaMult <= 0f) return;
        ensureDrawCapacity();
        float zoom = viewport.zoom();
        int drawnCount = 0;
        for (FactionFleet fleet : fleets) {
            FleetFlight flight = fleet.flight();
            float fleetAlpha = alphaMult * fleet.spawnLevel();
            if (fleetAlpha <= 0f
                    || !viewport.isVisible(viewport.screenX(flight.positionX()), viewport.screenY(flight.positionY()), fleet.cullRadius() * zoom)) {
                continue;
            }
            for (RoamingShip ship : fleet.ships()) {
                FleetShipDrift drift = ship.drift();
                float screenScale = drift.motion().scaleMult() * zoom;
                if (Math.max(ship.width(), ship.height()) * screenScale < SkillTreeFleetRenderer.MIN_SHIP_PIXELS) {
                    continue;
                }
                float screenX = viewport.screenX(fleet.shipX(ship));
                float screenY = viewport.screenY(fleet.shipY(ship));
                ship.engines().drawHull(spriteCache, ship.spritePath(), screenX, screenY, ship.width() * screenScale,
                        ship.height() * screenScale, screenScale, drift.facingDeg(), fleetAlpha);
                drawnShips[drawnCount] = ship;
                drawnEngineLevel[drawnCount] = flight.engineLevel();
                drawnAlpha[drawnCount] = fleetAlpha;
                drawnScreenX[drawnCount] = screenX;
                drawnScreenY[drawnCount] = screenY;
                drawnScreenScale[drawnCount] = screenScale;
                drawnCount++;
            }
        }
        if (drawnCount > 0) {
            renderFlames(drawnCount, zoom);
        }
        renderGlows(drawnCount, viewport, alphaMult);
    }

    private void ensureDrawCapacity() {
        int shipCount = 0;
        for (FactionFleet fleet : fleets) {
            shipCount += fleet.ships().size();
        }
        if (shipCount > drawnShips.length) {
            drawnShips = new RoamingShip[shipCount];
            drawnEngineLevel = new float[shipCount];
            drawnAlpha = new float[shipCount];
            drawnScreenX = new float[shipCount];
            drawnScreenY = new float[shipCount];
            drawnScreenScale = new float[shipCount];
        }
    }

    private void renderFlames(int drawnCount, float zoom) {
        SpriteAPI flameTexture = spriteCache.texture(HullEngines.FLAME_PATH);
        if (flameTexture == null) return;
        try (FlameBatch flames = flameBatch.begin(flameTexture)) {
            for (int i = 0; i < drawnCount; i++) {
                RoamingShip ship = drawnShips[i];
                ship.engines().addFlames(flames, drawnScreenX[i], drawnScreenY[i], Math.toRadians(ship.drift().facingDeg()),
                        drawnScreenScale[i], zoom, HullEngines.flameLengthFactor(drawnEngineLevel[i]), drawnAlpha[i]);
            }
        }
    }

    private void renderGlows(int drawnCount, TreeViewport viewport, float alphaMult) {
        if (drawnCount == 0 && effects.isEmpty()) return;
        SpriteAPI glowTexture = spriteCache.texture(HullEngines.GLOW_PATH);
        if (glowTexture == null) return;
        try (TexturedQuadBatch quads = glowQuads.beginAdditive(glowTexture)) {
            for (int i = 0; i < drawnCount; i++) {
                RoamingShip ship = drawnShips[i];
                ship.engines().addGlows(quads, drawnScreenX[i], drawnScreenY[i], Math.toRadians(ship.drift().facingDeg()),
                        drawnScreenScale[i], HullEngines.glowFactor(drawnEngineLevel[i]), drawnAlpha[i]);
            }
            effects.render(quads, viewport, alphaMult);
        }
    }
}
