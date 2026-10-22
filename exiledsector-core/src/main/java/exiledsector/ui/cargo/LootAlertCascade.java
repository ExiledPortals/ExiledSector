package exiledsector.ui.cargo;

import com.fs.starfarer.api.campaign.CargoStackAPI;
import com.fs.starfarer.api.campaign.SpecialItemData;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Deque;
import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.function.Function;

final class LootAlertCascade {

    static final long SPACING_NANOS = TimeUnit.MILLISECONDS.toNanos(200);
    static final long IDLE_TIMEOUT_NANOS = TimeUnit.SECONDS.toNanos(30);

    private record PendingDrop(SpecialItemData special, LootAlert alert, int quantity) {
    }

    private final List<PendingDrop> pendingDrops;
    private final Set<CargoStackAPI> seenStacks = Collections.newSetFromMap(new IdentityHashMap<>());
    private final Deque<LootAlert> queuedAlerts = new ArrayDeque<>();
    private long lastProgressNanos;
    private long lastPlayedNanos;
    private boolean anyPlayed;

    private LootAlertCascade(List<PendingDrop> pendingDrops, long startNanos) {
        this.pendingDrops = pendingDrops;
        this.lastProgressNanos = startNanos;
    }

    static LootAlertCascade of(List<CargoStackAPI> lootStacks, Function<CargoStackAPI, LootAlert> classifier, long startNanos) {
        List<PendingDrop> pendingDrops = new ArrayList<>();
        for (CargoStackAPI stack : lootStacks) {
            SpecialItemData special = stack.getSpecialDataIfSpecial();
            LootAlert alert = special == null ? null : classifier.apply(stack);
            if (alert != null) {
                pendingDrops.add(new PendingDrop(special, alert, Math.max(1, Math.round(stack.getSize()))));
            }
        }
        return new LootAlertCascade(pendingDrops, startNanos);
    }

    boolean hasAlerts() {
        return !pendingDrops.isEmpty();
    }

    void advance(List<CargoStackAPI> shownStacks, long nowNanos, Consumer<String> soundPlayer) {
        for (CargoStackAPI stack : shownStacks) {
            if (seenStacks.add(stack)) {
                lastProgressNanos = nowNanos;
                queueIfPending(stack.getSpecialDataIfSpecial());
            }
        }
        if (!queuedAlerts.isEmpty() && (!anyPlayed || nowNanos - lastPlayedNanos >= SPACING_NANOS)) {
            soundPlayer.accept(queuedAlerts.poll().soundId());
            lastPlayedNanos = nowNanos;
            anyPlayed = true;
        }
    }

    boolean finished(long nowNanos) {
        return queuedAlerts.isEmpty() && (pendingDrops.isEmpty() || nowNanos - lastProgressNanos > IDLE_TIMEOUT_NANOS);
    }

    private void queueIfPending(SpecialItemData special) {
        if (special == null) {
            return;
        }
        Iterator<PendingDrop> pending = pendingDrops.iterator();
        while (pending.hasNext()) {
            PendingDrop drop = pending.next();
            if (drop.special().equals(special)) {
                pending.remove();
                for (int instance = 0; instance < drop.quantity(); instance++) {
                    queuedAlerts.add(drop.alert());
                }
                return;
            }
        }
    }
}
