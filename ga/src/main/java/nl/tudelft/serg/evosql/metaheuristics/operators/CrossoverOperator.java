package nl.tudelft.serg.evosql.metaheuristics.operators;

import nl.tudelft.serg.evosql.fixture.Solution;

import java.util.List;

public interface CrossoverOperator<S extends Solution<?>> extends Operator<List<S>,List<S>> {

    /**
     * @return The number of parents required to apply the operator.
     */
    public int getNumberOfParents() ;
}