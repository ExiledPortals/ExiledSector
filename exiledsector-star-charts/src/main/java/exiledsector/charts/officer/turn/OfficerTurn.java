package exiledsector.charts.officer.turn;

import exiledsector.Seeds;
import exiledsector.charts.atlas.AtlasCell;
import exiledsector.charts.atlas.AtlasState;
import exiledsector.charts.atlas.AtlasTerritoryPlacer;
import exiledsector.charts.atlas.AtlasTerritoryTransfer;
import exiledsector.charts.officer.AtlasOfficer;
import exiledsector.charts.officer.OfficerBoardGenerator;
import exiledsector.charts.officer.OfficerSeats;
import exiledsector.charts.officer.OfficerText;
import exiledsector.charts.officer.reward.CarriedItem;
import exiledsector.charts.officer.turn.TurnResult.LossKind;
import exiledsector.charts.officer.turn.TurnResult.OfficerLoss;
import exiledsector.charts.officer.turn.TurnResult.Replacement;
import exiledsector.charts.officer.turn.TurnResult.ResolvedIntent;
import exiledsector.charts.officer.turn.TurnResult.Standing;
import exiledsector.charts.officer.turn.TurnResult.StandingChange;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Random;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

public final class OfficerTurn {

    public static final String CHALLENGE_WON_HISTORY = "challengeWon";
    public static final String CHALLENGE_LOST_HISTORY = "challengeLost";
    private static final long TURN_SALT = 0x4f4654555246L;
    private static final Comparator<AtlasCell> CELL_ORDER = Comparator.comparingLong(AtlasCell::pack);

    private final TurnTerrain terrain;
    private final AtlasState state;
    private final TurnSetup setup;
    private final TurnRules rules;
    private final Random random;
    private final Map<Integer, Standing> standingsBefore = new LinkedHashMap<>();
    private final List<ResolvedIntent> resolvedIntents = new ArrayList<>();
    private final List<OfficerLoss> losses = new ArrayList<>();
    private final List<Replacement> replacements = new ArrayList<>();
    private final Map<Integer, AtlasOfficer> fallen = new LinkedHashMap<>();
    private final Set<Integer> changedRankThisTurn = new HashSet<>();
    private final Set<Integer> beatenThisTurn = new HashSet<>();
    private final Set<AtlasCell> declaredCells = new HashSet<>();
    private final Set<Integer> declaredChallengers = new HashSet<>();
    private int declaredConflicts;

    private OfficerTurn(TurnTerrain terrain, AtlasState state, TurnSetup setup, Random random) {
        this.terrain = terrain;
        this.state = state;
        this.setup = setup;
        this.rules = setup.rules();
        this.random = random;
    }

    public static TurnResult take(TurnTerrain terrain, AtlasState state, TurnCause cause, TurnSetup setup) {
        int turn = state.advanceOfficerTurn();
        OfficerTurn officerTurn = new OfficerTurn(terrain, state, setup, Seeds.derived(state.seed(), TURN_SALT + turn * Seeds.GOLDEN_GAMMA));
        state.officers().forEach(officer -> officerTurn.standingsBefore.put(officer.recordId(), Standing.of(officer)));
        officerTurn.applyCause(cause);
        officerTurn.resolveDeclared(decodeIntents(state.declaredIntents()));
        state.clearDecidedContests();
        officerTurn.earnHoldingsRenown();
        officerTurn.swapByRenown();
        List<Intent> declared = officerTurn.declare();
        state.setDeclaredIntents(declared.stream().filter(intent -> intent.kind() != IntentKind.HOLD).map(Intent::encode).toList());
        TurnResult result = officerTurn.result(turn, cause);
        state.setLastTurnResult(result.encode());
        return result;
    }

    public static List<Intent> decodeIntents(List<String> lines) {
        return lines.stream().map(Intent::decode).filter(Objects::nonNull).toList();
    }

