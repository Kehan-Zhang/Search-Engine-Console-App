package com.project;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

public class SpiderLeg {
    private static final String USER_AGENT = "MyBot";
    private List<String> links = new ArrayList<>();
    private Document htmlDocument;

    // Make an HTTP request, check the response and gather all the links on this page.
    public boolean crawl(String url) {
        Connection connection = Jsoup.connect(url).userAgent(USER_AGENT);
        Document htmlDocument = null;

        try {
            htmlDocument = connection.get();
            this.htmlDocument = htmlDocument;
        } catch (IOException e) {

        }

        Elements linksOnPage = htmlDocument.select("a[href]");
        for (Element link : linksOnPage) {
            this.links.add(link.absUrl("href"));
        }
        return true;
    }

    public List<String> getLinks() {
        return this.links;
    }

    public String getBody() {
        return this.htmlDocument.body().wholeText();
    }
}
