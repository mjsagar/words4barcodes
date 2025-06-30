package com.example.barcodeconverter.model;

import org.junit.jupiter.api.Test;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

public class BarcodeSegmentRuleTests {

    @Test
    void constructor_validNumericRule() {
        BarcodeSegmentRule rule = new BarcodeSegmentRule(0, 4, SegmentType.NUMERIC, null, true);
        assertEquals(0, rule.getOrder());
        assertEquals(4, rule.getLength());
        assertEquals(SegmentType.NUMERIC, rule.getType());
        assertTrue(rule.isMapsToWord());
        assertNull(rule.getStaticValue());
        assertNull(rule.getStaticOrValues());
    }

    @Test
    void constructor_validStaticRule() {
        BarcodeSegmentRule rule = new BarcodeSegmentRule(1, 3, SegmentType.STATIC, "ABC", false);
        assertEquals(SegmentType.STATIC, rule.getType());
        assertEquals("ABC", rule.getStaticValue());
        assertEquals(3, rule.getLength());
        assertFalse(rule.isMapsToWord());
        assertNull(rule.getStaticOrValues());
    }

    @Test
    void constructor_staticRule_nullValue_throwsException() {
        Exception exception = assertThrows(IllegalArgumentException.class, () -> {
            new BarcodeSegmentRule(1, 3, SegmentType.STATIC, null, false);
        });
        assertTrue(exception.getMessage().contains("Static value cannot be null or empty"));
    }

    @Test
    void constructor_staticRule_emptyValue_throwsException() {
        Exception exception = assertThrows(IllegalArgumentException.class, () -> {
            new BarcodeSegmentRule(1, 3, SegmentType.STATIC, "", false);
        });
        assertTrue(exception.getMessage().contains("Static value cannot be null or empty"));
    }

    @Test
    void constructor_staticRule_lengthMismatch_throwsException() {
        Exception exception = assertThrows(IllegalArgumentException.class, () -> {
            new BarcodeSegmentRule(1, 3, SegmentType.STATIC, "AB", false);
        });
        assertTrue(exception.getMessage().contains("must match the segment length"));
    }

    @Test
    void constructor_validBase64Rule() {
        BarcodeSegmentRule rule = new BarcodeSegmentRule(2, 8, SegmentType.BASE64, null, false);
        assertEquals(SegmentType.BASE64, rule.getType());
        assertEquals(8, rule.getLength());
        assertFalse(rule.isMapsToWord());
        assertNull(rule.getStaticValue());
        assertNull(rule.getStaticOrValues());
    }

    @Test
    void constructor_validStaticOrRule() {
        List<String> values = Arrays.asList("VAL1", "VAL2");
        BarcodeSegmentRule rule = new BarcodeSegmentRule(3, 4, SegmentType.STATIC_OR, values, false);
        assertEquals(SegmentType.STATIC_OR, rule.getType());
        assertEquals(4, rule.getLength());
        assertFalse(rule.isMapsToWord());
        assertNotNull(rule.getStaticOrValues());
        assertEquals(2, rule.getStaticOrValues().size());
        assertTrue(rule.getStaticOrValues().containsAll(values));
        assertNull(rule.getStaticValue());
    }

    @Test
    void constructor_staticOrRule_convenienceConstructor() {
        List<String> values = Arrays.asList("VAL1", "VAL2");
        // Uses the constructor: BarcodeSegmentRule(int order, int length, SegmentType type, List<String> staticOrValues, boolean mapsToWord)
        BarcodeSegmentRule rule = new BarcodeSegmentRule(3, 4, SegmentType.STATIC_OR, values, false);
        assertEquals(SegmentType.STATIC_OR, rule.getType());
        assertEquals(4, rule.getLength());
        assertFalse(rule.isMapsToWord());
        assertNotNull(rule.getStaticOrValues());
        assertEquals(2, rule.getStaticOrValues().size());
        assertTrue(rule.getStaticOrValues().containsAll(values));
        assertNull(rule.getStaticValue());
    }