    private void applyCause(TurnCause cause) {
        AtlasOfficer officer = state.officer(cause.officerRecordId());
        if (officer == null) {
            return;
        }
        if (cause.kind() == TurnCause.Kind.OFFICER_DEFEATED) {
            beatenByPlayer(officer);
        } else if (cause.kind() == TurnCause.Kind.OFFICER_VICTORIOUS) {
            beatPlayer(officer);
        }
    }

    private void beatenByPlayer(AtlasOfficer officer) {
        if (officer.isActive()) {
            AtlasTerritoryTransfer.dismissOfficer(state, officer.commandCentre());
        }
        AtlasOfficer escaped = state.officer(officer.recordId());
        AtlasCell lostCentre = OfficerText.fledFrom(escaped);
        boolean wasLowestRank = setup.rankOrder(escaped.rankId()) == 0;
        state.putOfficer(escaped.withDefeats(escaped.defeats() + 1));
        boolean died = loseFight(escaped.recordId(), wasLowestRank);
        if (!wasLowestRank) {
            AtlasOfficer partner = mostRenownedSwappable(OfficerSeats.subordinates(setup.ranks(), state.officers(), state.officer(escaped.recordId())));
            if (partner != null) {
                swapRanks(escaped.recordId(), partner.recordId());
            }
        }
        losses.add(new OfficerLoss(escaped.recordId(), LossKind.BEATEN_BY_PLAYER, Intent.NO_OFFICER, lostCentre, !died, died));
    }

    private void beatPlayer(AtlasOfficer officer) {
        state.putOfficer(officer.withLastChance(false));
        AtlasOfficer superior = OfficerSeats.superior(setup.ranks(), state.officers(), officer);
        if (superior != null && canSwap(superior)) {
            swapRanks(officer.recordId(), superior.recordId());
        }
    }

    private boolean loseFight(int recordId, boolean wasLowestRank) {
        AtlasOfficer loser = state.officer(recordId);
        loser = loser.withRenown(rules.renownAfterLoss(loser.renown()));
        beatenThisTurn.add(recordId);
        if (wasLowestRank && loser.lastChance()) {
            die(loser);
            return true;
        }
        state.putOfficer(wasLowestRank ? loser.withLastChance(true) : loser);
        return false;
    }

    private void die(AtlasOfficer officer) {
        fallen.put(officer.recordId(), officer);
        state.removeOfficer(officer.recordId());
        AtlasOfficer replacement = OfficerBoardGenerator.replacement(state.seed(), state.newOfficerRecordId(), officer.factionId(), officer.seat(), setup.ranks(),
                setup.catalogue(), setup.startingItems()).withPendingOffers(officer.pendingOffers());
        if (officer.isActive()) {
            replacement = state.withLeftBehindItems(replacement.takingCommand(officer.commandCentre(),
                    AtlasTerritoryPlacer.history(AtlasTerritoryPlacer.ARRIVED_HISTORY, officer.commandCentre())));
        }
        state.putOfficer(replacement);
        replacements.add(new Replacement(officer.recordId(), replacement.recordId()));
    }

    private boolean canSwap(AtlasOfficer officer) {
        return officer.heldCommand() && officer.recordId() != setup.openRunOfficerId() && !changedRankThisTurn.contains(officer.recordId());
    }

    private AtlasOfficer mostRenownedSwappable(List<AtlasOfficer> candidates) {
        AtlasOfficer partner = null;
        for (AtlasOfficer officer : candidates) {
            if (canSwap(officer) && (partner == null || officer.renown() > partner.renown()
                    || officer.renown() == partner.renown() && officer.recordId() < partner.recordId())) {
                partner = officer;
            }
        }
        return partner;
    }

    private void swapRanks(int firstRecordId, int secondRecordId) {
        AtlasOfficer first = state.officer(firstRecordId);
        AtlasOfficer second = state.officer(secondRecordId);
        state.putOfficer(first.seatedAt(second.rankId(), second.seat()));
        state.putOfficer(second.seatedAt(first.rankId(), first.seat()));
        changedRankThisTurn.add(firstRecordId);
        changedRankThisTurn.add(secondRecordId);
    }

