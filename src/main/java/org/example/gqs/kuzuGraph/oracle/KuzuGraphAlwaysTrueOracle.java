package org.example.gqs.kuzuGraph.oracle;

import org.example.gqs.common.oracle.TestOracle;
import org.example.gqs.common.query.GQSResultSet;
import org.example.gqs.cypher.CypherQueryAdapter;
import org.example.gqs.cypher.ast.IClauseSequence;
import org.example.gqs.cypher.gen.query.RandomQueryGenerator;
import org.example.gqs.cypher.schema.CypherSchema;
import org.example.gqs.cypher.schema.IPropertyInfo;
import org.example.gqs.kuzuGraph.KuzuGraphGlobalState;
import org.example.gqs.kuzuGraph.KuzuGraphSchema;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class KuzuGraphAlwaysTrueOracle implements TestOracle {

    private final KuzuGraphGlobalState globalState;
    private RandomQueryGenerator<KuzuGraphSchema, KuzuGraphGlobalState> randomQueryGenerator;

    public KuzuGraphAlwaysTrueOracle(KuzuGraphGlobalState globalState) {
        this.globalState = globalState;
        this.randomQueryGenerator = new RandomQueryGenerator<KuzuGraphSchema, KuzuGraphGlobalState>();
    }

    @Override
    public void check() throws Exception {
        IClauseSequence sequence = randomQueryGenerator.generateQuery(globalState);
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
