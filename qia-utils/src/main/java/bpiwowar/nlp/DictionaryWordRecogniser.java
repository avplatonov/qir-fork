/**
 *
 */
package bpiwowar.nlp;

import bpiwowar.argparser.utils.ReadLineIterator;
import bpiwowar.utils.GenericHelper;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.TreeSet;
import java.util.zip.GZIPInputStream;

class DictionaryWordRecogniser implements WordRecogniser {
    TreeSet<String> words = GenericHelper.newTreeSet();

    public DictionaryWordRecogniser(File file, boolean gzippedWords) throws IOException {
        InputStream in = new FileInputStream(file);
        if (gzippedWords)
            in = new GZIPInputStream(in);

        for (String word : new ReadLineIterator(in)) {
            words.add(word);
        }
        in.close();
    }

    @Override
    public boolean exists(CharSequence word) {
        return words.contains(word);
    }

}
