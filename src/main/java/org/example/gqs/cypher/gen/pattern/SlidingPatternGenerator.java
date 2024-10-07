package org.example.gqs.cypher.gen.pattern;

import org.example.gqs.MainOptions;
import org.example.gqs.Randomly;
import org.example.gqs.cypher.ast.*;
import org.example.gqs.cypher.ast.analyzer.IMatchAnalyzer;
import org.example.gqs.cypher.dsl.BasicPatternGenerator;
import org.example.gqs.cypher.dsl.IIdentifierBuilder;
import org.example.gqs.cypher.gen.*;
import org.example.gqs.cypher.schema.CypherSchema;
import org.example.gqs.cypher.schema.ILabelInfo;
import org.example.gqs.cypher.standard_ast.*;
import org.example.gqs.cypher.standard_ast.expr.ConstExpression;
import org.example.gqs.cypher.standard_ast.expr.GraphObjectVal;

import java.util.*;

public class SlidingPatternGenerator<S extends CypherSchema<?, ?>> extends BasicPatternGenerator<S> {

    private final List<SubgraphTreeNode> selected = new ArrayList<>();
    private final GraphManager graphManager;
    private final long presentNum = 0;

    private final boolean overrideOld;

    private final Randomly randomly = new Randomly();

    private final Map<String, Object> varToProperties;

    private class PatternCluster {
        List<SubgraphTreeNodeInstance> treeNodes;
        Map<AbstractNode, INodeIdentifier> nodeMap = new HashMap<>();
        Map<AbstractRelationship, IRelationIdentifier> relationMap = new HashMap<>();

        List<IPattern> recordedClusters = new ArrayList<>();
    }

    private final List<PatternCluster> patternClusters = new ArrayList<>();


    public SlidingPatternGenerator(S schema, Map<String, Object> varToProperties, GraphManager graphManager, IIdentifierBuilder identifierBuilder, boolean overrideOld) {
        super(schema, identifierBuilder);
        this.graphManager = graphManager;
        long num = randomly.getInteger(1, 4);
        num = 1;
        for (int i = 0; i < num; i++) {
            PatternCluster cluster = new PatternCluster();
            cluster.nodeMap = new HashMap<>();
            cluster.relationMap = new HashMap<>();
            cluster.treeNodes = new ArrayList<>(graphManager.matrixCluster());
            patternClusters.add(cluster);
        }
        this.overrideOld = overrideOld;
        this.varToProperties = varToProperties;
    }

    @Override
    public List<IPattern> generatePattern(IMatchAnalyzer matchClause, IIdentifierBuilder identifierBuilder, S schema) {
        if (matchClause.getPatternTuple().size() > 0 && !overrideOld) {
            return matchClause.getPatternTuple();
        }
        List<IPattern> patterns = new ArrayList<>();
        long patternAmount = Math.min(randomly.getInteger(1, 4), patternClusters.get(0).treeNodes.size());
        if (MainOptions.mode != "memgraph")
            patternAmount = Math.min(randomly.getInteger(3, 7), patternClusters.get(0).treeNodes.size());
        long clusterIndex = randomly.getInteger(0, patternClusters.size());
        PatternCluster patternCluster = patternClusters.get((int) clusterIndex);
        List<SubgraphTreeNodeInstance> treeNodeInstances = patternCluster.treeNodes;
        Collections.shuffle(treeNodeInstances, new Random(Randomly.THREAD_SEED.get()));
        for (int i = 0; i < patternAmount; i++) {
            Map<AbstractNode, INodeIdentifier> nodeMap = patternCluster.nodeMap;
            Map<AbstractRelationship, IRelationIdentifier> relationMap = patternCluster.relationMap;

            SubgraphTreeNodeInstance treeNodeInstance = treeNodeInstances.get(i);
            Subgraph subgraph = treeNodeInstance.getTreeNode().getSubgraph();
            IPattern pattern = subgraph.translateMatch(identifierBuilder, nodeMap, relationMap);

            reducePattern(treeNodeInstance, pattern, subgraph, nodeMap, relationMap);

            List<IPatternElement> patternElements = pattern.getPatternElements();





            

            patterns.add(pattern);
        }
        for (IPattern pattern : patterns) {
            for (IPatternElement element : pattern.getPatternElements()) {
                if (element instanceof INodeIdentifier && Randomly.getBoolean()) {
                    NodeIdentifier elementNode = (NodeIdentifier) element;
                    if (elementNode.getProperties() == null || elementNode.getProperties().isEmpty()) {
                        AbstractNode node = elementNode.getActualNode();
                        if (node != null) {
                            Map<String, Object> properties = node.getProperties();
                            if (properties != null && !properties.isEmpty()) {
                                String randomKey = Randomly.fromList(new ArrayList<>(properties.keySet()));
                                elementNode.getProperties().add(new Property(randomKey, new ConstExpression(properties.get(randomKey))));
                            }
                        }
                    }
                } else if (element instanceof RelationIdentifier && Randomly.getBoolean()) {
                    AbstractRelationship relationship = ((RelationIdentifier) element).actualRelationship;
                    if (relationship != null) {
                        AbstractNode startNode = relationship.getFrom();
                        AbstractNode endNode = relationship.getTo();

                        List<AbstractRelationship> endRelation = endNode.getRelationships();
                        boolean flag = true;
                        for (AbstractRelationship endRelationShip : endRelation) {
                            if (endRelationShip.getFrom() == endNode && endRelationShip.getTo() == startNode) {
                                flag = false;
                                break;
                            }
                        }
                        if (flag && MainOptions.mode != "falkordb")
                        {
                            ((RelationIdentifier) element).setDirection(Direction.BOTH);
                        }
                    }
                }
            }
        }
        return patterns;
    }

