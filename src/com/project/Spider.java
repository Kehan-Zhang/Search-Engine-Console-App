package com.project;

import org.apache.commons.lang3.StringUtils;
import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.*;

import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;

public class Spider {
    private String root = "http://freemanmoore.net";
    private List<String> pagesVisited = new ArrayList<>();
    private List<String> pagesToVisit = new ArrayList<>();
    private List<String> pagesNotAllowed = new ArrayList<>();
    private List<String> pagesOutside = new ArrayList<>();
    private List<String> pagesBroken = new ArrayList<>();
    private List<String> pagesNotText = new ArrayList<>();
    private List<String> pagesBody = new ArrayList<>();
    private List<String> pagesDuplicateContent = new ArrayList<>();
    private List<String> pagesNoIndexTag = new ArrayList<>();
    private List<String> pageIndexed = new ArrayList<>();
    private List<PageDetail> pageDetails = new ArrayList<>();
    private List<PageInfo> pageInfo = new ArrayList<>();
    private HashMap<String, HashMap<Integer, ArrayList>> dictionary = new HashMap<>();
    private HashMap<String, int[]> matrix = new HashMap<>();

    public List<PageInfo> getPageInfo() {
        return pageInfo;
    }

    public List<String> getPageIndexed() {
        return pageIndexed;
    }

    public List<PageDetail> getPageDetails() {
        return pageDetails;
    }

    public HashMap<String, HashMap<Integer, ArrayList>> getDictionary() {
        return dictionary;
    }

    public HashMap<String, int[]> getMatrix() {
        return matrix;
    }

    // Main crawl function
    public void search(int n, String url) {
        // find disallowed path in robots.txt
        String robotUrl = url + "/robot.txt";
        SpiderLeg robotCrawl = new SpiderLeg();
        robotCrawl.crawl(robotUrl);
        String robotBody = robotCrawl.getBody();
        String removeComment = robotBody.replaceAll("#.*\\r\\n", "");

        int startIndex = removeComment.indexOf("/");
        int endIndex = removeComment.indexOf("#");
        String disAllow = removeComment.substring(startIndex, endIndex - 2);
        pagesNotAllowed.add(url + disAllow);

        // Start to crawl
        this.pagesToVisit.add(url);
        while(this.pagesVisited.size() < n) {
            String currentUrl;
            SpiderLeg leg = new SpiderLeg();
            currentUrl = this.nextUrl();

            // no more pages to crawl
            if (currentUrl.equals("")) {
                break;
            }

            // page is out of this site
            if (!currentUrl.startsWith(root)) {
                if (!pagesOutside.contains(currentUrl)) {
                    pagesOutside.add(currentUrl);
                }
                continue;
            }

            if (currentUrl.contains(pagesNotAllowed.get(0))) {
                continue;
            }

            // page broken
            try {
                HttpURLConnection urlConnection = (HttpURLConnection) new URL(currentUrl).openConnection();
                urlConnection.setRequestMethod("GET");
                urlConnection.connect();
                int responseCode = urlConnection.getResponseCode();
                if (responseCode != 200) {
                    if (!pagesBroken.contains(currentUrl)) {
                        pagesBroken.add(currentUrl);
                        continue;
                    }
                }
            } catch (IOException e) {

            }

            // page is non-text file
            if (!currentUrl.endsWith(".txt")
                && !currentUrl.endsWith(".html")
                && !currentUrl.endsWith(".htm")
                && !currentUrl.endsWith(".php")
                && !currentUrl.endsWith(".net")) {
                if (!pagesNotText.contains(currentUrl)) {
                    pagesNotText.add(currentUrl);
                }
                continue;
            }

            // page has been visited
            if (this.pagesVisited.contains(currentUrl)) {
                continue;
            }

            // crawl a page
            if (!pagesNotAllowed.contains(currentUrl)) {
                Connection connection = Jsoup.connect(currentUrl);
                try {
                    connection.get();
                    leg.crawl(currentUrl);
                    this.pagesVisited.add(currentUrl);
                    this.pagesToVisit.addAll(leg.getLinks());
                } catch (IOException e) {

                }
            }
        }

        getPageDetail();
        generateMatrix();
        getPageBriefInfo();
//        output();
//        print();
    }

