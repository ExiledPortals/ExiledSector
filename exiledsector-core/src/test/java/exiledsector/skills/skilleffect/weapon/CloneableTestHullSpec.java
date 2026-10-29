package exiledsector.skills.skilleffect.weapon;

import com.fs.starfarer.api.combat.ShipHullSpecAPI;

public abstract class CloneableTestHullSpec implements ShipHullSpecAPI {

    @Override
    public abstract Object clone();
}
