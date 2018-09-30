package bpiwowar.tasks;

import bpiwowar.argparser.Argument;
import bpiwowar.experiments.AbstractTask;
import bpiwowar.experiments.TaskDescription;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.StringReader;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.xml.sax.InputSource;

@TaskDescription(name = "evaluate-xpath", project = {"xml"})
public class EvaluateXPath extends AbstractTask {

    @Argument(name = "path")
    public String expr;

    @Argument(name = "eos")
    public String eos;

    private String xmlstring;

    @Override
    public String[] processTrailingArguments(String[] args) throws Exception {
        StringBuilder sb = new StringBuilder();
        for (String s : args) {
            sb.append(s);
            sb.append(' ');
        }
        this.xmlstring = sb.toString();
        return null;
    }

    @Override
    public int execute() throws Throwable {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        if (factory.isValidating())
            factory
                .setFeature(
                    "http://apache.org/xml/features/nonvalidating/load-external-dtd",
                    false);

        DocumentBuilder parser = factory.newDocumentBuilder();
        Document document = parser.parse(new InputSource(new StringReader(
            xmlstring)));
        XPath xpath = XPathFactory.newInstance().newXPath();

        Element root = document.getDocumentElement();
        if (expr != null)
            System.out.println(xpath.evaluate(expr, root));
        else {
            BufferedReader in = new BufferedReader(new InputStreamReader(
                System.in));
            while ((expr = in.readLine()) != null) {
                System.err.format("Evaluating %s%n", expr);
                System.out.println(xpath.evaluate(expr, root));
                if (eos != null) {
                    System.out.println(eos);
                }
                System.out.flush();
            }
        }
        return 0;
    }
}