    // Return the next url to visit
    private String nextUrl() {
        String nextUrl = "";
        if (!pagesToVisit.isEmpty()) {
            nextUrl = this.pagesToVisit.remove(0);
        }
        return nextUrl;
    }

    private String stem(String word) {
        Stemmer stemmer = new Stemmer();
        char[] stem = word.toCharArray();
        for (int i = 0 ; i < stem.length; i++) {
            stemmer.add(stem[i]);
        }
        stemmer.stem();
        return stemmer.toString();
    }

    // get page brief information(url, title, fragment, score)
    private void getPageBriefInfo() {
        for (int i = 0; i < pageIndexed.size(); i++) {
            String url = pageIndexed.get(i);
            Connection connection = Jsoup.connect(url);
            Document htmlDocument = null;
            try {
                htmlDocument = connection.get();
                PageInfo pageInfo = new PageInfo();
                pageInfo.setUrl(url);
                pageInfo.setTitle(htmlDocument.title());
                String body = htmlDocument.body().text();
                String fragment;
                int pos = StringUtils.ordinalIndexOf(body," ",20);
                if (pos == -1) {
                    fragment = body;
                } else {
                    fragment = body.substring(0, pos);
                }

                pageInfo.setFragment(fragment);
                this.pageInfo.add(pageInfo);

            } catch (IOException e) {

            }
        }
    }

    // get page detail(url, title, date, size) and index page
    private void getPageDetail() {
        for (int i = 0; i < pagesVisited.size(); i++) {
            String url = pagesVisited.get(i);
            Connection connection = Jsoup.connect(url);
            Document htmlDocument = null;
            try {
                htmlDocument = connection.get();
                PageDetail pageDetail = new PageDetail();
                pageDetail.setUrl(url);

                Elements elements = htmlDocument.select("meta[name=robots]");
                if(elements.size() != 0){
                    Element element = elements.get(0);
                    if (element.attr("content").equals("noindex")) {
                        this.pageDetails.add(pageDetail);
                        this.pagesNoIndexTag.add(url);
                        continue;
                    }
                }

                String content = htmlDocument.body().text();
                if (!pagesBody.contains(content)) {
                    pagesBody.add(content);
                } else {
                    pagesDuplicateContent.add(url);
                    continue;
                }

                pageDetail.setTitle(htmlDocument.title());

                Connection.Response response = connection.method(Connection.Method.GET).execute();
                Map<String, String> headers = response.headers();

                String date = headers.get("Date");
                String contentLength = headers.get("Content-Length");
                int size;
                if (contentLength == null) {
                    size = content.length();
                } else {
                    size = Integer.parseInt(contentLength);
                }

                pageDetail.setDate(date);
                pageDetail.setSize(size);

                this.pageDetails.add(pageDetail);
                this.pageIndexed.add(url);

            } catch (IOException e) {

            }
        }
    }

