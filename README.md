# Search-Engine-Console-App
This is a search engine project for Java with maven.

## Used Software 
IntelliJ

## Installation
1. Download and unpack the latest version of Maven (https://maven.apache.org/download.cgi) and put the install mvn command. 
2. Install a Java 1.8 (or higher) JDK.
3. Install intelliJ.

## Compilation
using IntelliJ IDEA
1. Open IntelliJ IDEA and select File > Open....
2. Choose the project directory and click OK.
3. run command 'mvn compile' in the terminal to compile the project

## Execution Instructions
Run Main.java. After the crawling completed, Number of documents indexed and the size of dictionary will be displayed in the console. And you will be asked to input the query. You are allowed to enter mutiple queries. After each query entered, top 5 results will be shown in the descending order(if the results is less than 5, than all the results related will be shown). Enter "stop" to stop the program.

## Reference
For basic web crawler realization:
http://www.netinstructions.com/how-to-make-a-simple-web-crawler-in-java/

## For Porter Stemm Algorithm realization:
https://tartarus.org/martin/PorterStemmer/java.txt
