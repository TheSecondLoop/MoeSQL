package nl.tudelft.serg.evosql.metaheuristics.operators;

import nl.tudelft.serg.evosql.fixture.Fixture;
import nl.tudelft.serg.evosql.fixture.FixtureTable;
import nl.tudelft.serg.evosql.fixture.TestCaseSolution;
import nl.tudelft.serg.evosql.util.random.Randomness;

import java.util.ArrayList;
import java.util.List;

public class TestCaseCrossover implements CrossoverOperator<TestCaseSolution>{

    private Randomness random;

    public TestCaseCrossover(Randomness random) {
        this.random = random;
    }

    public TestCaseCrossover() {
        this.random = new Randomness();
    }
    @Override
    public int getNumberOfParents() {
        return 2;
    }

    @Override
    public List<TestCaseSolution> execute(List<TestCaseSolution> parents) {

        List<TestCaseSolution> offspring = new ArrayList<TestCaseSolution>();


        Fixture parent1 = parents.get(0).getVariables();
        Fixture parent2 = parents.get(1).getVariables();


        FixtureCrossover fixtureCrossover = new FixtureCrossover(random);

        Fixture[] fixturesOffspring = fixtureCrossover.crossover(parent1, parent2);

        if (fixturesOffspring[0].isChanged() || fixturesOffspring[1].isChanged())
        {
            offspring.add(new TestCaseSolution(fixturesOffspring[0]));
            offspring.add(new TestCaseSolution(fixturesOffspring[1]));
            return offspring;
        }
        else
        {
            offspring.add(parents.get(0).copy(fixturesOffspring[0]));
            offspring.add(parents.get(1).copy(fixturesOffspring[1]));
            offspring.get(0).setChanged(false);
            offspring.get(1).setChanged(false);
            return offspring;
        }
    }
}
