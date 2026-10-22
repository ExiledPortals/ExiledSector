package exiledsector.effects;

import com.fs.starfarer.api.combat.HullModEffect;

public interface WrappingHullModEffect {

    HullModEffect original();

    static HullModEffect unwrapped(HullModEffect effect) {
        return effect instanceof WrappingHullModEffect wrapper ? wrapper.original() : effect;
    }
}
