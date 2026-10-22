package exiledlib.i18n;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.ModManagerAPI;
import com.fs.starfarer.api.SettingsAPI;
import com.fs.starfarer.api.campaign.econ.CommoditySpecAPI;
import com.fs.starfarer.api.loading.HullModSpecAPI;
import exiledlib.ProjectFiles;
import lunalib.lunaSettings.LunaSettings;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.function.BooleanSupplier;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public final class HeadlessGameText implements AutoCloseable {

    public static final Path HULL_MOD_NAMES_FILE = Path.of("exiled-lib/src/test/resources/i18n/hullmod_names.csv");

    private final MockedStatic<LunaSettings> lunaSettingsMock;
    private final MockedStatic<Global> globalMock;
    private final Set<String> unnamedHullMods = new TreeSet<>();

    private HeadlessGameText(BooleanSupplier secondInCommandEnabled) throws IOException {
        lunaSettingsMock = Mockito.mockStatic(LunaSettings.class, invocation -> null);
        SettingsAPI settings = mock(SettingsAPI.class);
        globalMock = Mockito.mockStatic(Global.class);
        globalMock.when(Global::getSettings).thenReturn(settings);
        Map<String, HullModSpecAPI> hullMods = namedHullModSpecs();
        when(settings.getHullModSpec(Mockito.anyString())).thenAnswer(invocation -> {
            String id = invocation.getArgument(0);
            if (!hullMods.containsKey(id)) {
                unnamedHullMods.add(id);
            }
            return hullMods.get(id);
        });
        Map<String, CommoditySpecAPI> commodities = new HashMap<>();
        when(settings.getCommoditySpec(Mockito.anyString())).thenAnswer(invocation -> commodities.computeIfAbsent(invocation.getArgument(0),
                HeadlessGameText::commodityNamed));
        ModManagerAPI modManager = mock(ModManagerAPI.class);
        when(settings.getModManager()).thenReturn(modManager);
        when(modManager.isModEnabled(Mockito.anyString())).thenAnswer(invocation -> secondInCommandEnabled.getAsBoolean());
    }

    private static CommoditySpecAPI commodityNamed(String commodityId) {
        CommoditySpecAPI commodity = mock(CommoditySpecAPI.class);
        when(commodity.getName()).thenReturn(commodityId);
        return commodity;
    }

    public static HeadlessGameText install(BooleanSupplier secondInCommandEnabled) throws IOException {
        return new HeadlessGameText(secondInCommandEnabled);
    }

    public Set<String> unnamedHullMods() {
        return unnamedHullMods;
    }

    private static Map<String, HullModSpecAPI> namedHullModSpecs() throws IOException {
        Map<String, HullModSpecAPI> specs = new HashMap<>();
        List<String> rows = Files.readAllLines(ProjectFiles.projectRoot().resolve(HULL_MOD_NAMES_FILE), StandardCharsets.UTF_8);
        for (String row : rows.subList(1, rows.size())) {
            int comma = row.indexOf(',');
            HullModSpecAPI spec = mock(HullModSpecAPI.class);
            when(spec.getDisplayName()).thenReturn(row.substring(comma + 1));
            specs.put(row.substring(0, comma), spec);
        }
        return specs;
    }

    @Override
    public void close() {
        globalMock.close();
        lunaSettingsMock.close();
    }
}
