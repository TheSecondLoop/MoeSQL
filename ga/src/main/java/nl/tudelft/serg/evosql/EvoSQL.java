package nl.tudelft.serg.evosql;

import java.io.*;
import java.nio.file.Paths;
import java.sql.SQLException;
import java.util.*;

import nl.tudelft.serg.evosql.db.ISchemaExtractor;
import nl.tudelft.serg.evosql.fixture.Solution;
import nl.tudelft.serg.evosql.fixture.TestCaseSolution;
import nl.tudelft.serg.evosql.metaheuristics.*;
import nl.tudelft.serg.evosql.metaheuristics.operators.*;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import nl.tudelft.serg.evosql.db.SchemaExtractor;
import nl.tudelft.serg.evosql.db.SeedExtractor;
import nl.tudelft.serg.evosql.db.Seeds;
import nl.tudelft.serg.evosql.fixture.Fixture;
import nl.tudelft.serg.evosql.path.PathExtractor;
import nl.tudelft.serg.evosql.sql.ColumnSchema;
import nl.tudelft.serg.evosql.sql.TableSchema;
import nl.tudelft.serg.evosql.sql.parser.SqlSecurer;
import nl.tudelft.serg.evosql.sql.parser.UsedColumnExtractor;

public class EvoSQL {

	private static Logger log = LogManager.getLogger(EvoSQL.class);
	
	private class PathState {
		int pathNo;
		String path;
		Approach approach;
		long timePassed;
		long timeBudget;
		List<Fixture> population;
		Set<ColumnSchema> usedColumns;
		
		PathState(int pathNo, String path, Approach approach, long timePassed, List<Fixture> population, long timeBudget) {
			this.pathNo = pathNo;
			this.path = path;
			this.approach = approach;
			this.timePassed = timePassed;
			this.timeBudget = timeBudget;
			this.population = population;
			usedColumns = null;
		}
	}
	
	private ISchemaExtractor schemaExtractor;
	private PathExtractor pathExtractor;

	private boolean baseline;
	
	public EvoSQL(String jdbcString, String dbDatabase, String dbUser, String dbPwd, boolean baseline) {
		this(new SchemaExtractor(jdbcString, dbDatabase, dbUser, dbPwd), baseline);
	}

