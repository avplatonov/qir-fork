package bpiwowar.ir.query;

import bpiwowar.text.Query.Operator;

public enum Restriction {
    NONE(""), PLUS("+"), NEGATIVE("-");

    String value;

    Restriction(String s) {
        value = s;
    }

    @Override
    public String toString() {
        return value;
    }

    static public Restriction get(Operator op) {
        switch (op) {
            case MINUS:
                return Restriction.NEGATIVE;
            case NONE:
                return Restriction.NONE;
            case PLUS:
                return Restriction.PLUS;
        }
        return null;
    }

}
