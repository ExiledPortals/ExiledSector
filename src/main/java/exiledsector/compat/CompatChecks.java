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

    public enum Level { INFO, WARN }

    public record Finding(Level level, String message) {
    }

    private CompatChecks() {
    }

    public static List<CompatTarget> targets() {
        return List.of(LostSectorCompat.TARGET, SecondInCommandCompat.TARGET, MagicLibCompat.TARGET);
    }

    public static void logAtStartup() {
        SettingsAPI settings = Global.getSettings();
        ModManagerAPI mods = settings == null ? null : settings.getModManager();
        if (mods == null) return;

        for (CompatTarget target : targets()) {
            for (Finding finding : check(target, modId -> mods.isModEnabled(modId) ? versionOf(mods, modId) : null)) {
                if (finding.level() == Level.WARN) {
                    LOG.warn(finding.message());
                } else {
                    LOG.info(finding.message());
                }
            }
        }
    }

    static List<Finding> check(CompatTarget target, Function<String, String> installedVersion) {
        String version = null;
        for (String modId : target.modIds()) {
            version = installedVersion.apply(modId);
            if (version != null) break;
        }
        if (version == null) {
            return List.of(new Finding(Level.INFO, PREFIX + target.name() + " isn't installed, so its compatibility code stays idle."));
        }
        List<Finding> findings = new ArrayList<>();
        if (!target.testedVersions().contains(version)) {
            findings.add(new Finding(Level.WARN, PREFIX + target.name() + " " + version + " is installed, but Exiled Sector's "
                    + "compatibility was last checked against " + String.join(" and ", target.testedVersions())
                    + ". If " + target.name() + " features stop working with Exiled Sector, start here."));
        }
        for (String missing : missingFeatures(target)) {
            findings.add(new Finding(Level.WARN, PREFIX + target.name() + " " + version + " no longer provides " + missing
                    + ", so the compatibility that relies on it won't work."));
        }
        if (findings.isEmpty()) {
            findings.add(new Finding(Level.INFO, PREFIX + target.name() + " " + version + " matches the version Exiled Sector "
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

    private static String versionOf(ModManagerAPI mods, String modId) {
        ModSpecAPI spec = mods.getModSpec(modId);
        String version = spec == null ? null : spec.getVersion();
        return version == null || version.isBlank() ? UNKNOWN_VERSION : version;
    }
}
