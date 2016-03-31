import org.apache.lucene.analysis.Tokenizer;
import org.apache.lucene.analysis.en.KStemFilter;
import org.apache.lucene.analysis.standard.StandardTokenizer;
import org.apache.lucene.analysis.util.CharArraySet;
import org.apache.lucene.analysis.util.StopwordAnalyzerBase;

public class KAnalyzer extends StopwordAnalyzerBase {

    KAnalyzer() {
        super();
    }

    KAnalyzer(CharArraySet set) {
        super(set);
    }

    @Override
    protected TokenStreamComponents createComponents(String fieldName) {
        Tokenizer source = new StandardTokenizer();
        return new TokenStreamComponents(source, new KStemFilter(source));
    }
}