    @Test
    void constructor_staticOrRule_nullList_throwsException() {
        Exception exception = assertThrows(IllegalArgumentException.class, () -> {
            new BarcodeSegmentRule(3, 4, SegmentType.STATIC_OR, (List<String>) null, false);
        });
        assertTrue(exception.getMessage().contains("staticOrValues cannot be null or empty"));
    }

    @Test
    void constructor_staticOrRule_emptyList_throwsException() {
        Exception exception = assertThrows(IllegalArgumentException.class, () -> {
            new BarcodeSegmentRule(3, 4, SegmentType.STATIC_OR, Collections.emptyList(), false);
        });
        assertTrue(exception.getMessage().contains("staticOrValues cannot be null or empty"));
    }

    @Test
    void constructor_staticOrRule_listWithNullValue_throwsException() {
        List<String> values = Arrays.asList("VAL1", null);
        Exception exception = assertThrows(IllegalArgumentException.class, () -> {
            new BarcodeSegmentRule(3, 4, SegmentType.STATIC_OR, values, false);
        });
        assertTrue(exception.getMessage().contains("Values in staticOrValues cannot be null or empty"));
    }

    @Test
    void constructor_staticOrRule_listWithEmptyValue_throwsException() {
        List<String> values = Arrays.asList("VAL1", "");
        Exception exception = assertThrows(IllegalArgumentException.class, () -> {
            new BarcodeSegmentRule(3, 4, SegmentType.STATIC_OR, values, false);
        });
        assertTrue(exception.getMessage().contains("Values in staticOrValues cannot be null or empty"));
    }

    @Test
    void constructor_staticOrRule_listValueLengthMismatch_throwsException() {
        List<String> values = Arrays.asList("VAL1", "V2"); // V2 has length 2, rule length is 4
        Exception exception = assertThrows(IllegalArgumentException.class, () -> {
            new BarcodeSegmentRule(3, 4, SegmentType.STATIC_OR, values, false);
        });
        assertTrue(exception.getMessage().contains("must match the segment length"));
        assertTrue(exception.getMessage().contains("'V2'"));
    }

    @Test
    void constructor_mapsToWordNotNumeric_throwsException() {
        Exception exception = assertThrows(IllegalArgumentException.class, () -> {
            new BarcodeSegmentRule(0, 4, SegmentType.STATIC, "ABCD", true);
        });
        assertTrue(exception.getMessage().contains("Only NUMERIC segments can be mapped to a word"));
    }

    @Test
    void constructor_staticOrRule_mapsToWord_throwsException() {
        List<String> values = Arrays.asList("VAL1", "VAL2");
        Exception exception = assertThrows(IllegalArgumentException.class, () -> {
            new BarcodeSegmentRule(3, 4, SegmentType.STATIC_OR, values, true);
        });
        assertTrue(exception.getMessage().contains("Only NUMERIC segments can be mapped to a word"));
    }

    @Test
    void constructor_base64Rule_mapsToWord_throwsException() {
        Exception exception = assertThrows(IllegalArgumentException.class, () -> {
            new BarcodeSegmentRule(2, 8, SegmentType.BASE64, null, true);
        });
        assertTrue(exception.getMessage().contains("Only NUMERIC segments can be mapped to a word"));
    }

