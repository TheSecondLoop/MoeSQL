package nl.tudelft.serg.evosql.fixture;

import nl.tudelft.serg.evosql.EvoSQLConfiguration;
import nl.tudelft.serg.evosql.db.Seeds;
import nl.tudelft.serg.evosql.metaheuristics.operators.AbstractFitness;
import nl.tudelft.serg.evosql.metaheuristics.operators.CoverageFitness;
import nl.tudelft.serg.evosql.metaheuristics.operators.FixtureFitness;
import nl.tudelft.serg.evosql.metaheuristics.operators.IntegerFitness;
import nl.tudelft.serg.evosql.sql.TableSchema;

import java.io.Serializable;
import java.sql.SQLException;
import java.util.*;

public abstract class Solution<T> implements Serializable {
    protected AbstractFitness[] objectives;
    public double crowdingDistance = 0.0;   //拥挤距离
    protected T variables;


    public int getSize()
    {
        int sizeIndex = objectives.length - 1;
        if (objectives[sizeIndex] instanceof IntegerFitness)
        {
            IntegerFitness fitness = (IntegerFitness) objectives[sizeIndex];
            return fitness.fitnessValue;
        }
        return 0;
    }
    public List<Double> getDistance()
    {
        List<Double> distances = new ArrayList<>(objectives.length - 1);
        for (int i = 0; i < objectives.length - 1; i++)
        {
            if (objectives[i] instanceof CoverageFitness)
            {
                CoverageFitness coverageFitness = (CoverageFitness) objectives[i];
                distances.add(coverageFitness.fixtureFitness.getDistance());
            }
        }
        return distances;
    }
    private boolean isChanged = true;
    public boolean getChanged()
    {
        return isChanged;
    }
    public void setChanged(boolean changed) {
        isChanged = changed;
    }

    public T getVariables()
    {
        return variables;
    }

    abstract public void calculateFitness(List<String> pathList) throws SQLException;

    abstract public void calculateFitness(List<String> pathList, boolean[] isCovered) throws SQLException;
    /**
     * Constructor
     */
    protected Solution(T variables) {
        this.variables = variables;
    }
//    protected Solution(T variables, AbstractFitness[] objectives) {     //拷贝
//        this.variables = variables;
//        this.objectives = objectives;
//        isChanged = false;
//    }


    public void setObjective(int index, AbstractFitness value) {
        objectives[index] = value ;
    }

    public AbstractFitness getObjective(int index) {
        return objectives[index];
    }

    public AbstractFitness[] getObjectives() {
        return objectives;
    }

    public int getNumberOfObjectives() {
        return objectives.length;
    }

    public abstract void initializeObjectiveValues(Map<String, TableSchema> tableSchemas, Seeds seeds);

    protected Fixture createFixture(Map<String, TableSchema> tableSchemas, Seeds seeds)
    {
        List<FixtureTable> tables = new ArrayList<FixtureTable>();
        for (TableSchema tableSchema : tableSchemas.values()) {
            tables.add(createFixtureTable(tableSchema, tables, seeds));
        }
        Fixture fixture = new Fixture(tables);
        fixture.setChanged(true);
        return fixture;
    }

    private FixtureTable createFixtureTable(TableSchema tableSchema, List<FixtureTable> tables, Seeds seeds) {

        List<FixtureRow> rows = new ArrayList<FixtureRow>();
        FixtureRowFactory rowFactory = new FixtureRowFactory();
        int numberOfRows = EvoSQLConfiguration.MIN_ROW_QTY;
        if (EvoSQLConfiguration.MAX_ROW_QTY > EvoSQLConfiguration.MIN_ROW_QTY)
            numberOfRows += new Random().nextInt(EvoSQLConfiguration.MAX_ROW_QTY - EvoSQLConfiguration.MIN_ROW_QTY);
        for(int j=0; j < numberOfRows; j++) {
            FixtureRow row = rowFactory.create(tableSchema, tables, seeds);
            rows.add(row);
        }
        return new FixtureTable(tableSchema, rows);
    }

    protected FixtureFitness calculateFixtureFitnessBySQLQuery(Fixture fixture, String sqlQuery) throws SQLException {

        // Truncate tables in Instrumented DB
        for (FixtureTable table : fixture.getTables()) {
            genetic.Instrumenter.execute(table.getSchema().getTruncateSQL());
        }

        // Insert population
        for (String sqlStatement : fixture.getInsertStatements()) {
            genetic.Instrumenter.execute(sqlStatement);
        }

        // Start instrumenter
        genetic.Instrumenter.startInstrumenting();

        // Execute the path
        genetic.Instrumenter.execute(sqlQuery);

        FixtureFitness ff = new FixtureFitness(genetic.Instrumenter.getFitness());
        //fixture.setFitness(ff);

//        // Store exceptions
//        if (!genetic.Instrumenter.getException().isEmpty() && !exceptions.contains(genetic.Instrumenter.getException())) {
//            exceptions += ", " + genetic.Instrumenter.getException();
//        }

        // Stop instrumenter
        genetic.Instrumenter.stopInstrumenting();

        // set the fixture as "not changed" to avoid future fitness function computation
        //fixture.setChanged(false);
        return ff;
    }

    protected FixtureFitness[] calculateFixtureFitnessByPathList(Fixture fixture, List<String> pathList, boolean[] isCovered) throws SQLException {
        FixtureFitness[] fitnesses = new FixtureFitness[pathList.size()];
        if (isCovered == null || isCovered.length != pathList.size())
        {
            isCovered = new boolean[pathList.size()];
        }

        for (FixtureTable table : fixture.getTables()) {
            genetic.Instrumenter.execute(table.getSchema().getTruncateSQL());
        }

        // Insert population
        for (String sqlStatement : fixture.getInsertStatements()) {
            genetic.Instrumenter.execute(sqlStatement);
        }


        for (int i = 0; i < pathList.size(); i++)
        {
            if (EvoSQLConfiguration.USE_DYNAMIC_OBJECTIVES && isCovered[i])            //是否使用动态目标策略
                continue;

            // Start instrumenter
            genetic.Instrumenter.startInstrumenting();

            // Execute the path
            genetic.Instrumenter.execute(pathList.get(i));
            FixtureFitness ff = new FixtureFitness(genetic.Instrumenter.getFitness());
            fitnesses[i] = ff;

            // Stop instrumenter
            genetic.Instrumenter.stopInstrumenting();
        }

        return fitnesses;
    }
    @Override
    public String toString() {

        String result = "Variables: " ;
        return result ;
    }

    @Override public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;

        Solution<?> that = (Solution<?>) o;

        if (!Arrays.equals(objectives, that.objectives))
            return false;
        if (!variables.equals(that.variables))
            return false;

        return true;
    }

    @Override public int hashCode() {
        int result = Arrays.hashCode(objectives);
        result = 31 * result + variables.hashCode();
        return result;
    }
}