package nl.tudelft.serg.evosql.metaheuristics.operators;


import nl.tudelft.serg.evosql.fixture.Solution;

public interface MutationOperator<Source extends Solution<?>> extends Operator<Source, Source> {
}
