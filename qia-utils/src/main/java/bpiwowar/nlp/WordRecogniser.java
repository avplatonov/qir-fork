package bpiwowar.nlp;

/**
 * Says if a word exists or not
 *
 * @author B. Piwowarski <benjamin@bpiwowar.net>
 */
public interface WordRecogniser {
    boolean exists(CharSequence word);
}
