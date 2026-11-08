package exiledsector.skills.skilleffect;

import com.fs.starfarer.api.combat.MutableShipStatsAPI;

public final class TaperedBonus {

    private final ScaledBonus.Tracker bonus;
    private final float taperSeconds;
    private final MutableShipStatsAPI stats;
    private final String modId;
    private final Taper taper = new Taper();

    public TaperedBonus(ScaledBonus bonus, float taperSeconds, MutableShipStatsAPI stats, String modId) {
        this.bonus = bonus.tracker();
        this.taperSeconds = taperSeconds;
        this.stats = stats;
        this.modId = modId;
    }

    public void start() {
        start(taperSeconds);
    }

    public void start(float taperSecondsThisTime) {
        taper.start(taperSecondsThisTime);
    }

    public void startWithIncreasedDuration(float durationIncreasePercent) {
        taper.start(taperSeconds * (1f + Math.max(0f, durationIncreasePercent) / 100f));
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
