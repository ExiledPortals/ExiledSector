package exiledsector.skills.skilleffect.onhit;

import com.fs.starfarer.api.combat.DamageAPI;
import com.fs.starfarer.api.combat.DamageType;
import com.fs.starfarer.api.combat.ShipAPI;
import org.lwjgl.util.vector.Vector2f;

final class IonSpillListener extends EmpConversionListener {

    static final String ENERGY_SHARE_PERCENT_KEY = "exiledSector_ionSpillEnergySharePercent";

    IonSpillListener(ShipAPI ownerShip) {
        super(ownerShip, ENERGY_SHARE_PERCENT_KEY, true, DamageType.ENERGY);
    }

    @Override
    float convertibleEmp(ShipAPI targetShip, Vector2f point, float emp, DamageAPI damage) {
        return emp;
    }
}
