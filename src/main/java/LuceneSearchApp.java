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
import org.apache.lucene.search.*;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.RAMDirectory;

import java.io.IOException;
import java.text.SimpleDateFormat;
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
			Document luceneDocument = new Document();
			luceneDocument.add(new TextField("title", doc.getTitle(), Field.Store.YES));
			luceneDocument.add(new TextField("abstract", doc.getAbstractText(), Field.Store.YES));
			//luceneDocument.add(new LongField("publish_date", doc.getPubDate().getTime(), Field.Store.YES));
			w.addDocument(luceneDocument);
		}
		w.close();
	}
	
	public List<String> search(List<String> inTitle, List<String> notInTitle, List<String> inDescription, List<String> notInDescription, String startDate, String endDate) throws IOException {
		
		printQuery(inTitle, notInTitle, inDescription, notInDescription, startDate, endDate);
		List<String> results = new LinkedList<String>();

		SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd");
		BooleanQuery.Builder queryBuilder = new BooleanQuery.Builder();

		if (inTitle != null) {
			BooleanQuery titleQuery = createBooleanQuery("title", inTitle, BooleanClause.Occur.MUST);
			queryBuilder.add(titleQuery, BooleanClause.Occur.MUST);
		}

		if (notInTitle != null) {
			BooleanQuery notTitleQuery = createBooleanQuery("title", notInTitle, BooleanClause.Occur.SHOULD);
			queryBuilder.add(notTitleQuery, BooleanClause.Occur.MUST_NOT);
		}

		if (inDescription != null) {
			BooleanQuery descriptionQuery = createBooleanQuery("description", inDescription, BooleanClause.Occur.MUST);
			queryBuilder.add(descriptionQuery, BooleanClause.Occur.MUST);
		}

		if (notInDescription != null) {
			BooleanQuery notDescriptionQuery = createBooleanQuery("description", notInDescription, BooleanClause.Occur.SHOULD);
			queryBuilder.add(notDescriptionQuery, BooleanClause.Occur.MUST_NOT);
		}

		Long parsedStartDate = null;
		Long parsedEndDate = null;

		if (startDate != null) {
			try {
				Date start = getStartOfDay(format.parse(startDate));
				parsedStartDate = start.getTime();
			} catch (Exception e) {
				// Ignore
			}
		}

		if (endDate != null) {
			try {
				Date end = getEndOfDay(format.parse(endDate));
				parsedEndDate = end.getTime();
			} catch (Exception e) {
				// Ignore
			}
		}

		NumericRangeQuery dateQuery = NumericRangeQuery.newLongRange("publish_date", parsedStartDate, parsedEndDate, true, true);
		queryBuilder.add(dateQuery, BooleanClause.Occur.MUST);

		IndexReader reader = DirectoryReader.open(luceneIndex);
		IndexSearcher searcher = new IndexSearcher(reader);
		TopDocs docs = searcher.search(queryBuilder.build(), reader.numDocs());

		for (ScoreDoc hit: docs.scoreDocs) {
			Document doc = searcher.doc(hit.doc);
			IndexableField title = doc.getField("title");
			results.add(title.stringValue());
		}

		return results;
	}

	private Date getEndOfDay(Date date) {
		Calendar calendar = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
		calendar.setTime(date);
		calendar.add(Calendar.DAY_OF_MONTH, 1);
		calendar.set(Calendar.HOUR_OF_DAY, 23);
		calendar.set(Calendar.MINUTE, 59);
		calendar.set(Calendar.SECOND, 59);
		calendar.set(Calendar.MILLISECOND, 999);
		return calendar.getTime();
	}

	private Date getStartOfDay(Date date) {
		Calendar calendar = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
		calendar.setTime(date);
		calendar.add(Calendar.DAY_OF_MONTH, 1);
		calendar.set(Calendar.HOUR_OF_DAY, 0);
		calendar.set(Calendar.MINUTE, 0);
		calendar.set(Calendar.SECOND, 0);
		calendar.set(Calendar.MILLISECOND, 0);
		return calendar.getTime();
	}

	private Date getStartOfNextDay(Date date) {
		Calendar calendar = Calendar.getInstance();
		calendar.setTime(date);
		calendar.add(Calendar.DAY_OF_MONTH, 1);
		calendar.set(Calendar.HOUR_OF_DAY, 0);
		calendar.set(Calendar.MINUTE, 0);
		calendar.set(Calendar.SECOND, 0);
		calendar.set(Calendar.MILLISECOND, 0);
		return calendar.getTime();
	}

	private BooleanQuery createBooleanQuery(String fieldName, List<String> tokens, BooleanClause.Occur clause) {
		BooleanQuery.Builder queryBuilder = new BooleanQuery.Builder();
		for (String token: tokens) {
			queryBuilder.add(new TermQuery(new Term(fieldName, token)), clause);
		}
		return queryBuilder.build();
	}

	public void printQuery(List<String> inTitle, List<String> notInTitle, List<String> inDescription, List<String> notInDescription, String startDate, String endDate) {
		System.out.print("Search (");
		if (inTitle != null) {
			System.out.print("in title: "+inTitle);
			if (notInTitle != null || inDescription != null || notInDescription != null || startDate != null || endDate != null)
				System.out.print("; ");
		}
		if (notInTitle != null) {
			System.out.print("not in title: "+notInTitle);
			if (inDescription != null || notInDescription != null || startDate != null || endDate != null)
				System.out.print("; ");
		}
		if (inDescription != null) {
			System.out.print("in description: "+inDescription);
			if (notInDescription != null || startDate != null || endDate != null)
				System.out.print("; ");
		}
		if (notInDescription != null) {
			System.out.print("not in description: "+notInDescription);
			if (startDate != null || endDate != null)
				System.out.print("; ");
		}
		if (startDate != null) {
			System.out.print("startDate: "+startDate);
			if (endDate != null)
				System.out.print("; ");
		}
		if (endDate != null)
			System.out.print("endDate: "+endDate);
		System.out.println("):");
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
	
	public static void main(String[] args) throws IOException {
		if (args.length > 0) {
			LuceneSearchApp engine = new LuceneSearchApp();
			
			DocumentCollectionParser parser = new DocumentCollectionParser();
			parser.parse(args[0]);
			List<DocumentInCollection> docs = parser.getDocuments();

			List<String> queries = Arrays.asList("motion tracking", "gesture user interface", "motion detection user interface");
			Analyzer analyzer;
			switch(Integer.valueOf(args[1])) {
				case 1:
					// VSM (lucene default) with Porter stemmer and stop words
					//analyzer = new PorterAnalyzer();
					analyzer = new EnglishAnalyzer();
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
			}


		}
		else
			System.out.println("ERROR: the path of a Document file has to be passed as a command line argument.");
	}
}
