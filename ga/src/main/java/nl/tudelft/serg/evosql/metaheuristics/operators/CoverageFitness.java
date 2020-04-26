package nl.tudelft.serg.evosql.metaheuristics.operators;

import genetic.QueryLevelData;

public class CoverageFitness extends AbstractFitness {

    public FixtureFitness fixtureFitness;

    public CoverageFitness(FixtureFitness fixtureFitness)
    {
        this.fixtureFitness = fixtureFitness;
    }

    @Override
    public String toString() {
        return Double.toString(fixtureFitness.lastLevelData.getDistance());
    }

    @Override
    public int compareTo(AbstractFitness targetFitness) {
        if (!(targetFitness instanceof CoverageFitness))
        {
            return 0;
        }
        FixtureFitness f1 = fixtureFitness;
        FixtureFitness f2 = ((CoverageFitness) targetFitness).fixtureFitness;

        if (f1 == null && f2 == null)
            return 0;
        else if (f1 == null)
            return 1;
        else if (f2 == null)
            return -1;

        // Compare max query levels, higher is better
        if (f1.getMaxQueryLevel() < f2.getMaxQueryLevel())
            return 1;
        else if (f1.getMaxQueryLevel() > f2.getMaxQueryLevel())
            return -1;

        // From max query level downwards check for differences
        for (int queryLevel = f1.getMaxQueryLevel(); queryLevel >= 0; queryLevel--) {
            QueryLevelData qld1 = f1.getQueryLevelData(queryLevel);
            QueryLevelData qld2 = f2.getQueryLevelData(queryLevel);

            int comp = qld1.compare(qld1, qld2);
            if (comp != 0)
                return comp;
        }

        return 0;

    }

    @Override
    public double doubleCompareTo(AbstractFitness targetFitness) {
        if (!(targetFitness instanceof CoverageFitness))
        {
            return 0;
        }
        FixtureFitness f1 = fixtureFitness;
        FixtureFitness f2 = ((CoverageFitness) targetFitness).fixtureFitness;

        if (f1 == null && f2 == null)
            return 0;
        else if (f1 == null)
            return 1 * 1000;
        else if (f2 == null)
            return -1 * 1000;

        // Compare max query levels, higher is better
        if (f1.getMaxQueryLevel() != f2.getMaxQueryLevel())
            return (f2.getMaxQueryLevel() - f1.getMaxQueryLevel()) * 100;

        int queryLevel = f1.getMaxQueryLevel();

        QueryLevelData qld1 = f1.getQueryLevelData(queryLevel);
        QueryLevelData qld2 = f2.getQueryLevelData(queryLevel);

        if (qld1.getMaxRangeVariableIndex() != qld2.getMaxRangeVariableIndex())
            return (qld2.getMaxRangeVariableIndex() - qld1.getMaxRangeVariableIndex()) * 10;

        // Compare distances, lower is better
        if (qld1.getDistance() != qld2.getDistance())
        {
            double dis = qld1.getDistance() - qld2.getDistance();
            return dis / (1 + dis);
        }

        return compareTo(targetFitness) * 0.1;
    }

    @Override
    public AbstractFitness copy() {
        FixtureFitness fitnessClone = null;
        if (fixtureFitness != null)
            fitnessClone = fixtureFitness.copy();
        AbstractFitness clone = new CoverageFitness(fixtureFitness);
        return clone;
    }

}
