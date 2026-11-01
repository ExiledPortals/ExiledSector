package exiledsector.ui.decoration;

import com.fs.starfarer.api.combat.ShipAPI.HullSize;
import org.junit.jupiter.api.Test;

import java.awt.Color;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FleetBattleTest {

    private static final float FRAME_SECONDS = 1f / 60f;
    private static final RoamVolume WEST = new RoamVolume(-6000f, 0f, 500f, 1400f);
    private static final RoamVolume EAST = new RoamVolume(6000f, 0f, 500f, 1400f);

    @Test
    void enemyFleetsMeetBetweenTheirVolumesFightAndOnlyTheWinnerFliesHome() {
        for (long seed = 1L; seed <= 20L; seed++) {
            Random random = new Random(seed);
            FactionFleet west = fleet("hegemony", WEST, random, HullSize.CRUISER, HullSize.FRIGATE, HullSize.FRIGATE, HullSize.DESTROYER);
            FactionFleet east = fleet("pirates", EAST, random, HullSize.DESTROYER, HullSize.FRIGATE, HullSize.FRIGATE);
            RoamingShip westFlagship = west.ships().get(0);
            RoamingShip eastFlagship = east.ships().get(0);
            BattleEffects effects = new BattleEffects();
            FleetBattle battle = FleetBattle.start(west, east, random);
            assertFalse(west.isFree());
            int mostEffects = 0;
            boolean metNearTheMiddle = false;
            for (int frame = 0; frame < 60 * 400 && !battle.isOver(); frame++) {
                west.advance(FRAME_SECONDS);
                east.advance(FRAME_SECONDS);
                battle.advance(FRAME_SECONDS, random, effects);
                effects.advance(FRAME_SECONDS);
                mostEffects = Math.max(mostEffects, effects.size());
                if (battle.isFighting()) {
                    metNearTheMiddle |= Math.abs(west.flight().positionX()) < 400f && Math.abs(east.flight().positionX()) < 400f;
                }
            }

            assertTrue(battle.isOver(), "seed " + seed);
            assertTrue(metNearTheMiddle, "seed " + seed);
            assertTrue(mostEffects > 0, "seed " + seed);
            FactionFleet winner = battle.winner();
            assertTrue(battle.loser().isDestroyed(), "seed " + seed);
            assertFalse(winner.isDestroyed());
            assertSame(winner == west ? westFlagship : eastFlagship, winner.ships().get(0));
            assertEquals(winner.home(), winner.flight().roamVolume());
            assertFalse(winner.flight().isAtHold());
            assertNull(winner.battle());
        }
    }

    @Test
    void theStrongerFleetUsuallyWins() {
        Random random = new Random(3L);
        int strongWins = 0;
        for (int battle = 0; battle < 400; battle++) {
            FactionFleet strong = fleet("hegemony", WEST, random, HullSize.CAPITAL_SHIP, HullSize.CRUISER, HullSize.DESTROYER);
            FactionFleet weak = fleet("pirates", EAST, random, HullSize.FRIGATE);
            if (FleetBattle.start(strong, weak, random).winner() == strong) {
                strongWins++;
            }
        }
        assertTrue(strongWins > 330 && strongWins < 400, "strong won " + strongWins);
    }

    private static FactionFleet fleet(String factionId, RoamVolume home, Random random, HullSize... hullSizes) {
        FactionFleet fleet = new FactionFleet(factionId, Color.RED, factionId, home, List.of(), 150f, random, (self, current) -> current);
        List<RoamingShip> ships = new ArrayList<>();
        for (HullSize hullSize : hullSizes) {
            FleetShipDrift.HullMotion motion = FleetShipDrift.HullMotion.of(hullSize);
            ships.add(new RoamingShip("graphics/ships/test.png", 100f, 100f, hullSize, new FleetShipDrift(motion, 20f, 0f, random),
                    HullEngines.of(FleetHullVisuals.NONE), HullModules.NONE));
        }
        fleet.addShips(ships, 40f);
        fleet.showFullySpawned();
        return fleet;
    }
}