    private void addRenown(int recordId, int gained) {
        AtlasOfficer officer = state.officer(recordId);
        if (officer != null && gained != 0) {
            state.putOfficer(officer.withRenown(officer.renown() + gained));
        }
    }

    private void resolveDeclared(List<Intent> intents) {
        Set<Integer> defending = new HashSet<>();
        for (Intent intent : intents) {
            if (intent.kind() == IntentKind.DEFEND && takesPart(intent.officerRecordId())) {
                defending.add(intent.officerRecordId());
            }
        }
        for (Intent intent : intents) {
            if (!takesPart(intent.officerRecordId()) && !settledInRun(intent)) {
                continue;
            }
            switch (intent.kind()) {
                case EXPAND -> resolveExpand(intent);
                case PUSH -> resolvePush(intent, defending);
                case CHALLENGE -> resolveChallenge(intent, defending);
                default -> {
                }
            }
        }
    }

    private boolean settledInRun(Intent intent) {
        AtlasOfficer attacker = state.officer(intent.officerRecordId());
        return intent.kind() == IntentKind.CHALLENGE && state.decidedContest(intent.encode()) != null && attacker != null && attacker.isActive()
                && !beatenThisTurn.contains(attacker.recordId());
    }

    private boolean takesPart(int recordId) {
        AtlasOfficer officer = state.officer(recordId);
        return officer != null && canDeclare(officer) && !beatenThisTurn.contains(recordId);
    }

    private boolean canDeclare(AtlasOfficer officer) {
        return officer.isActive() && officer.recordId() != setup.openRunOfficerId() && !officer.commandCentre().equals(setup.openRunCell());
    }

    private boolean holdsOpenRun(AtlasOfficer officer) {
        return setup.openRunCell() != null && officer.commandCentre().equals(state.commandCentreOf(setup.openRunCell()));
    }

    private void resolveExpand(Intent intent) {
        AtlasOfficer officer = state.officer(intent.officerRecordId());
        AtlasCell cell = intent.target();
        if (cell == null || cell.equals(setup.openRunCell()) || !terrain.canClaim(cell) || !borders(officer.commandCentre(), cell)) {
            return;
        }
        state.claim(cell, officer.commandCentre());
        addRenown(officer.recordId(), rules.renownPerClaim());
        resolvedIntents.add(new ResolvedIntent(IntentKind.EXPAND, officer.recordId(), Intent.NO_OFFICER, cell, true, false));
    }

    private void resolvePush(Intent intent, Set<Integer> defending) {
        AtlasOfficer attacker = state.officer(intent.officerRecordId());
        AtlasOfficer defender = state.officer(intent.targetRecordId());
        AtlasCell cell = intent.target();
        if (cell == null || defender == null || !takesPart(defender.recordId()) || !canPush(attacker, defender, cell)) {
            return;
        }
        boolean defended = defending.contains(defender.recordId());
        boolean attackerWins = attackerWins(intent, attacker, defender, defended);
        if (attackerWins) {
            AtlasTerritoryTransfer.transferTo(terrain::lanes, state, cell, attacker.commandCentre());
            addRenown(attacker.recordId(), rules.renownPerClaim());
        } else if (defended) {
            addRenown(defender.recordId(), rules.renownPerDefendHeld());
        }
        resolvedIntents.add(new ResolvedIntent(IntentKind.PUSH, attacker.recordId(), defender.recordId(), cell, attackerWins, defended));
    }

    private boolean canPush(AtlasOfficer attacker, AtlasOfficer defender, AtlasCell cell) {
        return !attacker.factionId().equals(defender.factionId()) && defender.commandCentre().equals(state.commandCentreOf(cell))
                && state.officerAt(cell) == null && !cell.equals(setup.openRunCell()) && !holdsOpenRun(defender)
                && borders(attacker.commandCentre(), cell);
    }

