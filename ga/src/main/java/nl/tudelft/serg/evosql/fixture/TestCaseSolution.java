package nl.tudelft.serg.evosql.fixture;

import nl.tudelft.serg.evosql.EvoSQLConfiguration;
import nl.tudelft.serg.evosql.db.Seeds;
import nl.tudelft.serg.evosql.metaheuristics.operators.AbstractFitness;
import nl.tudelft.serg.evosql.metaheuristics.operators.CoverageFitness;
import nl.tudelft.serg.evosql.metaheuristics.operators.FixtureFitness;
import nl.tudelft.serg.evosql.metaheuristics.operators.IntegerFitness;
import nl.tudelft.serg.evosql.sql.TableSchema;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class TestCaseSolution extends Solution<Fixture> {

    /**
     * Constructor
     *
     * @param variables
     */
    public TestCaseSolution(Fixture variables) {
        super(variables);
    }

//    public TestCaseSolution(Fixture variables, AbstractFitness[] objectives) {
//        super(variables, objectives);
//    }
    @Override
    public void setChanged(boolean changed)
    {
        super.setChanged(changed);
        variables.setChanged(changed);
    }



    @Override
    public void initializeObjectiveValues(Map<String, TableSchema> tableSchemas, Seeds seeds) {
        variables = createFixture(tableSchemas, seeds);
    }

    @Override
    public void calculateFitness(List<String> pathList) throws SQLException {
        boolean[] isCovered = new boolean[pathList.size()];
        calculateFitness(pathList, isCovered);
    }
    @Override
    public void calculateFitness(List<String> pathList, boolean[] isCovered) throws SQLException {

        if (variables.isChanged())
        {
            if (objectives == null || objectives.length != pathList.size() + 1)
            {
                objectives = new AbstractFitness[pathList.size() + 1];
            }

            FixtureFitness[] fitnesses = calculateFixtureFitnessByPathList(variables, pathList, isCovered);
            for (int i = 0; i < pathList.size(); i++)
            {
                CoverageFitness coverageFitness = new CoverageFitness(fitnesses[i]);
                setObjective(i, coverageFitness);
            }

            int size = 0;
            for (FixtureTable table : variables.getTables())
            {
                size += table.getRowCount();
            }
            IntegerFitness integerFitness = new IntegerFitness(size);
            setObjective(pathList.size(), integerFitness);

            variables.setChanged(false);
            setChanged(false);
        }
    }

    public TestCaseSolution copy() {
        Fixture fixtureClone = this.variables.copy();
        return copy(fixtureClone);
    }
    public TestCaseSolution copy(Fixture fixture) {
        TestCaseSolution solutionClone = new TestCaseSolution(fixture);
        if (this.objectives != null)
        {
            solutionClone.objectives = new AbstractFitness[this.objectives.length];
            for (int i = 0; i < this.objectives.length; i++)
            {
                if (objectives[i] != null)
                {
                    solutionClone.objectives[i] = objectives[i].copy();
                }
            }
        }
        solutionClone.crowdingDistance = crowdingDistance;
        return solutionClone;
    }
}
