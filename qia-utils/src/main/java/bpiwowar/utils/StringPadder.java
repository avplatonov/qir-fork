package bpiwowar.utils;

import java.text.CharacterIterator;
import java.text.StringCharacterIterator;

/**
 * StringPadder provides left pad and right pad functionality for Strings
 */
public class StringPadder {
    private static StringBuffer strb;
    private static StringCharacterIterator sci;

    /**
     * method to left pad a string with a given string to a given size. This method will repeat the padder string as
     * many times as is necessary until the exact specified size is reached. If the specified size is less than the size
     * of the original string then the original string is returned unchanged. Example1 - original string "cat", padder
     * string "white", size 8 gives "whitecat". Example2 - original string "cat", padder string "white", size 15 gives
     * "whitewhitewhcat". Example3 - original string "cat", padder string "white", size 2 gives "cat".
     *
     * @param stringToPad The original string
     * @param padder The string to pad onto the original string
     * @param size The required size of the new string
     * @return String the newly padded string
     */
    public static String leftPad(String stringToPad, String padder, int size) {
        if (padder.length() == 0) {
            return stringToPad;
        }
        strb = new StringBuffer(size);
        sci = new StringCharacterIterator(padder);

        while (strb.length() < (size - stringToPad.length())) {
            for (char ch = sci.first(); ch != CharacterIterator.DONE; ch = sci.next()) {
                if (strb.length() < size - stringToPad.length()) {
                    strb.insert(strb.length(), String.valueOf(ch));
                }
            }
        }
        return strb.append(stringToPad).toString();
    }

    /**
     * method to right pad a string with a given string to a given size. This method will repeat the padder string as
     * many times as is necessary until the exact specified size is reached. If the specified size is less than the size
     * of the original string then the original string is returned unchanged. Example1 - original string "cat", padder
     * string "white", size 8 gives "catwhite". Example2 - original string "cat", padder string "white", size 15 gives
     * "catwhitewhitewh". Example3 - original string "cat", padder string "white", size 2 gives "cat".
     *
     * @param stringToPad The original string
     * @param padder The string to pad onto the original string
     * @param size The required size of the new string
     * @return String the newly padded string
     */
    public static String rightPad(String stringToPad, String padder, int size) {
        if (padder.length() == 0) {
            return stringToPad;
        }
        strb = new StringBuffer(stringToPad);
        sci = new StringCharacterIterator(padder);

        while (strb.length() < size) {
            for (char ch = sci.first(); ch != CharacterIterator.DONE; ch = sci.next()) {
                if (strb.length() < size) {
                    strb.append(String.valueOf(ch));
                }
            }
        }
        return strb.toString();
    }
}
