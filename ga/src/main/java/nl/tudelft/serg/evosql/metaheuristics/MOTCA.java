package nl.tudelft.serg.evosql.metaheuristics;

import java.sql.SQLException;
import java.sql.Statement;
import java.util.*;

import nl.tudelft.serg.evosql.EvoSQLConfiguration;
import nl.tudelft.serg.evosql.fixture.*;
import nl.tudelft.serg.evosql.metaheuristics.operators.*;
import nl.tudelft.serg.evosql.sql.ColumnSchema;
import nl.tudelft.serg.evosql.sql.TableSchema;

public class MOTCA extends Algorithm<TestCaseSolution> {

    /** Current Population **/
    protected List<TestCaseSolution> population;
    protected List<TestCaseSolution> archive;

    /** Comparator **/
    protected FixtureComparator fc = new FixtureComparator();

    protected int populationSize;


    public MOTCA(List<TestCaseSolution> population, Map<String, TableSchema> pTableSchemas, List<String> pathList){
        super(pTableSchemas, pathList);

        //this.mutation = new FixtureMutation(rowFactory, seeds);
        this.population = population;

        // if it's baseline, there will be only a single generation, and population will be larger
        populationSize = EvoSQLConfiguration.POPULATION_SIZE;//EvoSQLConfiguration.POPULATION_SIZE; // * (baseline ? 2 : 1);
//        populationSize = pathList.size() + 1;
        isCovered = new boolean[pathList.size()];
        for (TestCaseSolution solution : population)
        {
            boolean[] solutionCovered = getCover(solution);
            for (int i = 0; i < isCovered.length; i++)
            {
                isCovered[i] = isCovered[i] || solutionCovered[i];
            }
        }
    }
    public List<Double> getCoveredDistance()
    {
        ArrayList<Double> coveredDistance = new ArrayList<>(pathList.size());
        List<TestCaseSolution> solution = population;
        for (int j = 0; j < pathList.size(); j++)
        {
            coveredDistance.add(Double.MAX_VALUE);
        }
        for (int i = 0; i < solution.size(); i++)
        {
            for (int j = 0; j < pathList.size(); j++)
            {
                if (solution.get(i).getObjective(j) instanceof CoverageFitness)
                {
                    CoverageFitness coverageFitness = (CoverageFitness) solution.get(i).getObjective(j);
                    if (coverageFitness.fixtureFitness.getDistance() < coveredDistance.get(j))
                    {
                        coveredDistance.set(j, coverageFitness.fixtureFitness.getDistance());
                    }
                }
            }
        }
        return coveredDistance;
    }

    public int getCoveredNum()
    {
        int count = 0;
        for (int i = 0; i < isCovered.length; i++)
        {
            if (isCovered[i])
                count++;
        }
        return count;
    }

    public long startTime;
    public long pathTime;
    @Override
    public List<TestCaseSolution> execute(long pathTime) throws SQLException {
        startTime = System.currentTimeMillis();
        this.pathTime = pathTime;

        resortPopulation();

        boolean[] isUsed = new boolean[population.size() * 3];

        //穷举
        LoopA: for (int i = 0; i < population.size(); i++)
        {
            if (isUsed[i])
                continue;
            // 单
            TestCaseSolution offspring = exhaustiveMutation(population.get(i));
            if (offspring != null)
            {
                isUsed[i] = true;
                generations++;
                population.add(offspring);

                offspring.setChanged(true);
                offspring.calculateFitness(pathList);
            }
            else
            {
                if (System.currentTimeMillis() - startTime >= pathTime)
                {
                    break LoopA;
                }
                // 双
                for (int j = i + 1; j < population.size(); j++)
                {
                    if (isUsed[j])
                        continue;
                    List<TestCaseSolution> parents = new ArrayList<>();
                    parents.add(population.get(i));
                    parents.add(population.get(j));
                    offspring = exhaustiveCrossover(parents);
                    if (offspring != null)
                    {
                        isUsed[i] = true;
                        isUsed[j] = true;
                        generations++;
                        population.add(offspring);

                        offspring.setChanged(true);
                        offspring.calculateFitness(pathList);
                        break;
                    }
                    if (System.currentTimeMillis() - startTime >= pathTime)
                    {
                        break LoopA;
                    }
                }
            }
        }


        // 合并
        if (System.currentTimeMillis() - startTime < pathTime)
        {
            for (int i = 0; i < population.size(); i++)
            {
                if (isUsed[i])
                    continue;
                for (int j = i + 1; j < population.size(); j++)
                {
                    if (isUsed[j])
                        continue;
                    List<TestCaseSolution> parents = new ArrayList<>();
                    parents.add(population.get(i));
                    parents.add(population.get(j));
                    TestCaseSolution offspring = unionSolution(parents);
                    if (offspring != null)
                    {
                        isUsed[i] = true;
                        isUsed[j] = true;
                        population.add(offspring);

                        offspring.setChanged(true);
                        offspring.calculateFitness(pathList);
                        break;
                    }
                }
            }
        }


        // 整理
        List<TestCaseSolution> finalSolutions = new ArrayList<>();
        for (int i = 0; i < population.size(); i++)
        {
            if (isUsed[i])
                continue;
            finalSolutions.add(population.get(i));
        }
        population = finalSolutions;
        return population;
    }