    private void resolveChallenge(Intent intent, Set<Integer> defending) {
        AtlasOfficer attacker = state.officer(intent.officerRecordId());
        AtlasOfficer defender = state.officer(intent.targetRecordId());
        if (defender == null || !takesPart(defender.recordId())
                || state.decidedContest(intent.encode()) == null && !OfficerSeats.adjacent(setup.ranks(), state.officers(), attacker, defender)) {
            return;
        }
        boolean defended = defending.contains(defender.recordId());
        boolean attackerWins = attackerWins(intent, attacker, defender, defended);
        AtlasOfficer winner = attackerWins ? attacker : defender;
        AtlasOfficer loser = attackerWins ? defender : attacker;
        boolean loserWasLowestRank = setup.rankOrder(loser.rankId()) == 0;
        boolean lowerRankWon = setup.rankOrder(winner.rankId()) < setup.rankOrder(loser.rankId());
        int renownGained = rules.renownPerChallengeWon() + (!attackerWins && defended ? rules.renownPerDefendHeld() : 0);
        state.putOfficer(winner.withRenown(winner.renown() + renownGained).withLastChance(false)
                .withCarried(CarriedItem.stack(winner.carried(), loser.carried()))
                .withHistory(AtlasTerritoryPlacer.history(CHALLENGE_WON_HISTORY, loser.commandCentre())));
        state.putOfficer(loser.withCarried(List.of())
                .withHistory(AtlasTerritoryPlacer.history(CHALLENGE_LOST_HISTORY, winner.commandCentre())));
        if (lowerRankWon) {
            swapRanks(winner.recordId(), loser.recordId());
        }
        boolean died = loseFight(loser.recordId(), loserWasLowestRank);
        losses.add(new OfficerLoss(loser.recordId(), LossKind.LOST_CHALLENGE, winner.recordId(), loser.commandCentre(), false, died));
        resolvedIntents.add(new ResolvedIntent(IntentKind.CHALLENGE, attacker.recordId(), defender.recordId(), null, attackerWins, defended));
    }

    private boolean attackerWins(Intent intent, AtlasOfficer attacker, AtlasOfficer defender, boolean defended) {
        float chance = rules.attackerChancePercent(setup.rankOrder(attacker.rankId()), setup.rankOrder(defender.rankId()), defended);
        Contest contest = new Contest(intent, attacker, defender, defended, chance, random.nextFloat());
        Boolean decided = state.decidedContest(intent.encode());
        return decided != null ? decided : setup.resolvers().forKind(intent.kind()).attackerWins(contest);
    }

    private boolean borders(AtlasCell commandCentre, AtlasCell cell) {
        for (AtlasCell lane : terrain.lanes(cell)) {
            if (commandCentre.equals(state.commandCentreOf(lane))) {
                return true;
            }
        }
        return false;
    }

    private void earnHoldingsRenown() {
        for (AtlasOfficer officer : byRecordId(state.officers())) {
            if (officer.isActive()) {
                addRenown(officer.recordId(), rules.holdingsRenown(state.claimsOf(officer.commandCentre()).size()));
            }
        }
    }

    private void swapByRenown() {
        for (int rankOrder = setup.ranks().size() - 1; rankOrder > 0; rankOrder--) {
            String rankId = setup.ranks().get(rankOrder).id();
            for (int seat = 0; seat < setup.ranks().get(rankOrder).places(); seat++) {
                AtlasOfficer superior = OfficerSeats.seated(state.officers(), rankId, seat);
                if (superior == null || !canSwap(superior)) {
                    continue;
                }
                AtlasOfficer rising = mostRenownedSwappable(OfficerSeats.subordinates(setup.ranks(), state.officers(), superior));
                if (rising != null && rising.renown() > superior.renown()) {
                    swapRanks(rising.recordId(), superior.recordId());
                }
            }
        }
    }

    private List<Intent> declare() {
        List<AtlasOfficer> officers = new ArrayList<>(byRecordId(state.officers()).stream().filter(this::canDeclare).toList());
        Collections.shuffle(officers, random);
        Map<Integer, List<Intent>> intentsByOfficer = new LinkedHashMap<>();
        for (AtlasOfficer officer : officers) {
            intentsByOfficer.put(officer.recordId(), declareFor(officer));
        }
        declareDefences(intentsByOfficer);
        List<Intent> declared = new ArrayList<>();
        intentsByOfficer.values().forEach(declared::addAll);
        return declared;
    }