    private void shrinkPattern(IPattern pattern) {
        long randNum = randomly.getInteger(0, 100);
        IRelationIdentifier relationIdentifier1 = (IRelationIdentifier) pattern.getPatternElements().get(1);
        IRelationIdentifier relationIdentifier2 = (IRelationIdentifier) pattern.getPatternElements().get(3);


        if (relationIdentifier1.isAnonymous() && !relationIdentifier2.isAnonymous()) {
            pattern.setPatternElements(pattern.getPatternElements().subList(2, 5));
            return;
        }
        if (!relationIdentifier1.isAnonymous() && relationIdentifier2.isAnonymous()) {
            pattern.setPatternElements(pattern.getPatternElements().subList(0, 3));
            return;
        }
        if (relationIdentifier1.isAnonymous() && relationIdentifier2.isAnonymous()) {
            long randPos = randomly.getInteger(0, 3);
            pattern.setPatternElements(pattern.getPatternElements().subList((int) randPos * 2, (int) randPos * 2 + 1));
            return;
        }


        if (randNum < 30) {
            if (randNum < 15) {
                pattern.setPatternElements(pattern.getPatternElements().subList(2, 5));
            } else {
                pattern.setPatternElements(pattern.getPatternElements().subList(0, 3));
            }
        } else if (randNum < 45) {
            if (randNum < 35) {
                pattern.setPatternElements(pattern.getPatternElements().subList(0, 1));
            } else if (randNum < 40) {
                pattern.setPatternElements(pattern.getPatternElements().subList(2, 3));
            } else {
                pattern.setPatternElements(pattern.getPatternElements().subList(4, 5));
            }
        }
    }

    private void editLabels(long id, INodeIdentifier nodeIdentifier, AbstractNode abstractNode) {
        while (randomly.getInteger(0, 100) < 20) {
            if (nodeIdentifier.getLabels().size() != 0) {
                nodeIdentifier.getLabels().remove(0);
            }
        }
        for (ILabelInfo labelInfo : abstractNode.getLabelInfos()) {
            if (nodeIdentifier.getLabels().stream().noneMatch(l -> l.getName().equals(labelInfo.getName()))) {
                if (randomly.getInteger(0, 100) < 30) {
                    nodeIdentifier.getLabels().add(new Label(labelInfo.getName()));
                }
            }
        }
    }

    private void recordPropertyInfo(INodeIdentifier nodeIdentifier, AbstractNode abstractNode, Map<String, Object> properties) {

        varToProperties.put(nodeIdentifier.getName(), new GraphObjectVal(nodeIdentifier.getName(), abstractNode.getProperties()));
    }

    private void recordPropertyInfo(IRelationIdentifier relationIdentifier, AbstractRelationship abstractRelation, Map<String, Object> properties) {

        varToProperties.put(relationIdentifier.getName(), new GraphObjectVal(relationIdentifier.getName(), abstractRelation.getProperties()));
    }

    private void editRelationTypes(long id, IRelationIdentifier relationIdentifier, AbstractRelationship abstractRelationship) {

        if (relationIdentifier.getTypes().size() != 0 && randomly.getInteger(0, 100) < 20) {
            relationIdentifier.getTypes().remove(0);
        } else if (randomly.getInteger(0, 100) < 30) {
            relationIdentifier.getTypes().add(new RelationType(abstractRelationship.getType().getName()));
        }
    }

    private void reducePattern(SubgraphTreeNodeInstance instance, IPattern pattern, Subgraph subgraph, Map<AbstractNode, INodeIdentifier> nodeMap, Map<AbstractRelationship, IRelationIdentifier> relationMap) {

        long patternSize = pattern.getPatternElements().size();
        long nodeSize = (patternSize + 1) / 2;
        long relationshipSize = (patternSize - 1) / 2;

        for (int i = 0; i < nodeSize; i++) {
            editLabels(instance.getIds().get(i * 2), (INodeIdentifier) pattern.getPatternElements().get(i * 2), subgraph.getNodes().get(i));
        }
        for (int i = 0; i < relationshipSize; i++) {
            editRelationTypes(instance.getIds().get(i * 2 + 1), (IRelationIdentifier) pattern.getPatternElements().get(i * 2 + 1), subgraph.getRelationships().get(i));
        }

        for (int i = 0; i < nodeSize; i++) {
            recordPropertyInfo((INodeIdentifier) pattern.getPatternElements().get(i * 2), subgraph.getNodes().get(i), instance.getProperties().get(i * 2));
        }
        for (int i = 0; i < relationshipSize; i++) {
            recordPropertyInfo((IRelationIdentifier) pattern.getPatternElements().get(i * 2 + 1), subgraph.getRelationships().get(i), instance.getProperties().get(i * 2 + 1));
        }

    }
}