	public EvoSQL(ISchemaExtractor se, boolean baseline) {
		this.schemaExtractor = se;
		pathExtractor = new PathExtractor(schemaExtractor);
		this.baseline = baseline;
	}
	public void replace(List<String> allPaths, String sqlToBeTested)
	{
		int preindex;
		int nextindex = -1;
		Map<String, String> nameList = new HashMap<>();
		while (true)
		{
			preindex = sqlToBeTested.indexOf('"',nextindex + 1);
			if (preindex == -1)
			{
				break;
			}
			nextindex = sqlToBeTested.indexOf('"',preindex + 1);
			if (nextindex == -1)
			{
				break;
			}

			String name = sqlToBeTested.substring(preindex, nextindex);
			nameList.put(name.toUpperCase(), name);
		}
		for (int i = 0; i < allPaths.size(); i++)
		{
			nextindex = -1;
			String path = allPaths.get(i);
			while (true)
			{
				preindex = path.indexOf('"',nextindex + 1);
				if (preindex == -1)
				{
					break;
				}
				nextindex = path.indexOf('"',preindex + 1);
				if (nextindex == -1)
				{
					break;
				}
				String name = path.substring(preindex, nextindex);
				path = path.replaceFirst(name, nameList.get(name));
			}
			path = path.replaceAll("%%", "%");
			allPaths.set(i, path);
		}
	}
	public void doDeletePath(int index, List<String> allPaths)
	{
		Map<Integer, List<Integer>> deleteMap = new HashMap<>();
		try
		{
			String deleteListPath = Paths.get(System.getProperty("user.dir")).toString()
					+ "/evaluation/scenarios/"
					+ EvoSQLConfiguration.dataSet + "/deleteList.csv";
			BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(new File(deleteListPath)),"UTF-8"));
			String lineTxt;
			while ((lineTxt = br.readLine()) != null) {
				String[] deleteString = lineTxt.split("\t");
				List<Integer> deleteListLine = new ArrayList<>();
				for (int i = 1; i < deleteString.length; i++) {
					deleteListLine.add(Integer.parseInt(deleteString[i]));
				}
				deleteMap.put(Integer.parseInt(deleteString[0]), deleteListLine);
			}
			br.close();
		}
		catch (Exception e)
		{

		}
		List<Integer> deleteList = deleteMap.get(index);
		if (deleteList != null)
		{
			for (int i = 0; i < deleteList.size(); i++)
			{
				allPaths.remove(deleteList.get(i) - i - 1);
			}
		}
		int x = 0;
	}

	public Result execute(String sqlToBeTested) {
		genetic.Instrumenter.startDatabase();

		log.info("SQL to be tested: " + sqlToBeTested);
		// Check if query can be parsed
		try {
			// Make sql safe
			sqlToBeTested = new SqlSecurer(sqlToBeTested).getSecureSql();
		} catch (RuntimeException e) {
			log.error("Could not parse input query.");
			e.printStackTrace();
			return null;
		}
		
		log.info("SQL to be tested: " + sqlToBeTested);
		
		// A path is a SQL query that only passes a certain condition set.
		List<String> allPaths;
		try {
			pathExtractor.initialize();
			allPaths = pathExtractor.getPaths(sqlToBeTested);
		} catch (Exception e) {
			log.error("Could not extract the paths, ensure that you are connected to the internet. Message: " + e.getMessage(), e);
			return null;
		}
		log.info("Found " + allPaths.size() + " paths");
		allPaths.stream().forEach(path -> log.info(path));


		replace(allPaths, sqlToBeTested);

		doDeletePath(EvoSQLConfiguration.queryNo, allPaths);


		///////////////修正数据集小错误
		if (EvoSQLConfiguration.dataSet == "erpnext" && EvoSQLConfiguration.queryNo == 1055)
		{
			sqlToBeTested = sqlToBeTested.replaceFirst("Status", "status");
			for (int i = 0; i < allPaths.size(); i++)
			{
				String path = allPaths.get(i).replaceFirst("Status", "status");
				allPaths.set(i, path);
			}
		}
		//////////////////////////////

		List<String> securePaths = new ArrayList<>();
		for (String pathStr : allPaths)
		{
			securePaths.add(new SqlSecurer(pathStr).getSecureSql());
		}

		if (EvoSQLConfiguration.USE_PATH_POPULATION_SIZE)
		{
			EvoSQLConfiguration.POPULATION_SIZE = (allPaths.size() + 1) / 2 * 2;
		}

		Map<String, TableSchema> tableSchemas;
		Seeds seeds;
		
		long start, end = -1;
		
		int   pathNo
			, totalPaths = allPaths.size()
			, coveredPaths = 0;
		
		long eachPathTime = (long)( EvoSQLConfiguration.MS_EXECUTION_TIME / (double)totalPaths );
		
		Result result = new Result(sqlToBeTested, System.currentTimeMillis());


		if (EvoSQLConfiguration.PRINT_PATH)
		{
			sqlToBeTested = new SqlSecurer(sqlToBeTested).getSecureSql();
			tableSchemas = schemaExtractor.getTablesFromQuery(sqlToBeTested);
			Set<ColumnSchema> usedColumns = new UsedColumnExtractor(sqlToBeTested, tableSchemas).extract();
			try {
				FileWriter writer = new FileWriter("./path.tsv", true);
				writer.write(EvoSQLConfiguration.queryNo + "\t" + allPaths.size() + "\t" + usedColumns.size() + "\t\t" + sqlToBeTested + "\n");
				for (int x = 0; x < allPaths.size(); x++)
				{
					writer.write(EvoSQLConfiguration.queryNo + "\t\t\t" + (x+1) + "\t" + allPaths.get(x) + "\n");
				}
				writer.close();
				return null;
			} catch (IOException e) {
				e.printStackTrace();
			}
		}

		List<Fixture> population = new ArrayList<Fixture>();

		// Holds all paths not yet solved and not tried in the current cycle
		Queue<PathState> unattemptedPaths = new LinkedList<PathState>();

		for (int iPathNo = 1; iPathNo <= allPaths.size(); iPathNo++) {
			String path = allPaths.get(iPathNo - 1);
			unattemptedPaths.add(new PathState(iPathNo, path, null, 0, null, eachPathTime));
		}

		// Holds all paths that have been attempted but are not yet solved
		List<PathState> attemptedPaths = new ArrayList<PathState>();

		while(!unattemptedPaths.isEmpty()) {
			PathState pathState = unattemptedPaths.poll();

			// Check if there is time budget right now
			if (pathState.timeBudget <= 0) {
				attemptedPaths.add(pathState);
				continue;
			}

			String pathSql = pathState.path;
			pathNo = pathState.pathNo;
			log.info("Testing " + pathSql);

			start = System.currentTimeMillis();

			// Secure sql
			pathSql = new SqlSecurer(pathSql).getSecureSql();

			try {
				if (pathState.approach == null) {
					// Get all table schema's for current path
					tableSchemas = schemaExtractor.getTablesFromQuery(pathSql);

					if (EvoSQLConfiguration.USE_LITERAL_SEEDING || (baseline && EvoSQLConfiguration.USE_SEEDED_RANDOM_BASELINE)) {
						// Get the seeds for the current path
						seeds = new SeedExtractor(pathSql).extract();
					} else {
						// Use no seeds
						seeds = Seeds.emptySeed();
					}
					if (baseline)
						pathState.approach = new RandomApproach(tableSchemas, pathSql, seeds);
					else
						pathState.approach = new StandardGA(population, tableSchemas, pathSql, seeds);
				} else {
					// Find table schemas from approach
					tableSchemas = pathState.approach.getTableSchemas();

					// Set the current population to where it left off
					population = pathState.population;
				}


				// Reset table schema usedness
				tableSchemas.forEach((name, ts) -> ts.resetUsed());

				// Set used columns
				if (EvoSQLConfiguration.USE_USED_COLUMN_EXTRACTION) {
					// Get the used columns in the current path
					pathState.usedColumns = new UsedColumnExtractor(pathSql, tableSchemas).extract();
					if (pathState.usedColumns != null) {
						 for (ColumnSchema col : pathState.usedColumns) {
							 col.setUsedColumn();
							 col.getTable().addUsedColumn();
						 }
					}
				} else {
					// Use all columns
					for (TableSchema ts : tableSchemas.values()) {
						for (ColumnSchema cs : ts.getColumns()) {
							cs.setUsedColumn();
							ts.addUsedColumn();
						}
					}
				}

				// Create schema on instrumenter
				for (TableSchema ts : tableSchemas.values()) {
					genetic.Instrumenter.execute(ts.getDropSQL());
					genetic.Instrumenter.execute(ts.getCreateSQL());
				}

				Fixture generatedFixture = pathState.approach.execute(pathState.timeBudget);

				///////////////////////
				if(EvoSQLConfiguration.IS_GET_MULTI_DATA)
				{
					generatedFixture.setChanged(true);
					TestCaseSolution tcs = new TestCaseSolution(generatedFixture);

					tcs.calculateFitness(securePaths);

					try {
						String fitPath = Paths.get(System.getProperty("user.dir")).toString()
								+ "/evaluation/scenarios/"
								+ EvoSQLConfiguration.dataSet + "/multifit.csv";
						FileWriter writer = new FileWriter(fitPath, true);
						writer.write(EvoSQLConfiguration.queryNo
								+"\t"+"EvoSQL"
								+"\t"+pathNo
								+"\t"+tcs.getSize()
						);
						for (double d : tcs.getDistance())
						{
							writer.write("\t"+d);
						}
						writer.write("\n");
						writer.close();
					} catch (IOException e) {
						e.printStackTrace();
					}
				}
				///////////////////////
				// Store some vars
				end = System.currentTimeMillis();
				pathState.timePassed += (end - start);

				log.debug("Generated fixture for this path: {}", generatedFixture);

				// Done with path
				if (pathState.approach.hasOutput(generatedFixture)) {
					// Add success
					result.addPathSuccess(pathNo, pathSql, pathState.timePassed, generatedFixture
							, pathState.approach.fetchOutput(generatedFixture, sqlToBeTested)
							, pathState.approach.getGenerations(), pathState.approach.getIndividualCount()
							, pathState.approach.getExceptions());

					// Update coverage
					coveredPaths++;
					result.addCoveragePercentage(100 * ((double)coveredPaths) / totalPaths);
				} else {
					// Check if it didn't think it was a solution (because then there is no point to keep trying
					if (generatedFixture.getFitness() != null && generatedFixture.getFitness().getDistance() != 0) {
						// Add this path to the attemptedPaths
						pathState.population = new ArrayList<Fixture>(population); // new list pointing to the last population
						attemptedPaths.add(pathState);
					}

					String msg = "Has no output, distance is ";
					if (generatedFixture.getFitness() != null)
						msg += generatedFixture.getFitness().getDistance();
					else
						msg += "unknown!";
					result.addPathFailure(pathNo, pathSql, pathState.timePassed, msg
							, pathState.approach.getGenerations(), pathState.approach.getIndividualCount()
							, pathState.approach.getExceptions());
				}
			} catch (Exception e) {
				if (end < start) {
					end = System.currentTimeMillis();
					pathState.timePassed += (end - start);
				}
				e.printStackTrace();
				StackTraceElement[] st = e.getStackTrace();
				String stackStr = "";
				for (StackTraceElement s : st)
					stackStr += s.toString() + '\t';
				result.addPathFailure(pathNo, pathSql, pathState.timePassed, e.getMessage() + "\t" + stackStr
						, pathState.approach.getGenerations(), pathState.approach.getIndividualCount()
						, pathState.approach.getExceptions());
			}

			// If it took shorter than given time budget, redistribute the time accordingly
			long timediff = (end - start);
			if (timediff < pathState.timeBudget) {
				int statesLeft = unattemptedPaths.size() + attemptedPaths.size();
				if (statesLeft > 0) {
					long spareTime = pathState.timeBudget - timediff;
					// Get time per path state
					long timeInc = (long)(spareTime / (double)statesLeft);
					// Increase budgets
					unattemptedPaths.forEach(ps -> ps.timeBudget += timeInc);
					attemptedPaths.forEach(ps -> ps.timeBudget += timeInc);
				}
			}
			pathState.timeBudget -= timediff;

			// If all paths are done, there are unsolved paths and we have time left, add the unsolved paths back in
			if (unattemptedPaths.size() == 0 && !attemptedPaths.isEmpty()) {
				// Check if any attempted paths have time left, if not stop
				boolean timeLeft = false;
				for (PathState ps : attemptedPaths) {
					if (ps.timeBudget > 0)
						timeLeft = true;
				}

				if (timeLeft)
					unattemptedPaths.addAll(attemptedPaths);
				attemptedPaths.clear();
			}
		}

		genetic.Instrumenter.stopDatabase();

		return result;
	}

	private void checkNullable(Map<String, TableSchema> tableSchemas) {
		System.out.println("//////////////////////////////");
		System.out.println(tableSchemas.get("tabStock Entry").getColumn("name").isNullable());
		System.out.println("//////////////////////////////");
	}

	public Result moeExecute(String sqlToBeTested) throws SQLException {
		genetic.Instrumenter.startDatabase();

		log.info("SQL to be tested1: " + sqlToBeTested);
		// Check if query can be parsed
		try {
			// Make sql safe
			sqlToBeTested = new SqlSecurer(sqlToBeTested).getSecureSql();
		} catch (RuntimeException e) {
			log.error("Could not parse input query.");
			e.printStackTrace();
			return null;
		}

		log.info("SQL to be tested: " + sqlToBeTested);

		// A path is a SQL query that only passes a certain condition set.
		List<String> allPaths;
		try {
			pathExtractor.initialize();
			allPaths = pathExtractor.getPaths(sqlToBeTested);
		} catch (Exception e) {
			log.error("Could not extract the paths, ensure that you are connected to the internet. Message: " + e.getMessage(), e);
			return null;
		}
		log.info("Found " + allPaths.size() + " paths");
		allPaths.stream().forEach(path -> log.info(path));


		replace(allPaths, sqlToBeTested);			//修正大小写问题

		doDeletePath(EvoSQLConfiguration.queryNo, allPaths);


		///////////////修正数据集小错误
		if (EvoSQLConfiguration.dataSet == "erpnext" && EvoSQLConfiguration.queryNo == 1055)
		{
			sqlToBeTested = sqlToBeTested.replaceFirst("Status", "status");
			for (int i = 0; i < allPaths.size(); i++)
			{
				String path = allPaths.get(i).replaceFirst("Status", "status");
				allPaths.set(i, path);
			}
		}
		//////////////////////////////
		if (EvoSQLConfiguration.USE_PATH_POPULATION_SIZE)
		{
			EvoSQLConfiguration.POPULATION_SIZE = (allPaths.size() + 1) / 2 * 2;
		}

		long start, end = -1;

		start = System.currentTimeMillis();

		MultiResult<TestCaseSolution> multiResult = new MultiResult<>(sqlToBeTested, start);
		if (allPaths.size() == 0)
		{
			multiResult.runtime = 0l;
			multiResult.generations = 0;
			multiResult.individualCount = 0;
			multiResult.pathList = new ArrayList<>();
			multiResult.solutions = new ArrayList<>();
			multiResult.coveredNum = 0;
			multiResult.coveredDistance = new ArrayList<>();
			multiResult.exceptions = "";
			multiResult.hasException = false;
			return multiResult;
		}
		// Secure sql
		for (int i = 0; i < allPaths.size(); i++)
		{
			allPaths.set(i, new SqlSecurer(allPaths.get(i)).getSecureSql());
		}
		Map<String, TableSchema> tableSchemas = null;
		tableSchemas = schemaExtractor.getTablesFromQuery(sqlToBeTested);
		Seeds seeds;
		try {

			seeds = new SeedExtractor(allPaths.get(0)).extract();
			for (int i = 1; i < allPaths.size(); i++)
			{
				seeds.addSeeds(new SeedExtractor(allPaths.get(i)).extract());
			}

			TCGA algorithm = new TCGA(null, tableSchemas, allPaths, seeds);
			if (EvoSQLConfiguration.algorithmType == EvoSQLConfiguration.AlgorithmType.MoeSQLnsgaii)
			{
				algorithm.selection = new RankingAndCrowdingSelection<>(algorithm.getMaxPopulationSize());
			}
			else if (EvoSQLConfiguration.algorithmType == EvoSQLConfiguration.AlgorithmType.MoeSQLmosa)
			{
				algorithm.selection = new MOSASelection<>(algorithm.getMaxPopulationSize());
			}
			else
			{
				algorithm.selection = new ParetoCornerSelection<>(algorithm.getMaxPopulationSize());
			}
			algorithm.crossover = new TestCaseCrossover();
			algorithm.mutation = new TestCaseMutation(seeds);
			algorithm.setPathTime(EvoSQLConfiguration.MS_EXECUTION_TIME);
			//algorithm.
			// Reset table schema usedness
			tableSchemas.forEach((name, ts) -> ts.resetUsed());

			// Set used columns
			if (EvoSQLConfiguration.USE_USED_COLUMN_EXTRACTION) {
				// Get the used columns in the current path
				Set<ColumnSchema> usedColumns = new UsedColumnExtractor(sqlToBeTested, tableSchemas).extract();
				if (usedColumns != null) {
					for (ColumnSchema col : usedColumns) {
						col.setUsedColumn();
						col.getTable().addUsedColumn();
					}
				}
			} else {
				// Use all columns
				for (TableSchema ts : tableSchemas.values()) {
					for (ColumnSchema cs : ts.getColumns()) {
						cs.setUsedColumn();
						ts.addUsedColumn();
					}
				}
			}

			// Create schema on instrumenter
			for (TableSchema ts : tableSchemas.values()) {
				genetic.Instrumenter.execute(ts.getDropSQL());
				genetic.Instrumenter.execute(ts.getCreateSQL());
			}

			List<TestCaseSolution> solutions = algorithm.execute(0);

			// Store some vars
			end = System.currentTimeMillis();

			List<TestCaseSolution> solutionList;
			//todo 整合起来
			if (EvoSQLConfiguration.IS_FIND_PARETO)
			{
				RankingAndCrowdingSelection<TestCaseSolution> selection = new RankingAndCrowdingSelection<>(0);
				solutionList = selection.computeDominanceRanking(solutions).get(0);
			}
			else
			{
				solutionList = solutions;
			}

			multiResult.runtime = end - start;
			multiResult.generations = algorithm.getGenerations();
			multiResult.individualCount = algorithm.getIndividualCount();
			multiResult.pathList = allPaths;
			multiResult.solutions = solutionList;
			multiResult.coveredNum = algorithm.getCoveredNum();
			multiResult.coveredDistance = algorithm.getCoveredDistance();
			multiResult.exceptions = algorithm.getExceptions();
			multiResult.hasException = false;

		} catch (Exception e) {
			if (end < start) {
				end = System.currentTimeMillis();
				multiResult.runtime = (end - start);
			}
			e.printStackTrace();
			StackTraceElement[] st = e.getStackTrace();
			String stackStr = "";
			for (StackTraceElement s : st)
				stackStr += s.toString() + '\t';

			multiResult.hasException = true;
			multiResult.exceptions = e.getMessage() + "\t" + stackStr;
		}




		if (EvoSQLConfiguration.USE_EXHAUSTIVE)
		{
			try
			{
				List<TestCaseSolution> newPopulation = new ArrayList<>(multiResult.solutions);
				MOTCA motca = new MOTCA(newPopulation, tableSchemas, allPaths);
				start = System.currentTimeMillis();
				List<TestCaseSolution> secSolution = motca.execute(EvoSQLConfiguration.MS_EXECUTION_TIME - multiResult.runtime);
				end = System.currentTimeMillis();
				multiResult.secondResult = new MultiResult<>(sqlToBeTested, start);
				multiResult.secondResult.runtime = end - start;
				multiResult.secondResult.generations = motca.getGenerations();
				multiResult.secondResult.individualCount = motca.getIndividualCount();
				multiResult.secondResult.pathList = allPaths;
				multiResult.secondResult.solutions = secSolution;
				multiResult.secondResult.coveredNum = motca.getCoveredNum();
				multiResult.secondResult.coveredDistance = motca.getCoveredDistance();
				multiResult.secondResult.exceptions = motca.getExceptions();
				multiResult.secondResult.hasException = false;
			}
			catch(Exception e)
			{
				multiResult.secondResult = new MultiResult<>(sqlToBeTested, start);
				multiResult.secondResult.runtime = end - start;
				multiResult.secondResult.pathList = allPaths;
				multiResult.secondResult.solutions = new ArrayList<>();
				multiResult.secondResult.exceptions = e.getMessage();
				multiResult.secondResult.hasException = true;
			}
		}




		genetic.Instrumenter.stopDatabase();

		return multiResult;
	}
	public void setPathExtractor(PathExtractor pe) {
		this.pathExtractor = pe;
	}
}
