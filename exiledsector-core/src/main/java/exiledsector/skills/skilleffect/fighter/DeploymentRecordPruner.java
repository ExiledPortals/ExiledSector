package exiledsector.skills.skilleffect.fighter;

import com.fs.starfarer.api.combat.CombatFleetManagerAPI;
import com.fs.starfarer.api.combat.DeployedFleetMemberAPI;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import org.apache.log4j.Logger;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.util.List;

final class DeploymentRecordPruner {

    static final String ALL_EVER_DEPLOYED_GETTER = "getAllEverDeployed";
    private static final MethodType LIST_GETTER = MethodType.methodType(List.class);
    private static final Logger LOG = Logger.getLogger(DeploymentRecordPruner.class);
    private static Class<?> resolvedFleetManagerClass;
    private static MethodHandle allEverDeployedGetter;
    private static boolean failureReported;

    private DeploymentRecordPruner() {
    }

    // java:S1181: MethodHandle.invoke declares Throwable, and a changed engine may only cost the pruning, never the battle
    @SuppressWarnings("java:S1181")
    static boolean forget(CombatFleetManagerAPI fleetManager, FleetMemberAPI temporaryMember) {
        MethodHandle getter = allEverDeployedGetterFor(fleetManager.getClass());
        if (getter == null) {
            return false;
        }
        try {
            if (getter.invoke(fleetManager) instanceof List<?> deploymentRecord) {
                return deploymentRecord.removeIf(entry -> entry instanceof DeployedFleetMemberAPI deployed && deployed.getMember() == temporaryMember);
            }
            reportFailure("its deployment record is no longer a list", null);
        } catch (Throwable e) {
            reportFailure("reading its deployment record failed", e);
        }
        return false;
    }

    private static MethodHandle allEverDeployedGetterFor(Class<?> fleetManagerClass) {
        if (fleetManagerClass != resolvedFleetManagerClass) {
            resolvedFleetManagerClass = fleetManagerClass;
            allEverDeployedGetter = resolveGetter(fleetManagerClass);
            if (allEverDeployedGetter == null) {
                reportFailure(fleetManagerClass.getName() + " has no public " + ALL_EVER_DEPLOYED_GETTER + "()", null);
            }
        }
        return allEverDeployedGetter;
    }

    private static MethodHandle resolveGetter(Class<?> fleetManagerClass) {
        MethodHandles.Lookup lookup = MethodHandles.publicLookup();
        for (Class<?> candidate = fleetManagerClass; candidate != null; candidate = candidate.getSuperclass()) {
            try {
                return lookup.findVirtual(candidate, ALL_EVER_DEPLOYED_GETTER, LIST_GETTER);
            } catch (ReflectiveOperationException | LinkageError | SecurityException e) {
                LOG.debug("No reachable " + ALL_EVER_DEPLOYED_GETTER + "() on " + candidate.getName(), e);
            }
        }
        return null;
    }

    private static void reportFailure(String reason, Throwable cause) {
        if (failureReported) {
            return;
        }
        failureReported = true;
        LOG.warn("Coalition Flight Doctrine can't tidy the fleet manager (" + reason + "); its copy fighters' temporary wings will stay in memory until the battle ends",
                cause);
    }
}
