package nl.tudelft.serg.evosql.metaheuristics;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Random;

import nl.tudelft.serg.evosql.EvoSQLConfiguration;
import nl.tudelft.serg.evosql.db.Seeds;
import nl.tudelft.serg.evosql.fixture.*;
import nl.tudelft.serg.evosql.metaheuristics.operators.*;
import nl.tudelft.serg.evosql.sql.TableSchema;
import nl.tudelft.serg.evosql.util.random.Randomness;

public class TCGA extends AbstractGA<TestCaseSolution> {

    public TCGA(List<TestCaseSolution> population, Map<String, TableSchema> pTableSchemas, List<String> pathList, Seeds seeds) {
        super(population, pTableSchemas, pathList, seeds);

    }

    @Override
    protected List<TestCaseSolution> createInitialPopulation() {
        List<TestCaseSolution> initialPopulation = new ArrayList<>();
        for (int i = 0; i < this.getMaxPopulationSize() * 2; i++)
        {
            TestCaseSolution solution = new TestCaseSolution(null);
            solution.initializeObjectiveValues(tableSchemas, seeds);
            initialPopulation.add(solution);
        }
        return initialPopulation;
    }
}
