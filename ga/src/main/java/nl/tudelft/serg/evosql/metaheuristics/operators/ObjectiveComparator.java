package nl.tudelft.serg.evosql.metaheuristics.operators;

import com.sun.xml.internal.ws.policy.spi.PolicyAssertionValidator;
import nl.tudelft.serg.evosql.fixture.Solution;

import java.io.Serializable;
import java.util.Comparator;

public class ObjectiveComparator<S extends Solution<?>> implements Comparator<S>, Serializable {
    public enum Ordering {ASCENDING, DESCENDING} ;
    private int objectiveId;

    private Ordering order;

    /**
     * Constructor.
     *
     * @param objectiveId The index of the objective to compare
     */
    public ObjectiveComparator(int objectiveId) {
        this.objectiveId = objectiveId;
        order = Ordering.ASCENDING;
    }

    /**
     * Comparator.
     * @param objectiveId The index of the objective to compare
     * @param order Ascending or descending order
     */
    public ObjectiveComparator(int objectiveId, Ordering order) {
        this.objectiveId = objectiveId;
        this.order = order ;
    }

    /**
     * Compares two solutions according to a given objective.
     *
     * @param solution1 The first solution
     * @param solution2 The second solution
     * @return -1, or 0, or 1 if solution1 is less than, equal, or greater than solution2,
     * respectively, according to the established order
     */
    @Override
    public int compare(S solution1, S solution2) {
        int result ;
        if (solution1 == null) {
            if (solution2 == null) {
                result = 0;
            } else {
                result =  1;
            }
        } else if (solution2 == null) {
            result =  -1;
        }
        else {
            AbstractFitness objective1 = solution1.getObjective(this.objectiveId);
            AbstractFitness objective2 = solution2.getObjective(this.objectiveId);

            result = objective1.compareTo(objective2);
            if (order == Ordering.DESCENDING) {
                result = -result;
            }
        }
        return result ;
    }
}
