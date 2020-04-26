package nl.tudelft.serg.evosql.metaheuristics.operators;

import nl.tudelft.serg.evosql.EvoSQLConfiguration;
import nl.tudelft.serg.evosql.fixture.Solution;

import java.util.*;

public class ParetoCornerSelection <S extends Solution<?>> implements SelectionOperator<S> {

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
        // 构建备选数列
        Set<Integer> selectedSet = new HashSet<>();
        int maxObjIndex = -1;
        int secondObjIndex = -1;
        int maxCount = 1;
        int secondCount = 1;
        int numberOfObjectives = s.get(0).getNumberOfObjectives();
        for (int i = 0; i < numberOfObjectives; i++) {
            // 默认s.size>2
            if (EvoSQLConfiguration.USE_DYNAMIC_OBJECTIVES && i < isCovered.length && isCovered[i])            //是否使用动态目标策略
            {
                continue;
            }
            maxObjIndex = -1;
            secondObjIndex = -1;
            for (int j = 0; j < s.size(); j++)
            {
                if (maxObjIndex == -1)
                {
                    maxObjIndex = j;
                }
                else
                {
                    int compareToMax = s.get(j).getObjective(i)
                            .compareTo(s.get(maxObjIndex).getObjective(i));
                    if (compareToMax == 0)         //概率替换1/n
                    {
                        maxCount++;
                        int replaceP = new Random().nextInt(maxCount);
                        if (replaceP == 0)
                        {
                            maxObjIndex = j;
                        }
                    }
                    else if (compareToMax < 0)      //替换
                    {
                        maxCount = 1;
                        secondObjIndex = maxObjIndex;
                        maxObjIndex = j;
                    }
                    else            //比对second
                    {
                        if (secondObjIndex == -1)
                        {
                            secondObjIndex = j;
                        }
                        else {
                            int compareToSecond = s.get(j).getObjective(i)
                                    .compareTo(s.get(secondObjIndex).getObjective(i));
                            if (compareToSecond == 0)         //概率替换1/n
                            {
                                secondCount++;
                                int replaceP = new Random().nextInt(secondCount);
                                if (replaceP == 0)
                                {
                                    secondObjIndex = j;
                                }
                            }
                            else if (compareToSecond < 0)      //替换
                            {
                                secondCount = 1;
                                secondObjIndex = j;
                            }
                        }
                    }
                }
            }
            selectedSet.add(maxObjIndex);
            selectedSet.add(secondObjIndex);
        }
        selectedSet.remove(-1);
        //补齐
        List<Integer> randomList = new ArrayList<>(s.size());
        for (int i = 0; i < s.size(); i++)
        {
            randomList.add(i);
        }
        Collections.shuffle(randomList);
        int count = 0;
        while (selectedSet.size() > selectSize)
        {
            selectedSet.remove(randomList.get(count));
            count++;
        }
        count = 0;
        while (selectedSet.size() < selectSize)
        {
            selectedSet.add(randomList.get(count));
            count++;
        }
        // 结算
        List<S> offspring = new ArrayList<>();
        for (int index : selectedSet)
        {
            offspring.add(s.get(index));
        }

//        Collections.shuffle(offspring);
        return offspring;
    }
    private int selectSize = 0 ;

    @Override
    public int getSelectSize() {
        return selectSize;
    }
    /** Constructor */
    public ParetoCornerSelection(int selectSize) {
        this.selectSize = selectSize ;
    }
}

