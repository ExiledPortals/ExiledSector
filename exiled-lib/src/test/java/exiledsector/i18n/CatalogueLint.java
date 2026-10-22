package exiledsector.i18n;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Pattern;

final class CatalogueLint {

    private static final String ONE_SUFFIX = ".one";
    private static final String OTHER_SUFFIX = ".other";
    private static final Pattern UNSPACED_SU = Pattern.compile("[0-9}]su(?![A-Za-z])");

    private CatalogueLint() {
    }

    static List<String> englishProblems(Map<String, String> english) {
        List<String> problems = new ArrayList<>();
        for (Map.Entry<String, String> entry : english.entrySet()) {
            String key = entry.getKey();
            checkValue(problems, "en", key, entry.getValue());
            if (key.endsWith(ONE_SUFFIX) && !english.containsKey(base(key) + OTHER_SUFFIX)) {
                problems.add("en " + key + ": has no matching " + base(key) + OTHER_SUFFIX);
            }
        }
        return problems;
    }

    static List<String> translationProblems(String locale, Map<String, String> english, Map<String, String> translated,
                                            Map<String, String> dataSources) {
        List<String> problems = new ArrayList<>();
        for (Map.Entry<String, String> entry : translated.entrySet()) {
            String key = entry.getKey();
            String value = entry.getValue();
            checkValue(problems, locale, key, value);
            if (key.startsWith(Catalogue.METADATA_PREFIX)) {
                continue;
            }
            String source = source(key, english, dataSources);
            if (source == null) {
                problems.add(locale + " " + key + ": no English string or data text with this key");
                continue;
            }
            Template expected = Template.parse(source);
            Template actual = Template.parse(value);
            boolean placeholdersMatch = key.endsWith(ONE_SUFFIX)
                    ? expected.placeholders().containsAll(actual.placeholders())
                    : expected.placeholders().equals(actual.placeholders());
            if (!placeholdersMatch) {
                problems.add(locale + " " + key + ": placeholders " + actual.placeholders()
                        + " differ from English " + expected.placeholders());
            }
            if (!sorted(expected.tagSequence()).equals(sorted(actual.tagSequence()))) {
                problems.add(locale + " " + key + ": highlight tags " + actual.tagSequence()
                        + " differ from English " + expected.tagSequence());
            }
        }
        return problems;
    }

    static Set<String> missing(Map<String, String> english, Map<String, String> translated, Map<String, String> dataSources) {
        Set<String> missing = new TreeSet<>();
        for (String key : english.keySet()) {
            if (!translated.containsKey(key) && !(key.endsWith(ONE_SUFFIX) && translated.containsKey(base(key) + OTHER_SUFFIX))) {
                missing.add(key);
            }
        }
        for (String key : dataSources.keySet()) {
            if (!translated.containsKey(key)) {
                missing.add(key);
            }
        }
        return missing;
    }

    static List<String> rawFileProblems(String name, String content) {
        List<String> problems = new ArrayList<>();
        if (content.startsWith("﻿")) {
            problems.add(name + ": starts with a byte order mark");
        }
        if (content.indexOf('#') >= 0) {
            problems.add(name + ": contains '#', which Starsector's JSON loader treats as a comment");
        }
        return problems;
    }

    private static void checkValue(List<String> problems, String locale, String key, String value) {
        if (!Template.parse(value).hasBalancedTags()) {
            problems.add(locale + " " + key + ": unbalanced or nested highlight tags");
        }
        if (key.startsWith("settings.") && value.indexOf('%') >= 0) {
            problems.add(locale + " " + key + ": LunaLib runs settings text through String.format, so it must not contain '%'");
        }
        if (UNSPACED_SU.matcher(value).find()) {
            problems.add(locale + " " + key + ": write a space before 'su', as vanilla does ('1000 su')");
        }
    }

    private static String source(String key, Map<String, String> english, Map<String, String> dataSources) {
        String source = english.get(key);
        if (source == null && (key.endsWith(ONE_SUFFIX) || key.endsWith(OTHER_SUFFIX))) {
            source = english.get(base(key) + OTHER_SUFFIX);
        }
        return source != null ? source : dataSources.get(key);
    }

    private static String base(String key) {
        return key.substring(0, key.lastIndexOf('.'));
    }

    private static List<String> sorted(List<String> tags) {
        List<String> copy = new ArrayList<>(tags);
        copy.sort(null);
        return copy;
    }
}
