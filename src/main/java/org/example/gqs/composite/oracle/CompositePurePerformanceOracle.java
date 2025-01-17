package org.example.gqs.composite.oracle;

import org.example.gqs.common.oracle.TestOracle;
import org.example.gqs.composite.CompositeConnection;
import org.example.gqs.composite.CompositeGlobalState;
import org.example.gqs.composite.CompositeSchema;
import org.example.gqs.cypher.CypherQueryAdapter;
import org.example.gqs.cypher.ast.IClauseSequence;
import org.example.gqs.cypher.dsl.IQueryGenerator;
import org.example.gqs.cypher.standard_ast.ClauseSequence;
import org.example.gqs.cypher.standard_ast.IClauseSequenceBuilder;

import java.util.*;

public class CompositePurePerformanceOracle implements TestOracle {

    private final CompositeGlobalState globalState;
    private final IQueryGenerator<CompositeSchema, CompositeGlobalState> queryGenerator;
    public static final long groupsize = 100000;
    public static final double MP = 0.01;
    public static final double CP = 0.5;
    public static final long ITERA = 100000;
    public List<WCInput> group = new ArrayList<>();

    public static double maxRatio = 1d;
    public static long numof1to10 = 0;
    public static long numof10to100 = 0;
    public static long numof100to1000 = 0;
    public static long numGreaterThan1000 = 0;
    public static long numofTimeOut = 0;


    public static class WCInput implements Comparable{
        IClauseSequence seq;
        List<Long> results;

        public WCInput(IClauseSequence seq, List<Long> results){
            this.seq = seq;
            this.results = results;
        }

        @Override
        public int compareTo(Object o) {
            WCInput o1 = (WCInput)o;
            double result1 = this.results.get(1) * 1.0 / this.results.get(0);
            double result2 = o1.results.get(1) * 1.0 / o1.results.get(0);
            if (result1 - result2 >= 0) {
                return 1;
            } else {
                return -1;
            }
        }
    }

    public CompositePurePerformanceOracle(CompositeGlobalState globalState) {
        this.globalState = globalState;
        this.queryGenerator = globalState.getDbmsSpecificOptions().getQueryGenerator();
    }

    public void generateQueries() throws Exception {
        CompositeSchema schema = globalState.getSchema();

        IClauseSequenceBuilder builder = ClauseSequence.createClauseSequenceBuilder();
        IClauseSequence sequence = queryGenerator.generateQuery(globalState);
        StringBuilder sb = new StringBuilder();
        sequence.toTextRepresentation(sb);
        System.out.println("：" + sb);
        List<Long> results = globalState.executeStatementAndGetTime(new CypherQueryAdapter(sb.toString()));
        System.out.println(results);
        long time1 = Math.max(results.get(0), results.get(1));
        long time2 = Math.min(results.get(0), results.get(1));
        if (time2 <= CompositeConnection.TIMEOUT) {
            double time = time1 * 1.0 / time2;
            System.out.println("：" + time);
        }
    }
    public List<IClauseSequence> selectQueries() throws Exception {
        return null;
    }

    public List<IClauseSequence> crossoverQueries(List<IClauseSequence> parents) throws Exception {
        return null;
    }

    public List<IClauseSequence> mutateQueries(List<IClauseSequence> parents) throws Exception {
        return null;
    }

    public List<WCInput> calculateFitness(List<IClauseSequence> parents) throws Exception {
        return null;
    }

    public void updateGroup(List<WCInput> results) throws Exception {
    }

    @Override
    public void check() throws Exception {
        generateQueries();
    }
}
