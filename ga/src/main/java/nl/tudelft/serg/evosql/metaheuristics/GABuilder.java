package nl.tudelft.serg.evosql.metaheuristics;

import nl.tudelft.serg.evosql.db.SchemaExtractor;
import nl.tudelft.serg.evosql.db.SeedExtractor;
import nl.tudelft.serg.evosql.db.Seeds;
import nl.tudelft.serg.evosql.fixture.*;
import nl.tudelft.serg.evosql.fixture.type.DBInteger;
import nl.tudelft.serg.evosql.fixture.type.DBString;
import nl.tudelft.serg.evosql.metaheuristics.operators.*;
import nl.tudelft.serg.evosql.sql.ColumnSchema;
import nl.tudelft.serg.evosql.sql.TableSchema;
import nl.tudelft.serg.evosql.util.random.Randomness;

import java.io.IOException;
import java.io.StringReader;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class GABuilder {

    static String jdbcUrl = "jdbc:hsqldb:mem/EvoSQLtest";
    static String user = "SA";
    static String database = "PUBLIC";
    static String schema = "PUBLIC";
    static String pwd = "";

    public static void main(String[] args) throws Exception {
        List<String> pathList = new ArrayList<>();
        String sql1 = "SELECT \"ATTI\" FROM \"TAB\" WHERE (\"TAB\".\"ATTI\" LIKE \'%%yoyo%%\')";
//        String sql1 = "SELECT \"tabDocType\".\"name\" FROM \"tabDocType\" WHERE (\"tabDocType\".\"name\" LIKE '%%yoyo%%')";
        String sql2 = "SELECT \"ATTI\" FROM \"TAB\" WHERE (\"TAB\".\"ATTI\" NOT LIKE \'%%yoyo%%\')";
//        String sql1 = "SELECT \"ATTI\" FROM \"TAB\" WHERE (\"TAB\".\"ATTI\" = 1)";
        pathList.add(sql1);
        pathList.add(sql2);
        //System.out.println(1111);
        String tableName = "TAB";
        String firstColumn = "ATTI";
        Map<String, TableSchema> tableSchemas = new HashMap<String, TableSchema>();
        List<ColumnSchema> columns = new ArrayList<>();
        TableSchema tableSchema = new TableSchema(tableName, columns);
        ColumnSchema columnSchema = new ColumnSchema(tableSchema, firstColumn, new DBString(255), false, false);
//        ColumnSchema columnSchema = new ColumnSchema(tableSchema, firstColumn, new DBInteger(), false, false);
        columns.add(columnSchema);

        tableSchemas.put(tableName, tableSchema);

        genetic.Instrumenter.startDatabase();
        for (TableSchema ts : tableSchemas.values()) {
            genetic.Instrumenter.execute(ts.getDropSQL());
            genetic.Instrumenter.execute(ts.getCreateSQL());
        }


        FixtureRowFactory rowFactory = new FixtureRowFactory();
        List<FixtureTable> tables = new ArrayList<FixtureTable>();


        // 初始化
        List<FixtureRow> rows = new ArrayList<FixtureRow>();
        FixtureRow row1 = rowFactory.create(tableSchema, tables, null);
        row1.set("ATTI", "oyo");
        rows.add(row1);
        FixtureRow row2 = rowFactory.create(tableSchema, tables, null);
        row2.set("ATTI", "ooyoyooo");
        rows.add(row2);
        tables.add(new FixtureTable(tableSchema, rows));

        Fixture fixture = new Fixture(tables);
        MOTCA motca = new MOTCA(new ArrayList<>(),tableSchemas,new ArrayList<>());
        boolean res1 = motca.hasOutput(fixture, sql1);
        boolean res2 = motca.hasOutput(fixture, sql2);

        StandardGA ga = new StandardGA(new ArrayList<>(), tableSchemas, sql1, null);
        ga.calculateFitness(fixture);

        fixture.setChanged(true);
        TestCaseSolution tcs = new TestCaseSolution(fixture);
        tcs.calculateFitness(pathList);
        int a = 0;
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
