package org.example.gqs.neo4j.oracle;

import org.example.gqs.common.oracle.TestOracle;
import org.example.gqs.common.query.GQSResultSet;
import org.example.gqs.cypher.CypherQueryAdapter;
import org.example.gqs.cypher.ast.IClauseSequence;
import org.example.gqs.cypher.schema.CypherSchema;
import org.example.gqs.cypher.schema.IPropertyInfo;
import org.example.gqs.neo4j.Neo4jGlobalState;
import org.example.gqs.cypher.gen.query.RandomQueryGenerator;
import org.example.gqs.neo4j.schema.Neo4jSchema;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Neo4jSmithCrashOracle implements TestOracle {

    private final Neo4jGlobalState globalState;
    private RandomQueryGenerator<Neo4jSchema, Neo4jGlobalState> randomQueryGenerator;
    private int[] numOfNonEmptyQueries;
    private int[] numOfTotalQueries;

    public Neo4jSmithCrashOracle(Neo4jGlobalState globalState) {
        this.globalState = globalState;
        this.randomQueryGenerator = new RandomQueryGenerator<Neo4jSchema, Neo4jGlobalState>();
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
            GQSResultSet r = globalState.executeStatementAndGet(new CypherQueryAdapter(sb.toString())).get(0);

            boolean isBugDetected = false;
            long resultLength = r.getRowNum();

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

            if (sequence.getClauseList().size() >= 3 && sequence.getClauseList().size() <= 7) {
                numOfTotalQueries[sequence.getClauseList().size() - 3]++;
                if (resultLength > 0) {
                    numOfNonEmptyQueries[sequence.getClauseList().size() - 3]++;
                }
                System.out.println(sequence.getClauseList().size() + " rate is: " + numOfNonEmptyQueries[sequence.getClauseList().size() - 3] * 1.0 / numOfTotalQueries[sequence.getClauseList().size() - 3]);
            }

            for (CypherSchema.CypherLabelInfo label : labels) {
                List<IPropertyInfo> props = label.getProperties();
                for (IPropertyInfo prop : props) {
                    System.out.println(label.getName() + ":" + prop.getKey() + ":" + ((CypherSchema.CypherPropertyInfo) prop).getFreq());
                }
            }
            for (CypherSchema.CypherRelationTypeInfo relation : relations) {
                List<IPropertyInfo> props = relation.getProperties();
                for (IPropertyInfo prop : props) {
                    System.out.println(relation.getName() + ":" + prop.getKey() + ":" + ((CypherSchema.CypherPropertyInfo) prop).getFreq());
                }
            }
        }
    }
}
