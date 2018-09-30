/**
 *
 */
package bpiwowar.utils;

import java.util.List;

/**
 * Compute the Levenshtein distance between two strings
 */
final public class Levenshtein {

    // ****************************
    // Get minimum of three values
    // ****************************

    final static private int Minimum(int a, int b, int c) {
        int mi;

        mi = a;
        if (b < mi) {
            mi = b;
        }
        if (c < mi) {
            mi = c;
        }
        return mi;

    }

    // *****************************
    // Compute Levenshtein distance
    // *****************************

    final static public int LD(final String s, final String t) {
        int d[][]; // matrix
        int n; // length of s
        int m; // length of t
        int i; // iterates through s
        int j; // iterates through t
        char s_i; // ith character of s
        char t_j; // jth character of t
        int cost; // cost

        // Step 1

        n = s.length();
        m = t.length();
        if (n == 0) {
            return m;
        }
        if (m == 0) {
            return n;
        }
        d = new int[n + 1][m + 1];

        // Step 2

        for (i = 0; i <= n; i++) {
            d[i][0] = i;
        }

        for (j = 0; j <= m; j++) {
            d[0][j] = j;
        }

        // Step 3

        for (i = 1; i <= n; i++) {

            s_i = s.charAt(i - 1);

            // Step 4

            for (j = 1; j <= m; j++) {

                t_j = t.charAt(j - 1);

                // Step 5

                if (s_i == t_j) {
                    cost = 0;
                }
                else {
                    cost = 1;
                }

                // Step 6

                d[i][j] = Minimum(d[i - 1][j] + 1, d[i][j - 1] + 1,
                    d[i - 1][j - 1] + cost);

            }

        }

        // Step 7

        return d[n][m];

    }

    final static public int LD(final List<?> s, final List<?> t) {
        int d[][]; // matrix
        int n; // length of s
        int m; // length of t
        int i; // iterates through s
        int j; // iterates through t
        Object s_i; // ith character of s
        Object t_j; // jth character of t
        int cost; // cost

        // Step 1

        n = s.size();
        m = t.size();
        if (n == 0) {
            return m;
        }
        if (m == 0) {
            return n;
        }
        d = new int[n + 1][m + 1];

        // Step 2

        for (i = 0; i <= n; i++) {
            d[i][0] = i;
        }

        for (j = 0; j <= m; j++) {
            d[0][j] = j;
        }

        // Step 3

        for (i = 1; i <= n; i++) {

            s_i = s.get(i - 1);

            // Step 4

            for (j = 1; j <= m; j++) {

                t_j = t.get(j - 1);

                // Step 5

                if (s_i.equals(t_j)) {
                    cost = 0;
                }
                else {
                    cost = 1;
                }

                // Step 6

                d[i][j] = Minimum(d[i - 1][j] + 1, d[i][j - 1] + 1,
                    d[i - 1][j - 1] + cost);

            }

        }

        // Step 7

        return d[n][m];

    }

    static public void main(final String[] args) {
        if (args.length != 2) {
            System.err.println("Levenshtein distance takes two strings");
            System.exit(1);
        }
        System.out.format("d(%s,%s) = %d (%f)%n", args[0], args[1], LD(args[0],
            args[1]), LD(args[0], args[1])
            / (double)Math.max(args[0].length(), args[1].length()));
    }

    /**
     * Normalized Levenshtein distance
     *
     * @param term1 the first word
     * @param term2 the second word
     * @return A value between 0 and 1
     */
    public static double normalizedLD(String term1, String term2) {
        return LD(term1, term2) / (double)Math.max(term1.length(), term2.length());
    }
}
