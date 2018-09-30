package bpiwowar.ir.query;

import bpiwowar.utils.GenericHelper;
import bpiwowar.utils.Output;
import java.util.ArrayList;
import java.util.Set;

public class COQuery extends Query {
    private static final long serialVersionUID = 1L;

    public ArrayList<Requirement> requirements = GenericHelper.newArrayList();

    public void add(Requirement req) {
        requirements.add(req);
    }

    @Override
    public String toString() {
        return Output.toString(", ", requirements);
    }

    @Override
    public void addTerms(Set<String> set) {
        for (Requirement requirement : requirements)
            requirement.addTerms(set);
    }

    public void add(COQuery coQuery) {
        for (Requirement req : coQuery.requirements)
            requirements.add(req);
    }
}
