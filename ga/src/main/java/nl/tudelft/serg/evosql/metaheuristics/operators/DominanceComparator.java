package nl.tudelft.serg.evosql.metaheuristics.operators;

import nl.tudelft.serg.evosql.EvoSQLConfiguration;
import nl.tudelft.serg.evosql.fixture.Solution;

import java.io.Serializable;
import java.util.Comparator;

public class DominanceComparator<S extends Solution<?>> implements Comparator<S>, Serializable {
    private double epsilon = 0.0 ;

    /** Constructor */
    public DominanceComparator() {
        this(0.0) ;
    }

    public boolean[] isCovered;
    public void setIsCovered(boolean[] isCovered)
    {
        this.isCovered = isCovered;
    }

    /** Constructor */
    public DominanceComparator(double epsilon) {
        this.epsilon = epsilon ;
    }

    /**
     * Compares two solutions.
     *
     * @param solution1 Object representing the first <code>Solution</code>.
     * @param solution2 Object representing the second <code>Solution</code>.
     * @return -1, or 0, or 1 if solution1 dominates solution2, both are
     * non-dominated, or solution1  is dominated by solution2, respectively.
     */
    @Override
    public int compare(S solution1, S solution2) {
        if (isCovered == null || isCovered.length < solution1.getNumberOfObjectives() - 1)
        {
            isCovered = new boolean[solution1.getNumberOfObjectives() - 1];
        }
        int result ;
        boolean solution1Dominates = false ;
        boolean solution2Dominates = false ;

        int flag;
        AbstractFitness value1, value2;
        int isUseSize = EvoSQLConfiguration.USE_SIZE ? 0 : 1;
        for (int i = 0; i < solution1.getNumberOfObjectives() - isUseSize; i++) {
            if (EvoSQLConfiguration.USE_DYNAMIC_OBJECTIVES && i < isCovered.length && isCovered[i])            //是否使用动态目标策略
            {
                continue;
            }
            value1 = solution1.getObjective(i);
            value2 = solution2.getObjective(i);
            try
            {
                flag = value1.compareTo(value2);
            }
            catch (Exception e)
            {
                int xx = 10;
                flag = 0;
            }
            if (flag == -1) {
                solution1Dominates = true ;
            }

            if (flag == 1) {
                solution2Dominates = true ;
            }
        }

        if (solution1Dominates == solution2Dominates) {
            // non-dominated solutions
            result = 0;
        } else if (solution1Dominates) {
            // solution1 dominates
            result = -1;
        } else {
            // solution2 dominates
            result = 1;
        }
        return result ;
    }
}
