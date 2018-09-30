package bpiwowar.xml;

import bpiwowar.argparser.ArgumentHandler;
import bpiwowar.argparser.handlers.StringConstructorHandler;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@ArgumentHandler(StringConstructorHandler.class)
public class SimpleXPath implements Serializable {
    private static final long serialVersionUID = 7790357381052466858L;

    static public class Step implements Serializable {
        private static final long serialVersionUID = 1L;
        String name;
        int position;

        public Step(String name, int position) {
            super();
            this.name = name;
            this.position = position;
        }

        public String getName() {
            return name;
        }

        public int getPosition() {
            return position;
        }
    }

    private static final String STEP_REGEX = "/([^/\\[]+)\\[(\\d+)\\]";

    private static final Pattern p = Pattern.compile(STEP_REGEX);

    Step[] steps;

    public Step[] getSteps() {
        return steps;
    }

    public SimpleXPath(final String path) throws InvalidXPathPointerException {
        final Matcher m = p.matcher(path);
        int lastMatch = 0;
        ArrayList<Step> steps = new ArrayList<Step>();

        while (m.find()) {
            // Axis step found
            if (m.start() != lastMatch)
                throw new InvalidXPathPointerException(
                    "Expression %s is not an xpath pointer expression.",
                    path);

            Step step = new Step(m.group(1), Integer.valueOf(m.group(2)));
            steps.add(step);
            lastMatch = m.end();
        }
        if (lastMatch != path.length())
            throw new InvalidXPathPointerException("Expression " + path
                + " is not an xpath pointer expression (partial parse).");

        this.steps = steps.toArray(new Step[steps.size()]);
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        for (Step step : steps) {
            sb.append('/');
            sb.append(step.name);
            sb.append('[');
            sb.append(step.position);
            sb.append(']');
        }
        return sb.toString();
    }

}
