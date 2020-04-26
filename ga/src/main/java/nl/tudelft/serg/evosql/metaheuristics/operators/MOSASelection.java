package nl.tudelft.serg.evosql.metaheuristics.operators;

import nl.tudelft.serg.evosql.EvoSQLConfiguration;
import nl.tudelft.serg.evosql.fixture.Solution;

import java.util.*;

public class MOSASelection<S extends Solution<?>> implements SelectionOperator<S> {

    public boolean[] isCovered;
    public void setIsCovered(boolean[] isCovered)
    {
        this.isCovered = isCovered;
    }

    @Override
    public List<S> execute(List<S> s) {
        if (s.size() > 0 && (isCovered == null || isCovered.length < s.get(0).getNumberOfObjectives() - 1))
        {
            isCovered = new boolean[s.get(0).getNumberOfObjectives() - 1];
        }
        List<List<S>> rankedSubpopulations = computeDominanceRanking(s);
        return crowdingDistanceSelection(rankedSubpopulations);
    }
    private int selectSize = 0 ;

    @Override
    public int getSelectSize() {
        return selectSize;
    }
    /** Constructor */
    public MOSASelection(int selectSize) {
        this.selectSize = selectSize ;
    }

    /** Execute() method */
//    public List<S> execute(List<S> solutionList) {
//        Ranking<S> ranking = new DominanceRanking<S>();
//        ranking.computeRanking(solutionList) ;
//
//        return crowdingDistanceSelection(ranking);
//    }

    public List<List<S>> computeDominanceRanking(List<S> solutionSet) {

        if (solutionSet.size() <= 0 || solutionSet.get(0).getNumberOfObjectives() <= 0)
        {
            List<List<S>> rankedSubpopulations = new ArrayList<>();
            rankedSubpopulations.add(solutionSet);
            return rankedSubpopulations;
        }

        DominanceComparator<S> comparator = new DominanceComparator<>();
        comparator.setIsCovered(isCovered);

        // dominateMe[i] contains the number of solutions dominating i
        int[] dominateMe = new int[solutionSet.size()];

        // iDominate[k] contains the list of solutions dominated by k
        List<Integer>[] iDominate = new List[solutionSet.size()];

        // front[i] contains the list of individuals belonging to the front i
        List<Integer>[] front = new List[solutionSet.size() + 1];

        // Initialize the fronts
        for (int i = 0; i < front.length; i++) {
            front[i] = new LinkedList<>();
        }

        // Fast non dominated sorting algorithm
        // Contribution of Guillaume Jacquenot
        for (int p = 0; p < solutionSet.size(); p++) {
            // Initialize the list of individuals that i dominate and the number
            // of individuals that dominate me
            iDominate[p] = new LinkedList<>();
            dominateMe[p] = 0;
        }

        int flagDominate;
        for (int p = 0; p < (solutionSet.size() - 1); p++) {
            // For all q individuals , calculate if p dominates q or vice versa
            for (int q = p + 1; q < solutionSet.size(); q++) {
                flagDominate = comparator.compare(solutionSet.get(p), solutionSet.get(q));
                if (flagDominate == -1) {
                    iDominate[p].add(q);
                    dominateMe[q]++;
                } else if (flagDominate == 1) {
                    iDominate[q].add(p);
                    dominateMe[p]++;
                }
            }
        }



        int isUseSize = EvoSQLConfiguration.USE_SIZE ? 0 : 1;
        for (int j = 0; j < solutionSet.get(0).getNumberOfObjectives() - isUseSize; j++)
        {
            if (EvoSQLConfiguration.USE_DYNAMIC_OBJECTIVES && j < isCovered.length && isCovered[j])            //是否使用动态目标策略
            {
                continue;
            }
            int betterIndex = 0;
            for (int i = 1; i < solutionSet.size(); i++) {
                if (solutionSet.get(i).getObjective(j)
                        .compareTo(solutionSet.get(betterIndex).getObjective(j)) < 0)
                {
                    betterIndex = i;
                }
            }
            if (!front[0].contains(betterIndex))
            {
                front[0].add(betterIndex);
                for (int i = 0; i < iDominate[betterIndex].size(); i++)
                {
                    dominateMe[iDominate[betterIndex].get(i)]--;
                }
            }
        }
        for (int i = 0; i < solutionSet.size(); i++) {
            if (dominateMe[i] == 0) {

                front[1].add(i);
                //RankingAndCrowdingAttr.getAttributes(solutionSet.get(0)).setRank(0);
                //solutionSet.get(i).setAttribute(getAttributeID(), 0);
            }
        }
        //Obtain the rest of fronts
        int i = 1;
        Iterator<Integer> it1, it2; // Iterators
        while (front[i].size() != 0) {
            i++;
            it1 = front[i - 1].iterator();
            while (it1.hasNext()) {
                it2 = iDominate[it1.next()].iterator();
                while (it2.hasNext()) {
                    int index = it2.next();
                    dominateMe[index]--;
                    if (dominateMe[index] == 0) {
                        front[i].add(index);
                        //RankingAndCrowdingAttr.getAttributes(solutionSet.get(index)).setRank(i);
                        //solutionSet.get(index).setAttribute(getAttributeID(), i);
                    }
                }
            }
        }

        List<List<S>> rankedSubpopulations = new ArrayList<>();
        //0,1,2,....,i-1 are fronts, then i fronts
        for (int j = 0; j < i; j++) {
            rankedSubpopulations.add(j, new ArrayList<S>(front[j].size()));
            it1 = front[j].iterator();
            while (it1.hasNext()) {
                rankedSubpopulations.get(j).add(solutionSet.get(it1.next()));
            }
        }

        return rankedSubpopulations;
    }