    @Test
    void equalsAndHashCode_consistentForSameValues() {
        BarcodeSegmentRule rule1 = new BarcodeSegmentRule(0, 4, SegmentType.NUMERIC, null, true);
        BarcodeSegmentRule rule2 = new BarcodeSegmentRule(0, 4, SegmentType.NUMERIC, null, true);
        assertEquals(rule1, rule2);
        assertEquals(rule1.hashCode(), rule2.hashCode());

        BarcodeSegmentRule rule3 = new BarcodeSegmentRule(1, 3, SegmentType.STATIC, "ABC", false);
        BarcodeSegmentRule rule4 = new BarcodeSegmentRule(1, 3, SegmentType.STATIC, "ABC", false);
        assertEquals(rule3, rule4);
        assertEquals(rule3.hashCode(), rule4.hashCode());

        List<String> staticOrValues1 = Arrays.asList("S1", "S2");
        List<String> staticOrValues2 = Arrays.asList("S1", "S2");
        BarcodeSegmentRule rule5 = new BarcodeSegmentRule(2, 2, SegmentType.STATIC_OR, staticOrValues1, false);
        BarcodeSegmentRule rule6 = new BarcodeSegmentRule(2, 2, SegmentType.STATIC_OR, staticOrValues2, false);
        assertEquals(rule5, rule6);
        assertEquals(rule5.hashCode(), rule6.hashCode());
    }

    @Test
    void equals_differentValues_returnsFalse() {
        BarcodeSegmentRule rule1 = new BarcodeSegmentRule(0, 4, SegmentType.NUMERIC, null, true);
        BarcodeSegmentRule rule2 = new BarcodeSegmentRule(1, 4, SegmentType.NUMERIC, null, true); // Different order
        assertNotEquals(rule1, rule2);

        BarcodeSegmentRule rule3 = new BarcodeSegmentRule(1, 3, SegmentType.STATIC, "ABC", false);
        BarcodeSegmentRule rule4 = new BarcodeSegmentRule(1, 3, SegmentType.STATIC, "XXX", false); // Different staticValue
        assertNotEquals(rule3, rule4);

        List<String> staticOrValues1 = Arrays.asList("S1", "S2");
        List<String> staticOrValues2 = Arrays.asList("S3", "S4");
        BarcodeSegmentRule rule5 = new BarcodeSegmentRule(2, 2, SegmentType.STATIC_OR, staticOrValues1, false);
        BarcodeSegmentRule rule6 = new BarcodeSegmentRule(2, 2, SegmentType.STATIC_OR, staticOrValues2, false); // Different staticOrValues
        assertNotEquals(rule5, rule6);

        BarcodeSegmentRule rule7 = new BarcodeSegmentRule(2, 2, SegmentType.STATIC_OR, staticOrValues1, false);
        BarcodeSegmentRule rule8 = new BarcodeSegmentRule(2, 2, SegmentType.STATIC, "S1", false); // Different type
        assertNotEquals(rule7, rule8);
    }

    @Test
    void toString_containsAllFields() {
        BarcodeSegmentRule ruleStatic = new BarcodeSegmentRule(1, 3, SegmentType.STATIC, "VAL", false);
        String strStatic = ruleStatic.toString();
        assertTrue(strStatic.contains("order=1"));
        assertTrue(strStatic.contains("length=3"));
        assertTrue(strStatic.contains("type=STATIC"));
        assertTrue(strStatic.contains("staticValue='VAL'"));
        assertTrue(strStatic.contains("mapsToWord=false"));

        List<String> orValues = Arrays.asList("V1", "V2");
        BarcodeSegmentRule ruleStaticOr = new BarcodeSegmentRule(2, 2, SegmentType.STATIC_OR, orValues, false);
        String strStaticOr = ruleStaticOr.toString();
        assertTrue(strStaticOr.contains("order=2"));
        assertTrue(strStaticOr.contains("length=2"));
        assertTrue(strStaticOr.contains("type=STATIC_OR"));
        assertTrue(strStaticOr.contains("staticOrValues=[V1, V2]"));
        assertTrue(strStaticOr.contains("mapsToWord=false"));
        assertFalse(strStaticOr.contains("staticValue=")); // Should not have staticValue if STATIC_OR
    }

    @Test
    void defaultConstructor_initializesStaticOrValues() {
        BarcodeSegmentRule rule = new BarcodeSegmentRule();
        assertNotNull(rule.getStaticOrValues(), "staticOrValues should be initialized to an empty list, not null.");
        assertTrue(rule.getStaticOrValues().isEmpty(), "staticOrValues should be empty by default.");
    }
}
