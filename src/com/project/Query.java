package com.project;

import org.apache.xmlbeans.impl.schema.XQuerySchemaTypeSystem;
import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;

import java.io.IOException;
import java.util.*;

public class Query {
    double[] documentFrequency = new double[1079];

    public boolean searchForExistence(String queries[], HashMap<String, HashMap<Integer, ArrayList>> dictionary) {
        for (String query: queries) {
            if (dictionary.containsKey(stem(query))) {
                return true;
            }
        }
        return false;
    }

    public double[][] normalizeDocument(HashMap<String, int[]> matrix, int documentNumber) {

        double[][] tfdf = new double[matrix.size() + 1][documentNumber];
        double[][] normalization = new double[matrix.size()][documentNumber];

        int pos = 0;

        Set<Map.Entry<String, int[]>> map = matrix.entrySet();
        for (Map.Entry entry : map) {
            int count = 0;
            int[] frequency = (int[]) entry.getValue();
            for (int i = 0; i < frequency.length; i++) {
                if (frequency[i] != 0) {
                    count++;
                }
            }
            documentFrequency[pos] = Math.log10((double)documentNumber/(double)count);

            for (int i = 0; i < frequency.length; i++) {
                tfdf[pos][i] = documentFrequency[pos] * frequency[i];
            }
            pos++;
        }

        for (int i = 0; i < documentNumber; i++) {
            double sum = 0;
            for (int j = 0; j < matrix.size(); j++) {
                sum += Math.pow(tfdf[j][i], 2);
            }
            tfdf[pos][i] = Math.sqrt(sum);
        }

        for (int i = 0; i < matrix.size(); i++) {
            for (int j = 0; j < documentNumber; j++) {
                normalization[i][j] = tfdf[i][j] / tfdf[matrix.size()][j];
            }
        }

        return normalization;
    }

    public double[] normalizeQuery(HashMap<String, int[]> matrix, String queries[]) {
        String stemmedQueries[] = new String[queries.length];
        double[] tfdf = new double[matrix.size() + 1];
        for (int i = 0; i < queries.length; i++) {
            stemmedQueries[i] = stem(queries[i]);
        }

        int pos = 0;
        double[] termFrequency = new double[matrix.size()];
        Set<Map.Entry<String, int[]>> map = matrix.entrySet();
        for (Map.Entry entry : map) {
            termFrequency[pos] = 0;
            for (int i = 0; i < queries.length; i++) {
                if (entry.getKey().equals(stemmedQueries[i])) {
                    termFrequency[pos]++;
                }
            }
            pos++;
        }

        for (int i = 0; i < matrix.size(); i++) {
            tfdf[i] = termFrequency[i] * documentFrequency[i];
        }

        double sum = 0;
        for (int i = 0; i < matrix.size(); i++) {
            sum += Math.pow(tfdf[i], 2);
        }
        tfdf[matrix.size()] = Math.sqrt(sum);

        double[] normalization = new double[matrix.size()];
        for (int i = 0; i < matrix.size(); i++) {
            normalization[i] = tfdf[i] / tfdf[matrix.size()];
        }

        return normalization;
    }

    public String stem(String word) {
        Stemmer stemmer = new Stemmer();
        char[] stem = word.toCharArray();
        for (int i = 0 ; i < stem.length; i++) {
            stemmer.add(stem[i]);
        }
        stemmer.stem();
        return stemmer.toString();
    }

    public double[] cosine(double[] queryNormalization, double[][] documentNormalization) {
        double[] cosine = new double[documentNormalization[0].length];
        for (int i = 0; i < cosine.length; i++) {
            double sum = 0;
            for (int j = 0; j < queryNormalization.length; j++) {
                sum += queryNormalization[j] * documentNormalization[j][i];
            }
            cosine[i] = sum;
        }
        return cosine;
    }

    public double[] score(double[] cosine,
                          String queries[],
                          List<PageInfo> pageInfo,
                          HashMap<String, HashMap<Integer, ArrayList>> dictionary) {
        double[] score = cosine;

        for (int i = 0; i < pageInfo.size(); i++) {
            for (int j = 0; j < queries.length; j++) {
                boolean flag = false;
                if (pageInfo.get(i).getTitle() != null) {
                    String titles[] = pageInfo.get(i).getTitle().toLowerCase(Locale.ROOT).split("\\s+");
                    for (int k = 0; k < titles.length; k++) {
                        if (titles[k].equals(queries[j])) {
                            score[i] += 0.1;
                            flag = true;
                            break;
                        }
                    }
                }
                if (flag) {
                    break;
                }
            }
        }

        String stemmedQueries[] = new String[queries.length];
        for (int i = 0; i < queries.length; i++) {
            stemmedQueries[i] = stem(queries[i]);
        }

        for (int i = 0; i < score.length; i++) {
            for (int j = 0; j < stemmedQueries.length; j++) {
                if (dictionary.containsKey(stemmedQueries[j]) && dictionary.get(stemmedQueries[j]).containsKey(i + 1)) {
                    if (Integer.parseInt(dictionary.get(stemmedQueries[j]).get(i + 1).get(0).toString()) <= 20) {
                        score[i] += 0.1;
                        break;
                    }
                }
            }
        }

        return score;
    }

    public String thesaurus(String input) {
        String expansion = input.toLowerCase(Locale.ROOT);
        if (expansion.contains("beautiful")) {
            expansion += " nice fancy";
        }

        if (expansion.contains("chapter")) {
            expansion += " chpt";
        }

        if (expansion.contains("chpt")) {
            expansion += " chapter";
        }

        if (expansion.contains("responsible")) {
            expansion += " owner accountable";
        }

        if (expansion.contains("freemanmoore")) {
            expansion += " freeman moore";
        }

        if (expansion.contains("dept")) {
            expansion += " department";
        }

        if (expansion.contains("photo")) {
            expansion += " photograph image picture";
        }

        if (expansion.contains("brown")) {
            expansion += " beige tan auburn";
        }

        if (expansion.contains("tues")) {
            expansion += " Tuesday";
        }

        if (expansion.contains("sole")) {
            expansion += " owner single shoe boot";
        }

        if (expansion.contains("hmwk")) {
            expansion += " homework home work";
        }

        if (expansion.contains("novel")) {
            expansion += " book unique";
        }

        if (expansion.contains("computer")) {
            expansion += " cse";
        }

        if (expansion.contains("story")) {
            expansion += " novel book";
        }

        if (expansion.contains("hocuspocus")) {
            expansion += " magic abracadabra";
        }

        if (expansion.contains("thisworks")) {
            expansion += " this work";
        }

        return expansion;
    }
}
