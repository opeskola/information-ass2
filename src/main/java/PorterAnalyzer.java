import org.apache.lucene.analysis.Tokenizer;
import org.apache.lucene.analysis.en.PorterStemFilter;
import org.apache.lucene.analysis.standard.StandardTokenizer;
import org.apache.lucene.analysis.util.CharArraySet;
import org.apache.lucene.analysis.util.StopwordAnalyzerBase;

class PorterAnalyzer extends StopwordAnalyzerBase {

    PorterAnalyzer() {
        super();
    }

    PorterAnalyzer(CharArraySet set) {
        super(set);
    }

    @Override
    protected TokenStreamComponents createComponents(String fieldName) {
        Tokenizer source = new StandardTokenizer(); //new LowerCaseTokenizer();
        return new TokenStreamComponents(source, new PorterStemFilter(source));
    }
}