    // generate term-document frequency matrix
    private void generateMatrix() {

        // generate dictionary
        for (int i = 0; i < this.pageIndexed.size(); i++) {
            String url = pageIndexed.get(i);
            Connection connection = Jsoup.connect(url);
            Document htmlDocument = null;
            try {
                htmlDocument = connection.get();
                String body = htmlDocument.body().text();
                String words[] = body.split(" ");
                for (int j = 0; j < words.length; j++) {
                    StringBuilder word = new StringBuilder(words[j]);
                    while (word.length() != 0) {
                        if (!Character.isAlphabetic(word.charAt(0))) {
                            word.deleteCharAt(0);
                        } else {
                            break;
                        }
                    }

                    if (word.length() == 0) {
                        continue;
                    }

                    char lastChar = word.charAt(word.length() - 1);
                    if (Character.isAlphabetic(lastChar) || Character.isDigit(lastChar)) {
                        String key = word.toString().toLowerCase();
                        //String stemmedKey = key;
                        String stemmedKey = stem(key);
                        //System.out.println(stemmedKey);
                        if (this.dictionary.containsKey(stemmedKey)) { // word is already in the dictionary
                            if (this.dictionary.get(stemmedKey).containsKey(i + 1)) { // word is already in this document
                                this.dictionary.get(stemmedKey).get(i + 1).add(j + 1);
                            } else { // word didn't show up in this document yet
                                HashMap<Integer, ArrayList> postingList = new HashMap<>();
                                ArrayList<Integer> position = new ArrayList<>();
                                position.add(j + 1);
                                this.dictionary.get(stemmedKey).put(i + 1, position);
                            }
                        } else { // word is not in the dictionary
                            HashMap<Integer, ArrayList> postingList = new HashMap<>();
                            ArrayList<Integer> position = new ArrayList<>();
                            position.add(j + 1);
                            postingList.put(i + 1, position);
                            this.dictionary.put(stemmedKey, postingList);
                        }

                    }
                }

            } catch (IOException e) {

            }

        }

        // generate term frequency matrix
        Set<Map.Entry<String, HashMap<Integer, ArrayList>>> map1 = dictionary.entrySet();
        for (Map.Entry entry1: map1) {
            int documentNumber = this.pageIndexed.size();
            int[] frequency = new int[documentNumber];

            for (int i = 0; i < documentNumber; i++) {
                frequency[i] = 0;
            }

            HashMap<Integer, ArrayList> map2 = (HashMap<Integer, ArrayList>) entry1.getValue();
            Set<Map.Entry<Integer, ArrayList>> map3 = map2.entrySet();
            for (Map.Entry entry2: map3) {
                frequency[(int) entry2.getKey() - 1] = ((ArrayList) entry2.getValue()).size();
            }

            this.matrix.put(entry1.getKey().toString(), frequency);
        }
    }

    private void output() {

        try {
            File txtFile = new File("/Users/kehanzhang/Desktop/School/SMU/Course/CS 7337 Information Retrieval and Web Search/Project/urls.txt");
            txtFile.createNewFile();
            BufferedWriter ouptut = new BufferedWriter(new FileWriter(txtFile));
            ouptut.write("Question2\nLinks going out of the test data:\n");
            for (String outLinks: pagesOutside) {
                ouptut.write(outLinks + "\n");
            }

            ouptut.write("\nPages that have a tag of noindex:\n");
            for (String noIndexTagLinks: pagesNoIndexTag) {
                ouptut.write(noIndexTagLinks + "\n");
            }

            ouptut.write("\nQuestion3\nPages that contain already seen content:\n");
            for (String duplicateLinks: pagesDuplicateContent) {
                ouptut.write(duplicateLinks + "\n");
            }

            ouptut.write("\nQuestion4\nBroken links:\n");
            for (String brokenLinks: pagesBroken) {
                ouptut.write(brokenLinks + "\n");
            }

            ouptut.write("\nQuestion5\nLinks of non-text files:\n");
            for (String notTextLinks: pagesNotText) {
                ouptut.write(notTextLinks + "\n");
            }

            ouptut.write("\n\n\nPages indexed:\n");
            for (String indexedLinks: pageIndexed) {
                ouptut.write(indexedLinks + "\n");
            }
            ouptut.flush();
            ouptut.close();

            String excelFilePageInformation = "/Users/kehanzhang/Desktop/School/SMU/Course/CS 7337 Information Retrieval and Web Search/Project/page_information.csv";
            Workbook workbook1 = null;
            workbook1 = new HSSFWorkbook();
            Sheet sheet1 = workbook1.createSheet();

            List<String> excelTitlePageInformation = new ArrayList();
            excelTitlePageInformation.add("URL");
            excelTitlePageInformation.add("Title");
            excelTitlePageInformation.add("date");
            excelTitlePageInformation.add("size");

            int rowIndex1 = 0;
            try{
                Row titleRow = sheet1.createRow(rowIndex1);
                for (int i = 0; i < excelTitlePageInformation.size(); i++) {
                    titleRow.createCell(i).setCellValue(excelTitlePageInformation.get(i));
                }
                rowIndex1++;

                for (PageDetail pageDetail: pageDetails) {
                    Row row = sheet1.createRow(rowIndex1);
                    row.createCell(0).setCellValue(pageDetail.getUrl());
                    row.createCell(1).setCellValue(pageDetail.getTitle());
                    row.createCell(2).setCellValue(pageDetail.getDate());
                    row.createCell(3).setCellValue(pageDetail.getSize());
                    rowIndex1++;
                }

                FileOutputStream fos1 = new FileOutputStream(excelFilePageInformation);
                workbook1.write(fos1);
                fos1.close();


            } catch (IOException e) {
                e.printStackTrace();
            }

            String excelFileTermFrequencyMatrix = "/Users/kehanzhang/Desktop/School/SMU/Course/CS 7337 Information Retrieval and Web Search/Project/term_frequency_matrix.csv";
            Workbook workbook2 = null;
            workbook2 = new HSSFWorkbook();
            Sheet sheet2 = workbook2.createSheet();

            List<String> excelTitleTermFrequencyMatrix = new ArrayList();
            excelTitleTermFrequencyMatrix.add("DocID");

            for (int i = 1; i <= pageIndexed.size(); i ++) {
                excelTitleTermFrequencyMatrix.add(String.valueOf(i));
            }

            int rowIndex2 = 0;
            try{
                Row titleRow = sheet2.createRow(rowIndex2);
                for (int i = 0; i < excelTitleTermFrequencyMatrix.size(); i++) {
                    titleRow.createCell(i).setCellValue(excelTitleTermFrequencyMatrix.get(i));
                }
                rowIndex2++;

                Set<Map.Entry<String, int[]>> map = matrix.entrySet();

                for (Map.Entry entry : map) {
                    Row row = sheet2.createRow(rowIndex2);
                    row.createCell(0).setCellValue(entry.getKey().toString());

                    int[] postings = (int[]) entry.getValue();
                    for (int i = 0; i < postings.length; i++) {
                        row.createCell(i + 1).setCellValue(postings[i]);
                    }
                    rowIndex2++;
                }

                FileOutputStream fos2 = new FileOutputStream(excelFileTermFrequencyMatrix);
                workbook2.write(fos2);
                fos2.close();

            } catch (IOException e) {
                e.printStackTrace();
            }

        } catch (IOException e) {

        }

    }

