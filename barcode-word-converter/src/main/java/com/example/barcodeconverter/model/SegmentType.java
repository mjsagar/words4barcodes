package com.example.barcodeconverter.model;

public enum SegmentType {
    NUMERIC,    // Represents a numeric segment
    BASE64,     // Represents a Base64 encoded segment
    STATIC,     // Represents a fixed, static string segment
    STATIC_OR   // Represents a segment that can match one of several static values
}
