package com.example.barcodeconverter.model;

import java.util.List;
import java.util.Objects;
import java.util.ArrayList;


public class BarcodeSegmentRule {
    private int order;             // The order of the segment in the barcode sequence
    private int length;            // The character length of this segment in the barcode
    private SegmentType type;      // The type of data this segment represents (NUMERIC, BASE64, STATIC, STATIC_OR)
    private String staticValue;    // The actual static value if the type is STATIC (null otherwise)
    private List<String> staticOrValues; // Possible static values if type is STATIC_OR
    private boolean mapsToWord;    // If true and type is NUMERIC, this segment's value is an index into the word list

    // Constructors
    public BarcodeSegmentRule() {
        // Default constructor for frameworks like Jackson
        this.staticOrValues = new ArrayList<>(); // Initialize to avoid null pointer issues
    }

    // Constructor for STATIC, NUMERIC, BASE64 types
    public BarcodeSegmentRule(int order, int length, SegmentType type, String staticValue, boolean mapsToWord) {
        this(order, length, type, staticValue, null, mapsToWord);
    }

    // Constructor for STATIC_OR type
    public BarcodeSegmentRule(int order, int length, SegmentType type, List<String> staticOrValues, boolean mapsToWord) {
        this(order, length, type, null, staticOrValues, mapsToWord);
    }

    // Master constructor
    public BarcodeSegmentRule(int order, int length, SegmentType type, String staticValue, List<String> staticOrValues, boolean mapsToWord) {
        this.order = order;
        this.length = length;
        this.type = type;
        this.mapsToWord = mapsToWord;

        if (mapsToWord && type != SegmentType.NUMERIC) {
            throw new IllegalArgumentException("Only NUMERIC segments can be mapped to a word. Rule order: " + order);
        }

        if (type == SegmentType.STATIC) {
            if (staticValue == null || staticValue.isEmpty()) {
                throw new IllegalArgumentException("Static value cannot be null or empty for STATIC segment type. Rule order: " + order);
            }
            if (staticValue.length() != length) {
                throw new IllegalArgumentException("Length of staticValue ('" + staticValue + "', length " + staticValue.length() +
                        ") must match the segment length (" + length + ") for STATIC type. Rule order: " + order);
            }
            this.staticValue = staticValue;
            this.staticOrValues = null; // Ensure other type's value field is null
        } else if (type == SegmentType.STATIC_OR) {
            if (staticOrValues == null || staticOrValues.isEmpty()) {
                throw new IllegalArgumentException("staticOrValues cannot be null or empty for STATIC_OR segment type. Rule order: " + order);
            }
            for (String val : staticOrValues) {
                if (val == null || val.isEmpty()) {
                    throw new IllegalArgumentException("Values in staticOrValues cannot be null or empty. Rule order: " + order);
                }
                if (val.length() != length) {
                    throw new IllegalArgumentException("Length of each value in staticOrValues ('" + val + "', length " + val.length() +
                            ") must match the segment length (" + length + ") for STATIC_OR type. Rule order: " + order);
                }
            }
            this.staticOrValues = new ArrayList<>(staticOrValues);
            this.staticValue = null; // Ensure other type's value field is null
        } else {
            // For NUMERIC, BASE64 or other future types not requiring specific value validation here
            this.staticValue = null;
            this.staticOrValues = null;
        }
    }


    // Getters
    public int getOrder() {
        return order;
    }

    public int getLength() {
        return length;
    }

    public SegmentType getType() {
        return type;
    }

    public String getStaticValue() {
        return staticValue;
    }

    public List<String> getStaticOrValues() {
        return staticOrValues;
    }

    public boolean isMapsToWord() {
        return mapsToWord;
    }

    // Setters
    public void setOrder(int order) {
        this.order = order;
    }

    public void setLength(int length) {
        this.length = length;
    }

    public void setType(SegmentType type) {
        this.type = type;
    }

    public void setStaticValue(String staticValue) {
        // This setter should be used carefully, ideally only by deserialization frameworks
        // or when type is already STATIC. Validation should be re-triggered if type changes.
        this.staticValue = staticValue;
    }

    public void setStaticOrValues(List<String> staticOrValues) {
        // Similar to setStaticValue, for deserialization or careful manual setup.
        this.staticOrValues = (staticOrValues != null) ? new ArrayList<>(staticOrValues) : null;
    }

    public void setMapsToWord(boolean mapsToWord) {
        this.mapsToWord = mapsToWord;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        BarcodeSegmentRule that = (BarcodeSegmentRule) o;
        return order == that.order &&
               length == that.length &&
               mapsToWord == that.mapsToWord &&
               type == that.type &&
               Objects.equals(staticValue, that.staticValue) &&
               Objects.equals(staticOrValues, that.staticOrValues);
    }

    @Override
    public int hashCode() {
        return Objects.hash(order, length, type, staticValue, staticOrValues, mapsToWord);
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder("BarcodeSegmentRule{");
        sb.append("order=").append(order);
        sb.append(", length=").append(length);
        sb.append(", type=").append(type);
        if (staticValue != null) {
            sb.append(", staticValue='").append(staticValue).append('\'');
        }
        if (staticOrValues != null && !staticOrValues.isEmpty()) {
            sb.append(", staticOrValues=").append(staticOrValues);
        }
        sb.append(", mapsToWord=").append(mapsToWord);
        sb.append('}');
        return sb.toString();
    }
}
