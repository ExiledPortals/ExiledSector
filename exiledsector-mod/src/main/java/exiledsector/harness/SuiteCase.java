package exiledsector.harness;

import java.util.List;

record SuiteCase(String id, String groupId, List<String> tags, long seed) {

    static String fullId(String groupId, String caseId) {
        return groupId + "/" + caseId;
    }

    static long defaultSeed(String fullId) {
        return fullId.hashCode() & 0xFFFFFFFFL;
    }

    record Filter(String groupId, String caseId, String tag, String startAfter) {

        static final Filter ALL = new Filter(null, null, null, null);
        static final String GROUP_PROPERTY = "exiledsector.harness.group";
        static final String CASE_PROPERTY = "exiledsector.harness.case";
        static final String TAG_PROPERTY = "exiledsector.harness.tag";
        static final String START_AFTER_PROPERTY = "exiledsector.harness.startAfter";

        static Filter fromSystemProperties() {
            return new Filter(property(GROUP_PROPERTY), property(CASE_PROPERTY), property(TAG_PROPERTY), property(START_AFTER_PROPERTY));
        }

        private static String property(String name) {
            String value = System.getProperty(name);
            return value == null || value.isBlank() ? null : value.trim();
        }

        boolean selects(SuiteCase suiteCase) {
            return (groupId == null || groupId.equals(suiteCase.groupId()))
                    && (caseId == null || caseId.equals(suiteCase.id()))
                    && (tag == null || suiteCase.tags().contains(tag));
        }
    }
}
