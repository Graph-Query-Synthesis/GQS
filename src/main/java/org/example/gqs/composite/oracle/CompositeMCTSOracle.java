package org.example.gqs.composite.oracle;

import org.example.gqs.Randomly;
import org.example.gqs.common.oracle.TestOracle;
import org.example.gqs.composite.CompositeConnection;
import org.example.gqs.composite.CompositeGlobalState;
import org.example.gqs.composite.CompositeSchema;
import org.example.gqs.cypher.CypherQueryAdapter;
import org.example.gqs.cypher.ast.*;
import org.example.gqs.cypher.dsl.QueryFiller;
import org.example.gqs.cypher.gen.alias.RandomAliasGenerator;
import org.example.gqs.cypher.gen.condition.RandomConditionGenerator;
import org.example.gqs.cypher.gen.list.RandomListGenerator;
import org.example.gqs.cypher.gen.pattern.RandomPatternGenerator;
import org.example.gqs.cypher.gen.query.RandomQueryGenerator;
import org.example.gqs.cypher.standard_ast.ClauseSequence;
import org.example.gqs.cypher.standard_ast.IClauseSequenceBuilder;

import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

public class CompositeMCTSOracle implements TestOracle {
    private static CompositeGlobalState globalState;
    private static RandomQueryGenerator<CompositeSchema, CompositeGlobalState> randomQueryGenerator;
    private static final int nActions = 5;
    private static final long nItes = 1000;
    private static final long nRollout = 5;
    private static final double epsilon = 1e-6;

    public static double maxDc = 0d;
    public static double maxDr = 0d;
    public static double maxDa = 0d;
    public static long numofTimeOut = 0;
    public static long numofQueries = 0;
    public static String maxSeq = null;
    public static Long maxT1 = 0L;
    public static Long maxT2 = 0L;

    public CompositeMCTSOracle(CompositeGlobalState globalState) {
        CompositeMCTSOracle.globalState = globalState;
        randomQueryGenerator = new RandomQueryGenerator<>();
    }

    public static class TreeNode {
        public IClauseSequence seq;
        public TreeNode[] children;
        public long nVisits;
        public double totValue;

        private IClauseSequence generateClauseSequence() {
            IClauseSequenceBuilder builder;
            IClauseSequence newSeq = null;
            String clause;
            if (seq == null) {
                builder = ClauseSequence.createClauseSequenceBuilder();
                clause = Randomly.fromList(Arrays.asList("MATCH", "OPTIONAL MATCH", "UNWIND"));
                switch (clause) {
                    case "MATCH":
                        newSeq = builder.MatchClause().build();
                        break;
                    case "OPTIONAL MATCH":
                        newSeq = builder.OptionalMatchClause().build();
                        break;
                    case "UNWIND":
                        newSeq = builder.UnwindClause().build();
                        break;
                }
            } else {
                builder = ClauseSequence.createClauseSequenceBuilder(seq);
                List<ICypherClause> clauseList = seq.getClauseList();
                int len = clauseList.size();
                if (clauseList.get(len - 1) instanceof IMatch) {
                    if (!((IMatch) clauseList.get(len - 1)).isOptional()) {
                        clause = Randomly.fromList(Arrays.asList("MATCH", "OPTIONAL MATCH", "WITH", "UNWIND"));
                        switch (clause) {
                            case "MATCH":
                                newSeq = builder.MatchClause().build();
                                break;
                            case "OPTIONAL MATCH":
                                newSeq = builder.OptionalMatchClause().build();
                                break;
                            case "WITH":
                                newSeq = builder.WithClause().build();
                                break;
                            case "UNWIND":
                                newSeq = builder.UnwindClause().build();
                                break;
                        }
                    } else {
                        clause = Randomly.fromList(Arrays.asList("OPTIONAL MATCH", "WITH"));
                        switch (clause) {
                            case "OPTIONAL MATCH":
                                newSeq = builder.OptionalMatchClause().build();
                                break;
                            case "WITH":
                                newSeq = builder.WithClause().build();
                                break;
                        }
                    }
                } else if ((clauseList.get(len - 1) instanceof IWith) || (clauseList.get(len - 1) instanceof IUnwind)) {
                    clause = Randomly.fromList(Arrays.asList("MATCH", "OPTIONAL MATCH", "WITH", "UNWIND"));
                    switch (clause) {
                        case "MATCH":
                            newSeq = builder.MatchClause().build();
                            break;
                        case "OPTIONAL MATCH":
                            newSeq = builder.OptionalMatchClause().build();
                            break;
                        case "WITH":
                            newSeq = builder.WithClause().build();
                            break;
                        case "UNWIND":
                            newSeq = builder.UnwindClause().build();
                            break;
                    }
                }
            }
            CompositeSchema schema = globalState.getSchema();
            new QueryFiller<>(newSeq,
                    new RandomPatternGenerator<>(schema, builder.getIdentifierBuilder(), false),
                    new RandomConditionGenerator<>(schema, false),
                    new RandomAliasGenerator<>(schema, builder.getIdentifierBuilder(), false),
                    new RandomListGenerator<>(schema, builder.getIdentifierBuilder(), false),
                    schema, builder.getIdentifierBuilder()).startVisit();
            return newSeq;
        }

