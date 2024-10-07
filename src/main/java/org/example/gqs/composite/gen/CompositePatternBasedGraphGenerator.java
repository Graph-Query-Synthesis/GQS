package org.example.gqs.composite.gen;

import org.example.gqs.Randomly;
import org.example.gqs.composite.CompositeGlobalState;
import org.example.gqs.composite.CompositeSchema;
import org.example.gqs.cypher.CypherQueryAdapter;
import org.example.gqs.cypher.ast.IPattern;
import org.example.gqs.cypher.dsl.IGraphGenerator;
import org.example.gqs.cypher.gen.SubgraphManager;
import org.example.gqs.cypher.schema.CypherSchema;
import org.example.gqs.cypher.schema.IPropertyInfo;
import org.example.gqs.cypher.standard_ast.CypherType;
import org.example.gqs.cypher.standard_ast.expr.ConstExpression;

import java.util.List;

public class CompositePatternBasedGraphGenerator implements IGraphGenerator<CompositeGlobalState> {

    private static long minNumOfNodes = 200;
    private static long maxNumOfNodes = 200;
    private static double percentOfEdges = 0.001;
    private static List<IPattern> INodesPattern;

    private final CompositeGlobalState globalState;




    public CompositePatternBasedGraphGenerator(CompositeGlobalState globalState){
        this.globalState = globalState;
    }

    private ConstExpression generatePropertyValue(Randomly r, CypherType type) throws Exception {
        switch (type) {
            case NUMBER:
                return new ConstExpression(r.getInteger());
            case STRING:
                return new ConstExpression(r.getString());
            case BOOLEAN:
                return new ConstExpression(r.getInteger(0, 2) == 0);
            default:
                throw new Exception("undefined type in generator!");
        }
    }

    public List<CypherQueryAdapter> createGraph(CompositeGlobalState globalState) throws Exception {
        List<CypherQueryAdapter> queries = new SubgraphManager(globalState.getSchema(), globalState.getOptions()).generateCreateGraphQueries();

        CompositeSchema schema = globalState.getSchema();
        for (CypherSchema.CypherLabelInfo l : schema.getLabels()) {
            for (IPropertyInfo propertyInfo : l.getProperties()) {
                for (int i = 0; i < 20; i++) {
                    StringBuilder sb = new StringBuilder();
                    sb.append("MATCH (n: ").append(l.getName()).append(") WITH * WHERE rand() < 0.2 SET n.")
                            .append(propertyInfo.getKey()).append(" = ");
                    generatePropertyValue(new Randomly(), propertyInfo.getType()).toTextRepresentation(sb);
                    queries.add(new CypherQueryAdapter(sb.toString()));
                }
            }
        }

        for (CypherSchema.CypherRelationTypeInfo r : schema.getRelationTypes()) {
            for (IPropertyInfo propertyInfo : r.getProperties()) {
                for (int i = 0; i < 20; i++) {
                    StringBuilder sb = new StringBuilder();
                    sb.append("MATCH ()-[r: ").append(r.getName()).append("]->() WITH * WHERE rand() < 0.2 SET r.")
                            .append(propertyInfo.getKey()).append(" = ");
                    generatePropertyValue(new Randomly(), propertyInfo.getType()).toTextRepresentation(sb);
                    queries.add(new CypherQueryAdapter(sb.toString()));
                }
            }
        }
        return queries;
    }
}