    private void print() {
        System.out.println("\nPages not allowed");
        for (String notAllowedLinks: pagesNotAllowed) {
            System.out.println(notAllowedLinks);
        }
        System.out.println(pagesNotAllowed.size());

        System.out.println("\nPages in the site");
        for (String links: pagesVisited) {
            System.out.println(links);
        }
        System.out.println(pagesVisited.size());

        System.out.println("\nPages out of the site");
        for (String outLinks: pagesOutside) {
            System.out.println(outLinks);
        }
        System.out.println(pagesOutside.size());

        System.out.println("\nPages not text file");
        for (String notTextLinks: pagesNotText) {
            System.out.println(notTextLinks);
        }
        System.out.println(pagesNotText.size());

        System.out.println("\nPages broken");
        for (String brokenLinks: pagesBroken) {
            System.out.println(brokenLinks);
        }
        System.out.println(pagesBroken.size());

        System.out.println("\nPages duplicate");
        for (String duplicateLinks: pagesDuplicateContent) {
            System.out.println(duplicateLinks);
        }
        System.out.println(pagesDuplicateContent.size());

        System.out.println("\nPages indexed");
        for (String indexedLinks: pageIndexed) {
            System.out.println(indexedLinks);
        }
        System.out.println(pageIndexed.size());

        System.out.println("\nDictionary");

        Set<Map.Entry<String, HashMap<Integer, ArrayList>>> dict = dictionary.entrySet();
        for (Map.Entry entry : dict) {
            System.out.print("\n" + entry.getValue() + ": ");
        }

        System.out.println("\nMatrix");
        Set<Map.Entry<String, int[]>> map = matrix.entrySet();
        for (Map.Entry entry : map) {
            System.out.print("\n" + entry.getKey() + ": ");
            int[] postings = (int[]) entry.getValue();
            for (int i = 0; i < postings.length; i++) {
                System.out.print(postings[i] + " ");
            }
        }
    }
}
