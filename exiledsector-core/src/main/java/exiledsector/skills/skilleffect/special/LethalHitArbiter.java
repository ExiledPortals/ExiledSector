package exiledsector.skills.skilleffect.special;

import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.listeners.HullDamageAboutToBeTakenListener;
import exiledsector.skills.skilleffect.SkillEffectSupport;
import org.lwjgl.util.vector.Vector2f;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public final class LethalHitArbiter implements HullDamageAboutToBeTakenListener {

    public static final int RISE_FROM_THE_ASHES_PRIORITY = 100;
    public static final int PHASE_ANCHOR_DIVE_PRIORITY = 200;

    public interface Saver {

        default boolean blocksHullDamage() {
            return false;
        }

        boolean saveFromLethalHit(ShipAPI ship, float damageAmount);
    }

    public interface Reactor {

        void reactToLethalHit(ShipAPI ship, float damageAmount);
    }

    private record RankedSaver(int priority, Saver saver) {
    }

    private final List<RankedSaver> savers = new ArrayList<>();
    private final List<Reactor> reactors = new ArrayList<>();

    public static void addSaver(ShipAPI ship, int priority, Saver saver) {
        LethalHitArbiter arbiter = on(ship);
        if (arbiter.savers.stream().noneMatch(ranked -> ranked.saver() == saver)) {
            arbiter.savers.add(new RankedSaver(priority, saver));
            arbiter.savers.sort(Comparator.comparingInt(RankedSaver::priority));
        }
    }

    public static void addReactor(ShipAPI ship, Reactor reactor) {
        LethalHitArbiter arbiter = on(ship);
        if (!arbiter.reactors.contains(reactor)) {
            arbiter.reactors.add(reactor);
        }
    }

    private static LethalHitArbiter on(ShipAPI ship) {
        return SkillEffectSupport.ensureListener(ship, LethalHitArbiter.class, s -> new LethalHitArbiter());
    }

    @Override
    public boolean notifyAboutToTakeHullDamage(Object param, ShipAPI damagedShip, Vector2f point, float damageAmount) {
        boolean lethal = damageAmount >= damagedShip.getHitpoints();
        if (lethal) {
            for (Reactor reactor : reactors) {
                reactor.reactToLethalHit(damagedShip, damageAmount);
            }
        }
        for (RankedSaver ranked : savers) {
            if (ranked.saver().blocksHullDamage()) {
                return true;
            }
        }
        if (!lethal) {
            return false;
        }
        for (RankedSaver ranked : savers) {
            if (ranked.saver().saveFromLethalHit(damagedShip, damageAmount)) {
                return true;
            }
        }
        return false;
    }
}
