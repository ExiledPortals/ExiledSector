package exiledsector.harness;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

record NewGameSetup(List<String> choices, String characterName) {

    static final String DEFAULT_CHARACTER_NAME = "Harness";
    static final List<String> DEFAULT_CHOICES = List.of("ngcDevStart");

    static NewGameSetup parse(JSONObject newGame) throws JSONException {
        JSONArray choiceArray = newGame.optJSONArray("choices");
        List<String> choices = new ArrayList<>();
        if (choiceArray == null) {
            choices.addAll(DEFAULT_CHOICES);
        } else {
            for (int index = 0; index < choiceArray.length(); index++) {
                choices.add(choiceArray.getString(index));
            }
        }
        if (choices.isEmpty()) {
            throw new IllegalArgumentException("newGame needs at least one choice, such as \"ngcDevStart\"");
        }
        String characterName = newGame.optString("characterName", DEFAULT_CHARACTER_NAME);
        if (characterName.isBlank()) {
            throw new IllegalArgumentException("newGame needs a characterName");
        }
        return new NewGameSetup(List.copyOf(choices), characterName);
    }
}
