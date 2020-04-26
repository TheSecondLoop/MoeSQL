package nl.tudelft.serg.evosql.metaheuristics.operators;

import nl.tudelft.serg.evosql.fixture.Solution;

import java.io.Serializable;
import java.util.Comparator;

public class CrowdingDistanceComparator<S extends Solution<?>> implements Comparator<S>, Serializable {

    /**
     * Compare two solutions.
     *
     * @param solution1 Object representing the first <code>Solution</code>.
     * @param solution2 Object representing the second <code>Solution</code>.
     * @return -1, or 0, or 1 if solution1 is has greater, equal, or less distance value than solution2,
     * respectively.
     */
    @Override
    public int compare(S solution1, S solution2) {
        int result ;
        if (solution1 == null) {
            if (solution2 == null) {
                result = 0;
            } else {
                result = 1 ;
            }
        } else if (solution2 == null) {
            result = -1;
        } else {

            result = - Double.compare(solution1.crowdingDistance, solution2.crowdingDistance);  //越大越好
//            double distance1 = solution1.crowdingDistance ;
//            double distance2 = solution2.crowdingDistance ;
//            if (distance1 > distance2) {
//                result = -1;
//            } else  if (distance1 < distance2) {
//                result = 1;
//            } else {
//                result = 0;
//            }
        }

        return result ;
    }
}

