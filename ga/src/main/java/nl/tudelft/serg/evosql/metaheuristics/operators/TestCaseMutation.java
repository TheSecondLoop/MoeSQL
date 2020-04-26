package nl.tudelft.serg.evosql.metaheuristics.operators;

import nl.tudelft.serg.evosql.db.Seeds;
import nl.tudelft.serg.evosql.fixture.FixtureRowFactory;
import nl.tudelft.serg.evosql.fixture.TestCaseSolution;

public class TestCaseMutation implements MutationOperator<TestCaseSolution>{
    public Seeds seeds;
    public TestCaseMutation(Seeds seeds)
    {
        this.seeds = seeds;
    }
    @Override
    public TestCaseSolution execute(TestCaseSolution testCaseSolution) {
        FixtureMutation fixtureMutation = new FixtureMutation(new FixtureRowFactory(), seeds);
        fixtureMutation.mutate(testCaseSolution.getVariables());
        testCaseSolution.setChanged(testCaseSolution.getVariables().isChanged());
        return testCaseSolution;
    }
}
