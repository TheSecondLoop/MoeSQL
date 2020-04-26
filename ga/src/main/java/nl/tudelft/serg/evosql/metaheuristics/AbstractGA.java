package nl.tudelft.serg.evosql.metaheuristics;

import java.io.FileWriter;
import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Random;

import nl.tudelft.serg.evosql.EvoSQLConfiguration;
import nl.tudelft.serg.evosql.MultiResult;
import nl.tudelft.serg.evosql.db.Seeds;
import nl.tudelft.serg.evosql.fixture.*;
import nl.tudelft.serg.evosql.metaheuristics.operators.*;
import nl.tudelft.serg.evosql.sql.TableSchema;
import nl.tudelft.serg.evosql.util.random.Randomness;

public abstract class AbstractGA<S extends Solution<?>> extends Algorithm<S> {


    /** Current Population **/
    protected List<S> population;
    protected List<S> archive;
    /** Selection operator **/
    public SelectionOperator<S> selection;

    /** Mutation operator **/
    public MutationOperator<S> mutation;

    /** Comparator **/
    protected FixtureComparator fc = new FixtureComparator();

    /** Crossover operator **/
    public CrossoverOperator<S> crossover;

    /** Seeds store **/
    protected Seeds seeds;


    protected int populationSize;

    /* Getters */
    public SelectionOperator<S> getSelectionOperator() {
        return selection;
    }

    public CrossoverOperator<S> getCrossoverOperator() {
        return crossover;
    }

    public MutationOperator<S> getMutationOperator() {
        return mutation;
    }


    public AbstractGA(List<S> population, Map<String, TableSchema> pTableSchemas, List<String> pathList, Seeds seeds){
        super(pTableSchemas, pathList);

        this.seeds = seeds;

        //this.mutation = new FixtureMutation(rowFactory, seeds);
        this.population = population;

        // if it's baseline, there will be only a single generation, and population will be larger
        populationSize = EvoSQLConfiguration.POPULATION_SIZE;//EvoSQLConfiguration.POPULATION_SIZE; // * (baseline ? 2 : 1);
//        populationSize = pathList.size() + 1;
    }
    public List<Double> getCoveredDistance()
    {
        ArrayList<Double> coveredDistance = new ArrayList<>(pathList.size());
        List<S> solution = population;
        if (EvoSQLConfiguration.USE_DYNAMIC_OBJECTIVES)
        {
            solution = archive;
        }
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
    protected boolean isStoppingConditionReached()
    {
        if (!EvoSQLConfiguration.IS_FIND_PARETO && getCoveredNum() == pathList.size())
        {
            return true;
        }
        if (timelimit && System.currentTimeMillis() - startTime > pathTime)
        {
            return true;
        }
        if (!timelimit && generations > maxGenerations)
        {
            return true;
        }
        return false;
    }

    /**
     * This method iteratively applies a {@link SelectionOperator} to the population to fill the mating pool population.
     *
     * @param population
     * @return The mating pool population
     */
    protected List<S> doSelection(List<S> population) {
        List<S> matingPopulation = new ArrayList<>(population.size());
        selection.setIsCovered(isCovered);
        for (int i = 0; i < getMaxPopulationSize(); i += selection.getSelectSize()) {
            List<S> solution = selection.execute(population);
            matingPopulation.addAll(solution);
        }

        return matingPopulation;
    }

    protected List<S> evaluatePopulation(List<S> population) throws SQLException {
        for (S solution : population)
        {
            if (solution.getChanged())
            {
                individualCount++;
            }
            solution.calculateFitness(pathList, isCovered);
            if (EvoSQLConfiguration.USE_DYNAMIC_OBJECTIVES)
            {
                boolean[] solutionCovered = new boolean[pathList.size()];
                boolean isFinalSolution = false;
                for (int i = 0; i < pathList.size(); i++)
                {
                    if (solution.getObjective(i) instanceof CoverageFitness)
                    {
                        CoverageFitness coverageFitness = (CoverageFitness) solution.getObjective(i);
                        if (coverageFitness.fixtureFitness != null
                        && coverageFitness.fixtureFitness.getDistance() <= 0)
                        {
                            if (!isCovered[i])
                            {
                                solutionCovered[i] = true;
                                isFinalSolution = true;
                                isCovered[i] = true;
                            }
                        }
                    }
                }
                if (EvoSQLConfiguration.USE_MINIMIZE && isFinalSolution && solution instanceof TestCaseSolution)
                {
                    if (archive == null)
                    {
                        archive = new ArrayList<>();
                    }
                    S finalSolution = (S) minimize((TestCaseSolution) solution, solutionCovered);
                    finalSolution.setChanged(true);
                    finalSolution.calculateFitness(pathList);
                    archive.add(finalSolution);
                }
            }

            // Store exceptions
            if (!genetic.Instrumenter.getException().isEmpty() && !exceptions.contains(genetic.Instrumenter.getException())) {
                exceptions += ", " + genetic.Instrumenter.getException();
            }
        }
        return population;
    }


    protected void updateProgress()
    {
        generations++;
    }

    protected void initProgress()
    {
        this.pathTime = pathTime;
        startTime = System.currentTimeMillis();
        generations = 0;
    }

    @Override
    public List<S> execute(long pathTime) throws SQLException {
        // Initialize first

        List<S> offspringPopulation;
        List<S> matingPopulation;

        population = createInitialPopulation();
        population = evaluatePopulation(population);
        initProgress();
        while (!isStoppingConditionReached()) {
            matingPopulation = doSelection(population);
            offspringPopulation = reproduction(matingPopulation);
            offspringPopulation = evaluatePopulation(offspringPopulation);
            population = replacement(population, matingPopulation, offspringPopulation);
            updateProgress();
        }
        if (EvoSQLConfiguration.USE_DYNAMIC_OBJECTIVES)
        {
            evaluatePopulation(archive);        //更新isCovered
            return archive;
        }
        return population;
    }

    protected abstract List<S> createInitialPopulation();


    public List<S> getPopulation() {
        return population;
    }
    public void setPopulation(List<S> population) {
        this.population = population;
    }

    public void setMaxPopulationSize(int maxPopulationSize) {
        this.populationSize = maxPopulationSize ;
    }
    public int getMaxPopulationSize() {
        return populationSize;
    }


    protected List<S> replacement(List<S> population, List<S> matingPopulation, List<S> offspringPopulation)
    {
        matingPopulation.addAll(offspringPopulation);
        return matingPopulation;
    }




    protected List<S> reproduction(List<S> population) {
        int numberOfParents = crossover.getNumberOfParents() ;

        List<S> offspringPopulation = new ArrayList<>(getMaxPopulationSize());
        for (int i = 0; i < getMaxPopulationSize(); i += numberOfParents) {
            List<S> parents = new ArrayList<>(numberOfParents);
            for (int j = 0; j < numberOfParents; j++) {
                if (i + j < population.size())
                {
                    parents.add(population.get(i+j));
                }
                else
                {
                    parents.add(population.get(i+j-2));
                }
            }

            List<S> offspring = crossover.execute(parents);

            for(S s: offspring){
                mutation.execute(s);
                offspringPopulation.add(s);
            }
        }
        return offspringPopulation;
    }

}
