package bpiwowar.tasks;

import bpiwowar.argparser.Argument;
import bpiwowar.experiments.AbstractTask;
import bpiwowar.experiments.TaskDescription;
import bpiwowar.utils.GenericHelper;
import java.io.File;
import java.util.ArrayList;

@TaskDescription(name = "get-source-code", description = "Get source code for a set of java classes", project = {"java"})
public class GetSourceCode extends AbstractTask {
    private String[] args;

    @Argument(name = "src", help = "Source directory")
    ArrayList<File> files = GenericHelper.newArrayList();

    @Override
    public String[] processTrailingArguments(String[] args) throws Exception {
        this.args = args;
        return null;
    }

    @Override
    public int execute() throws Throwable {
        return 0;

    }
}