        private IClauseSequence generateQuery() {
            List<ICypherClause> clauseList = seq.getClauseList();
            int len = clauseList.size();
            if (clauseList.get(len - 1) instanceof IReturn) {
                return seq;
            } else {
                IClauseSequence sequence = null;
                IClauseSequenceBuilder builder = ClauseSequence.createClauseSequenceBuilder(seq);
                long numOfClauses = Randomly.smallNumber();
                if (clauseList.get(len - 1) instanceof IMatch) {
                    if (!((IMatch) clauseList.get(len - 1)).isOptional()) {
                        sequence = randomQueryGenerator.generateClauses(builder, numOfClauses, Arrays.asList("MATCH", "OPTIONAL MATCH", "WITH", "UNWIND")).ReturnClause().build();
                    } else {
                        sequence = randomQueryGenerator.generateClauses(builder, numOfClauses, Arrays.asList("OPTIONAL MATCH", "WITH")).ReturnClause().build();
                    }
                } else if ((clauseList.get(len - 1) instanceof IWith) || (clauseList.get(len - 1) instanceof IUnwind)) {
                    sequence = randomQueryGenerator.generateClauses(builder, numOfClauses, Arrays.asList("MATCH", "OPTIONAL MATCH", "WITH", "UNWIND")).ReturnClause().build();
                }
                CompositeSchema schema = globalState.getSchema();
                new QueryFiller<>(sequence,
                        new RandomPatternGenerator<>(schema, builder.getIdentifierBuilder(), false),
                        new RandomConditionGenerator<>(schema, false),
                        new RandomAliasGenerator<>(schema, builder.getIdentifierBuilder(), false),
                        new RandomListGenerator<>(schema, builder.getIdentifierBuilder(), false),
                        schema, builder.getIdentifierBuilder()).startVisit();
                return sequence;
            }
        }

        public boolean isLeaf() {
            return children == null;
        }

        public TreeNode select() {
            TreeNode selected = null;
            double bestValue = -Double.MAX_VALUE;
            for (TreeNode c : children) {
                double uctValue = c.totValue / (c.nVisits + epsilon) +
                        Math.sqrt(2 * Math.log(nVisits + 1) / (c.nVisits + epsilon));
                if (uctValue > bestValue) {
                    selected = c;
                    bestValue = uctValue;
                }
            }
            return selected;
        }

        public void expand() {
            children = new TreeNode[nActions];
            for (int i = 0; i < nActions; i++) {
                children[i] = new TreeNode();
                children[i].seq = generateClauseSequence();
                children[i].nVisits = 0;
            }
        }

        public double rollOut() throws Exception {
            double maxReward = 0d;
            for (int i = 0; i < nRollout; i++) {
                System.out.println("Rollout：" + i);
                IClauseSequence query = generateQuery();
                StringBuilder sb = new StringBuilder();
                query.toTextRepresentation(sb);
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
                if (reward > maxReward) {
                    maxReward = reward;
                }
                numofQueries++;
                if (time1 > CompositeConnection.TIMEOUT && time2 > CompositeConnection.TIMEOUT) {
                    numofTimeOut++;
                }
            }
            System.out.println("：" + maxDr);
            System.out.println("：" + maxDa);
            System.out.println("：" + maxDc);
            return maxReward;
        }

        public void updateState(double value) {
            nVisits++;
            totValue = value;
        }

        public void selectAction() throws Exception {
            List<TreeNode> visited = new LinkedList<>();
            TreeNode cur = this;
            visited.add(this);
            while (!cur.isLeaf()) {
                cur = cur.select();
                visited.add(cur);
            }
            if (cur.nVisits > 0) {
                cur.expand();
                cur = cur.select();
                visited.add(cur);
            }
            double value = cur.rollOut();
            for (TreeNode node : visited) {
                node.updateState(value);
            }
        }
    }

    @Override
    public void check() throws Exception {
        TreeNode tree = new TreeNode();
        tree.totValue = 0d;
        tree.nVisits = 1;
        for (int i = 0; i < nItes; i++) {
            System.out.println("**********************************************************");
            System.out.println("：" + i);
            tree.selectAction();
        }
    }
}
