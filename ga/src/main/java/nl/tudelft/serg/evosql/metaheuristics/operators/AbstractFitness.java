package nl.tudelft.serg.evosql.metaheuristics.operators;

import nl.tudelft.serg.evosql.fixture.Fixture;

import java.util.Comparator;

public abstract class AbstractFitness implements Comparable<AbstractFitness> {    //A.compareTo(B) > 0 表示 A劣于B
    public abstract double doubleCompareTo(AbstractFitness targetFitness);
    public abstract AbstractFitness copy();
}