    private TestCaseSolution unionSolution(List<TestCaseSolution> parents) {
        // 收集目标
        boolean[] paCover = getCover(parents.get(0));
        boolean[] pbCover = getCover(parents.get(1));
        boolean[] parentsCover = new boolean[paCover.length];
        boolean isACoverB = true;
        boolean isBCoverA = true;
        for (int i = 0; i < parentsCover.length; i++)
        {
            parentsCover[i] = paCover[i] || pbCover[i];
            if (paCover[i] && !pbCover[i])
            {
                isBCoverA = false;
            }
            else if (!paCover[i] && pbCover[i])
            {
                isACoverB = false;
            }
        }
        if (isACoverB || isBCoverA)         // 相互覆盖
        {
            return null;
        }

        // 合并
        int tableNum = parents.get(0).getVariables().getNumberOfTables();
        FixtureRowFactory rowFactory = new FixtureRowFactory();

        List<FixtureTable> tables = new ArrayList<>();
        for (int i = 0; i < tableNum; i++)
        {
            List<FixtureRow> rows = new ArrayList<>();
            TableSchema tableSchema = parents.get(0).getVariables().getTable(i).getSchema();
            for (int j = 0; j < parents.size(); j++)
            {
                FixtureTable table = parents.get(j).getVariables().getTable(i);
                for (int k = 0; k < table.getRows().size(); k++)
                {
                    FixtureRow sourceRow = table.getRows().get(k);    //第k个元组
                    FixtureRow newRow = rowFactory.create(tableSchema, tables, null);
                    for (int columnIndex = 0; columnIndex < tableSchema.getColumns().size(); columnIndex++)
                    {
                        ColumnSchema columnSchema = tableSchema.getColumns().get(columnIndex);
                        newRow.set(columnSchema.getName(), sourceRow.getValueFor(columnSchema.getName()));
                    }
                    rows.add(newRow);
                }
            }
            tables.add(new FixtureTable(tableSchema, rows));
        }

        // 验证
        Fixture fixture = new Fixture(tables);
        TestCaseSolution testCaseSolution = new TestCaseSolution(fixture);
        boolean needed;
        try
        {
            needed = hasOutput(testCaseSolution, parentsCover);
        }
        catch (Exception e)
        {
            needed = false;
        }
        if (needed)
        {
            return testCaseSolution;
        }
        else
        {
            return null;
        }
    }

