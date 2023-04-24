package final_project;

import org.apache.lucene.analysis.CharArraySet;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.StringField;
import org.apache.lucene.document.TextField;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.search.similarities.ClassicSimilarity;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.ByteBuffersDirectory;
import org.apache.lucene.util.Version;
import java.util.ArrayList;
import java.util.List;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Scanner;

public class QueryEngine {
    private static final CharArraySet noStopWords = null;
	private static final Path DATA_PATH = Paths.get("resources", "data");
	boolean indexExists=false;
    static Directory index;
    //static StandardAnalyzer analyzer = new StandardAnalyzer();
    static StandardAnalyzer analyzer = new StandardAnalyzer(noStopWords);

    public QueryEngine(){
        index = buildIndex();
    }

    private Directory buildIndex() {
        //Get file from resources folder
                
        //initializes data structures for Lucene index
        
        Directory index = new ByteBuffersDirectory();

        IndexWriterConfig config = new IndexWriterConfig(analyzer);

        IndexWriter w;
		try {
			w = new IndexWriter(index, config);

			// get the path of the current workspace then concatenate onto that this file
			String path = new File("").getAbsolutePath();
			//path = path + "/bot_detection/src/main/resources/combined_data.csv";
			File myObj = new File(getClass().getClassLoader().getResource("data_to_query.csv").getFile());
	        try (Scanner inputScanner = new Scanner(new BufferedReader(new FileReader(myObj)))) {
	        	
	            while (inputScanner.hasNext()) {
	            	String input = inputScanner.nextLine();
	            	if (input.length() > 1) {
	            		String content = input.substring(0, input.length()-2);
	            		String troll = input.substring(input.length()-1);
	            		if (troll.equals("0") || troll.equals("1")) {
	            			//System.out.println(content + " troll: " + troll);

	            	
	            	//adds docID and body of text to index
	            	addDoc(w, content, troll);}
	            		}
	            }

	            inputScanner.close();
	        } catch (IOException e) {
	            e.printStackTrace();
	        }
	        indexExists = true;
	        w.close();
	        
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return index;
    }

    private void addDoc(IndexWriter w, String content, String troll) throws IOException {
    	  Document doc = new Document();
    	  doc.add(new TextField("content", content, Field.Store.YES));
    	  doc.add(new StringField("troll", troll, Field.Store.YES));
    	  w.addDocument(doc);
    	}

	private File openDataFile(String filename) {
		ClassLoader classLoader = getClass().getClassLoader();
		File file = new File(classLoader.getResource(filename).getFile());
		return file;
	}

	public static void main(String[] args ) {


        try {
            System.out.println("********Welcome To The Final Project!");
            QueryEngine objQueryEngine = new QueryEngine();

			Scanner userScanner = new Scanner(System.in);

            String userInput = "1";
			while (userInput.equals("1") || userInput.equals("2")){
				float total = 0;
				float correct = 0;

				System.out.println("Enter the following commands to choose a ranking method -\n" + 
						"\tEnter 1 for tf-idf\n" + 
						"\tEnter 2 for BM25\n" + 
						"\tEnter anything else to exit");
				userInput = userScanner.nextLine();
				if (! (userInput.equals("1") || userInput.equals("2"))){
					System.out.println("***************** Thanks for using the program! *****************\n");
					userScanner.close();
					break;
				}


				File file = objQueryEngine.openDataFile("query.csv");
				Scanner myReader = new Scanner(new BufferedReader(new FileReader(file)));
				myReader.nextLine();
				while (myReader.hasNextLine()) {
					
					String data = myReader.nextLine();
					if (data.length() > 3) {
						String troll = data.substring(data.length()-1, data.length());
						String[] query = normalizeQuery((data.substring(0, data.length()-2)).split(" "));
						
						if (query.length > 1 && (troll.equals("0") || troll.equals("1"))) {
							total += 1;
							String[] answer = runQuery(query, userInput);
							
							System.out.println("Query " + (int) (total));
							System.out.println("Query = " + String.join(" ", query) + " (Troll Query: " + troll +
									")\nTop Result = " + answer[0] + " (Troll Result: " +answer[1]+ ")\n");
							
							
							if (answer[1] != null) {
								if (answer[1].equals(troll))
									correct +=1;
							}      
						}
					}
				}
				System.out.println("total = " + (int) total + " Correct = " + (int) correct);
				System.out.println(correct/total);
				myReader.close();
			}    
            userScanner.close();
        }
        catch (Exception ex) {
            System.out.println(ex.getMessage());
			System.out.println("An error occurred.");
			ex.printStackTrace();
        }
    }

    public static String[] runQuery(String[] query, String rankingMethod) throws java.io.FileNotFoundException,java.io.IOException {
        
        //creates query with query terms separated by a space
        
        String[] top = new String[2];
        
        String querystr = String.join(" ", query);
        try {
			Query q = new QueryParser("content", analyzer).parse(QueryParser.escape(querystr));
	        int hitsPerPage = 1;
	        IndexReader reader = DirectoryReader.open(index);
	        IndexSearcher searcher = new IndexSearcher(reader);
		
			if (rankingMethod.equals("1")){
				searcher.setSimilarity(new ClassicSimilarity());
			}

	        TopDocs docs = searcher.search(q, hitsPerPage);
	        ScoreDoc[] hits = docs.scoreDocs;
			
	        for(int i=0;i<hits.length;++i) {
	            int docId = hits[i].doc;
	            Document d = searcher.doc(docId);
                top[0] = d.get("content"); 
                top[1] = d.get("troll");
	        }
		} catch (ParseException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
              
        return top;
    }

	private static String[] normalizeQuery(String[] query) {
		List<String> lst = new ArrayList<String>();
		for (int i = 0; i < query.length; i++) {
			if (query[i].length() > 0)
				if (!(query[i].charAt(0)=='?'))
					lst.add(query[i]);
			}
		String[] returnarr = new String[lst.size()];
		for (int j = 0; j < lst.size(); j++)
			returnarr[j] = lst.get(j);
		
		return returnarr;
	}

}
