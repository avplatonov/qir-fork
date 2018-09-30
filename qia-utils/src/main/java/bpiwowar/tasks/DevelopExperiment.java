package bpiwowar.tasks;

import bpiwowar.experiments.AbstractTask;
import bpiwowar.experiments.TaskDescription;
import bpiwowar.log.Logger;

@TaskDescription(name = "develop-experiment", description = "Develop an experimental plan", project = {""})
public class DevelopExperiment extends AbstractTask {
    final static private Logger LOGGER = Logger.getLogger();
    private String planExpr;

    @Override
    public String[] processTrailingArguments(String[] args) throws Exception {
        StringBuilder sb = new StringBuilder();
//		boolean first = true;
//		for (String x : args) {
//			if (first)
//				first = false;
//			else
//				sb.append(' ');
//			sb.append(x);
//		}
//		planExpr = sb.toString();
        return new String[] {};
    }

    @Override
    public int execute() throws Throwable {
//		PlanParser planParser = new PlanParser(new StringReader(planExpr));
//		Node plan = planParser.plan();
//		LOGGER.info("Plan is %s", plan);
//		for (Map<String, String> x : plan) {
//			Output.print(System.out, "; ", x.entrySet());
//			System.out.println();
//		}

        return 0;
    }
}
