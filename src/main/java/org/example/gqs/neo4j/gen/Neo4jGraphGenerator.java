package org.example.gqs.neo4j.gen;

import org.example.gqs.Randomly;
import org.example.gqs.cypher.ast.Direction;
import org.example.gqs.cypher.ast.INodeIdentifier;
import org.example.gqs.cypher.ast.IPattern;
import org.example.gqs.cypher.schema.CypherSchema;
import org.example.gqs.cypher.standard_ast.*;
import org.example.gqs.neo4j.Neo4jGlobalState;
import org.example.gqs.cypher.standard_ast.expr.ConstExpression;
import org.example.gqs.cypher.schema.IPropertyInfo;
import org.example.gqs.neo4j.schema.Neo4jSchema;

import java.util.ArrayList;
import java.util.List;

public class Neo4jGraphGenerator {
    private static int minNumOfNodes = 3;
    private static int maxNumOfNodes = 3;
    private static double percentOfEdges = 1;
    private static List<IPattern> INodesPattern;

    private final Neo4jGlobalState globalState;

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

    public Neo4jGraphGenerator(Neo4jGlobalState globalState){
        this.globalState = globalState;
    }

    public static List<ClauseSequence> createGraph(Neo4jGlobalState globalState) throws Exception {
        return new Neo4jGraphGenerator(globalState).generateGraph(globalState.getSchema());
    }

    public List<ClauseSequence> generateGraph(Neo4jSchema schema) throws Exception {
        List<ClauseSequence> queries = new ArrayList<>();
        IClauseSequenceBuilder builder = ClauseSequence.createClauseSequenceBuilder();

        Randomly r = new Randomly();
        INodesPattern = new ArrayList<>();
        int numOfNodes = r.getInteger(minNumOfNodes, maxNumOfNodes);
        List<CypherSchema.CypherLabelInfo> labels = schema.getLabels();
        for (int i = 0; i < numOfNodes; ++i) {
            Pattern.PatternBuilder.OngoingNode n = new Pattern.PatternBuilder(builder.getIdentifierBuilder()).newNamedNode();
            for (CypherSchema.CypherLabelInfo l : labels) {
                if (r.getBoolean()) {
                    n = n.withLabels(new Label(l.getName()));
                    for (IPropertyInfo p : l.getProperties()) {
                        if (r.getBoolean()) {
                            n = n.withProperties(new Property(p.getKey(), p.getType(), generatePropertyValue(r, p.getType())));
                        }
                    }
                }
            }
            IPattern pattern = n.build();
            INodesPattern.add(pattern);
            ClauseSequence sequence = (ClauseSequence) ClauseSequence.createClauseSequenceBuilder().CreateClause(pattern).ReturnClause(Ret.createStar()).build();
            queries.add(sequence);
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

                        for (IPropertyInfo p : relationType.getProperties()) {
                            if (r.getBoolean()) {
                                rel = rel.withProperties(new Property(p.getKey(), p.getType(), generatePropertyValue(r, p.getType())));
                            }
                        }

                        int dirChoice = r.getInteger(0, 2);
                        Direction dir = (dirChoice == 0) ? Direction.LEFT : Direction.RIGHT;
                        rel = rel.withDirection(dir);

                        IPattern merge = rel.newNodeRef(nodeJ).build();

                        ClauseSequence sequence = (ClauseSequence) ClauseSequence.createClauseSequenceBuilder()
                                .MatchClause(null, patternI, patternJ).MergeClause(merge).ReturnClause(Ret.createStar()).build();
                        queries.add(sequence);
                    }
                }
            }
        }

        return queries;
    }
}