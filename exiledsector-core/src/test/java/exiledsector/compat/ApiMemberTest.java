package exiledsector.compat;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ApiMemberTest {

    @Test
    void anInstanceGetterResolvesAPublicFieldAndReportsAMissingOne() {
        ApiMember present = ApiMember.instanceGetter("java.awt.Point", "x", ApiMember.INT);
        ApiMember missing = ApiMember.instanceGetter("java.awt.Point", "z", ApiMember.INT);

        assertEquals(List.of(missing), ApiMember.missingFrom(List.of(present, missing), ApiMemberTest.class.getClassLoader()));
    }

    @Test
    void aStaticSetterCountsAFinalFieldAsMissing() {
        ApiMember finalField = ApiMember.staticSetter("java.lang.Integer", "MAX_VALUE", ApiMember.INT);

        assertEquals(List.of(finalField), ApiMember.missingFrom(List.of(finalField), ApiMemberTest.class.getClassLoader()));
    }
}
