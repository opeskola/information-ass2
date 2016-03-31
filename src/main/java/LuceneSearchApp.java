/*
 * Skeleton class for the Lucene search program implementation
 *
 * Created on 2011-12-21
 * * Jouni Tuominen <jouni.tuominen@aalto.fi>
 * 
 * Modified on 2015-30-12
 * * Esko Ikkala <esko.ikkala@aalto.fi>
 * 
 */

import ir_course.DocumentCollectionParser;
import ir_course.DocumentInCollection;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.en.EnglishAnalyzer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.analysis.util.CharArraySet;
import org.apache.lucene.document.*;
import org.apache.lucene.index.*;
import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.search.*;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.RAMDirectory;
import org.apache.lucene.queryparser.classic.QueryParser;

import java.io.IOException;
import java.util.*;


public class LuceneSearchApp {

	private Directory luceneIndex;

	public LuceneSearchApp() {}

	// VSM (lucene default) with Porter stemmer and stop words
	// VSM (lucene default) with Porter stemmer and no stop words
	// BM25 with Porter stemmer and stop words
	// BM25 with Porter stemmer and no stop words
	// VSM (lucene default) with some other stemmer and stop words
	// VSM (lucene default) with some other stemmer and no stop words

	public void index(List<DocumentInCollection> docs, Analyzer analyzer) throws IOException {

		// using an in-memory index
		luceneIndex = new RAMDirectory();
		IndexWriterConfig config = new IndexWriterConfig(analyzer);
		IndexWriter w = new IndexWriter(luceneIndex, config);

		for (DocumentInCollection doc : docs) {
			if (doc.getSearchTaskNumber() == 1) {
				Document luceneDocument = new Document();
				//luceneDocument.add(new TextField("title", doc.getTitle(), Field.Store.YES));
				luceneDocument.add(new TextField("abstract", doc.getAbstractText(), Field.Store.YES));
				luceneDocument.add(new IntField("relevance", doc.isRelevant() ? 1 : 0, Field.Store.YES));
				w.addDocument(luceneDocument);
			}
		}
		w.close();
	}
	
	public List<String> search(String words, Analyzer analyzer) throws IOException, ParseException {
		
		System.out.println("query: " + words);
		List<String> results = new LinkedList<String>();

		QueryParser parser = new QueryParser("abstract", analyzer);
		Query query = parser.parse(words);

		/*
		BooleanQuery.Builder queryBuilder = new BooleanQuery.Builder();
		for (String word: words) {
			queryBuilder.add(new TermQuery(new Term("abstract", word)), BooleanClause.Occur.SHOULD);
		}
		BooleanQuery query = queryBuilder.build();
		*/

		IndexReader reader = DirectoryReader.open(luceneIndex);
		IndexSearcher searcher = new IndexSearcher(reader);
		TopDocs docs = searcher.search(query, reader.numDocs());

		for (ScoreDoc hit: docs.scoreDocs) {
			Document doc = searcher.doc(hit.doc);
			IndexableField abstr = doc.getField("abstract");
			results.add(abstr.stringValue());
		}

		return results;
	}

	private BooleanQuery createBooleanQuery(String fieldName, List<String> tokens, BooleanClause.Occur clause) {
		BooleanQuery.Builder queryBuilder = new BooleanQuery.Builder();
		for (String token: tokens) {
			queryBuilder.add(new TermQuery(new Term(fieldName, token)), clause);
		}
		return queryBuilder.build();
	}
	
	public void printResults(List<String> results) {
		if (results.size() > 0) {
			Collections.sort(results);
			for (int i=0; i<results.size(); i++)
				System.out.println(" " + (i+1) + ". " + results.get(i));
		}
		else
			System.out.println(" no results");
	}
	
	public static void main(String[] args) throws IOException, ParseException {
		if (args.length > 0) {
			LuceneSearchApp engine = new LuceneSearchApp();
			
			DocumentCollectionParser parser = new DocumentCollectionParser();
			parser.parse(args[0]);
			List<DocumentInCollection> docs = parser.getDocuments();

			List<String> queries = Arrays.asList("motion tracking", "gesture user interface", "motion detection user interface");
			Analyzer analyzer = null;
			switch(Integer.valueOf(args[1])) {
				case 1:
					// VSM (lucene default) with Porter stemmer and stop words
					analyzer = new PorterAnalyzer();
					//analyzer = new EnglishAnalyzer();
					engine.index(docs, analyzer);
					break;
				case 2:
					// VSM (lucene default) with Porter stemmer and no stop words
					analyzer = new EnglishAnalyzer(CharArraySet.EMPTY_SET);
					engine.index(docs, analyzer);
					break;
				case 3:
					// BM25 with Porter stemmer and stop words
					analyzer = new EnglishAnalyzer();
					engine.index(docs, analyzer);
					break;
				case 4:
					// BM25 with Porter stemmer and no stop words
					analyzer = new EnglishAnalyzer(CharArraySet.EMPTY_SET);
					engine.index(docs, analyzer);
					break;
				case 5:
					// VSM (lucene default) with some other stemmer and stop words
					analyzer = new StandardAnalyzer();
					engine.index(docs, analyzer);
					break;
				case 6:
					// VSM (lucene default) with some other stemmer and no stop words
					analyzer = new StandardAnalyzer(CharArraySet.EMPTY_SET);
					engine.index(docs, analyzer);
					break;
				case 7:
					// VSM (lucene default) with no stemming and stop words
					analyzer = new StandardAnalyzer();
					engine.index(docs, analyzer);
					break;
				default:
					System.out.println("Indexing option was missing");
					break;
			}
			Random random = new Random();
			//List<String> queryWords = Arrays.asList(queries.get(random.nextInt(3)).split(" "));
			List<String> queryWords = Arrays.asList("gesture", "user", "interface");
			//List<String> queryWords = Arrays.asList("human", "interface");
			List<String> results = engine.search("gesture AND user AND interface", analyzer);
			engine.printResults(results);
		}
		else
			System.out.println("ERROR: the path of a Document file has to be passed as a command line argument.");
	}
}
