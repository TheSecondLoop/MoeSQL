package nl.tudelft.serg.evosql.metaheuristics.operators;

import com.sun.org.apache.xpath.internal.operations.Bool;
import nl.tudelft.serg.evosql.EvoSQLConfiguration;
import nl.tudelft.serg.evosql.fixture.Solution;

import java.util.*;

public class MaxObjectiveSelection<S extends Solution<?>> implements SelectionOperator<S> {

    @Override
    public List<S> execute(List<S> s) {
        List<List<S>> rankedSubpopulations = computeMaxRanking(s);
        return rankingAndRandomSelection(rankedSubpopulations);
    }
    private int selectSize = 0 ;

    @Override
    public void setIsCovered(boolean[] isCovered) {
        //todo
    }

    @Override
    public int getSelectSize() {
        return selectSize;
    }
    /** Constructor */
    public MaxObjectiveSelection(int selectSize) {
        this.selectSize = selectSize ;
    }

    /** Execute() method */
//    public List<S> execute(List<S> solutionList) {
//        Ranking<S> ranking = new DominanceRanking<S>();
//        ranking.computeRanking(solutionList) ;
//
//        return crowdingDistanceSelection(ranking);
//    }

    public List<List<S>> computeMaxRanking(List<S> solutionSet) {


        if (solutionSet.size() <= 0 || solutionSet.get(0).getNumberOfObjectives() <= 0)
        {
            List<List<S>> rankedSubpopulations = new ArrayList<>();
            rankedSubpopulations.add(solutionSet);
            return rankedSubpopulations;
        }

        int numberOfObjectives = solutionSet.get(0).getNumberOfObjectives() ;
        for (int i = 0; i < numberOfObjectives; i++) {
            // Sort the population by Obj n
            Collections.sort(solutionSet, new ObjectiveComparator<S>(i)) ;
            for (int j = 0; j < solutionSet.size(); j++)
            {
                solutionSet.get(j).crowdingDistance = Math.min(solutionSet.get(j).crowdingDistance, j);     //crowdingDistance暂表示层数
            }
        }

        int layerNum = 0;
        for (int j = 0; j < solutionSet.size(); j++)
        {
            layerNum = Math.max(layerNum, (int) solutionSet.get(j).crowdingDistance);
        }
        layerNum++;
        List<List<S>> rankedSubpopulations = new ArrayList<>();
        //0,1,2,....,i-1 are fronts, then i fronts
        for (int j = 0; j < layerNum; j++) {
            rankedSubpopulations.add(j, new ArrayList<S>());
        }

        for (int j = 0; j < solutionSet.size(); j++)
        {
            rankedSubpopulations.get((int) solutionSet.get(j).crowdingDistance).add(solutionSet.get(j));
        }
        return rankedSubpopulations;
    }

    protected List<S> rankingAndRandomSelection(List<List<S>> rankedSubpopulations) {
        List<S> population = new ArrayList<>(selectSize) ;
        int rankingIndex = 0;
        while (population.size() < selectSize) {
            if (subfrontFillsIntoThePopulation(rankedSubpopulations, rankingIndex, population)) {
                addRankedSolutionsToPopulation(rankedSubpopulations, rankingIndex, population);
                rankingIndex++;
            } else {
                Random random = new Random();
                boolean[] selected = new boolean[getSubfront(rankingIndex, rankedSubpopulations).size()];
                while (population.size() < selectSize) {
                    int index = random.nextInt(selected.length);
                    if (!selected[index])
                    {
                        population.add(getSubfront(rankingIndex, rankedSubpopulations).get(index));
                    }
                }
                break;
            }
        }
        if (EvoSQLConfiguration.cot % 1000 == 0)
        {
            System.out.println();
            EvoSQLConfiguration.cot = 0;
        }
        EvoSQLConfiguration.cot++;
        return population ;
    }

    protected boolean subfrontFillsIntoThePopulation(List<List<S>> rankedSubpopulations, int rank, List<S> population) {
        return getSubfront(rank, rankedSubpopulations).size() < (selectSize - population.size()) ;
    }

    public List<S> getSubfront(int rank, List<List<S>> rankedSubpopulations) {
        return rankedSubpopulations.get(rank);
    }

    protected void addRankedSolutionsToPopulation(List<List<S>> rankedSubpopulations, int rank, List<S> population) {
        List<S> front ;

        front = getSubfront(rank, rankedSubpopulations);

        for (int i = 0 ; i < front.size(); i++) {
            population.add(front.get(i));
        }
    }

}

