package nl.tudelft.serg.evosql.metaheuristics.operators;

import nl.tudelft.serg.evosql.fixture.Solution;

import java.util.List;

public interface SelectionOperator<S extends Solution<?>> extends Operator<List<S>, List<S>> {
    public void setIsCovered(boolean[] isCovered);
    public int getSelectSize();
}
