package exiledsector.socketables;

import com.fs.starfarer.api.combat.ShipAPI.HullSize;
import org.junit.jupiter.api.Test;

import java.util.EnumMap;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class HullFrameworkRollerTest {

    @Test
    void socketCountsSplitSeventyTwentyTen() {
        Random random = new Random(1L);
        Map<Integer, Integer> counts = new HashMap<>();
        int trials = 100_000;
        for (int i = 0; i < trials; i++) {
            counts.merge(HullFrameworkRoller.rollSocketCount(random), 1, Integer::sum);
        }

        assertEquals(Set.of(2, 3, 4), counts.keySet());
        assertEquals(0.7, counts.get(2) / (double) trials, 0.01);
        assertEquals(0.2, counts.get(3) / (double) trials, 0.01);
        assertEquals(0.1, counts.get(4) / (double) trials, 0.01);
    }

    @Test
    void twoSocketsAreCommonAndMoreAreRare() {
        assertEquals(SocketableRarity.COMMON, HullFrameworkRoller.rarityFor(2));
        assertEquals(SocketableRarity.RARE, HullFrameworkRoller.rarityFor(3));
        assertEquals(SocketableRarity.RARE, HullFrameworkRoller.rarityFor(4));
    }

    @Test
    void rolledFrameworksNeverRepeatATypeAndCanRollEveryTypeOnEveryHullSize() {
        for (HullSize hullSize : HullFrameworkRoller.HULL_SIZES) {
            Random random = new Random(hullSize.ordinal());
            Set<SocketType> seenTypes = EnumSet.noneOf(SocketType.class);
            for (int i = 0; i < 2000; i++) {
                HullFrameworkData framework = HullFrameworkRoller.roll(hullSize, random);
                assertEquals(hullSize, framework.hullSize());
                assertEquals(framework.socketTypes().size(), new HashSet<>(framework.socketTypes()).size());
                assertEquals(HullFrameworkRoller.rarityFor(framework.socketTypes().size()), framework.rarity());
                seenTypes.addAll(framework.socketTypes());
            }
            assertEquals(EnumSet.copyOf(SocketType.frameworkTypes()), seenTypes, hullSize.name());
        }
    }

    @Test
    void everyTypeIsAboutEquallyLikely() {
        Random random = new Random(7L);
        Map<SocketType, Integer> counts = new EnumMap<>(SocketType.class);
        int frameworks = 40_000;
        int sockets = 0;
        for (int i = 0; i < frameworks; i++) {
            for (SocketType socketType : HullFrameworkRoller.roll(HullSize.CRUISER, random).socketTypes()) {
                counts.merge(socketType, 1, Integer::sum);
                sockets++;
            }
        }
        double expectedShare = 1.0 / SocketType.frameworkTypes().size();
        for (SocketType socketType : SocketType.frameworkTypes()) {
            assertEquals(expectedShare, counts.get(socketType) / (double) sockets, 0.01, socketType.name());
        }
    }

    @Test
    void onlyAllowedTypesAreRolledAndAShortAllowanceShrinksTheFramework() {
        Random random = new Random(3L);
        List<SocketType> allowed = List.of(SocketType.BRIDGE, SocketType.REACTOR, SocketType.SUBROUTINE);
        for (int i = 0; i < 500; i++) {
            HullFrameworkData framework = HullFrameworkRoller.roll(HullSize.FRIGATE, allowed, random);
            assertTrue(Set.of(SocketType.BRIDGE, SocketType.REACTOR).containsAll(framework.socketTypes()), framework.toString());
            assertEquals(Math.min(2, framework.socketTypes().size()), framework.socketTypes().size());
        }
        assertNull(HullFrameworkRoller.roll(HullSize.FRIGATE, List.of(SocketType.SUBROUTINE), random));
    }

    @Test
    void theSameSeedGivesTheSameFramework() {
        assertEquals(HullFrameworkRoller.roll(HullSize.DESTROYER, new Random(9L)), HullFrameworkRoller.roll(HullSize.DESTROYER, new Random(9L)));
    }

    @Test
    void cargoDataRoundTripsAndRejectsGarbage() {
        HullFrameworkData framework = new HullFrameworkData(HullSize.CAPITAL_SHIP, SocketableRarity.RARE,
                List.of(SocketType.FLIGHT_DECK, SocketType.BRIDGE, SocketType.PHASE_COIL), -12L);

        assertEquals(framework, HullFrameworkData.parse(framework.encode()));
        assertEquals(framework, HullFrameworkData.of(framework.toSpecialItem()));
        assertEquals(framework, HullFrameworkData.ofNpcId(framework.npcId()));
        assertTrue(HullFrameworkData.isNpcId(framework.npcId()));
        assertNull(HullFrameworkData.parse(null));
        assertNull(HullFrameworkData.parse("CRUISER/RARE/bridge"));
        assertNull(HullFrameworkData.parse("FIGHTER/RARE/bridge/1"));
        assertNull(HullFrameworkData.parse("CRUISER/RARE/subroutine/1"));
        assertNull(HullFrameworkData.parse("CRUISER/SHINY/bridge/1"));
        assertNull(HullFrameworkData.parse("CRUISER/RARE/bridge/seed"));
        assertNull(HullFrameworkData.ofNpcId(framework.encode()));
    }

    @Test
    void unknownSocketTypesInASavedFrameworkAreDropped() {
        HullFrameworkData parsed = HullFrameworkData.parse("CRUISER/RARE/bridge+teleporter+reactor/5");

        assertEquals(List.of(SocketType.BRIDGE, SocketType.REACTOR), parsed.socketTypes());
    }
}
