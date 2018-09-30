package bpiwowar.ir.query;

import bpiwowar.NotImplementedException;
import bpiwowar.text.Query.Component;
import bpiwowar.text.Query.Phrase;
import bpiwowar.utils.Output;
import it.unimi.dsi.io.FastBufferedReader;
import it.unimi.dsi.lang.MutableString;
import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.Set;

/**
 * A requirement (conditions are separated by commas in the query)
 *
 * @author B. Piwowarski <benjamin@bpiwowar.net>
 */
public class Requirement extends Query {
    private static final long serialVersionUID = 1L;

    public ArrayList<Text> terms = new ArrayList<Text>();

    /**
     * Build from a SE query
     *
     * @param query The query to build from
     */
    public Requirement(bpiwowar.text.Query seQuery) {
        for (Component c : seQuery.getSequence()) {
            if (c instanceof bpiwowar.text.Query.Term) {
                // FIXME: uses the same default reader as MG4J, but should be
                // adapted!
                FastBufferedReader reader = new FastBufferedReader();
                bpiwowar.text.Query.Term term = (bpiwowar.text.Query.Term)c;
                reader.setReader(new StringReader(term.getTerm()));
                MutableString word = new MutableString();
                MutableString nonWord = new MutableString();
                try {
                    while (reader.next(word, nonWord)) {
                        if (word.length() > 0)
                            terms.add(new Term(term.getOperator(), word
                                .toString()));
                    }
                }
                catch (IOException e) {
                    // Should not happen!
                    throw new RuntimeException(e);
                }
            }
            else if (c instanceof Phrase)
                terms.add(new bpiwowar.ir.query.Phrase((Phrase)c));
            else
                throw new NotImplementedException();
        }
    }

    public Requirement() {
    }

    @Override
    public String toString() {
        return Output.toString(" ", terms);
    }

    @Override
    public void addTerms(Set<String> set) {
        for (Text text : terms)
            text.addAllTerms(set);
    }
}