    protected List<S> crowdingDistanceSelection(List<List<S>> rankedSubpopulations) {
        List<S> population = new ArrayList<>(selectSize) ;
        int rankingIndex = 0;
        while (population.size() < selectSize) {
            if (subfrontFillsIntoThePopulation(rankedSubpopulations, rankingIndex, population)) {
                addRankedSolutionsToPopulation(rankedSubpopulations, rankingIndex, population);
                rankingIndex++;
            } else {
                computeDensityEstimator(getSubfront(rankingIndex, rankedSubpopulations));
                addLastRankedSolutionsToPopulation(rankedSubpopulations, rankingIndex, population);
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

    protected void addLastRankedSolutionsToPopulation(List<List<S>> rankedSubpopulations, int rank, List<S>population) {
        List<S> currentRankedFront = getSubfront(rank, rankedSubpopulations);

        Collections.sort(currentRankedFront, new CrowdingDistanceComparator<S>()) ;

        int i = 0 ;
        while (population.size() < selectSize) {
            population.add(currentRankedFront.get(i)) ;
            i++ ;
        }
    }
    public void computeDensityEstimator(List<S> solutionList) {
        int size = solutionList.size();


        if (size == 0) {
            return;
        }
        if (size == 1) {
            solutionList.get(0).crowdingDistance = Double.POSITIVE_INFINITY;
            return;
        }
        if (size == 2) {
            solutionList.get(0).crowdingDistance = Double.POSITIVE_INFINITY;
            solutionList.get(1).crowdingDistance = Double.POSITIVE_INFINITY;
            return;
        }

        //Use a new SolutionSet to avoid altering the original solutionSet
        List<S> front = new ArrayList<>(size);
        front.addAll(solutionList);

//        for (int i = 0; i < size; i++) {
//            front.get(i).setAttribute(getAttributeID(), 0.0);
//        }

        AbstractFitness objetiveMaxn;
        AbstractFitness objetiveMinn;
        double distance;

        int numberOfObjectives = solutionList.get(0).getNumberOfObjectives() ;

        int isUseSize = EvoSQLConfiguration.USE_SIZE ? 0 : 1;
        for (int i = 0; i < numberOfObjectives - isUseSize; i++) {
            if (EvoSQLConfiguration.USE_DYNAMIC_OBJECTIVES && i < isCovered.length && isCovered[i])            //是否使用动态目标策略
            {
                continue;
            }
            // Sort the population by Obj n
            Collections.sort(front, new ObjectiveComparator<S>(i)) ;
            objetiveMinn = front.get(0).getObjective(i);
            objetiveMaxn = front.get(front.size() - 1).getObjective(i);
            double objectiveRange = objetiveMinn.doubleCompareTo(objetiveMaxn);
            //Set de crowding distance
            front.get(0).crowdingDistance = Double.POSITIVE_INFINITY;
            front.get(size - 1).crowdingDistance = Double.POSITIVE_INFINITY;

            for (int j = 1; j < size - 1; j++) {
                distance = front.get(j + 1).getObjective(i).doubleCompareTo(front.get(j - 1).getObjective(i));
                distance = distance / objectiveRange;
                front.get(j).crowdingDistance += distance;
            }
        }
        return;
    }
}
