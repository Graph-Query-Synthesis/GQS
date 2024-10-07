package org.example.gqs.arcadeDB.gen;

import org.example.gqs.Randomly;
import org.example.gqs.cypher.CypherQueryAdapter;
import org.example.gqs.cypher.ast.Direction;
import org.example.gqs.cypher.ast.INodeIdentifier;
import org.example.gqs.cypher.ast.IPattern;
import org.example.gqs.cypher.schema.CypherSchema;
import org.example.gqs.arcadeDB.ArcadeDBGlobalState;
import org.example.gqs.cypher.standard_ast.*;
import org.example.gqs.cypher.standard_ast.expr.ConstExpression;
import org.example.gqs.cypher.schema.IPropertyInfo;
import org.example.gqs.arcadeDB.ArcadeDBSchema;

import java.util.ArrayList;
import java.util.List;

public class ArcadeDBGraphGenerator {
    private static long minNumOfNodes = 10;
    private static long maxNumOfNodes = 10;
    private static double percentOfEdges = 0.01;
    private static List<IPattern> INodesPattern;

    private final ArcadeDBGlobalState globalState;

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

    public ArcadeDBGraphGenerator(ArcadeDBGlobalState globalState){
        this.globalState = globalState;
    }

    public static List<CypherQueryAdapter> createGraph(ArcadeDBGlobalState globalState) throws Exception {
        return new ArcadeDBGraphGenerator(globalState).generateGraph(globalState.getSchema());
    }

    public List<CypherQueryAdapter> generateGraph(ArcadeDBSchema schema) throws Exception {
        List<CypherQueryAdapter> queries = new ArrayList<>();
        IClauseSequenceBuilder builder = ClauseSequence.createClauseSequenceBuilder();

        Randomly r = new Randomly();
        INodesPattern = new ArrayList<>();
        long numOfNodes = r.getInteger(minNumOfNodes, maxNumOfNodes);
        List<CypherSchema.CypherLabelInfo> labels = schema.getLabels();
        for (int i = 0; i < numOfNodes; ++i) {
            Pattern.PatternBuilder.OngoingNode n = new Pattern.PatternBuilder(builder.getIdentifierBuilder()).newNamedNode();
            CypherSchema.CypherLabelInfo l = labels.get(r.getInteger(0, labels.size() - 1));

            n = n.withLabels(new Label(l.getName()));
            for (IPropertyInfo p : l.getProperties()) {
                if (r.getBooleanWithRatherLowProbability()) {
                    n = n.withProperties(new Property(p.getKey(), p.getType(), generatePropertyValue(r, p.getType())));
                }
            }
            n = n.withProperties(new Property("id", CypherType.NUMBER, new ConstExpression(i)));
            IPattern pattern = n.build();
            INodesPattern.add(pattern);
            ClauseSequence sequence = (ClauseSequence) ClauseSequence.createClauseSequenceBuilder().CreateClause(pattern).build();
            StringBuilder sb = new StringBuilder();
            sequence.toTextRepresentation(sb);
            queries.add(new CypherQueryAdapter(sb.toString()));
        }
        List<CypherSchema.CypherRelationTypeInfo> relationTypes = schema.getRelationTypes();
        for (int i = 0; i < numOfNodes; ++i) {
            for (int j = 0; j < numOfNodes; ++j) {
                for (CypherSchema.CypherRelationTypeInfo relationType : relationTypes) {
                    if (r.getInteger(0, 1000000) < percentOfEdges * 1000000) {
                        IPattern patternI = INodesPattern.get(i);
                        IPattern patternJ = INodesPattern.get(j);
                        INodeIdentifier nodeI = (INodeIdentifier) patternI.getPatternElements().get(0);
                        INodeIdentifier nodeJ = (INodeIdentifier) patternJ.getPatternElements().get(0);

                        Pattern.PatternBuilder.OngoingRelation rel = new Pattern.PatternBuilder(builder.getIdentifierBuilder())
                                .newRefDefinedNode(nodeI)
                                .newNamedRelation().withType(new RelationType(relationType.getName()));

                        IPropertyInfo p = relationType.getProperties().get(r.getInteger(0, relationType.getProperties().size() - 1));

                        rel = rel.withProperties(new Property(p.getKey(), p.getType(), generatePropertyValue(r, p.getType())));

                        long dirChoice = r.getInteger(0, 2);
                        Direction dir = (dirChoice == 0) ? Direction.LEFT : Direction.RIGHT;
                        rel = rel.withDirection(dir);

                        IPattern merge = rel.newNodeRef(nodeJ).build();
                        ConstExpression n0 = (ConstExpression) nodeI.getProperties().get(nodeI.getProperties().size() - 1).getVal();
                        ConstExpression n1 = (ConstExpression) nodeJ.getProperties().get(nodeJ.getProperties().size() - 1).getVal();
                        StringBuilder str = new StringBuilder();
                        str.append("MATCH (" + nodeI.getName() + "), (" + nodeJ.getName() + ") WHERE " + nodeI.getName() + ".id = ");
                        StringBuilder n0v = new StringBuilder();
                        StringBuilder n1v = new StringBuilder();
                        n0.toTextRepresentation(n0v);
                        n1.toTextRepresentation(n1v);
                        str.append(n0v);
                        str.append(" AND " + nodeJ.getName() + ".id = ");
                        str.append(n1v);

                        ClauseSequence sequence = (ClauseSequence) ClauseSequence.createClauseSequenceBuilder()
                                .MergeClause(merge).build();
                        StringBuilder sb = new StringBuilder();
                        sequence.toTextRepresentation(sb);
                        str.append(" " + sb);
                        queries.add(new CypherQueryAdapter(str.toString()));
                    }
                }
            }
        }
        return queries;
    }
}
