package nl.tudelft.serg.evosql.metaheuristics;

import nl.tudelft.serg.evosql.fixture.Fixture;
import nl.tudelft.serg.evosql.fixture.FixtureTable;
import nl.tudelft.serg.evosql.fixture.Solution;
import nl.tudelft.serg.evosql.fixture.TestCaseSolution;
import nl.tudelft.serg.evosql.sql.TableSchema;
import nl.tudelft.serg.evosql.util.random.Randomness;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public abstract class Algorithm<S extends Solution<?>> {
    protected static Logger log = LogManager.getLogger(Approach.class);
    protected static Randomness random = new Randomness();

    protected int generations = 0;
    protected int individualCount = 0;

    protected boolean timelimit = false;
    protected long startTime;
    protected long pathTime;

    //    protected int maxGenerations = 500000;
    protected int maxGenerations = 1;
    public void setPathTime(long pathTime)
    {
        this.pathTime = pathTime;
        timelimit = true;
    }
    public void setMaxGenerations(int maxGenerations)
    {
        this.maxGenerations = maxGenerations;
        timelimit = false;
    }
    /** Path under test**/
    protected List<String> pathList;
    protected boolean[] isCovered;
    public boolean[] getIsCovered()
    {
        boolean[] result = new boolean[isCovered.length];
        for (int i = 0; i < result.length; i++)
        {
            result[i] = isCovered[i];
        }
        return result;
    }

    protected Map<String, TableSchema> tableSchemas;

    protected String exceptions;

    public Algorithm(Map<String, TableSchema> pTableSchemas, List<String> pathList) {
        this.tableSchemas = pTableSchemas;
        this.pathList = pathList;
        this.exceptions = "";

        this.isCovered = new boolean[pathList.size()];
    }

    public List<S> execute(long pathTime) throws SQLException {
        throw new RuntimeException("Approach did not implement execute.");
    }

    public int getGenerations() {
        return generations;
    }

    public int getIndividualCount() {
        return individualCount;
    }

    public String getExceptions() {
        return exceptions;
    }

    public Map<String, TableSchema> getTableSchemas() {
        return tableSchemas;
    }

    protected Fixture minimize (Fixture fixture, String sqlToTest) throws SQLException {
        Fixture minimizedFixture = fixture.copy();

        if (!hasOutput(minimizedFixture, sqlToTest))
            return minimizedFixture;

        // Loop over all tables
        for (FixtureTable ft : minimizedFixture.getTables()) {
            // Go over all rows, test them without this row and if hasOutput becomes true remove row
            for (int i = 0; i < ft.getRowCount() && ft.getRowCount() > 1 /*At least 1 row per table remains*/; i++) {
                if (hasOutput(minimizedFixture, sqlToTest, ft.getName(), i)) {
                    minimizedFixture.remove(ft.getName(), i);
                    i--;
                }
            }
        }

        return minimizedFixture;
    }

    protected TestCaseSolution minimize (TestCaseSolution solution, boolean[] solutionCovered) throws SQLException {
        TestCaseSolution minimizedSolution = solution.copy();
        minimizedSolution.setChanged(true);

        Fixture minimizedFixture = minimizedSolution.getVariables();

        if (!hasOutput(minimizedSolution, solutionCovered))
            return minimizedSolution;

        // Loop over all tables
        for (FixtureTable ft : minimizedFixture.getTables()) {
            // Go over all rows, test them without this row and if hasOutput becomes true remove row
            for (int i = 0; i < ft.getRowCount() && ft.getRowCount() > 1 /*At least 1 row per table remains*/; i++) {
                if (hasOutput(minimizedSolution, solutionCovered, ft.getName(), i)) {
                    minimizedFixture.remove(ft.getName(), i);
                    i--;
                }
            }
        }

        return minimizedSolution;
    }

    public DatabaseOutput fetchOutput(Fixture fixture, String sql) throws SQLException {
        insertData(fixture, "", -1);
        Statement st = genetic.Instrumenter.getStatement();

        // Initialize ResultSet
        ResultSet res = st.executeQuery(sql);
        DatabaseOutput dbOutput = new DatabaseOutput();
        ResultSetMetaData meta = res.getMetaData();

        // Extract column names
        List<String> columns = new ArrayList<>();
        for (int i = 1; i <= meta.getColumnCount(); i++) {
            columns.add(meta.getColumnLabel(i));
        }

        // For each row we make a map of values for each column
        while(res.next()) {
            Map<String, String> values = new HashMap<>();
            for (String column : columns) {
                Object value = res.getObject(column);
                values.put(column, value != null ? value.toString() : "NULL");
            }
            dbOutput.add(values);
        }

        return dbOutput;
    }

    public boolean hasOutput(Fixture fixture, String pathToTest) throws SQLException {
        return hasOutput(fixture, pathToTest, "", -1);
    }

    protected boolean hasOutput (Fixture fixture, String sql, String excludeTableName, int excludeIndex) throws SQLException {
        insertData(fixture, excludeTableName, excludeIndex);

        Statement st = genetic.Instrumenter.getStatement();

        try {
            ResultSet res = st.executeQuery(sql);
            // If next returns true there is at least one row.
            return res.next();
        } catch (SQLException e) {
            if (!exceptions.contains(e.getMessage())) {
                exceptions += ", " + e.getMessage();
            }
            log.error(e.toString());
            return false;
        }
    }

    protected boolean hasOutput (TestCaseSolution solution, boolean[] solutionCovered) throws SQLException {
        return hasOutput(solution, solutionCovered, "", -1);
    }

    protected boolean hasOutput (TestCaseSolution solution, boolean[] solutionCovered, String excludeTableName, int excludeIndex) throws SQLException {
        insertData(solution.getVariables(), excludeTableName, excludeIndex);
        Statement st = genetic.Instrumenter.getStatement();
        boolean result = true;
        try {
            for (int i = 0; i < pathList.size(); i++)
            {
                if (solutionCovered[i])
                {
                    boolean newResult = st.executeQuery(pathList.get(i)).next();
                    if (!newResult)
                    {
                        result = false;
                        break;
                    }
                }
            }
            return result;
        } catch (SQLException e) {
            if (!exceptions.contains(e.getMessage())) {
                exceptions += ", " + e.getMessage();
            }
            log.error(e.toString());
            return false;
        }
    }
    private void insertData(Fixture fixture, String excludeTableName, int excludeIndex) throws SQLException {
        // Truncate tables in Instrumented DB
        for (TableSchema tableSchema : tableSchemas.values()) {
            genetic.Instrumenter.execute(tableSchema.getTruncateSQL());
        }

        // Insert population
        for (String sqlStatement : fixture.getInsertStatements(excludeTableName, excludeIndex)) {
            genetic.Instrumenter.execute(sqlStatement);
        }
    }
}
