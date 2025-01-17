package org.example.gqs.composite.oracle;

import org.example.gqs.common.oracle.TestOracle;
import org.example.gqs.composite.CompositeConnection;
import org.example.gqs.composite.CompositeGlobalState;
import org.example.gqs.composite.CompositeSchema;
import org.example.gqs.cypher.CypherQueryAdapter;
import org.example.gqs.cypher.ast.IClauseSequence;
import org.example.gqs.cypher.dsl.IQueryGenerator;
import org.example.gqs.cypher.dsl.QueryFiller;
import org.example.gqs.cypher.gen.alias.RandomAliasGenerator;
import org.example.gqs.cypher.gen.condition.RandomConditionGenerator;
import org.example.gqs.cypher.gen.list.RandomListGenerator;
import org.example.gqs.cypher.gen.pattern.RandomPatternGenerator;
import org.example.gqs.cypher.standard_ast.ClauseSequence;
import org.example.gqs.cypher.standard_ast.IClauseSequenceBuilder;

import java.util.*;

public class CompositePerformanceOracle implements TestOracle {

    private final CompositeGlobalState globalState;
    private final IQueryGenerator<CompositeSchema, CompositeGlobalState> queryGenerator;
    public static final long groupsize = 100000;

    public List<WCInput> group = new ArrayList<>();

    public static long numofTimeOut = 0;
    public static double maxDc = 0d;
    public static double maxDr = 0d;
    public static double maxDa = 0d;
    public static long numofQueries = 0;
    public static String maxSeq = null;
    public static Long maxT1 = 0L;
    public static Long maxT2 = 0L;

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

    public CompositePerformanceOracle(CompositeGlobalState globalState) {
        this.globalState = globalState;
        this.queryGenerator = globalState.getDbmsSpecificOptions().getQueryGenerator();
    }

    public void generateQueries() throws Exception {
        CompositeSchema schema = globalState.getSchema();

        for (int i=0; i<groupsize; i++) {
            IClauseSequenceBuilder builder = ClauseSequence.createClauseSequenceBuilder();
            IClauseSequence sequence = queryGenerator.generateQuery(globalState);
            new QueryFiller<>(sequence,
                    new RandomPatternGenerator<>(schema, builder.getIdentifierBuilder(), false),
                    new RandomConditionGenerator<>(schema, false),
                    new RandomAliasGenerator<>(schema, builder.getIdentifierBuilder(), false),
                    new RandomListGenerator<>(schema, builder.getIdentifierBuilder(), false),
                    schema, builder.getIdentifierBuilder()).startVisit();
            StringBuilder sb = new StringBuilder();
            sequence.toTextRepresentation(sb);
            System.out.println("：" + sb);
            List<Long> results = globalState.executeStatementAndGetTime(new CypherQueryAdapter(sb.toString()));
            if (results.get(0) < 0 || results.get(1) < 0) {
                continue;
            }
            long time1 = Math.max(results.get(0), 1L);
            long time2 = Math.max(results.get(1), 1L);
            double retime = time1 * 1.0 / time2;
            System.out.println("：" + retime);
            double abtime = time1 * 1.0 - time2;
            System.out.println("：" + abtime);
            double reward = Math.sqrt(retime) * Math.sqrt(abtime);
            if (reward > maxDc) {
                maxDc = reward;
                maxDr = retime;
                maxDa = abtime;
                maxSeq = String.valueOf(sb);
                maxT1 = time1;
                maxT2 = time2;
            }
            numofQueries++;
            if (time1 > CompositeConnection.TIMEOUT && time2 > CompositeConnection.TIMEOUT) {
                numofTimeOut++;
            }
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
