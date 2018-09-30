package bpiwowar.tasks;

import bpiwowar.argparser.Argument;
import bpiwowar.experiments.AbstractTask;
import bpiwowar.experiments.TaskDescription;
import bpiwowar.utils.iterators.ReadLineIterator;

@TaskDescription(name = "check-number-of-tabs", description = "Check that there is a given number of tabs on each line, redirecting to standard output the wrong lines", project = {"data"})
public class CheckNumberTabs extends AbstractTask {
    @Argument(name = "n")
    int n;

    @Override
    public int execute() throws Throwable {
        for (String line : new ReadLineIterator(System.in)) {
            int count = 0;
            for (int i = line.length(); --i >= 0 && count <= n; )
                if (line.charAt(i) == '\t')
                    count++;
            if (count == n)
                System.out.println(line);
            else
                System.err.println(line);

        }
        return 0;
    }
}
