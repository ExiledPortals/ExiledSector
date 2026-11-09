package exiledsector.skills.skilleffect;

import com.fs.starfarer.api.combat.MutableShipStatsAPI;

public final class TaperedBonus {

    private final ScaledBonus.Tracker bonus;
    private final MutableShipStatsAPI stats;
    private final String modId;
    private final Taper taper = new Taper();

    public TaperedBonus(ScaledBonus bonus, MutableShipStatsAPI stats, String modId) {
        this.bonus = bonus.tracker();
        this.stats = stats;
        this.modId = modId;
    }

    public void start(float taperSeconds) {
        taper.start(taperSeconds);
    }

    public void advance(float amount) {
        taper.advance(amount);
        bonus.apply(stats, modId, taper.share());
    }

    public void stop() {
        taper.stop();
        bonus.apply(stats, modId, 0f);
    }
}
