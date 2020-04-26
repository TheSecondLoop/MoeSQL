package nl.tudelft.serg.evosql.metaheuristics.operators;

import java.io.Serializable;

public interface Operator<Source, Result> extends Serializable {
    /**
     * @param source The data to process
     */
    public Result execute(Source source) ;
}