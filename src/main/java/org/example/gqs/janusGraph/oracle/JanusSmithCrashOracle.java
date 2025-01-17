package org.example.gqs.janusGraph.oracle;

import org.example.gqs.common.oracle.TestOracle;
import org.example.gqs.common.query.GQSResultSet;
import org.example.gqs.cypher.CypherQueryAdapter;
import org.example.gqs.cypher.ast.IClauseSequence;
import org.example.gqs.cypher.gen.query.RandomQueryGenerator;
import org.example.gqs.cypher.schema.CypherSchema;
import org.example.gqs.cypher.schema.IPropertyInfo;
import org.example.gqs.janusGraph.JanusGlobalState;
import org.example.gqs.janusGraph.schema.JanusSchema;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CompletionException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class JanusSmithCrashOracle implements TestOracle {

    private final JanusGlobalState globalState;
    private RandomQueryGenerator<JanusSchema, JanusGlobalState> randomQueryGenerator;
    private int[] numOfNonEmptyQueries;
    private int[] numOfTotalQueries;

    public JanusSmithCrashOracle(JanusGlobalState globalState) {
        this.globalState = globalState;
        this.randomQueryGenerator = new RandomQueryGenerator<JanusSchema, JanusGlobalState>();
        numOfNonEmptyQueries = new int[]{0, 0, 0, 0, 0};
        numOfTotalQueries = new int[]{0, 0, 0, 0, 0};
    }

    @Override
    public void check() throws Exception {
        IClauseSequence sequence = randomQueryGenerator.generateQuery(globalState);
        if (sequence.getClauseList().size() <= 8) {
            StringBuilder sb = new StringBuilder();
            sequence.toTextRepresentation(sb);
            System.out.println(sb);
            GQSResultSet r = null;
            long resultLength = 0;
            try {
                r = globalState.executeStatementAndGet(new CypherQueryAdapter(sb.toString())).get(0);
                resultLength = r.getRowNum();
            } catch (CompletionException e) {
                System.out.println("CypherGremlin！");
                System.out.println(e.getMessage());
            }

            boolean isBugDetected = false;

            List<CypherSchema.CypherLabelInfo> labels = globalState.getSchema().getLabels();
            List<CypherSchema.CypherRelationTypeInfo> relations = globalState.getSchema().getRelationTypes();
            if (resultLength > 0) {
                randomQueryGenerator.addExecutionRecord(sequence, isBugDetected, resultLength);

                List<String> coveredProperty = new ArrayList<>();
                Pattern allProps = Pattern.compile("(\\.)(k\\d+)(\\))");
                Matcher matcher = allProps.matcher(sb);
                while (matcher.find()) {
                    if (!coveredProperty.contains(matcher.group(2))) {
                        coveredProperty.add(matcher.group(2));
                    }
                }

                for (String name : coveredProperty) {
                    found:
                    {
                        for (CypherSchema.CypherLabelInfo label : labels) {
                            List<IPropertyInfo> props = label.getProperties();
                            for (IPropertyInfo prop : props) {
                                if (Objects.equals(prop.getKey(), name)) {
                                    ((CypherSchema.CypherPropertyInfo) prop).addFreq();
                                    break found;
                                }
                            }
                        }
                        for (CypherSchema.CypherRelationTypeInfo relation : relations) {
                            List<IPropertyInfo> props = relation.getProperties();
                            for (IPropertyInfo prop : props) {
                                if (Objects.equals(prop.getKey(), name)) {
                                    ((CypherSchema.CypherPropertyInfo) prop).addFreq();
                                    break found;
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