    private List<Intent> declareFor(AtlasOfficer officer) {
        List<Intent> intents = new ArrayList<>();
        Set<AtlasCell> territory = state.claimsOf(officer.commandCentre());
        int actions = setup.rank(officer.rankId()).actions();
        for (int slot = 0; slot < actions; slot++) {
            List<AtlasCell> expandCells = expandCandidates(territory);
            boolean underConflictCap = declaredConflicts < rules.maxConflictsPerTurn();
            List<Intent> pushes = underConflictCap ? pushCandidates(officer, territory) : List.of();
            List<Intent> challenges = underConflictCap && !declaredChallengers.contains(officer.recordId())
                    ? challengeCandidates(officer) : List.of();
            Map<IntentKind, Float> weights = new EnumMap<>(IntentKind.class);
            if (!expandCells.isEmpty()) {
                weights.put(IntentKind.EXPAND, rules.expandWeight(officer.rankId(), officer.traitId(), territory.size()));
            }
            if (!pushes.isEmpty()) {
                weights.put(IntentKind.PUSH, rules.intentWeight(officer.rankId(), officer.traitId(), IntentKind.PUSH));
            }
            if (!challenges.isEmpty()) {
                weights.put(IntentKind.CHALLENGE, rules.intentWeight(officer.rankId(), officer.traitId(), IntentKind.CHALLENGE));
            }
            weights.put(IntentKind.HOLD, rules.intentWeight(officer.rankId(), officer.traitId(), IntentKind.HOLD));
            Intent intent = switch (pick(weights)) {
                case EXPAND -> Intent.expand(officer.recordId(), pickOne(expandCells));
                case PUSH -> pickOne(pushes);
                case CHALLENGE -> pickOne(challenges);
                default -> Intent.hold(officer.recordId());
            };
            noteDeclared(intent);
            intents.add(intent);
        }
        return intents;
    }

    private void noteDeclared(Intent intent) {
        if (intent.target() != null) {
            declaredCells.add(intent.target());
        }
        if (intent.kind().isConflict()) {
            declaredConflicts++;
        }
        if (intent.kind() == IntentKind.CHALLENGE) {
            declaredChallengers.add(intent.officerRecordId());
            declaredChallengers.add(intent.targetRecordId());
        }
    }

    private List<AtlasCell> expandCandidates(Set<AtlasCell> territory) {
        Set<AtlasCell> candidates = new TreeSet<>(CELL_ORDER);
        for (AtlasCell cell : territory) {
            for (AtlasCell lane : terrain.lanes(cell)) {
                if (!declaredCells.contains(lane) && !lane.equals(setup.openRunCell()) && terrain.canClaim(lane)) {
                    candidates.add(lane);
                }
            }
        }
        return List.copyOf(candidates);
    }

    private List<Intent> pushCandidates(AtlasOfficer officer, Set<AtlasCell> territory) {
        Map<AtlasCell, Intent> candidates = new TreeMap<>(CELL_ORDER);
        for (AtlasCell cell : territory) {
            for (AtlasCell lane : terrain.lanes(cell)) {
                AtlasOfficer defender = targetableNeighbour(officer, lane);
                if (defender != null && state.officerAt(lane) == null && !declaredCells.contains(lane) && !lane.equals(setup.openRunCell())
                        && !holdsOpenRun(defender)) {
                    candidates.put(lane, Intent.push(officer.recordId(), lane, defender.recordId()));
                }
            }
        }
        return List.copyOf(candidates.values());
    }

