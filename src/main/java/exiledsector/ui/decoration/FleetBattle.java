package exiledsector.ui.decoration;

import com.fs.starfarer.api.combat.ShipAPI.HullSize;

import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

final class FleetBattle {

    static final float ENGAGE_GAP = 220f;
    static final float MIN_FIGHT_SECONDS = 8f;
    static final float MAX_FIGHT_SECONDS = 14f;
    static final float MAX_APPROACH_SECONDS = 600f;
    static final float MIN_SHOT_SECONDS = 0.5f;
    static final float MAX_SHOT_SECONDS = 1.5f;
    static final float FIRST_LOSS_FRACTION = 0.2f;
    static final float WINNER_LOSS_END_FRACTION = 0.9f;

    private final FactionFleet first;
    private final FactionFleet second;
    private final FactionFleet winner;
    private final float fightSeconds;
    private final Map<RoamingShip, Float> deathSeconds = new IdentityHashMap<>();
    private final Map<RoamingShip, Float> nextShotSeconds = new IdentityHashMap<>();
    private float approachSeconds;
    private float fightElapsed;
    private boolean fighting;
    private boolean over;

    private FleetBattle(FactionFleet first, FactionFleet second, FactionFleet winner, float fightSeconds) {
        this.first = first;
        this.second = second;
        this.winner = winner;
        this.fightSeconds = fightSeconds;
    }

    static FleetBattle start(FactionFleet first, FactionFleet second, Random random) {
        float strengthFirst = strength(first.ships());
        float strengthSecond = strength(second.ships());
        FactionFleet winner = random.nextFloat() * (strengthFirst + strengthSecond) < strengthFirst ? first : second;
        float fightSeconds = MIN_FIGHT_SECONDS + random.nextFloat() * (MAX_FIGHT_SECONDS - MIN_FIGHT_SECONDS);
        FleetBattle battle = new FleetBattle(first, second, winner, fightSeconds);
        battle.scheduleLosses(random);
        RoamVolume firstVolume = first.flight().roamVolume();
        RoamVolume secondVolume = second.flight().roamVolume();
        float pointX = (firstVolume.centerX() + secondVolume.centerX()) / 2f;
        float pointY = (firstVolume.centerY() + secondVolume.centerY()) / 2f;
        float awayX = secondVolume.centerX() - firstVolume.centerX();
        float awayY = secondVolume.centerY() - firstVolume.centerY();
        float apart = (float) Math.hypot(awayX, awayY);
        if (apart <= 0f) {
            awayX = 1f;
            awayY = 0f;
            apart = 1f;
        }
        float gapX = awayX / apart * ENGAGE_GAP / 2f;
        float gapY = awayY / apart * ENGAGE_GAP / 2f;
        first.flight().holdAt(pointX - gapX, pointY - gapY, pointX + gapX, pointY + gapY);
        second.flight().holdAt(pointX + gapX, pointY + gapY, pointX - gapX, pointY - gapY);
        first.setBattle(battle);
        second.setBattle(battle);
        return battle;
    }

    static float strength(List<RoamingShip> ships) {
        float strength = 0f;
        for (RoamingShip ship : ships) {
            HullSize hullSize = ship.hullSize();
            strength += hullSize == HullSize.CAPITAL_SHIP ? 5f : hullSize == HullSize.CRUISER ? 3f : hullSize == HullSize.DESTROYER ? 2f : 1f;
        }
        return strength;
    }

    private void scheduleLosses(Random random) {
        FactionFleet loser = loser();
        List<RoamingShip> losing = loser.ships();
        for (int index = 0; index < losing.size(); index++) {
            float latest = (index + 1f) / losing.size();
            float earliest = Math.max(FIRST_LOSS_FRACTION, (float) index / losing.size());
            deathSeconds.put(losing.get(losing.size() - 1 - index), fightSeconds * (index == losing.size() - 1 ? 1f : earliest + random.nextFloat() * (latest - earliest)));
        }
        List<RoamingShip> escorts = new ArrayList<>(winner.ships().subList(1, winner.ships().size()));
        int winnerLosses = escorts.isEmpty() ? 0 : random.nextInt(escorts.size() / 2 + 1);
        for (int loss = 0; loss < winnerLosses; loss++) {
            RoamingShip ship = escorts.remove(random.nextInt(escorts.size()));
            deathSeconds.put(ship, fightSeconds * (FIRST_LOSS_FRACTION + random.nextFloat() * (WINNER_LOSS_END_FRACTION - FIRST_LOSS_FRACTION)));
        }
        for (FactionFleet fleet : List.of(first, second)) {
            for (RoamingShip ship : fleet.ships()) {
                nextShotSeconds.put(ship, random.nextFloat() * MAX_SHOT_SECONDS);
            }
        }
    }

    void advance(float amount, Random random, BattleEffects effects) {
        if (over) {
            return;
        }
        if (!fighting) {
            approachSeconds += amount;
            fighting = (first.flight().isAtHold() && second.flight().isAtHold()) || approachSeconds >= MAX_APPROACH_SECONDS;
            return;
        }
        fightElapsed += amount;
        fire(first, second, random, effects);
        fire(second, first, random, effects);
        destroyDue(first, effects);
        destroyDue(second, effects);
        if (fightElapsed >= fightSeconds) {
            over = true;
            first.setBattle(null);
            second.setBattle(null);
            winner.flight().release(winner.home());
        }
    }

    private void fire(FactionFleet shooters, FactionFleet targets, Random random, BattleEffects effects) {
        if (targets.ships().isEmpty()) {
            return;
        }
        for (RoamingShip shooter : shooters.ships()) {
            Float due = nextShotSeconds.get(shooter);
            if (due != null && fightElapsed >= due) {
                RoamingShip target = targets.ships().get(random.nextInt(targets.ships().size()));
                effects.shot(shooters.shipX(shooter), shooters.shipY(shooter), targets.shipX(target), targets.shipY(target), shooters.factionColor());
                nextShotSeconds.put(shooter, fightElapsed + MIN_SHOT_SECONDS + random.nextFloat() * (MAX_SHOT_SECONDS - MIN_SHOT_SECONDS));
            }
        }
    }

    private void destroyDue(FactionFleet fleet, BattleEffects effects) {
        for (int index = fleet.ships().size() - 1; index >= 0; index--) {
            RoamingShip ship = fleet.ships().get(index);
            Float due = deathSeconds.get(ship);
            if (due != null && fightElapsed >= due) {
                effects.explosion(fleet.shipX(ship), fleet.shipY(ship), ship.longestSide());
                fleet.destroy(ship);
            }
        }
    }

    boolean isFighting() {
        return fighting && !over;
    }

    boolean isOver() {
        return over;
    }

    FactionFleet winner() {
        return winner;
    }

    FactionFleet loser() {
        return winner == first ? second : first;
    }
}
