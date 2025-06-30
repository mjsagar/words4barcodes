package com.example.barcodeconverter.model;

import java.util.Objects;

public class BarcodeSegmentRule {
    private int order;             // The order of the segment in the barcode sequence
    private int length;            // The character length of this segment in the barcode
    private SegmentType type;      // The type of data this segment represents (NUMERIC, BASE64, STATIC)
    private String staticValue;    // The actual static value if the type is STATIC (null otherwise)
    private boolean mapsToWord;    // If true and type is NUMERIC, this segment's value is an index into the word list

    // Constructors
    public BarcodeSegmentRule() {
    }

    public BarcodeSegmentRule(int order, int length, SegmentType type, String staticValue, boolean mapsToWord) {
        this.order = order;
        this.length = length;
        this.type = type;
        this.staticValue = staticValue;
        this.mapsToWord = mapsToWord;

        if (type == SegmentType.STATIC && (staticValue == null || staticValue.isEmpty())) {
            throw new IllegalArgumentException("Static value cannot be null or empty for STATIC segment type.");
        }
        if (type == SegmentType.STATIC && staticValue.length() != length) {
            throw new IllegalArgumentException("Length of staticValue must match the segment length for STATIC type.");
        }
        if (mapsToWord && type != SegmentType.NUMERIC) {
            throw new IllegalArgumentException("Only NUMERIC segments can be mapped to a word.");
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
        this.staticValue = staticValue;
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
               Objects.equals(staticValue, that.staticValue);
    }

    @Override
    public int hashCode() {
        return Objects.hash(order, length, type, staticValue, mapsToWord);
    }

    @Override
    public String toString() {
        return "BarcodeSegmentRule{" +
               "order=" + order +
               ", length=" + length +
               ", type=" + type +
               (staticValue != null ? ", staticValue='" + staticValue + '\'' : "") +
               ", mapsToWord=" + mapsToWord +
               '}';
    }
}