    private List<Intent> challengeCandidates(AtlasOfficer officer) {
        List<AtlasOfficer> linked = new ArrayList<>(OfficerSeats.subordinates(setup.ranks(), state.officers(), officer));
        AtlasOfficer superior = OfficerSeats.superior(setup.ranks(), state.officers(), officer);
        if (superior != null) {
            linked.add(superior);
        }
        Map<Integer, Intent> candidates = new TreeMap<>();
        for (AtlasOfficer defender : linked) {
            if (canDeclare(defender) && !declaredChallengers.contains(defender.recordId())) {
                candidates.put(defender.recordId(), Intent.challenge(officer.recordId(), defender.recordId()));
            }
        }
        return List.copyOf(candidates.values());
    }

    private AtlasOfficer targetableNeighbour(AtlasOfficer officer, AtlasCell lane) {
        AtlasCell centre = state.commandCentreOf(lane);
        if (centre == null || centre.equals(officer.commandCentre())) {
            return null;
        }
        AtlasOfficer neighbour = state.officerAt(centre);
        return neighbour != null && !neighbour.factionId().equals(officer.factionId()) && canDeclare(neighbour) ? neighbour : null;
    }

    private void declareDefences(Map<Integer, List<Intent>> intentsByOfficer) {
        Set<Integer> targeted = new HashSet<>();
        intentsByOfficer.values().forEach(intents -> intents.stream().filter(intent -> intent.kind().isConflict())
                .forEach(intent -> targeted.add(intent.targetRecordId())));
        for (Map.Entry<Integer, List<Intent>> entry : intentsByOfficer.entrySet()) {
            List<Intent> intents = entry.getValue();
            if (!targeted.contains(entry.getKey()) || intents.isEmpty()) {
                continue;
            }
            int lastSlot = intents.size() - 1;
            IntentKind lastKind = intents.get(lastSlot).kind();
            if (lastKind.isConflict()) {
                continue;
            }
            AtlasOfficer officer = state.officer(entry.getKey());
            float defendWeight = rules.intentWeight(officer.rankId(), officer.traitId(), IntentKind.DEFEND);
            float keepWeight = lastKind == IntentKind.EXPAND
                    ? rules.expandWeight(officer.rankId(), officer.traitId(), state.claimsOf(officer.commandCentre()).size())
                    : rules.intentWeight(officer.rankId(), officer.traitId(), lastKind);
            if (defendWeight > 0f && random.nextFloat() * (defendWeight + Math.max(0f, keepWeight)) < defendWeight) {
                intents.set(lastSlot, Intent.defend(officer.recordId()));
            }
        }
    }

    private IntentKind pick(Map<IntentKind, Float> weights) {
        float total = 0f;
        for (float weight : weights.values()) {
            total += Math.max(0f, weight);
        }
        if (total <= 0f) {
            return IntentKind.HOLD;
        }
        float roll = random.nextFloat() * total;
        for (Map.Entry<IntentKind, Float> entry : weights.entrySet()) {
            roll -= Math.max(0f, entry.getValue());
            if (roll < 0f) {
                return entry.getKey();
            }
        }
        return IntentKind.HOLD;
    }

    private <T> T pickOne(List<T> options) {
        return options.get(random.nextInt(options.size()));
    }

    private static List<AtlasOfficer> byRecordId(List<AtlasOfficer> officers) {
        return officers.stream().sorted(Comparator.comparingInt(AtlasOfficer::recordId)).toList();
    }

    private TurnResult result(int turn, TurnCause cause) {
        Map<Integer, Standing> standingsAfter = new HashMap<>();
        state.officers().forEach(officer -> standingsAfter.put(officer.recordId(), Standing.of(officer)));
        Set<Integer> recordIds = new TreeSet<>(standingsBefore.keySet());
        recordIds.addAll(standingsAfter.keySet());
        List<StandingChange> changes = new ArrayList<>();
        for (int recordId : recordIds) {
            Standing before = standingsBefore.get(recordId);
            Standing after = standingsAfter.get(recordId);
            if (!Objects.equals(before, after)) {
                changes.add(new StandingChange(recordId, before, after));
            }
        }
        return new TurnResult(turn, cause, changes, resolvedIntents, losses, replacements, fallen);
    }
}
