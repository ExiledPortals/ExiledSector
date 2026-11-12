package exiledsector.charts.officer.turn;

import exiledsector.charts.atlas.AtlasCell;
import exiledsector.charts.atlas.AtlasState;
import exiledsector.charts.officer.AtlasOfficer;
import exiledsector.charts.officer.OfficerRank;
import exiledsector.charts.officer.OfficerRanks;
import exiledsector.charts.officer.OfficerText;
import exiledsector.charts.officer.reward.CarriedItem;
import exiledsector.charts.officer.reward.RewardKind;
import exiledsector.charts.officer.turn.TurnResult.LossKind;
import exiledsector.charts.officer.turn.TurnResult.OfficerLoss;
import exiledsector.charts.officer.turn.TurnResult.ResolvedIntent;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static exiledsector.charts.officer.turn.TurnBoard.ATTACKER_WINS;
import static exiledsector.charts.officer.turn.TurnBoard.DEFENDER_WINS;
import static exiledsector.charts.officer.turn.TurnBoard.SEED;
import static exiledsector.charts.officer.turn.TurnBoard.declare;
import static exiledsector.charts.officer.turn.TurnBoard.place;
import static exiledsector.charts.officer.turn.TurnBoard.setup;
import static exiledsector.charts.officer.turn.TurnBoard.take;
import static exiledsector.charts.officer.turn.TurnBoard.update;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class OfficerTurnTest {

    private static final AtlasCell HEGEMONY_CENTRE = new AtlasCell(0, 0);
    private static final AtlasCell HEGEMONY_BORDER = new AtlasCell(1, 0);
    private static final AtlasCell PIRATE_BORDER = new AtlasCell(2, 0);
    private static final AtlasCell PIRATE_CENTRE = new AtlasCell(3, 0);
    private static final int HEGEMONY_OFFICER = 1;
    private static final int PIRATE_OFFICER = 2;

    private static AtlasState borderingTerritories() {
        AtlasState state = new AtlasState(SEED);
        place(state, HEGEMONY_OFFICER, "hegemony", "commander", HEGEMONY_CENTRE, HEGEMONY_BORDER);
        place(state, PIRATE_OFFICER, "pirates", "lieutenant", PIRATE_CENTRE, PIRATE_BORDER);
        return state;
    }

    private static AtlasState crowdedBoard(long seed) {
        AtlasState state = new AtlasState(seed);
        List<String> factionIds = List.of("hegemony", "pirates", "tritachyon");
        List<String> rankIds = OfficerRanks.all().stream().map(OfficerRank::id).toList();
        int recordId = 1;
        for (int gridX = -6; gridX <= 6; gridX += 3) {
            for (int gridY = -6; gridY <= 6; gridY += 3) {
                place(state, recordId, factionIds.get(recordId % factionIds.size()), rankIds.get(recordId % rankIds.size()),
                        new AtlasCell(gridX, gridY));
                recordId++;
            }
        }
        return state;
    }

    private static ResolvedIntent resolved(TurnResult result, IntentKind kind) {
        return result.resolvedIntents().stream().filter(intent -> intent.kind() == kind).findFirst().orElse(null);
    }

    @Test
    void theSameStateSeedAndCounterTakeTheSameTurns() {
        AtlasState first = crowdedBoard(SEED);
        AtlasState second = crowdedBoard(SEED);
        int resolvedCount = 0;
        for (int turn = 0; turn < 8; turn++) {
            TurnResult firstResult = take(first, TurnCause.forced(), setup(OfficerTurnTables.DEFAULTS));
            TurnResult secondResult = take(second, TurnCause.forced(), setup(OfficerTurnTables.DEFAULTS));
            assertEquals(firstResult.encode(), secondResult.encode());
            assertEquals(first.declaredIntents(), second.declaredIntents());
            resolvedCount += firstResult.resolvedIntents().size();
        }
        assertEquals(first.officers(), second.officers());
        assertEquals(first.claims(), second.claims());
        assertTrue(resolvedCount > 0);
    }

    @Test
    void aDifferentSeedDeclaresDifferently() {
        AtlasState first = crowdedBoard(SEED);
        AtlasState second = crowdedBoard(SEED + 1);
        take(first, TurnCause.forced(), setup(OfficerTurnTables.DEFAULTS));
        take(second, TurnCause.forced(), setup(OfficerTurnTables.DEFAULTS));

        assertNotEquals(first.declaredIntents(), second.declaredIntents());
    }

    @Test
    void aTurnAdvancesTheCounterAndSavesItsResult() {
        AtlasState state = borderingTerritories();
        TurnResult result = take(state, TurnCause.forced(), setup(ATTACKER_WINS));

        assertEquals(1, state.officerTurn());
        assertEquals(1, result.turn());
        assertEquals(result, TurnResult.decode(state.lastTurnResult()));
        assertEquals(2, take(state, TurnCause.forced(), setup(ATTACKER_WINS)).turn());
    }

    @Test
    void everyOfficerDeclaresAtMostItsRanksActionsAndHoldsAreNotSaved() {
        AtlasState state = crowdedBoard(SEED);
        take(state, TurnCause.forced(), setup(OfficerTurnTables.DEFAULTS));

        List<Intent> declared = OfficerTurn.decodeIntents(state.declaredIntents());
        assertFalse(declared.isEmpty());
        for (AtlasOfficer officer : state.officers()) {
            long count = declared.stream().filter(intent -> intent.officerRecordId() == officer.recordId()).count();
            assertTrue(count <= OfficerRanks.rank(officer.rankId()).actions());
        }
        assertTrue(declared.stream().noneMatch(intent -> intent.kind() == IntentKind.HOLD));
    }

    @Test
    void anExpandClaimsAFreeBorderingSystem() {
        AtlasState state = borderingTerritories();
        AtlasCell target = new AtlasCell(0, 1);
        declare(state, Intent.expand(HEGEMONY_OFFICER, target));

        TurnResult result = take(state, TurnCause.forced(), setup(ATTACKER_WINS));

        assertEquals(HEGEMONY_CENTRE, state.commandCentreOf(target));
        assertEquals(1, state.officer(HEGEMONY_OFFICER).renown());
        assertTrue(resolved(result, IntentKind.EXPAND).succeeded());
    }

    @Test
    void anExpandToASystemOffTheBorderDoesNothing() {
        AtlasState state = borderingTerritories();
        AtlasCell target = new AtlasCell(0, 3);
        declare(state, Intent.expand(HEGEMONY_OFFICER, target));

        TurnResult result = take(state, TurnCause.forced(), setup(ATTACKER_WINS));

        assertFalse(state.isClaimed(target));
        assertEquals(0, state.officer(HEGEMONY_OFFICER).renown());
        assertNull(resolved(result, IntentKind.EXPAND));
    }

    @Test
    void aWonPushTakesTheBorderSystem() {
        AtlasState state = borderingTerritories();
        declare(state, Intent.push(HEGEMONY_OFFICER, PIRATE_BORDER, PIRATE_OFFICER));

        TurnResult result = take(state, TurnCause.forced(), setup(ATTACKER_WINS));

        assertEquals(HEGEMONY_CENTRE, state.commandCentreOf(PIRATE_BORDER));
        assertEquals(1, state.officer(HEGEMONY_OFFICER).renown());
        assertTrue(resolved(result, IntentKind.PUSH).succeeded());
    }

    @Test
    void aHeldDefenceKeepsTheSystemAndEarnsRenown() {
        AtlasState state = borderingTerritories();
        declare(state, Intent.push(HEGEMONY_OFFICER, PIRATE_BORDER, PIRATE_OFFICER), Intent.defend(PIRATE_OFFICER));

        TurnResult result = take(state, TurnCause.forced(), setup(DEFENDER_WINS));

        assertEquals(PIRATE_CENTRE, state.commandCentreOf(PIRATE_BORDER));
        assertEquals(DEFENDER_WINS.renownPerDefendHeld(), state.officer(PIRATE_OFFICER).renown());
        ResolvedIntent push = resolved(result, IntentKind.PUSH);
        assertFalse(push.succeeded());
        assertTrue(push.defended());
        assertTrue(result.losses().isEmpty());
    }

    @Test
    void aResolverForAnIntentKindReplacesTheAbstractRoll() {
        AtlasState state = borderingTerritories();
        declare(state, Intent.push(HEGEMONY_OFFICER, PIRATE_BORDER, PIRATE_OFFICER));
        List<Contest> contests = new ArrayList<>();
        IntentResolvers resolvers = IntentResolvers.ABSTRACT.with(IntentKind.PUSH, contest -> {
            contests.add(contest);
            return true;
        });

        take(state, TurnCause.forced(), setup(DEFENDER_WINS, resolvers, null, Intent.NO_OFFICER));

        assertEquals(HEGEMONY_CENTRE, state.commandCentreOf(PIRATE_BORDER));
        assertEquals(1, contests.size());
        assertEquals(HEGEMONY_OFFICER, contests.get(0).attacker().recordId());
        assertEquals(PIRATE_OFFICER, contests.get(0).defender().recordId());
        assertEquals(0f, contests.get(0).attackerChancePercent());
        assertFalse(contests.get(0).abstractAttackerWins());
    }

    @Test
    void aPushNeverReachesTheTerritoryHoldingTheOpenRun() {
        AtlasState state = borderingTerritories();
        AtlasCell openRunCell = new AtlasCell(4, 0);
        state.claim(openRunCell, PIRATE_CENTRE);
        declare(state, Intent.push(HEGEMONY_OFFICER, PIRATE_BORDER, PIRATE_OFFICER));

        TurnResult result = take(state, TurnCause.forced(), setup(ATTACKER_WINS, IntentResolvers.ABSTRACT, openRunCell, Intent.NO_OFFICER));

        assertEquals(PIRATE_CENTRE, state.commandCentreOf(PIRATE_BORDER));
        assertNull(resolved(result, IntentKind.PUSH));
        assertTrue(OfficerTurn.decodeIntents(state.declaredIntents()).stream().noneMatch(intent -> intent.kind() == IntentKind.PUSH));
    }

    @Test
    void theOpenRunOfficerNeitherActsNorIsTargeted() {
        AtlasState state = borderingTerritories();
        declare(state, Intent.expand(PIRATE_OFFICER, new AtlasCell(3, 1)), Intent.challenge(HEGEMONY_OFFICER, PIRATE_OFFICER));

        TurnResult result = take(state, TurnCause.forced(), setup(ATTACKER_WINS, IntentResolvers.ABSTRACT, PIRATE_CENTRE, PIRATE_OFFICER));

        assertTrue(result.resolvedIntents().isEmpty());
        assertTrue(OfficerTurn.decodeIntents(state.declaredIntents()).stream()
                .noneMatch(intent -> intent.officerRecordId() == PIRATE_OFFICER || intent.targets(PIRATE_OFFICER)));
    }

    @Test
    void aChallengeWinnerTakesRenownAndTheLosersItems() {
        AtlasState state = borderingTerritories();
        update(state, HEGEMONY_OFFICER, officer -> officer.withRenown(4).withCarried(List.of(kernels(1))));
        update(state, PIRATE_OFFICER, officer -> officer.withRenown(6).withCarried(List.of(kernels(2), wards(1))));
        declare(state, Intent.challenge(HEGEMONY_OFFICER, PIRATE_OFFICER));

        TurnResult result = take(state, TurnCause.forced(), setup(ATTACKER_WINS));

        AtlasOfficer winner = state.officer(HEGEMONY_OFFICER);
        AtlasOfficer loser = state.officer(PIRATE_OFFICER);
        assertEquals(7, winner.renown());
        assertEquals(List.of(kernels(3), wards(1)), winner.carried());
        assertEquals(OfficerTurn.CHALLENGE_WON_HISTORY, OfficerText.latestEntry(winner).event());
        assertEquals(PIRATE_CENTRE, OfficerText.latestEntry(winner).latest());
        assertEquals(3, loser.renown());
        assertTrue(loser.carried().isEmpty());
        assertTrue(loser.lastChance());
        assertTrue(loser.isActive());
        assertEquals(OfficerTurn.CHALLENGE_LOST_HISTORY, OfficerText.latestEntry(loser).event());
        assertEquals(new OfficerLoss(PIRATE_OFFICER, LossKind.LOST_CHALLENGE, HEGEMONY_OFFICER, PIRATE_CENTRE, false, false),
                result.lossOf(PIRATE_OFFICER));
    }

    @Test
    void aLowerRankedChallengerWhoWinsTakesTheLosersRank() {
        AtlasState state = borderingTerritories();
        declare(state, Intent.challenge(PIRATE_OFFICER, HEGEMONY_OFFICER));

        TurnResult result = take(state, TurnCause.forced(), setup(ATTACKER_WINS));

        assertEquals("commander", state.officer(PIRATE_OFFICER).rankId());
        assertEquals("lieutenant", state.officer(HEGEMONY_OFFICER).rankId());
        assertFalse(state.officer(HEGEMONY_OFFICER).lastChance());
        assertEquals(2, result.rankChanges().size());
    }

    @Test
    void aChallengeLoserTakesNoFurtherPartThatTurn() {
        AtlasState state = borderingTerritories();
        declare(state, Intent.challenge(HEGEMONY_OFFICER, PIRATE_OFFICER), Intent.expand(PIRATE_OFFICER, new AtlasCell(3, 1)));

        take(state, TurnCause.forced(), setup(ATTACKER_WINS));

        assertFalse(state.isClaimed(new AtlasCell(3, 1)));
    }

    @Test
    void aMarkedLieutenantWhoLosesDiesAndARecruitTakesTheirCommand() {
        AtlasState state = borderingTerritories();
        update(state, PIRATE_OFFICER, officer -> officer.withLastChance(true));
        declare(state, Intent.challenge(HEGEMONY_OFFICER, PIRATE_OFFICER));

        TurnResult result = take(state, TurnCause.forced(), setup(ATTACKER_WINS));

        assertNull(state.officer(PIRATE_OFFICER));
        assertTrue(result.fallen().containsKey(PIRATE_OFFICER));
        assertTrue(result.lossOf(PIRATE_OFFICER).died());
        int recruitId = result.replacements().get(0).replacementRecordId();
        assertEquals(PIRATE_OFFICER, result.replacements().get(0).fallenRecordId());
        AtlasOfficer recruit = state.officerAt(PIRATE_CENTRE);
        assertEquals(recruitId, recruit.recordId());
        assertEquals("pirates", recruit.factionId());
        assertEquals(OfficerRanks.lowest().id(), recruit.rankId());
        assertEquals(PIRATE_CENTRE, state.commandCentreOf(PIRATE_BORDER));
        assertNull(result.standingChange(recruitId).before());
        assertNull(result.standingChange(PIRATE_OFFICER).after());
    }

    @Test
    void anOfficerBeatenByThePlayerFleesTheTerritoryBreaksUpAndTheyDropARank() {
        AtlasState state = borderingTerritories();
        place(state, 3, "hegemony", "lieutenant", new AtlasCell(-4, 0));
        place(state, 4, "pirates", "lieutenant", new AtlasCell(-4, 4));
        update(state, 3, officer -> officer.withRenown(5));
        update(state, 4, officer -> officer.withRenown(2));

        TurnResult result = take(state, TurnCause.officerDefeated(HEGEMONY_OFFICER), setup(ATTACKER_WINS));

        AtlasOfficer beaten = state.officer(HEGEMONY_OFFICER);
        assertTrue(beaten.hasFled());
        assertEquals(1, beaten.defeats());
        assertEquals(HEGEMONY_CENTRE, OfficerText.fledFrom(beaten));
        assertFalse(state.isClaimed(HEGEMONY_CENTRE));
        assertFalse(state.isClaimed(HEGEMONY_BORDER));
        assertEquals("lieutenant", beaten.rankId());
        assertEquals("commander", state.officer(3).rankId());
        assertEquals("lieutenant", state.officer(4).rankId());
        assertEquals(new OfficerLoss(HEGEMONY_OFFICER, LossKind.BEATEN_BY_PLAYER, Intent.NO_OFFICER, HEGEMONY_CENTRE, true, false),
                result.lossOf(HEGEMONY_OFFICER));
    }

    @Test
    void aLieutenantBeatenByThePlayerIsMarkedAndDiesTheSecondTime() {
        AtlasState state = borderingTerritories();

        take(state, TurnCause.officerDefeated(PIRATE_OFFICER), setup(ATTACKER_WINS));
        assertTrue(state.officer(PIRATE_OFFICER).lastChance());

        TurnResult result = take(state, TurnCause.officerDefeated(PIRATE_OFFICER), setup(ATTACKER_WINS));
        assertNull(state.officer(PIRATE_OFFICER));
        assertTrue(result.lossOf(PIRATE_OFFICER).died());
        AtlasOfficer recruit = state.officer(result.replacements().get(0).replacementRecordId());
        assertTrue(recruit.neverPlaced());
        assertEquals("pirates", recruit.factionId());
    }

    @Test
    void anOfficerWhoBeatsThePlayerClearsTheMarkAndRisesARank() {
        AtlasState state = borderingTerritories();
        place(state, 5, "pirates", "commander", new AtlasCell(-4, 4));
        update(state, PIRATE_OFFICER, officer -> officer.withLastChance(true));
        update(state, HEGEMONY_OFFICER, officer -> officer.withRenown(9));
        update(state, 5, officer -> officer.withRenown(1));

        take(state, TurnCause.officerVictorious(PIRATE_OFFICER), setup(ATTACKER_WINS));

        assertFalse(state.officer(PIRATE_OFFICER).lastChance());
        assertEquals("commander", state.officer(PIRATE_OFFICER).rankId());
        assertEquals("lieutenant", state.officer(5).rankId());
        assertEquals("commander", state.officer(HEGEMONY_OFFICER).rankId());
    }

    @Test
    void renownLiftsAnOfficerPastTheLeastRenownedOfTheRankAbove() {
        AtlasState state = borderingTerritories();
        update(state, PIRATE_OFFICER, officer -> officer.withRenown(10));
        update(state, HEGEMONY_OFFICER, officer -> officer.withRenown(2));

        TurnResult result = take(state, TurnCause.forced(), setup(ATTACKER_WINS));

        assertEquals("commander", state.officer(PIRATE_OFFICER).rankId());
        assertEquals("lieutenant", state.officer(HEGEMONY_OFFICER).rankId());
        assertTrue(result.standingChange(PIRATE_OFFICER).rankChanged());
    }

    @Test
    void heldSystemsEarnRenownEachTurn() {
        AtlasState state = new AtlasState(SEED);
        place(state, HEGEMONY_OFFICER, "hegemony", "commander", HEGEMONY_CENTRE, new AtlasCell(1, 0), new AtlasCell(0, 1), new AtlasCell(-1, 0));
        TurnRules rules = new TurnRules(4, 6f, 50f, 0f, 0f, 1, 3, 2, 2, 50, 6, Map.of());

        take(state, TurnCause.forced(), setup(rules));

        assertEquals(2, state.officer(HEGEMONY_OFFICER).renown());
    }

    @Test
    void theConflictCapStopsPushesAndChallenges() {
        AtlasState state = crowdedBoard(SEED);
        TurnRules rules = new TurnRules(4, 6f, 50f, 6.25f, 10f, 1, 3, 2, 4, 50, 0, Map.of());
        for (int turn = 0; turn < 6; turn++) {
            take(state, TurnCause.forced(), setup(rules));
            assertTrue(OfficerTurn.decodeIntents(state.declaredIntents()).stream().noneMatch(intent -> intent.kind().isConflict()));
        }
    }

    @Test
    void onlyATargetedOfficerDeclaresADefence() {
        AtlasState state = borderingTerritories();
        boolean defended = false;
        for (int turn = 0; turn < 40; turn++) {
            take(state, TurnCause.forced(), setup(DEFENDER_WINS));
            List<Intent> declared = OfficerTurn.decodeIntents(state.declaredIntents());
            for (Intent defence : declared.stream().filter(intent -> intent.kind() == IntentKind.DEFEND).toList()) {
                assertTrue(declared.stream().anyMatch(other -> other.targets(defence.officerRecordId())));
                defended = true;
            }
        }
        assertTrue(defended);
    }

    private static CarriedItem kernels(int count) {
        return new CarriedItem(RewardKind.CRAFTING_ITEM, "kernel", null, count);
    }

    private static CarriedItem wards(int count) {
        return new CarriedItem(RewardKind.CRAFTING_ITEM, "ward", null, count);
    }
}
