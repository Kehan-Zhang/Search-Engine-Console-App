package com.project;

import java.util.*;

public class Main {

    public static void main(String[] args) {
//        // Part1 entrance
//        Scanner scanner = new Scanner(System.in);
//        int n = scanner.nextInt();
//        Spider spider = new Spider();
//        spider.search(75, "http://freemanmoore.net");

        // Part2 entrance
        Spider spider = new Spider();
        spider.search(75, "http://freemanmoore.net");

        HashMap<String, HashMap<Integer, ArrayList>> dictionary = spider.getDictionary();
        HashMap<String, int[]> matrix = spider.getMatrix();
        List<PageDetail> pageDetails = spider.getPageDetails();
        List<String> pageIndexed = spider.getPageIndexed();
        List<PageInfo> pageInfo = spider.getPageInfo();

        System.out.println("Index " + pageIndexed.size() + " documents. Stemmed dictionary has " + dictionary.size() + " words");

        int count = 0;
        Scanner scanner = new Scanner(System.in);
        Query query = new Query();

        double[][] documentNormalization = query.normalizeDocument(matrix, pageIndexed.size());

        System.out.print("Query: ");
        String input = scanner.nextLine();
        String expansion = query.thesaurus(input);
        String queries[] = expansion.split("\\s+");
        if (expansion.equals("stop")) {
            System.out.println(count + " queries processed");
            return;
        }

        if (query.searchForExistence(queries, dictionary)) {
            double[] queryNormalization = query.normalizeQuery(matrix, queries);
            double[] cosine = query.cosine(queryNormalization, documentNormalization);
            double[] score = query.score(cosine, queries, pageInfo, dictionary);
            int matches = 0;
            for (int i = 0; i < score.length; i++) {
                if (score[i] > 0) {
                    matches++;
                }
            }

            if (matches >= 5) {
                System.out.println(matches + " documents match, displaying top K=5");
                matches = 5;
            } else {
                System.out.println(matches + " documents match, displaying top K=" + matches);
            }

            for (int i = 0; i < matches; i++) {
                double max = 0;
                int pos = 0;
                for (int j = 0; j < score.length; j++) {
                    if (score[j] > max) {
                        max = score[j];
                        pos = j;
                    }
                }
                System.out.println("score:" + String.format("%.4f", score[pos]));
                System.out.println("url: " + pageInfo.get(pos).getUrl());
                if (pageInfo.get(pos).getTitle() != null) {
                    System.out.println("title: " + pageInfo.get(pos).getTitle());
                } else {
                    System.out.println("title: ");
                }
                System.out.println("snippet: " + pageInfo.get(pos).getFragment() + "..." + "\n");
                score[pos] = 0;
            }
        } else {
            System.out.println("No match.\n");
        }

        while (!input.equals("stop")) {
            count++;
            System.out.print("Query: ");
            input = scanner.nextLine();
            expansion = query.thesaurus(input);
            queries = expansion.split("\\s+");
            if (query.searchForExistence(queries, dictionary)) {
                double[] queryNormalization = query.normalizeQuery(matrix, queries);
                double[] cosine = query.cosine(queryNormalization, documentNormalization);
                double[] score = query.score(cosine, queries, pageInfo, dictionary);
                int matches = 0;
                for (int i = 0; i < score.length; i++) {
                    if (score[i] > 0) {
                        matches++;
                    }
                }

                if (matches >= 5) {
                    System.out.println(matches + " documents match, displaying top K=5");
                    matches = 5;
                } else {
                    System.out.println(matches + " documents match, displaying top K=" + matches);
                }

                for (int i = 0; i < matches; i++) {
                    double max = 0;
                    int pos = 0;
                    for (int j = 0; j < score.length; j++) {
                        if (score[j] > max) {
                            max = score[j];
                            pos = j;
                        }
                    }
                    System.out.println("score:" + String.format("%.4f", score[pos]));
                    System.out.println("url: " + pageInfo.get(pos).getUrl());
                    if (pageInfo.get(pos).getTitle() != null) {
                        System.out.println("title: " + pageInfo.get(pos).getTitle());
                    } else {
                        System.out.println("title: ");
                    }
                    System.out.println("snippet: " + pageInfo.get(pos).getFragment() + "..." + "\n");
                    score[pos] = 0;
                }
            } else {
                if (!expansion.equals("stop")) {
                    System.out.println("No match.\n");
                }
            }

        }
        System.out.println(count + " queries processed");
    }
}
