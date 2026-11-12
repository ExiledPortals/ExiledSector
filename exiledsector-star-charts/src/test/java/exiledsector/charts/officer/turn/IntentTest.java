package exiledsector.charts.officer.turn;

import exiledsector.charts.atlas.AtlasCell;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class IntentTest {

    @Test
    void everyKindOfIntentRoundTrips() {
        List<Intent> intents = List.of(Intent.expand(1, new AtlasCell(-3, 4)), Intent.push(2, new AtlasCell(5, -1), 7), Intent.challenge(3, 9),
                Intent.defend(4), Intent.hold(5));
        for (Intent intent : intents) {
            assertEquals(intent, Intent.decode(intent.encode()));
        }
    }

    @Test
    void aMalformedIntentDecodesToNothing() {
        assertNull(Intent.decode(null));
        assertNull(Intent.decode(""));
        assertNull(Intent.decode("raid,1,,0"));
        assertNull(Intent.decode("challenge,one,,2"));
        assertNull(Intent.decode("challenge,1,,2,extra"));
        assertEquals(List.of(Intent.defend(4)), OfficerTurn.decodeIntents(List.of("defend,4,,0", "nonsense")));
    }

    @Test
    void onlyPushesAndChallengesTargetAnOfficer() {
        assertTrue(Intent.push(1, new AtlasCell(0, 1), 2).targets(2));
        assertTrue(Intent.challenge(1, 2).targets(2));
        assertFalse(Intent.challenge(1, 2).targets(1));
        assertFalse(Intent.expand(1, new AtlasCell(0, 1)).targets(Intent.NO_OFFICER));
        assertFalse(Intent.defend(2).targets(2));
    }

    @Test
    void everyKindIsFoundByItsId() {
        Arrays.stream(IntentKind.values()).forEach(kind -> assertEquals(kind, IntentKind.fromId(kind.id())));
        assertNull(IntentKind.fromId("raid"));
    }
}
