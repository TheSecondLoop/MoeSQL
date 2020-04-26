package nl.tudelft.serg.evosql.evaluation;

import com.mysql.cj.core.util.StringUtils;
import in2test.application.common.SQLToolsConfig;
import in2test.application.services.SQLMutationWSFacade;
import nl.tudelft.serg.evosql.EvoSQLException;
import nl.tudelft.serg.evosql.db.ISchemaExtractor;
import nl.tudelft.serg.evosql.db.TableXMLFormatter;
import nl.tudelft.serg.evosql.sql.TableSchema;
import nl.tudelft.serg.evosql.path.PathExtractor;
import nl.tudelft.serg.evosql.sql.parser.SqlSecurer;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class DemoRunner {
    public static void main(String[] args) throws Exception {

        String projectPath = "I:/workspace/Java/evosql-master/evaluation/scenarios/espocrm/";


        List<ScenarioQuery> queries = new ArrayList<ScenarioQuery>();
        List<String> lines = Files.readAllLines(Paths.get(projectPath + "queries.sql"), StandardCharsets.UTF_8);

        int queryNo = 0;
        ScenarioQuery sq;
        for (String line : lines) {
            queryNo++;
            String[] parts = line.split(",", 2);
            if (parts.length == 2 && StringUtils.isStrictlyNumeric(parts[0])) { // If line as: "<nr>,<query>"
                sq = new ScenarioQuery(Integer.parseInt(parts[0]), parts[1]);
            } else { // If line as: "<query>"
                sq = new ScenarioQuery(queryNo, line);
            }
            queries.add(sq);
        }

        Map<String, TableSchema> schemas;
        try {
            File file = Paths.get(projectPath + "database_schema.ser").toFile();
            if(!file.exists()) {
                System.out.println("mocked schema not found!");
                return;
            }

            FileInputStream fileIn = new FileInputStream(file);
            ObjectInputStream in = new ObjectInputStream(fileIn);
            schemas = (Map<String,TableSchema>) in.readObject();
            in.close();
            fileIn.close();
        }catch(IOException i) {
            i.printStackTrace();
            System.exit(-1);
            return;
        }catch(ClassNotFoundException c) {
            c.printStackTrace();
            System.exit(-1);
            return;
        }

        String sqlToBeTested = queries.get(0).getQuery();

        try {
            // Make sql safe
            sqlToBeTested = new SqlSecurer(sqlToBeTested).getSecureSql();
        } catch (RuntimeException e) {
            System.out.println("Could not parse input query.");
            e.printStackTrace();
            return;
        }
        System.out.println(0);
        PathExtractor pathExtractor = new PathExtractor(new MockedSchemaExtractor(schemas));
        System.out.println(0.1);
        pathExtractor.initialize();

        List<String> strings = pathExtractor.getPaths(sqlToBeTested);

        Package pack=Package.getPackage("java.lang");

        // get the annotation for lang package
        System.out.println("---"+pack.getImplementationTitle());


        String query = sqlToBeTested;

//        String title = (new ArrayList()).getClass().getPackage().getImplementationTitle();
//        System.out.println((new ArrayList()));
//        System.out.println((new ArrayList()).getClass());
//        System.out.println((new ArrayList()).getClass().getPackage());
//        System.out.println((new ArrayList()).getClass().getPackage().getImplementationTitle());
//        Package p = (new ArrayList()).getClass().getPackage();
//        ConfigDemo d = new ConfigDemo();
//        SQLToolsConfig.configure();
//        //if (!configured) throw new EvoSQLException("Path extractor has not been initialized");
//        List<String> paths = new ArrayList<String>();
//        ISchemaExtractor schemaExtractor = new MockedSchemaExtractor(schemas);
//        String schemaXml;
//        TableXMLFormatter tableXMLFormatter = new TableXMLFormatter(schemaExtractor, query);
//        try {
//            schemaXml = tableXMLFormatter.getSchemaXml();
//        } catch (Exception e) {
//            throw new Exception("Failed to extract the schema from the running database.", e);
//        }
//        String sqlfpcXml ="";
////		SQLFpcWSFacade wsFpc=new SQLFpcWSFacade();
////		sqlfpcXml=wsFpc.getRules(query, schemaXml, "");
//
//        SQLMutationWSFacade wsMut = new SQLMutationWSFacade();
//        sqlfpcXml = wsMut.getMutants(query, schemaXml, "");
//        System.out.println("00");
//        //extractPaths(sqlfpcXml, paths);

    }
}