    public boolean[] getCover(TestCaseSolution solution)
    {
        boolean[] solutionCovered = new boolean[solution.getNumberOfObjectives() - 1];
        for (int i = 0; i < solutionCovered.length; i++)
        {
            if (solution.getObjective(i) instanceof CoverageFitness)
            {
                CoverageFitness coverageFitness = (CoverageFitness) solution.getObjective(i);
                solutionCovered[i] = (coverageFitness.fixtureFitness.getDistance() <= 0);
            }
        }
        return solutionCovered;
    }
    public List<TestCaseSolution> resortPopulation()
    {
        List<TestCaseSolution> finalSolutions = new ArrayList<>();
        boolean[] finalCovered = new boolean[isCovered.length];
        for (int i = population.size() - 1; i >= 0; i--)
        {
            boolean[] solutionCovered = getCover(population.get(i));
            boolean addToFinal = false;
            for (int j = 0; j < isCovered.length; j++)
            {
                if (isCovered[j] && !finalCovered[j] && solutionCovered[j])
                {
                    addToFinal = true;
                    break;
                }
            }
            if (addToFinal)
            {
                finalSolutions.add(population.get(i));
                boolean allCovered = true;
                for (int j = 0; j < isCovered.length; j++) {
                    if (isCovered[j])
                    {
                        finalCovered[j] = finalCovered[j] || solutionCovered[j];
                        allCovered = allCovered && finalCovered[j];
                    }
                }
                if (allCovered)
                {
                    break;
                }
            }
        }
        population = finalSolutions;
        return population;
    }

    public TestCaseSolution exhaustiveMutation(TestCaseSolution parent)
    {
        boolean[] pCover = getCover(parent);
        // 收集素材
        int tableNum = parent.getVariables().getNumberOfTables();
        List<Map<String, Set<String>>> material = new ArrayList<>(tableNum);        //Map=table String=column name
        int[] numberOfRowsList = new int[tableNum];
        TableSchema[] tableSchemaList = new TableSchema[tableNum];

        for (int i = 0; i < tableNum; i++)
        {
            Map<String, Set<String>> mapMaterial = new HashMap<>();
            material.add(mapMaterial);
        }

        List<FixtureTable> source = parent.getVariables().getTables();
        for (int j = 0; j < source.size(); j++)
        {
            List<FixtureRow> sourceRowList = source.get(j).getRows();    //第j个表
            TableSchema tableSchema =  source.get(j).getSchema();
            numberOfRowsList[j] = sourceRowList.size();
            tableSchemaList[j] = tableSchema;
            for (int k = 0; k < sourceRowList.size(); k++)
            {
                FixtureRow sourceRow = sourceRowList.get(k);    //第k个元组
                for (int columnIndex = 0; columnIndex < tableSchema.getColumns().size(); columnIndex++)
                {
                    ColumnSchema columnSchema = tableSchema.getColumns().get(columnIndex);
                    if (columnSchema.isUsedColumn())
                    {
                        Set<String> valueSet = material.get(j).computeIfAbsent(columnSchema.getName(), k1 -> new HashSet<>());
                        valueSet.add(sourceRow.getValueFor(columnSchema.getName()));
                    }
                }
            }
        }

        return getSpringByMaterial(material, numberOfRowsList, pCover, tableSchemaList);

    }
    public TestCaseSolution exhaustiveCrossover(List<TestCaseSolution> parents) {
        // 收集目标
        boolean[] paCover = getCover(parents.get(0));
        boolean[] pbCover = getCover(parents.get(1));
        boolean[] parentsCover = new boolean[paCover.length];
        boolean isACoverB = true;
        boolean isBCoverA = true;
        for (int i = 0; i < parentsCover.length; i++)
        {
            parentsCover[i] = paCover[i] || pbCover[i];
            if (paCover[i] && !pbCover[i])
            {
                isBCoverA = false;
            }
            else if (!paCover[i] && pbCover[i])
            {
                isACoverB = false;
            }
        }
        if (isACoverB || isBCoverA)         // 相互覆盖
        {
            return null;
        }
        // 收集素材
        int tableNum = parents.get(0).getVariables().getNumberOfTables();
        List<Map<String, Set<String>>> material = new ArrayList<>(tableNum);        //Map=table String=column name
        int[] numberOfRowsList = new int[tableNum];
        TableSchema[] tableSchemaList = new TableSchema[tableNum];

        for (int i = 0; i < tableNum; i++)
        {
            Map<String, Set<String>> mapMaterial = new HashMap<>();
            material.add(mapMaterial);
        }

        for (int i = 0; i < parents.size(); i++)
        {
            List<FixtureTable> source = parents.get(i).getVariables().getTables();    // 第i个父代

            for (int j = 0; j < source.size(); j++)
            {
                List<FixtureRow> sourceRowList = source.get(j).getRows();    //第j个表
                TableSchema tableSchema =  source.get(j).getSchema();
                numberOfRowsList[j] += sourceRowList.size();
                tableSchemaList[j] = tableSchema;

                for (int k = 0; k < sourceRowList.size(); k++)
                {
                    FixtureRow sourceRow = sourceRowList.get(k);    //第k个元组
                    for (int columnIndex = 0; columnIndex < tableSchema.getColumns().size(); columnIndex++)
                    {
                        ColumnSchema columnSchema = tableSchema.getColumns().get(columnIndex);
                        if (columnSchema.isUsedColumn())
                        {
                            Set<String> valueSet = material.get(j).computeIfAbsent(columnSchema.getName(), k1 -> new HashSet<>());
                            valueSet.add(sourceRow.getValueFor(columnSchema.getName()));
                        }
                    }
                }
            }
        }

        return getSpringByMaterial(material, numberOfRowsList, parentsCover, tableSchemaList);
    }




