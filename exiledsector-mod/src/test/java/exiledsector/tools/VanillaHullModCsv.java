package exiledsector.tools;

import com.fs.starfarer.api.combat.HullModEffect;
import exiledsector.skills.VanillaHullModLines;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

final class VanillaHullModCsv implements VanillaHullModLines.Source {

    private final Map<String, Map<String, String>> rowsById = new HashMap<>();
    private final Map<String, HullModEffect> effectsById = new HashMap<>();

    private VanillaHullModCsv(List<List<String>> rows) {
        if (rows.isEmpty()) {
            return;
        }
        List<String> header = rows.get(0);
        for (List<String> row : rows.subList(1, rows.size())) {
            Map<String, String> fields = new HashMap<>();
            for (int column = 0; column < header.size() && column < row.size(); column++) {
                fields.put(header.get(column), row.get(column));
            }
            rowsById.put(fields.getOrDefault("id", ""), fields);
        }
    }

    static VanillaHullModCsv read(Path vanillaCoreDir) throws IOException {
        return new VanillaHullModCsv(parse(Files.readString(vanillaCoreDir.resolve("data/hullmods/hull_mods.csv"), StandardCharsets.UTF_8)));
    }

    static List<List<String>> parse(String csv) {
        List<List<String>> rows = new ArrayList<>();
        List<String> row = new ArrayList<>();
        StringBuilder field = new StringBuilder();
        boolean quoted = false;
        for (int index = 0; index < csv.length(); index++) {
            char character = csv.charAt(index);
            if (quoted) {
                if (character != '"') {
                    field.append(character);
                } else if (index + 1 < csv.length() && csv.charAt(index + 1) == '"') {
                    field.append('"');
                    index++;
                } else {
                    quoted = false;
                }
            } else if (character == '"') {
                quoted = true;
            } else if (character == ',') {
                row.add(field.toString());
                field.setLength(0);
            } else if (character == '\n') {
                row.add(field.toString());
                field.setLength(0);
                rows.add(row);
                row = new ArrayList<>();
            } else if (character != '\r') {
                field.append(character);
            }
        }
        if (field.length() > 0 || !row.isEmpty()) {
            row.add(field.toString());
            rows.add(row);
        }
        return rows;
    }

    @Override
    public String descriptionFormat(String hullModId) {
        Map<String, String> row = rowsById.get(hullModId);
        return row == null ? null : row.get("desc");
    }

    @Override
    public HullModEffect effect(String hullModId) {
        return effectsById.computeIfAbsent(hullModId, this::createEffect);
    }

    private HullModEffect createEffect(String hullModId) {
        Map<String, String> row = rowsById.get(hullModId);
        String script = row == null ? "" : row.getOrDefault("script", "");
        try {
            return script.isEmpty() ? null : (HullModEffect) Class.forName(script).getDeclaredConstructor().newInstance();
        } catch (ReflectiveOperationException | LinkageError | ClassCastException e) {
            return null;
        }
    }
}
