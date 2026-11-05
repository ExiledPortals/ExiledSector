package exiledsector.loot;

import com.fs.starfarer.api.campaign.FleetEncounterContextPlugin;
import com.fs.starfarer.api.fleet.FleetMemberAPI;

import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.Set;

public record EncounterSalvage(float playerShare, Set<FleetMemberAPI> recoveredEnemies) {

    public static final EncounterSalvage FULL = new EncounterSalvage(1f, Set.of());
    public static final EncounterSalvage NO_WRECKS = new EncounterSalvage(0f, Set.of());

    public static EncounterSalvage of(FleetEncounterContextPlugin encounter) {
        if (encounter == null) {
            return FULL;
        }
        Set<FleetMemberAPI> recovered = Collections.newSetFromMap(new IdentityHashMap<>());
        FleetEncounterContextPlugin.DataForEncounterSide winner = encounter.getWinnerData();
        if (winner != null) {
            for (FleetEncounterContextPlugin.FleetMemberData casualty : winner.getEnemyCasualties()) {
                if (casualty.getStatus() == FleetEncounterContextPlugin.Status.REPAIRED) {
                    recovered.add(casualty.getMember());
                }
            }
        }
        return new EncounterSalvage(Math.max(0f, Math.min(1f, encounter.computePlayerContribFraction())), recovered);
    }

    public boolean recovered(FleetMemberAPI member) {
        return recoveredEnemies.contains(member);
    }
}
