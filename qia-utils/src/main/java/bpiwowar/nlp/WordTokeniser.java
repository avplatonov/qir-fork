package bpiwowar.nlp;

import bpiwowar.utils.IntegerPair;
import java.text.BreakIterator;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class WordTokeniser {
    private final Locale locale;

    public WordTokeniser() {
        locale = Locale.getDefault();
    }

    public WordTokeniser(Locale locale) {
        this.locale = locale;
    }

    public List<IntegerPair> tokenise(String input) {
        BreakIterator wordBreaker = BreakIterator.getWordInstance(locale);
        wordBreaker.setText(input);
        int start = wordBreaker.first();
        int end = wordBreaker.next();

        ArrayList<IntegerPair> list = new ArrayList<IntegerPair>();
        while (end != BreakIterator.DONE) {
            if (!Character.isWhitespace(input.charAt(start)))
                list.add(new IntegerPair(start, end));
            start = end;
            end = wordBreaker.next();
        }

        return list;
    }

}
