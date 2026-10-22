package exiledsector.compat;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.ModManagerAPI;
import com.fs.starfarer.api.ModSpecAPI;
import com.fs.starfarer.api.SettingsAPI;
import org.apache.log4j.Logger;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

public final class CompatChecks {

    private static final Logger LOG = Logger.getLogger(CompatChecks.class);
    private static final String PREFIX = "[ExiledSector] ";
    private static final String UNKNOWN_VERSION = "(unknown version)";
    static final List<ApiMember> ENGINE_MEMBERS = List.of(
            ApiMember.instanceMethod("com.fs.starfarer.combat.CombatFleetManager", "getAllEverDeployed", "java.util.List"));

    public enum Level { INFO, WARN }

    public record Finding(Level level, String message) {
    }

    private CompatChecks() {
    }

    public static List<CompatTarget> coreTargets() {
        List<CompatTarget> targets = new ArrayList<>(List.of(SecondInCommandCompat.TARGET, RandomAssortmentCompat.TARGET, GraphicsLibCompat.TARGET));
        targets.addAll(SalvageSiteCompat.targets());
        return targets;
    }

    public static void logAtStartup(List<CompatTarget> targets) {
        SettingsAPI settings = Global.getSettings();
        ModManagerAPI modManager = settings == null ? null : settings.getModManager();
        if (modManager == null) return;

        for (CompatTarget target : targets) {
            check(target, modId -> modManager.isModEnabled(modId) ? versionOf(modManager, modId) : null).forEach(CompatChecks::log);
        }
        checkEngine(ENGINE_MEMBERS, settings.getScriptClassLoader()).forEach(CompatChecks::log);
    }

    private static void log(Finding finding) {
        if (finding.level() == Level.WARN) {
            LOG.warn(finding.message());
        } else {
            LOG.info(finding.message());
        }
    }

    static List<Finding> checkEngine(List<ApiMember> engineMembers, ClassLoader loader) {
        List<ApiMember> missing = ApiMember.missingFrom(engineMembers, loader);
        if (missing.isEmpty()) {
            return List.of(new Finding(Level.INFO, PREFIX + "Starsector still provides the engine methods Exiled Sector relies on."));
        }
        return missing.stream().map(member -> new Finding(Level.WARN, PREFIX + "This Starsector version no longer provides "
                + member.displayName() + ", so Coalition Flight Doctrine's copy fighters will keep their temporary wings in memory until each battle ends."))
                .toList();
    }

    static List<Finding> check(CompatTarget target, Function<String, String> installedVersion) {
        String installedModVersion = null;
        for (String modId : target.modIds()) {
            installedModVersion = installedVersion.apply(modId);
            if (installedModVersion != null) break;
        }
        if (installedModVersion == null) {
            return List.of(new Finding(Level.INFO, PREFIX + target.displayName() + " isn't installed, so its compatibility code stays idle."));
        }
        List<Finding> findings = new ArrayList<>();
        if (!target.testedVersions().contains(installedModVersion)) {
            findings.add(new Finding(Level.WARN, PREFIX + target.displayName() + " " + installedModVersion + " is installed, but Exiled Sector's "
                    + "compatibility was last checked against " + String.join(" and ", target.testedVersions())
                    + ". If " + target.displayName() + " features stop working with Exiled Sector, start here."));
        }
        for (String missingFeature : missingFeatures(target)) {
            findings.add(new Finding(Level.WARN, PREFIX + target.displayName() + " " + installedModVersion + " no longer provides " + missingFeature
                    + ", so the compatibility that relies on it won't work."));
        }
        if (findings.isEmpty()) {
            findings.add(new Finding(Level.INFO, PREFIX + target.displayName() + " " + installedModVersion + " matches the version Exiled Sector "
                    + "was tested against, and everything Exiled Sector uses from it is present."));
        }
        return findings;
    }

    // java:S1181: a broken or renamed API in another mod can surface as any Throwable here, and the check must report it rather than stop loading
    @SuppressWarnings("java:S1181")
    private static List<String> missingFeatures(CompatTarget target) {
        try {
            return target.missingFeatures().get();
        } catch (Throwable e) {
            return List.of("what Exiled Sector expects (" + e + ")");
        }
    }

    private static String versionOf(ModManagerAPI modManager, String modId) {
        ModSpecAPI modSpec = modManager.getModSpec(modId);
        String modVersion = modSpec == null ? null : modSpec.getVersion();
        return modVersion == null || modVersion.isBlank() ? UNKNOWN_VERSION : modVersion;
    }
}
