package exiledsector.skills.skilleffect.fighter;

import com.fs.starfarer.api.combat.CombatEngineAPI;
import com.fs.starfarer.api.combat.FighterWingAPI;
import com.fs.starfarer.api.combat.ShipAPI;

import java.util.List;

final class WingMemberCache {

    static final float REFRESH_SECONDS = 0.25f;

    private FighterWingAPI cachedWing;
    private List<ShipAPI> members = List.of();
    private float refreshedAt = Float.NaN;

    List<ShipAPI> membersOf(ShipAPI fighter, CombatEngineAPI engine) {
        FighterWingAPI wing = fighter.getWing();
        if (wing == null) {
            return List.of();
        }
        float now = engine == null ? Float.NaN : engine.getTotalElapsedTime(false);
        boolean fresh = wing == cachedWing && now >= refreshedAt && now - refreshedAt < REFRESH_SECONDS;
        if (!fresh) {
            cachedWing = wing;
            members = wing.getWingMembers();
            refreshedAt = now;
        }
        return members;
    }
}