    public TestCaseSolution getSpringByMaterialPast(List<Map<String, Set<String>>> material,
                                                int[] numberOfRowsList,
                                                boolean[] parentsCover,
                                                TableSchema[] tableSchemaList) {
        int tableNum = material.size();
        FixtureRowFactory rowFactory = new FixtureRowFactory();
        List<FixtureTable> tables = new ArrayList<FixtureTable>();

        Fixture fixture = new Fixture(tables);
        boolean needed = false;
        exhaustiveloop: for (int decIndex = 0; decIndex < tableNum; decIndex++)
        {
            // 初始化
            tables.clear();

            for (int i = 0; i < tableNum; i++) {
                List<FixtureRow> rows = new ArrayList<FixtureRow>();
                int numberOfRows = numberOfRowsList[i];
                if (decIndex == i)
                {
                    numberOfRows--;
                }
                TableSchema tableSchema = tableSchemaList[i];
                for(int j = 0; j < numberOfRows; j++) {
                    FixtureRow row = rowFactory.create(tableSchema, tables, null);
                    rows.add(row);
                }
                tables.add(new FixtureTable(tableSchema, rows));
            }

            List<List<Map<String, Integer>>> maxCountList = new ArrayList<>();
            List<List<Map<String, Integer>>> countList = new ArrayList<>();
            for (int i = 0; i < tableNum; i++)
            {
                maxCountList.add(new ArrayList<>());
                countList.add(new ArrayList<>());
                int numberOfRows =  tables.get(i).getRowCount();;
                for (int j = 0; j < numberOfRows; j++)
                {
                    maxCountList.get(i).add(new HashMap<>());
                    countList.get(i).add(new HashMap<>());
                    for (Map.Entry<String, Set<String>> entry : material.get(i).entrySet())
                    {
                        maxCountList.get(i).get(j).put(entry.getKey(), entry.getValue().size());
                        countList.get(i).get(j).put(entry.getKey(), 0);
                    }
                }
            }
            // 穷举
            while (true)
            {
                if (System.currentTimeMillis() - startTime >= pathTime)
                {
                    break exhaustiveloop;
                }
                // 赋值
                for (int i = 0; i < tableNum; i++) {
                    for (int j = 0; j < countList.get(i).size(); j++) {
                        for (Map.Entry<String, Integer> entry : countList.get(i).get(j).entrySet())
                        {
                            String value = (String) material.get(i).get(entry.getKey()).toArray()[entry.getValue()];
                            tables.get(i).getRows().get(j).set(entry.getKey(), value);
                        }
                    }
                }
                // 验证
                individualCount++;
                try
                {
                    needed = hasOutput(new TestCaseSolution(fixture), parentsCover);
                }
                catch (Exception e)
                {
                    needed = false;
                }
                if (needed)
                {
                    break exhaustiveloop;
                }
                // 下轮循环准备
                boolean overflow = true;
                start: for (int i = 0; i < tableNum; i++) {
                    for (int j = 0; j < countList.get(i).size(); j++) {
                        for (Map.Entry<String, Integer> entry : countList.get(i).get(j).entrySet())
                        {
                            if (entry.getValue() + 1 >= maxCountList.get(i).get(j).get(entry.getKey()))
                            {
                                countList.get(i).get(j).put(entry.getKey(), 0);
                            }
                            else
                            {
                                countList.get(i).get(j).put(entry.getKey(), entry.getValue() + 1);
                                overflow = false;
                                break start;
                            }
                        }
                    }
                }
                if (overflow)
                {
                    break;
                }
            }
        }

        if (needed)
        {
            TestCaseSolution solution = new TestCaseSolution(fixture);
            solution.setChanged(true);
            try
            {
                solution.calculateFitness(pathList);
                return minimize(solution, parentsCover);
            }
            catch (Exception e)
            {
                return null;
            }
        }
        else
            return null;
    }



