package exiledsector.skills.skilleffect.scar;

import com.fs.starfarer.api.combat.CombatEntityAPI;
import com.fs.starfarer.api.combat.DamageAPI;
import com.fs.starfarer.api.combat.DamageType;
import com.fs.starfarer.api.combat.ShipAPI;
import exiledsector.skills.skilleffect.ArcWeapons;
import exiledsector.skills.skilleffect.Chance;
import exiledsector.skills.skilleffect.CombatQueries;
import exiledsector.skills.skilleffect.HitMix;
import exiledsector.skills.skilleffect.HitSideEffectListener;
import exiledsector.skills.skilleffect.TypeShares;
import org.lwjgl.util.vector.Vector2f;

import java.util.Random;

final class HighExplosiveConversionListener extends HitSideEffectListener implements HitMix.Resolver {

    static final String CHANCE_PERCENT_KEY = "exiledSector_scarHighExplosiveConversionChancePercent";

    private final Random random;
    private final ArcWeapons arcWeapons;

    HighExplosiveConversionListener(ShipAPI ownerShip) {
        this(ownerShip, new Random());
    }

    HighExplosiveConversionListener(ShipAPI ownerShip, Random random) {
        super(ownerShip);
        this.random = random;
        this.arcWeapons = new ArcWeapons(ownerShip);
    }

    static boolean converts(TypeShares shares) {
        return shares.share(DamageType.KINETIC) > 0f || shares.share(DamageType.ENERGY) > 0f;
    }

    @Override
    protected void onHit(Object param, CombatEntityAPI target, DamageAPI damage, Vector2f point, boolean shieldHit) {
        HitMix.resolve(ownerShip, this, param, target, damage, point, shieldHit);
    }

    @Override
    public void resolve(HitMix.Hit hit) {
        boolean fromWeapon = arcWeapons.weaponBehind(hit.param(), hit.target()) != null;
        if (!fromWeapon || CombatQueries.hostileShip(ownerShip, hit.target()) == null || !converts(hit.originalShares())) {
            return;
        }
        if (Chance.roll(random, magnitude(CHANCE_PERCENT_KEY))) {
            hit.convert(null, DamageType.HIGH_EXPLOSIVE, 1f);
        }
    }
}
