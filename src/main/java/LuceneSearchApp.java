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
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.analysis.util.CharArraySet;
import org.apache.lucene.document.*;
import org.apache.lucene.index.*;
import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.search.*;
import org.apache.lucene.search.similarities.BM25Similarity;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.RAMDirectory;
import org.apache.lucene.queryparser.classic.QueryParser;

import java.io.IOException;
import java.util.*;


public class LuceneSearchApp {

	private Directory luceneIndex;

	public LuceneSearchApp() {}

	public void index(List<DocumentInCollection> docs, Analyzer analyzer) throws IOException {

		// using an in-memory index
		luceneIndex = new RAMDirectory();
		IndexWriterConfig config = new IndexWriterConfig(analyzer);
		IndexWriter w = new IndexWriter(luceneIndex, config);

		for (DocumentInCollection doc : docs) {
			if (doc.getSearchTaskNumber() == 1) {
				Document luceneDocument = new Document();
				luceneDocument.add(new TextField("title", doc.getTitle(), Field.Store.YES));
				luceneDocument.add(new TextField("abstract", doc.getAbstractText(), Field.Store.YES));
				luceneDocument.add(new IntField("relevance", doc.isRelevant() ? 1 : 0, Field.Store.YES));
				w.addDocument(luceneDocument);
			}
		}
		w.close();
	}
	
	public List<String> search(String words, Analyzer analyzer, boolean BMSearcher) throws IOException, ParseException {
		
		System.out.println("query: " + words);
		List<String> results = new ArrayList<String>();

		QueryParser parser = new QueryParser("abstract", analyzer);
		Query query = parser.parse(words);

		IndexReader reader = DirectoryReader.open(luceneIndex);
		IndexSearcher searcher = new IndexSearcher(reader);
		if (BMSearcher) {
			searcher.setSimilarity(new BM25Similarity());
		}
		TopDocs docs = searcher.search(query, reader.numDocs());
		for (ScoreDoc hit: docs.scoreDocs) {
			Document doc = searcher.doc(hit.doc);
			IndexableField abstr = doc.getField("abstract");
			IndexableField relevance = doc.getField("relevance");
			IndexableField title = doc.getField("title");
			results.add("Relevance: " + relevance.stringValue() +"; Score: " + hit.score +  "; Title: " + title.stringValue());
		}

		return results;
	}
	
	public void printResults(List<String> results) {
		if (results.size() > 0) {
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

			List<String> queries = Arrays.asList("motion AND tracking", "gesture AND user AND interface",
					"motion AND detection AND user AND interface", "motion AND control AND human AND computer AND interaction");
			Analyzer analyzer = null;
			boolean BMSearcher = false;
			switch(Integer.valueOf(args[1])) {
				case 1:
					// VSM (lucene default) with Porter stemmer and stop words
					analyzer = new PorterAnalyzer();
					engine.index(docs, analyzer);
					break;
				case 2:
					// VSM (lucene default) with Porter stemmer and no stop words
					analyzer = new PorterAnalyzer(CharArraySet.EMPTY_SET);
					engine.index(docs, analyzer);
					break;
				case 3:
					// BM25 with Porter stemmer and stop words
					analyzer = new PorterAnalyzer();
					engine.index(docs, analyzer);
					BMSearcher = true;
					break;
				case 4:
					// BM25 with Porter stemmer and no stop words
					analyzer = new PorterAnalyzer(CharArraySet.EMPTY_SET);
					engine.index(docs, analyzer);
					BMSearcher = true;
					break;
				case 5:
					// VSM (lucene default) with K stemmer and stop words
					analyzer = new KAnalyzer();
					engine.index(docs, analyzer);
					break;
				case 6:
					// VSM (lucene default) with K stemmer and no stop words
					analyzer = new KAnalyzer(CharArraySet.EMPTY_SET);
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

			List<String> results = engine.search("motion AND tracking", analyzer, BMSearcher);
			engine.printResults(results);
		}
		else
			System.out.println("ERROR: the path of a Document file has to be passed as a command line argument.");
	}
}
