package nl.tudelft.serg.evosql;

import nl.tudelft.serg.evosql.fixture.Solution;

import java.util.List;

public class MultiResult<S extends Solution<?>> extends Result{
    public String inputQuery;
    public List<String> pathList;
    public List<S> solutions;


    public MultiResult<S> secondResult; // 获取二阶段结果

    public int coveredNum;
    public List<Double> coveredDistance;
    public boolean[] isCovered;

    public long startTime;
    public Long runtime;

    public int generations;
    public int individualCount;
    public String exceptions;

    public boolean hasException;

    public MultiResult(String query, long startTime) {
        super(query, startTime);
        this.inputQuery = query;
        this.startTime = startTime;
    }
}
