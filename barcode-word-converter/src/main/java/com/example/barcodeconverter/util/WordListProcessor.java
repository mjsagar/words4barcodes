package com.example.barcodeconverter.util;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.InputStream;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;
import java.util.stream.Collectors;

public class WordListProcessor {

    private static final String WORD_LIST_URL = "https://raw.githubusercontent.com/first20hours/google-10000-english/master/google-10000-english-usa-no-swears.txt";
    private static final String OUTPUT_FILE_PATH = "src/main/resources/words.txt";
    private static final int MAX_WORDS = 9999;
    private static final int MIN_WORD_LENGTH = 4; // Words > 3 characters

    public static void main(String[] args) {
        try {
            System.out.println("Starting word list generation...");
            List<String> words = fetchWordList();
            List<String> filteredWords = processWords(words);
            writeWordsToFile(filteredWords);
            System.out.println("Successfully generated word list at: " + OUTPUT_FILE_PATH);
            System.out.println("Total words in the list: " + filteredWords.size());
            if (!filteredWords.isEmpty()) {
                System.out.println("First word: " + filteredWords.get(0));
                System.out.println("Last word: " + filteredWords.get(filteredWords.size() - 1));
            }
        } catch (IOException e) {
            System.err.println("Error generating word list: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private static List<String> fetchWordList() throws IOException {
        System.out.println("Fetching word list from: " + WORD_LIST_URL);
        URL url = new URL(WORD_LIST_URL);
        try (InputStream inputStream = url.openStream();
             BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream))) {
            return reader.lines().collect(Collectors.toList());
        }
    }

    private static List<String> processWords(List<String> words) {
        System.out.println("Processing " + words.size() + " words...");
        List<String> processed = words.stream()
                .map(String::trim)
                .filter(word -> !word.isEmpty())
                .filter(word -> word.length() >= MIN_WORD_LENGTH)
                .distinct() // Ensure uniqueness, though the source list is likely unique
                .limit(MAX_WORDS)
                .collect(Collectors.toList());
        System.out.println("Filtered down to " + processed.size() + " words.");
        return processed;
    }

    private static void writeWordsToFile(List<String> words) throws IOException {
        // Ensure the parent directory exists
        Files.createDirectories(Paths.get(OUTPUT_FILE_PATH).getParent());

        System.out.println("Writing words to file: " + OUTPUT_FILE_PATH);
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(OUTPUT_FILE_PATH))) {
            for (String word : words) {
                writer.write(word);
                writer.newLine();
            }
        }
    }
}
