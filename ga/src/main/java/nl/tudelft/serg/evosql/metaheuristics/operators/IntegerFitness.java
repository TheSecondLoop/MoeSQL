package nl.tudelft.serg.evosql.metaheuristics.operators;

public class IntegerFitness extends AbstractFitness{
    public int fitnessValue;

    public IntegerFitness(int fitnessValue)
    {
        this.fitnessValue = fitnessValue;
    }

    @Override
    public int compareTo(AbstractFitness targetFitness) {
        double dis = doubleCompareTo(targetFitness);
        if (dis > 0)
            return 1;
        else if (dis == 0)
            return 0;
        else
            return -1;
    }

    @Override
    public double doubleCompareTo(AbstractFitness targetFitness) {

        if (!(targetFitness instanceof IntegerFitness))
        {
            return 0;
        }
        int f1 = fitnessValue;
        int f2 = ((IntegerFitness) targetFitness).fitnessValue;
        return f1 - f2;
    }

    @Override
    public AbstractFitness copy() {
        AbstractFitness clone = new IntegerFitness(fitnessValue);
        return clone;
    }

    @Override
    public String toString() {
        return Integer.toString(fitnessValue);
    }
}
