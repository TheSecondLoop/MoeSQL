package nl.tudelft.serg.evosql.metaheuristics.operators;

import nl.tudelft.serg.evosql.fixture.Solution;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class RandomSelection<S extends Solution<?>> implements SelectionOperator<S> {

    @Override
    public void setIsCovered(boolean[] isCovered) {
        //todo
    }

    @Override
    public int getSelectSize()
    {
        return 1;
    }

    @Override
    public List<S> execute(List<S> s) {
        List<S> result = new ArrayList<>();
        result.add(s.get(new Random().nextInt(s.size())));
        return result;
    }
}
