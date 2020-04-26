package nl.tudelft.serg.evosql.metaheuristics;

import nl.tudelft.serg.evosql.EvoSQLConfiguration;
import nl.tudelft.serg.evosql.fixture.*;
import nl.tudelft.serg.evosql.fixture.type.DBString;
import nl.tudelft.serg.evosql.fixture.type.DBInteger;
import nl.tudelft.serg.evosql.sql.ColumnSchema;
import nl.tudelft.serg.evosql.sql.TableSchema;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MOTCATester {

    public static void main(String[] args) throws Exception {
        List<String> pathList = new ArrayList<>();
        String sql1 = "SELECT \"A1\" FROM \"TAB\" WHERE (\"TAB\".\"A1\" = 1) AND NOT (\"TAB\".\"A2\" = 1)";
        String sql2 = "SELECT \"A1\" FROM \"TAB\" WHERE NOT (\"TAB\".\"A1\" = 2) AND (\"TAB\".\"A2\" = 2)";
        pathList.add(sql1);
        pathList.add(sql2);
        //System.out.println(1111);
        String tableName = "TAB";
        String firstColumn = "A1";
        String SecondColumn = "A2";
        Map<String, TableSchema> tableSchemas = new HashMap<String, TableSchema>();
        List<ColumnSchema> columns = new ArrayList<>();
        TableSchema tableSchema = new TableSchema(tableName, columns);
        ColumnSchema columnSchemaA = new ColumnSchema(tableSchema, firstColumn, new DBInteger(), false, false);
        ColumnSchema columnSchemaB = new ColumnSchema(tableSchema, SecondColumn, new DBInteger(), false, false);

        columnSchemaA.setUsedColumn();
        columnSchemaB.setUsedColumn();
//        ColumnSchema columnSchema = new ColumnSchema(tableSchema, firstColumn, new DBInteger(), false, false);
        columns.add(columnSchemaA);
        columns.add(columnSchemaB);

        tableSchemas.put(tableName, tableSchema);

        genetic.Instrumenter.startDatabase();
        for (TableSchema ts : tableSchemas.values()) {
            genetic.Instrumenter.execute(ts.getDropSQL());
            genetic.Instrumenter.execute(ts.getCreateSQL());
        }


        FixtureRowFactory rowFactory = new FixtureRowFactory();

        List<FixtureTable> tablesA = new ArrayList<FixtureTable>();
        // 初始化
        List<FixtureRow> rowsA = new ArrayList<FixtureRow>();
        FixtureRow rowA1 = rowFactory.create(tableSchema, tablesA, null);
        rowA1.set("A1", "1");
        rowA1.set("A2", "3");
        rowsA.add(rowA1);
        FixtureRow rowA2 = rowFactory.create(tableSchema, tablesA, null);
        rowA2.set("A1", "4");
        rowA2.set("A2", "2");
        rowsA.add(rowA2);
        tablesA.add(new FixtureTable(tableSchema, rowsA));
        Fixture fixtureA = new Fixture(tablesA);

//        List<FixtureTable> tablesB = new ArrayList<FixtureTable>();
//        // 初始化
//        List<FixtureRow> rowsB = new ArrayList<FixtureRow>();
//        FixtureRow rowB1 = rowFactory.create(tableSchema, tablesB, null);
//        rowB1.set("A1", "4");
//        rowB1.set("A2", "2");
//        rowsB.add(rowB1);
//        tablesB.add(new FixtureTable(tableSchema, rowsB));
//        Fixture fixtureB = new Fixture(tablesB);


        TestCaseSolution tcsA = new TestCaseSolution(fixtureA);
        tcsA.setChanged(true);
        tcsA.calculateFitness(pathList);

        List<TestCaseSolution> pop = new ArrayList<>();
        pop.add(tcsA);
        MOTCA motca = new MOTCA(pop, tableSchemas, pathList);

        List<TestCaseSolution> secSolution = motca.execute(10000000l);

        genetic.Instrumenter.stopDatabase();
//        Seeds seeds = new SeedExtractor(sql1).extract();
//        Seeds seeds2 = new SeedExtractor(sql2).extract();
//        seeds.addSeeds(seeds2);

//        TCGA algorithm = new TCGA(null, tableSchemas, pathList, seeds);
//        //algorithm.selection = new RankingAndCrowdingSelection<>(algorithm.getMaxPopulationSize());
//        algorithm.selection = new MaxObjectiveSelection<>(algorithm.getMaxPopulationSize());
//        algorithm.crossover = new TestCaseCrossover();
//        algorithm.mutation = new TestCaseMutation(seeds);
//        algorithm.setMaxGenerations(5);
//        algorithm.execute(0);
    }


}
