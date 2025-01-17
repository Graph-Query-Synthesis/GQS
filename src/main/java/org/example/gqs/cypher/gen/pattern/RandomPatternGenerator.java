package org.example.gqs.cypher.gen.pattern;

import org.example.gqs.Randomly;
import org.example.gqs.cypher.ast.*;
import org.example.gqs.cypher.ast.analyzer.IMatchAnalyzer;
import org.example.gqs.cypher.ast.analyzer.INodeAnalyzer;
import org.example.gqs.cypher.schema.CypherSchema;
import org.example.gqs.cypher.standard_ast.Label;
import org.example.gqs.cypher.standard_ast.Pattern;
import org.example.gqs.cypher.standard_ast.RelationType;
import org.example.gqs.cypher.dsl.BasicPatternGenerator;
import org.example.gqs.cypher.dsl.IIdentifierBuilder;

import java.util.ArrayList;
import java.util.List;

public class RandomPatternGenerator<S extends CypherSchema<?,?>> extends BasicPatternGenerator<S> {

    private boolean overrideOld;
    public RandomPatternGenerator(S schema, IIdentifierBuilder identifierBuilder, boolean overrideOld) {
        super(schema, identifierBuilder);
        this.overrideOld = overrideOld;
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
                boolean withLabel = Randomly.getBoolean();
                boolean isNamed = !Randomly.getBooleanWithSmallProbability();
                if (withLabel) {
                    CypherSchema.CypherLabelInfo labelInfo = schema.getLabels().get(r.getInteger(0, sizeOfLabels));
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
            } else {
                List<INodeAnalyzer> idNode = matchClause.getExtendableNodeIdentifiers();
                if (idNode.size() == 0) {
                    result = new Pattern.PatternBuilder(identifierBuilder).newNamedNode().build();
                } else {
                    INodeAnalyzer node = idNode.get(r.getInteger(0, idNode.size()));
                    result = new Pattern.PatternBuilder(identifierBuilder).newRefDefinedNode(node).build();
                }
            }
        } else {
            Pattern.PatternBuilder.OngoingNode leftNode;
            boolean isNewLeft = Randomly.getBoolean();
            if (isNewLeft) {
                boolean withLabelLeft = Randomly.getBoolean();
                boolean isNamedLeft = !Randomly.getBooleanWithSmallProbability();
                if (withLabelLeft) {
                    CypherSchema.CypherLabelInfo labelInfo = schema.getLabels().get(r.getInteger(0, sizeOfLabels));
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
            } else {
                List<INodeAnalyzer> idNode = matchClause.getExtendableNodeIdentifiers();
                if (idNode.size() == 0) {
                    leftNode = new Pattern.PatternBuilder(identifierBuilder).newNamedNode();
                } else {
                    INodeAnalyzer node = idNode.get(r.getInteger(0, idNode.size()));
                    leftNode = new Pattern.PatternBuilder(identifierBuilder).newRefDefinedNode(node);
                }
            }

            Pattern.PatternBuilder.OngoingRelation relation;
            boolean withType = false;
            boolean isNamed = !Randomly.getBooleanWithSmallProbability();
            Direction direction = Randomly.fromOptions(Direction.LEFT, Direction.RIGHT, Direction.BOTH);
            long typeOfLength = r.getInteger(0, 4);
            if (withType) {
                CypherSchema.CypherRelationTypeInfo typeInfo = schema.getRelationTypes().get(r.getInteger(0, sizeOfTypes));
                IType type = new RelationType(typeInfo.getName());
                if (isNamed) {
                    if (typeOfLength == 0) {
                        relation = leftNode.newNamedRelation().withType(type).withDirection(direction).withLengthUnbounded();
                    } else if (typeOfLength == 1) {
                        relation = leftNode.newNamedRelation().withType(type).withDirection(direction).withOnlyLengthLowerBound(r.getInteger(1, 4));
                    } else if (typeOfLength == 2) {
                        relation = leftNode.newNamedRelation().withType(type).withDirection(direction).withOnlyLengthUpperBound(r.getInteger(1, 4));
                    } else {
                        relation = leftNode.newNamedRelation().withType(type).withDirection(direction).withLength(r.getInteger(1, 4));
                    }
                } else {
                    if (typeOfLength == 0) {
                        relation = leftNode.newAnonymousRelation().withType(type).withDirection(direction).withLengthUnbounded();
                    } else if (typeOfLength == 1) {
                        relation = leftNode.newAnonymousRelation().withType(type).withDirection(direction).withOnlyLengthLowerBound(r.getInteger(1, 4));
                    } else if (typeOfLength == 2) {
                        relation = leftNode.newAnonymousRelation().withType(type).withDirection(direction).withOnlyLengthUpperBound(r.getInteger(1, 4));
                    } else {
                        relation = leftNode.newAnonymousRelation().withType(type).withDirection(direction).withLength(r.getInteger(1, 4));
                    }
                }
            } else {
                if (isNamed) {
                    if (typeOfLength == 0) {
                        relation = leftNode.newNamedRelation().withDirection(direction).withLengthUnbounded();
                    } else if (typeOfLength == 1) {
                        relation = leftNode.newNamedRelation().withDirection(direction).withOnlyLengthLowerBound(r.getInteger(1, 4));
                    } else if (typeOfLength == 2) {
                        relation = leftNode.newNamedRelation().withDirection(direction).withOnlyLengthUpperBound(r.getInteger(1, 4));
                    } else {
                        relation = leftNode.newNamedRelation().withDirection(direction).withLength(r.getInteger(1, 4));
                    }
                } else {
                    if (typeOfLength == 0) {
                        relation = leftNode.newAnonymousRelation().withDirection(direction).withLengthUnbounded();
                    } else if (typeOfLength == 1) {
                        relation = leftNode.newAnonymousRelation().withDirection(direction).withOnlyLengthLowerBound(r.getInteger(1, 4));
                    } else if (typeOfLength == 2) {
                        relation = leftNode.newAnonymousRelation().withDirection(direction).withOnlyLengthUpperBound(r.getInteger(1, 4));
                    } else {
                        relation = leftNode.newAnonymousRelation().withDirection(direction).withLength(r.getInteger(1, 4));
                    }
                }
            }

            Pattern.PatternBuilder.OngoingNode rightNode;
            boolean isNewRight = Randomly.getBoolean();
            if (isNewRight) {
                boolean withLabelRight = Randomly.getBoolean();
                boolean isNamedRight = !Randomly.getBooleanWithSmallProbability();
                if (withLabelRight) {
                    CypherSchema.CypherLabelInfo labelInfo = schema.getLabels().get(r.getInteger(0, sizeOfLabels));
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
            } else {
                List<INodeAnalyzer> idNode = matchClause.getExtendableNodeIdentifiers();
                if (idNode.size() == 0) {
                    rightNode = new Pattern.PatternBuilder(identifierBuilder).newNamedNode();
                } else {
                    INodeAnalyzer node = idNode.get(r.getInteger(0, idNode.size()));
                    rightNode = new Pattern.PatternBuilder(identifierBuilder).newRefDefinedNode(node);
                }
            }
            result = rightNode.build();
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
