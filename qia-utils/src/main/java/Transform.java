import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;

public class Transform {
    public static void main(String[] args) throws IOException {
        ArrayList<String[]> lines = new ArrayList<String[]>();

        int N = -1;
        System.err.println("Reading the file");
        BufferedReader in = new BufferedReader(new InputStreamReader(System.in));
        String line = null;
        while ((line = in.readLine()) != null) {
            final String[] fields = line.split("\t");
            if (N == -1)
                N = fields.length;
            else if (N != fields.length)
                throw new RuntimeException("Differing lengths");
            lines.add(fields);
        }

        final int M = lines.size();
        System.err.format("Outputing %d x %d matrix%n", N, M);

        for (int i = 0; i < N; i++) {
            for (int j = 0; j < M; j++) {
                if (j > 0)
                    System.out.print('\t');
                System.out.print(lines.get(j)[i]);
            }
            System.out.println();
        }

    }
}