    public TestCaseSolution getSpringByMaterial(List<Map<String, Set<String>>> material,
                                                int[] numberOfRowsList,
                                                boolean[] parentsCover,
                                                TableSchema[] tableSchemaList) {
        int tableNum = material.size();
        FixtureRowFactory rowFactory = new FixtureRowFactory();
        List<FixtureTable> tables = new ArrayList<FixtureTable>();

        Fixture fixture = new Fixture(tables);



        List<List<Integer>> rowState = new ArrayList<>();           //0-待考察/可用, 1-可不用，2-删除
        // 构造所有元组
        for (int i = 0; i < tableNum; i++)
        {
            rowState.add(new ArrayList<>());
            List<FixtureRow> rows = new ArrayList<FixtureRow>();
            TableSchema tableSchema = tableSchemaList[i];

            Map<String, Integer> maxCountList = new HashMap<>();
            Map<String, Integer> countList = new HashMap<>();

            for (Map.Entry<String, Set<String>> entry : material.get(i).entrySet())
            {
                maxCountList.put(entry.getKey(), entry.getValue().size());
                countList.put(entry.getKey(), 0);
            }

            while (true)
            {
                FixtureRow row = rowFactory.create(tableSchema, tables, null);
                // 赋值
                for (Map.Entry<String, Integer> entry : countList.entrySet())
                {
                    String value = (String) material.get(i).get(entry.getKey()).toArray()[entry.getValue()];
                    row.set(entry.getKey(), value);
                }
                rows.add(row);
                rowState.get(i).add(0);

                boolean overflow = true;
                for (Map.Entry<String, Integer> entry : countList.entrySet())
                {
                    if (entry.getValue() + 1 >= maxCountList.get(entry.getKey()))
                    {
                        countList.put(entry.getKey(), 0);
                    }
                    else
                    {
                        countList.put(entry.getKey(), entry.getValue() + 1);
                        overflow = false;
                        break;
                    }
                }
                if (overflow)
                {
                    break;
                }
            }
            tables.add(new FixtureTable(tableSchema, rows));
        }

        //验证

        int upperSize = 0;
        for (int i = 0; i < numberOfRowsList.length; i++)
        {
            upperSize += numberOfRowsList[i];
        }
        TestCaseSolution solution = new TestCaseSolution(fixture);

        boolean needed = false;
        boolean canBeMaxSet = false;
        individualCount++;
        try
        {
            canBeMaxSet = hasOutput(new TestCaseSolution(fixture), parentsCover);
        }
        catch (Exception e)
        {
        }
        if (canBeMaxSet)
        {
            while (true)
            {
                if (System.currentTimeMillis() - startTime >= pathTime)
                {
                    break;
                }
                // 去除脂肪/肌肉元素
                // Loop over all tables
                firstStart:
                for (int i = 0; i < tableNum; i++) {
                    // Go over all rows, test them without this row and if hasOutput becomes true remove row
                    for (int j = 0; j < rowState.get(i).size(); j++) {

                        if (System.currentTimeMillis() - startTime >= pathTime)
                        {
                            break firstStart;
                        }
                        if (rowState.get(i).get(j) == 0)
                        {
                            rowState.get(i).set(j, 1);
                            boolean result = true;
                            individualCount++;
                            try{
                                // 建表
                                for (int tableIndex = 0; tableIndex < tableNum; tableIndex++) {
                                    TableSchema tableSchema = tables.get(tableIndex).getSchema();

                                    genetic.Instrumenter.execute(tableSchema.getTruncateSQL());
                                    String sql = tableSchema.getInsertSQL() + " VALUES ";
                                    int rowSize = tables.get(tableIndex).getRows().size();
                                    for (int rowIndex = 0; rowIndex < rowSize; rowIndex++) {
                                        if (rowState.get(tableIndex).get(rowIndex) != 0)
                                            continue;
                                        sql += tables.get(tableIndex).getRows().get(rowIndex).getValuesSQL() + ", ";
                                    }
                                    sql = sql.substring(0, sql.length() - 2);
                                    genetic.Instrumenter.execute(sql);
                                }

                                // 验证
                                Statement st = genetic.Instrumenter.getStatement();
                                for (int pathIndex = 0; pathIndex < pathList.size(); pathIndex++)
                                {
                                    if (parentsCover[pathIndex])
                                    {
                                        boolean newResult = st.executeQuery(pathList.get(pathIndex)).next();
                                        if (!newResult)
                                        {
                                            result = false;
                                            break;
                                        }
                                    }
                                }
                            }
                            catch (Exception e)
                            {
                                result = false;
                            }
                            if (!result) {
                                rowState.get(i).set(j, 0);
                            }
                        }
                    }
                }
                // 判断大小
                int size = 0;
                for (int i = 0; i < tableNum; i++) {
                    for (int j = 0; j < rowState.get(i).size(); j++)
                    {
                        if (rowState.get(i).get(j) == 0) {
                            size++;
                        }
                    }
                }
                if (size < upperSize)
                {
                    needed = true;
                    break;
                }
                // 去除肌肉元素
                boolean isDeleted = false;

                secondStart:
                for (int i = 0; i < tableNum; i++) {
                    for (int j = 0; j < rowState.get(i).size(); j++)
                    {
                        if (System.currentTimeMillis() - startTime >= pathTime)
                        {
                            break secondStart;
                        }

                        if (rowState.get(i).get(j) == 0)
                        {
                            rowState.get(i).set(j, 2);
                            boolean result = true;
                            individualCount++;
                            try{
                                // 建表
                                for (int tableIndex = 0; tableIndex < tableNum; tableIndex++) {
                                    TableSchema tableSchema = tables.get(tableIndex).getSchema();

                                    genetic.Instrumenter.execute(tableSchema.getTruncateSQL());
                                    String sql = tableSchema.getInsertSQL() + " VALUES ";
                                    int rowSize = tables.get(tableIndex).getRows().size();
                                    for (int rowIndex = 0; rowIndex < rowSize; rowIndex++) {
                                        if (rowState.get(tableIndex).get(rowIndex) == 2)
                                            continue;
                                        sql += tables.get(tableIndex).getRows().get(rowIndex).getValuesSQL() + ", ";
                                    }
                                    sql = sql.substring(0, sql.length() - 2);
                                    genetic.Instrumenter.execute(sql);
                                }

                                // 验证
                                Statement st = genetic.Instrumenter.getStatement();
                                for (int pathIndex = 0; pathIndex < pathList.size(); pathIndex++)
                                {
                                    if (parentsCover[pathIndex])
                                    {
                                        boolean newResult = st.executeQuery(pathList.get(pathIndex)).next();
                                        if (!newResult)
                                        {
                                            result = false;
                                            break;
                                        }
                                    }
                                }
                            }
                            catch (Exception e)
                            {
                                result = false;
                            }
                            if (!result) {
                                rowState.get(i).set(j, 0);
                            }
                            else
                            {
                                isDeleted = true;
                                break secondStart;
                            }
                        }
                    }
                }
                if (!isDeleted)     //全为骨架元素
                    break;
                // 整理
                for (int i = 0; i < tableNum; i++) {
                    for (int j = 0; j < rowState.get(i).size(); j++) {
                        if (rowState.get(i).get(j) == 1) {
                            rowState.get(i).set(j, 0);
                        }
                    }
                }
            }
        }



        if (needed)
        {
            solution.setChanged(true);
            TestCaseSolution minimizedSolution = solution.copy();
            for (int i = 0; i < tableNum; i++) {
                int deleteNum = 0;
                for (int j = 0; j < rowState.get(i).size(); j++) {
                    if (rowState.get(i).get(j) != 0) {
                        String tableName = minimizedSolution.getVariables().getTable(i).getName();
                        minimizedSolution.getVariables().remove(tableName, j - deleteNum);
                        deleteNum++;
                    }
                }
            }
            return minimizedSolution;
        }
        else
            return null;
    }
}
