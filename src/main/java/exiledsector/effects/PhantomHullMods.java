package exiledsector.effects;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.HullModEffect;
import com.fs.starfarer.api.combat.HullModFleetEffect;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.loading.HullModSpecAPI;
import exiledsector.skills.AllocatedNode;
import exiledsector.skills.PhantomHullModStatus;
import exiledsector.skills.ShipSkillData;
import exiledsector.skills.SkillDataResolver;
import org.apache.log4j.Logger;

import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

public final class PhantomHullMods {

    private static final Logger LOG = Logger.getLogger(PhantomHullMods.class);
    private static final String LOG_PREFIX = "[ExiledSector] ";
    private static final String CANNOT_MAKE = LOG_PREFIX + "Can't make ";
    private static final Map<String, String> ORIGINAL_EFFECT_CLASSES = new HashMap<>();

    private PhantomHullMods() {
    }

    public static void install(Collection<String> hullModIds) {
        for (String hullModId : hullModIds) {
            installOne(hullModId);
        }
    }

    private static void installOne(String hullModId) {
        if (isActive(hullModId)) return;
        HullModSpecAPI spec = Global.getSettings().getHullModSpec(hullModId);
        if (spec == null) {
            LOG.info(LOG_PREFIX + hullModId + " isn't loaded, so its nodes won't place it as a phantom hull mod.");
            return;
        }
        String originalClass = spec.getEffectClass();
        HullModEffect probe = originalClass == null ? null : instantiate(originalClass);
        if (probe == null) {
            LOG.error(CANNOT_MAKE + hullModId + " a phantom hull mod: no usable effect class; nodes will not place it.");
            return;
        }
        if (probe instanceof HullModFleetEffect) {
            LOG.error(CANNOT_MAKE + hullModId + " a phantom hull mod: it has a fleet effect the game creates "
                    + "separately; nodes will not place it.");
            return;
        }
        ORIGINAL_EFFECT_CLASSES.put(hullModId, originalClass);
        spec.setEffectClass(PhantomHullModEffect.class.getName());
        HullModEffect effect = createdEffect(spec);
        if (effect instanceof PhantomHullModEffect phantom && phantom.wrapsOriginal()) {
            PhantomHullModStatus.markActive(hullModId);
            LOG.info(LOG_PREFIX + hullModId + " is now a phantom hull mod (vanilla effect " + originalClass + ").");
            return;
        }
        if (effect instanceof PhantomHullModEffect) {
            LOG.error(LOG_PREFIX + hullModId + " could not recreate its vanilla effect " + originalClass
                    + "; the hull mod will do nothing this session and nodes will not place it.");
            return;
        }
        spec.setEffectClass(originalClass);
        ORIGINAL_EFFECT_CLASSES.remove(hullModId);
        LOG.error(CANNOT_MAKE + hullModId + " a phantom hull mod: the game already created its effect; "
                + "nodes will not place it.");
    }

    private static HullModEffect createdEffect(HullModSpecAPI spec) {
        try {
            return spec.getEffect();
        } catch (RuntimeException e) {
            LOG.error(LOG_PREFIX + "The game failed to create the phantom effect for " + spec.getId(), e);
            return null;
        }
    }

    public static boolean isActive(String hullModId) {
        return PhantomHullModStatus.isActive(hullModId);
    }

    public static HullModEffect vanillaEffect(HullModEffect effect) {
        return effect instanceof PhantomHullModEffect phantom ? phantom.original() : effect;
    }

    static HullModEffect createOriginal(String hullModId) {
        String originalClass = ORIGINAL_EFFECT_CLASSES.get(hullModId);
        return originalClass == null ? null : instantiate(originalClass);
    }

    private static HullModEffect instantiate(String className) {
        try {
            Class<?> type = Global.getSettings().getScriptClassLoader().loadClass(className);
            Object effect = MethodHandles.publicLookup().findConstructor(type, MethodType.methodType(void.class)).invoke();
            return effect instanceof HullModEffect hullModEffect ? hullModEffect : null;
        } catch (Throwable e) {
            LOG.error(LOG_PREFIX + "Could not create hull mod effect " + className, e);
            return null;
        }
    }

    static String providingNodeName(ShipAPI ship, String hullModId) {
        if (ship == null) return null;
        ShipSkillData data = SkillDataResolver.resolve(ship.getFleetMember(), ship.getVariant());
        if (data == null) return null;
        for (AllocatedNode allocated : AllocatedNode.of(data)) {
            if (allocated.effectiveType().getPhantomHullModIds().contains(hullModId)) {
                return allocated.effectiveType().getDisplayName();
            }
        }
        return null;
    }

    static void clearForTests() {
        ORIGINAL_EFFECT_CLASSES.clear();
        PhantomHullModStatus.clear();
    }
}
