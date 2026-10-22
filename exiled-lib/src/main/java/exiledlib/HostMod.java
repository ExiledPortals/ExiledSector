package exiledlib;

import java.util.List;

public record HostMod(String modId, String configDirectory, List<String> stringsDirectories, String keyPrefix) {

    private static volatile HostMod registered;

    public HostMod {
        stringsDirectories = List.copyOf(stringsDirectories);
    }

    public static HostMod register(HostMod hostMod) {
        registered = hostMod;
        return hostMod;
    }

    public static HostMod get() {
        HostMod hostMod = registered;
        if (hostMod == null) {
            throw new IllegalStateException("No mod has registered with the library");
        }
        return hostMod;
    }

    public static String id() {
        return get().modId();
    }

    public static String configPath(String fileName) {
        return get().configDirectory() + fileName;
    }

    public static List<String> stringsPaths(String fileName) {
        return get().stringsDirectories().stream().map(directory -> directory + fileName).toList();
    }

    public static String settingId(String name) {
        return get().keyPrefix() + name;
    }

    public static String dataKey(String name) {
        return get().keyPrefix() + name;
    }
}
