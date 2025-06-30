package com.example.barcodeconverter.dto;

import java.util.List;

public class ConversionResponse {
    private String barcode;
    private List<String> words;
    private String status; // e.g., "success", "error"
    private String message; // Optional message, especially for errors

    // Constructors
    public ConversionResponse() {
    }

    public ConversionResponse(String status, String message) {
        this.status = status;
        this.message = message;
    }

    public static ConversionResponse successBarcode(String barcode) {
        ConversionResponse response = new ConversionResponse("success", "Barcode generated successfully.");
        response.setBarcode(barcode);
        return response;
    }

    public static ConversionResponse successWords(List<String> words) {
        ConversionResponse response = new ConversionResponse("success", "Words generated successfully.");
        response.setWords(words);
        return response;
    }

    public static ConversionResponse error(String message) {
        return new ConversionResponse("error", message);
    }

    // Getters and Setters
    public String getBarcode() {
        return barcode;
    }

    public void setBarcode(String barcode) {
        this.barcode = barcode;
    }

    public List<String> getWords() {
        return words;
    }

    public void setWords(List<String> words) {
        this.words = words;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    @Override
    public String toString() {
        return "ConversionResponse{" +
               "barcode='" + barcode + '\'' +
               ", words=" + words +
               ", status='" + status + '\'' +
               ", message='" + message + '\'' +
               '}';
    }
}
