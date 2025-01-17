package org.example.gqs.cypher.gen.pattern;

import org.example.gqs.Randomly;
import org.example.gqs.cypher.ast.*;
import org.example.gqs.cypher.ast.analyzer.IMatchAnalyzer;
import org.example.gqs.cypher.ast.analyzer.INodeAnalyzer;
import org.example.gqs.cypher.dsl.BasicPatternGenerator;
import org.example.gqs.cypher.dsl.IIdentifierBuilder;
import org.example.gqs.cypher.gen.AbstractNode;
import org.example.gqs.cypher.gen.AbstractRelationship;
import org.example.gqs.cypher.gen.GraphManager;
import org.example.gqs.cypher.schema.CypherSchema;
import org.example.gqs.cypher.standard_ast.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class ComparedPatternGenerator<S extends CypherSchema<?,?>> extends BasicPatternGenerator<S> {

    private boolean overrideOld;
    private Map<String, Object> varToProperties;
    private GraphManager graphManager;
    public ComparedPatternGenerator(S schema, Map<String, Object> varToProperties, GraphManager graphManager, IIdentifierBuilder identifierBuilder, boolean overrideOld) {
        super(schema, identifierBuilder);
        this.overrideOld = overrideOld;
        this.varToProperties = varToProperties;
        this.graphManager = graphManager;
    }



    public IPattern generateSinglePattern(IMatchAnalyzer matchClause, IIdentifierBuilder identifierBuilder, S schema) {

        Randomly r = new Randomly();

        long sizeOfLabels = schema.getLabels().size();
        long sizeOfTypes = schema.getRelationTypes().size();
        long lenOfPattern = Randomly.fromOptions(2, 3);
        IPattern result = null;
        if (lenOfPattern == 1) {
            boolean isNew = Randomly.getBoolean();
            if (isNew) {
                boolean isNamed = true;
                AbstractNode node = graphManager.getNodes().get(r.getInteger(0, graphManager.getNodes().size()));
                boolean withLabel = Randomly.getBoolean() && node.getLabels().size() > 0;
                if (withLabel) {
                    CypherSchema.CypherLabelInfo labelInfo = (CypherSchema.CypherLabelInfo) node.getLabelInfos().get(r.getInteger(0, node.getLabelInfos().size()));
                    ILabel label = new Label(labelInfo.getName());
                    if (isNamed) {
                        result = new Pattern.PatternBuilder(identifierBuilder).newNamedNode().withLabels(label).build();
                    } else {
                        result = new Pattern.PatternBuilder(identifierBuilder).newAnonymousNode().withLabels(label).build();
                    }
                } else {
                    if (isNamed) {
                        result = new Pattern.PatternBuilder(identifierBuilder).newNamedNode().build();
                    } else {
                        result = new Pattern.PatternBuilder(identifierBuilder).newAnonymousNode().build();
                    }
                }
                ((NodeIdentifier) result.getPatternElements().get(0)).actualNode = node;
            } else {
                List<INodeAnalyzer> idNode = matchClause.getExtendableNodeIdentifiers();
                if (idNode.size() == 0) {
                    AbstractNode node = graphManager.getNodes().get(r.getInteger(0, graphManager.getNodes().size()));
                    result = new Pattern.PatternBuilder(identifierBuilder).newNamedNode().build();
                    ((NodeIdentifier) result.getPatternElements().get(0)).actualNode = node;
                } else {
                    INodeAnalyzer node = idNode.get(r.getInteger(0, idNode.size()));
                    assert (((NodeAnalyzer) node).getActualNode() != null);
                    result = new Pattern.PatternBuilder(identifierBuilder).newRefDefinedNode(node).build();
                    ((NodeIdentifier) result.getPatternElements().get(0)).actualNode = ((NodeAnalyzer) node).getActualNode();
                }
            }
        } else {
            Pattern.PatternBuilder.OngoingNode leftNode;
            List<Object> actualElements = new ArrayList<>();
            boolean isNewLeft = Randomly.getBoolean();
            AbstractNode node = graphManager.getNodes().get(r.getInteger(0, graphManager.getNodes().size()));
            if (isNewLeft) {
                boolean withLabelLeft = Randomly.getBoolean() && node.getLabels().size() > 0;
                boolean isNamedLeft = true;
                if (withLabelLeft) {
                    CypherSchema.CypherLabelInfo labelInfo = (CypherSchema.CypherLabelInfo) node.getLabelInfos().get(r.getInteger(0, node.getLabelInfos().size()));
                    ILabel label = new Label(labelInfo.getName());
                    if (isNamedLeft) {
                        leftNode = new Pattern.PatternBuilder(identifierBuilder).newNamedNode().withLabels(label);
                    } else {
                        leftNode = new Pattern.PatternBuilder(identifierBuilder).newAnonymousNode().withLabels(label);
                    }
                } else {
                    if (isNamedLeft) {
                        leftNode = new Pattern.PatternBuilder(identifierBuilder).newNamedNode();
                    } else {
                        leftNode = new Pattern.PatternBuilder(identifierBuilder).newAnonymousNode();
                    }
                }
                actualElements.add(node);
            } else {
                List<INodeAnalyzer> idNode = matchClause.getExtendableNodeIdentifiers();
                if (idNode.size() == 0) {
                    node = graphManager.getNodes().get(r.getInteger(0, graphManager.getNodes().size()));
                    leftNode = new Pattern.PatternBuilder(identifierBuilder).newNamedNode();
                    actualElements.add(node);
                } else {
                    INodeAnalyzer nodeExist = idNode.get(r.getInteger(0, idNode.size()));
                    assert (((NodeAnalyzer) nodeExist).getActualNode() != null);
                    leftNode = new Pattern.PatternBuilder(identifierBuilder).newRefDefinedNode(nodeExist);
                    actualElements.add(((NodeAnalyzer) nodeExist).getActualNode());
                }
            }
            if (((AbstractNode) actualElements.get(actualElements.size() - 1)).getRelationships().size() == 0) {
                result = leftNode.build();
                assert (result.getPatternElements().size() == actualElements.size());
                for (int i = 0; i < actualElements.size(); i++) {
                    IPatternElement element = result.getPatternElements().get(i);
                    if (element instanceof NodeIdentifier) {
                        ((NodeIdentifier) element).actualNode = (AbstractNode) actualElements.get(i);
                    } else {
                        ((RelationIdentifier) element).actualRelationship = (AbstractRelationship) actualElements.get(i);
                    }
                }
                return result;
            }

            Pattern.PatternBuilder.OngoingRelation relation = null;
            boolean isNamed = true;
            Direction direction = null;
            long typeOfLength = 0;
            AbstractRelationship rel = ((AbstractNode) (actualElements.get(actualElements.size() - 1))).getRelationships().get(r.getInteger(0, ((AbstractNode) (actualElements.get(actualElements.size() - 1))).getRelationships().size()));
            AbstractNode right;
            if (rel.getFrom().getId() == ((AbstractNode) actualElements.get(actualElements.size() - 1)).getId()) {
                direction = Direction.RIGHT;
                right = rel.getTo();
            } else {
                right = rel.getFrom();
                direction = Direction.LEFT;
            }
            boolean withType = Randomly.getBoolean() && rel.getType() != null;
            if (withType) {
                CypherSchema.CypherRelationTypeInfo typeInfo = (CypherSchema.CypherRelationTypeInfo) rel.getType();
                IType type = new RelationType(typeInfo.getName());
                if (isNamed) {
                    if (typeOfLength == 0) {
                        relation = leftNode.newNamedRelation().withType(type).withDirection(direction).withLength(1);
                    }
                } else {
                    if (typeOfLength == 0) {
                        relation = leftNode.newAnonymousRelation().withType(type).withDirection(direction).withLength(1);
                    }
                }
            } else {
                if (isNamed) {
                    if (typeOfLength == 0) {
                        relation = leftNode.newNamedRelation().withDirection(direction).withLength(1);
                    }
                } else {
                    if (typeOfLength == 0) {
                        relation = leftNode.newAnonymousRelation().withDirection(direction).withLength(1);
                    }
                }
            }
            actualElements.add(rel);

            Pattern.PatternBuilder.OngoingNode rightNode;
            boolean withLabelRight = Randomly.getBoolean() && right.getLabels().size() > 0;
            boolean isNamedRight = true;

            if (withLabelRight) {
                CypherSchema.CypherLabelInfo labelInfo = (CypherSchema.CypherLabelInfo) right.getLabelInfos().get(r.getInteger(0, right.getLabelInfos().size()));
                ILabel label = new Label(labelInfo.getName());
                if (isNamedRight) {
                    rightNode = relation.newNamedNode().withLabels(label);
                } else {
                    rightNode = relation.newAnonymousNode().withLabels(label);
                }
            } else {
                if (isNamedRight) {
                    rightNode = relation.newNamedNode();
                } else {
                    rightNode = relation.newAnonymousNode();
                }
            }
            actualElements.add(right);
            result = rightNode.build();
            assert (result.getPatternElements().size() == actualElements.size());
            for (int i = 0; i < actualElements.size(); i++) {
                IPatternElement element = result.getPatternElements().get(i);
                if (element instanceof NodeIdentifier) {
                    ((NodeIdentifier) element).actualNode = (AbstractNode) actualElements.get(i);
                } else {
                    ((RelationIdentifier) element).actualRelationship = (AbstractRelationship) actualElements.get(i);
                }
            }
        }
        return result;
    }
    @Override
    public List<IPattern> generatePattern(IMatchAnalyzer matchClause, IIdentifierBuilder identifierBuilder, S schema) {
        List<IPattern> matchPattern = matchClause.getPatternTuple();
        if (matchPattern.size() > 0 && !overrideOld) {
            return matchPattern;
        }

        List<IPattern> patternTuple = new ArrayList<>();
        Randomly r = new Randomly();
        long numOfPatterns = 1;

        for (int i = 0; i < numOfPatterns; i++) {
            patternTuple.add(generateSinglePattern(matchClause, identifierBuilder, schema));
        }
        return patternTuple;
    }
}
