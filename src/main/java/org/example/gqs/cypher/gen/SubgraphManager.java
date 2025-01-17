package org.example.gqs.cypher.gen;

import org.example.gqs.MainOptions;
import org.example.gqs.Randomly;
import org.example.gqs.cypher.CypherQueryAdapter;
import org.example.gqs.cypher.ICypherSchema;
import org.example.gqs.cypher.ast.*;
import org.example.gqs.cypher.dsl.IIdentifierBuilder;
import org.example.gqs.cypher.schema.ILabelInfo;
import org.example.gqs.cypher.schema.IPropertyInfo;
import org.example.gqs.cypher.schema.IRelationTypeInfo;
import org.example.gqs.cypher.standard_ast.ClauseSequence;
import org.example.gqs.cypher.standard_ast.Ret;
import org.example.gqs.cypher.standard_ast.expr.CallExpression;
import org.example.gqs.cypher.standard_ast.expr.ConstExpression;
import org.example.gqs.cypher.standard_ast.IClauseSequenceBuilder;

import java.util.*;
import java.util.stream.Collectors;

public class SubgraphManager {
    private static long maxNodeColor = 3;

    private List<Subgraph> subgraphs = new ArrayList<>();
    private ICypherSchema schema;

    private SubgraphTreeNode root;

    private long singleRootPrimaryNum = 1;
    private long splitPerLayer = 1;
    private long splitFactor = 1;
    private long layerNum = 0;

    private static final long maxGraphNum = 8;

    private long presentID = 0;

    private Map<IPropertyInfo, List<Object>> propertyValues = new HashMap<>();


    private Randomly randomly = new Randomly();
    private MainOptions options;

    public SubgraphManager(ICypherSchema schema, MainOptions mainOptions, long singleRootPrimaryNum) {
        this.schema = schema;
        this.options = mainOptions;
        this.singleRootPrimaryNum = singleRootPrimaryNum;
        splitPerLayer = randomly.getInteger(0, 3);
        splitFactor = randomly.getInteger(0, 3);

        layerNum = 0;
        long presentNum = 1;
        long lastLayerLimit = 1 << singleRootPrimaryNum;
        long accPerLayer = 1;
        long totalQueryNum = 1;
        while (presentNum < (1 << maxGraphNum) && totalQueryNum < 128) {
            layerNum++;
            lastLayerLimit = lastLayerLimit >> splitFactor;
            accPerLayer *= (1 << splitPerLayer);
            if (lastLayerLimit == 0) {
                lastLayerLimit = 1;
            }
            presentNum += accPerLayer * lastLayerLimit;
            totalQueryNum += accPerLayer;
        }

        System.out.println("singleRootPrimaryNum:" + (1 << singleRootPrimaryNum));
        System.out.println("splitPerLayer:" + (1 << splitPerLayer));
        System.out.println("splitFactor:" + (1 << splitFactor));
        System.out.println("layerNum" + layerNum);
    }

    public SubgraphManager(ICypherSchema schema, MainOptions options) {
        this(schema, options, new Randomly().getInteger(0, 6));
    }

    public void generateAllSubgraphs() {
        for (int i = 0; i < 100; i++) {
            subgraphs.add(generateSubgraph());
        }
    }

    public List<CypherQueryAdapter> generateCreateGraphQueries() {
        List<CypherQueryAdapter> result = new ArrayList<>();
        List<SubgraphTreeNode> subgraphQueue = new LinkedList<>();

        Subgraph root = generateThreeNodesGraph();
        SubgraphTreeNode rootNode = new SubgraphTreeNode(root);
        subgraphQueue.add(rootNode);
        this.root = rootNode;
        for (int i = 0; i < (1 << singleRootPrimaryNum); i++) {
            result.addAll(generateCreate(rootNode, 1));
        }


        while (subgraphQueue.size() != 0) {
            SubgraphTreeNode subgraphTreeNode = subgraphQueue.get(0);
            subgraphQueue.remove(0);
            long depth = subgraphTreeNode.getDepth();
            if (depth != 1) {
            }
            if (depth >= layerNum) {
                continue;
            }
            for (int i = 0; i < (1 << splitPerLayer); i++) {
                SubgraphTreeNode child = generateSubgraphMutation(subgraphTreeNode, result);
                subgraphQueue.add(child);
            }
        }

        return result;
    }

    private Object generateValue(IPropertyInfo propertyInfo) {
        if (propertyValues.containsKey(propertyInfo)) {
            List<Object> values = propertyValues.get(propertyInfo);
            return values.get(randomly.getInteger(0, values.size()));
        } else {
            List<Object> values = new ArrayList<>();
            for (int i = 0; i < 20; i++) {
                switch (propertyInfo.getType()) {
                    case NUMBER:
                        if (MainOptions.mode == "falkordb")
                            values.add(randomly.getLong(Integer.MIN_VALUE, Integer.MAX_VALUE));
                        else
                            values.add(randomly.getLong(Long.MIN_VALUE, Long.MAX_VALUE));
                        break;
                    case BOOLEAN:
                        values.add(randomly.getLong(0, 2) == 0);
                        break;
                    case STRING:
                        values.add(randomly.getString());
                        break;
                }
            }
            propertyValues.put(propertyInfo, values);
            return values.get(randomly.getInteger(0, values.size()));
        }
    }

    private Map<String, Object> generatePropertiesInstance(AbstractNode abstractNode, long id) {
        Map<String, Object> result = new HashMap<>();
        result.put("id", id);
        for (ILabelInfo labelInfo : abstractNode.getLabelInfos()) {
            for (IPropertyInfo propertyInfo : labelInfo.getProperties()) {
                if (randomly.getInteger(0, 100) < 95) {
                    result.put(propertyInfo.getKey(), generateValue(propertyInfo));
                }
            }
        }
        return result;
    }

    private Map<String, Object> generatePropertiesInstance(AbstractRelationship abstractRelationship, long id) {
        Map<String, Object> result = new HashMap<>();
        result.put("id", id);
        if (abstractRelationship.getType() != null) {
            for (IPropertyInfo propertyInfo : abstractRelationship.getType().getProperties()) {
                if (randomly.getInteger(0, 100) < 95) {
                    result.put(propertyInfo.getKey(), generateValue(propertyInfo));
                }
            }
        }
        return result;
    }


    private IPattern generateMatchPattern(Subgraph subgraph, IIdentifierBuilder identifierBuilder, Map<AbstractNode, INodeIdentifier> nodeMap, Map<AbstractRelationship, IRelationIdentifier> relationMap) {
        IPattern pattern = subgraph.translateMatch(identifierBuilder, nodeMap, relationMap);
        return pattern;
    }

    public List<CypherQueryAdapter> generateMatchMerge(SubgraphTreeNode treeNode) {
        List<CypherQueryAdapter> queries = new ArrayList<>();
        Subgraph child = treeNode.getSubgraph();
        Set<SubgraphTreeNode> ancestors = treeNode.getAncestors();
        subgraphs.add(child);

        IClauseSequenceBuilder clauseSequenceBuilder = ClauseSequence.createClauseSequenceBuilder();
        Map<AbstractNode, INodeIdentifier> nodeMap = new HashMap<>();
        Map<AbstractRelationship, IRelationIdentifier> relationMap = new HashMap<>();

        List<IPattern> parentPatterns = new ArrayList<>();
        ancestors.stream().forEach(
                p -> {
                    IPattern parentPattern = generateMatchPattern(p.getSubgraph(), clauseSequenceBuilder.getIdentifierBuilder(), nodeMap, relationMap);
                    parentPatterns.add(parentPattern);
                }
        );

        long limitLog = singleRootPrimaryNum - treeNode.getDepth() * splitFactor;
        long limit;
        if (limitLog < 0) {
            limit = 1;
        } else {
            limit = 1 << (singleRootPrimaryNum - treeNode.getDepth() * splitFactor);
        }

        IClauseSequenceBuilder.IOngoingMatch ongoingMatch = null;
        for (IPattern pattern : parentPatterns) {
            if (ongoingMatch == null) {
                ongoingMatch = clauseSequenceBuilder.MatchClause(null, pattern);
            } else {
                ongoingMatch = ongoingMatch.MatchClause(null, pattern);
            }
        }

        IPattern childPattern = child.translateMerge(clauseSequenceBuilder.getIdentifierBuilder(), nodeMap, relationMap);

        IClauseSequence clauseSequence = ongoingMatch
                .WithClause(null, Ret.createStar())
                .limit(new ConstExpression(limit))
                .orderBy(false, new CallExpression("rand()", "rand()", new ArrayList<>()))
                .MergeClause(childPattern).build();
        StringBuilder stringBuilder = new StringBuilder();
        clauseSequence.toTextRepresentation(stringBuilder);


        queries.add(new CypherQueryAdapter(stringBuilder.toString()));
        return queries;
    }

    private void setProperties(StringBuilder sb, IPattern pattern, List<Integer> ids, long from, long to) {
        for (int i = (int) from; i < to; i++) {
            IPatternElement patternElement = pattern.getPatternElements().get(i);
            sb.append(" SET " + patternElement.getName() + ".id = " + ids.get(i));


        }
    }


    private List<CypherQueryAdapter> generateCreate(SubgraphTreeNode subgraphTreeNode, long totalNum) {
        List<CypherQueryAdapter> queries = new ArrayList<>();

        Subgraph subgraph = subgraphTreeNode.getSubgraph();


        for (int i = 0; i < totalNum; i++) {
            SubgraphTreeNodeInstance instance = new SubgraphTreeNodeInstance();
            List<Integer> ids = new ArrayList<>();
            for (int j = 0; j < subgraph.getNodes().size() + subgraph.getRelationships().size(); j++) {
                ids.add((int) presentID);
                presentID++;
            }
            instance.setIds(ids);
            subgraphTreeNode.addInstance(instance);

            AbstractNode node1 = subgraph.getNodes().get(0), node2 = subgraph.getNodes().get(1), node3 = subgraph.getNodes().get(2);
            AbstractRelationship relation1 = subgraph.getRelationships().get(0), relation2 = subgraph.getRelationships().get(1);

            List<Boolean> newElements = new ArrayList<>();
            List<Map<String, Object>> properties = new ArrayList<>();
            newElements.add(true);
            properties.add(generatePropertiesInstance(subgraph.getNodes().get(0), ids.get(0)));
            newElements.add(true);
            properties.add(generatePropertiesInstance(subgraph.getRelationships().get(0), ids.get(1)));
            newElements.add(true);
            properties.add(generatePropertiesInstance(subgraph.getNodes().get(1), ids.get(2)));
            newElements.add(true);
            properties.add(generatePropertiesInstance(subgraph.getRelationships().get(1), ids.get(3)));
            newElements.add(true);
            properties.add(generatePropertiesInstance(subgraph.getNodes().get(2), ids.get(4)));

            instance.setProperties(properties);

            queries.add(generateMerge(subgraph, ids, properties, newElements));
        }

        return queries;
    }


    private Subgraph generateSubgraph() {

        long randNum = randomly.getInteger(0, 100);
        if (randNum < 75) {
            return generateThreeNodesGraph();
        } else {
            if (randNum < 87) {
                return generateTwoNodesGraph();
            }
        }
        return generateOneNodeGraph();
    }

    private void printProperties(StringBuilder sb, Map<String, Object> properties) {
        if (properties.size() != 0) {
            sb.append("{");
            boolean first = true;
            for (Map.Entry<String, Object> pair : properties.entrySet()) {
                if (!first) {
                    sb.append(", ");
                }
                first = false;
                sb.append(pair.getKey());
                sb.append(" : ");
                if (pair.getValue() instanceof String) {
                    sb.append("\"").append(pair.getValue()).append("\"");
                } else if (pair.getValue() instanceof Number) {
                    sb.append(pair.getValue());
                } else if (pair.getValue() instanceof Boolean) {
                    sb.append(pair.getValue());
                }
            }
            sb.append("}");
        }
    }

    private CypherQueryAdapter generateMerge(Subgraph subgraph, List<Integer> ids, List<Map<String, Object>> properties, List<Boolean> newElement) {
        StringBuilder sb = new StringBuilder();
        if (newElement.stream().anyMatch(e -> !e)) {
            sb.append("MATCH ");
            boolean first = true;
            for (int i = 0; i < 5; i++) {
                if (!newElement.get(i)) {
                    if (!first) {
                        sb.append(", ");
                    }
                    if (i % 2 == 0) {
                        AbstractNode node = subgraph.getNodes().get(i / 2);
                        sb.append("(n").append(i).append(" { id: ").append(ids.get(i)).append("})");
                    } else {
                        AbstractRelationship relationship = subgraph.getRelationships().get(i / 2);
                        if (relationship.getFrom() == subgraph.getNodes().get(i / 2)) {
                            sb.append("()-[r").append(i).append(" { id: ").append(ids.get(i)).append("}]->()");
                        } else {
                            sb.append("()<-[r").append(i).append(" { id: ").append(ids.get(i)).append("}]-()");
                        }
                    }
                    first = false;
                }
            }
            sb.append(" ");
        }
        sb.append("MERGE ");
        for (int i = 0; i < 5; i++) {
            if (!newElement.get(i)) {
                if (i % 2 == 0) {
                    if (i == 0) {
                        if (!newElement.get(i + 1)) {
                            continue;
                        }
                    } else if (i == 4) {
                        if (!newElement.get(i - 1)) {
                            continue;
                        }
                    } else if (!newElement.get(i - 1) && !newElement.get(i + 1)) {
                        continue;
                    }
                    AbstractNode node = subgraph.getNodes().get(i / 2);
                    sb.append("(n").append(i).append(")");
                } else {
                    if (!newElement.get(i)) {
                        continue;
                    }
                    AbstractRelationship relationship = subgraph.getRelationships().get(i / 2);
                    if (relationship.getFrom() == subgraph.getNodes().get(i / 2)) {
                        sb.append("-[r").append(i).append("]->");
                    } else {
                        sb.append("<-[r").append(i).append("]-");
                    }
                }
            } else {
                if (i % 2 == 0) {
                    AbstractNode node = subgraph.getNodes().get(i / 2);
                    sb.append("(n").append(i);
                    node.getLabelInfos().forEach(
                            l -> {
                                sb.append(" :").append(l.getName());
                            }
                    );
                    printProperties(sb, properties.get(i));
                    sb.append(")");
                } else {
                    AbstractRelationship relationship = subgraph.getRelationships().get(i / 2);
                    if (relationship.getFrom() == subgraph.getNodes().get(i / 2)) {
                        sb.append("-[r").append(i);
                    } else {
                        sb.append("<-[r").append(i);
                    }
                    if (relationship.getType() != null) {
                        sb.append(" :").append(relationship.getType().getName());
                    }
                    printProperties(sb, properties.get(i));
                    if (relationship.getFrom() == subgraph.getNodes().get(i / 2)) {
                        sb.append("]->");
                    } else {
                        sb.append("]-");
                    }
                }
            }
        }

        return new CypherQueryAdapter(sb.toString());
    }

    private SubgraphTreeNode generateSubgraphMutation(SubgraphTreeNode parentNode, List<CypherQueryAdapter> results) {

        long limitLog = singleRootPrimaryNum - (parentNode.getDepth() + 1) * splitFactor;
        long limit;
        if (limitLog < 0) {
            limit = 1;
        } else {
            limit = 1 << (singleRootPrimaryNum - (parentNode.getDepth() + 1) * splitFactor);
        }

        if (parentNode == null) {
            throw new RuntimeException();
        }
        Subgraph parent = parentNode.getSubgraph();
        if (parent.getNodes().size() != 3) {
            throw new RuntimeException();
        }
        List<AbstractNode> nodes = parent.getNodes();
        long randNum = randomly.getInteger(0, 100);
        Subgraph result = new Subgraph();

        if (randNum < 30) {
            result.addNode(nodes.get(0));
            result.addNode(nodes.get(1));
            AbstractNode newNode = randomColorNode();
            result.addNode(newNode);
            result.addRelationship(parent.getRelationships().get(0));
            result.addRelationship(connectNodesRandom(nodes.get(1), newNode));

            SubgraphTreeNode resultTreeNode = new SubgraphTreeNode(result);
            resultTreeNode.addParent(parentNode);

            List<SubgraphTreeNodeInstance> instances = new ArrayList<>(parentNode.getInstances());
            Collections.shuffle(instances, new Random(Randomly.THREAD_SEED.get()));
            instances = instances.subList(0, (int) limit);
            for (SubgraphTreeNodeInstance instance : instances) {
                SubgraphTreeNodeInstance childInstance = new SubgraphTreeNodeInstance();
                List<Integer> ids = new ArrayList<>();
                List<Boolean> newElements = new ArrayList<>();
                List<Map<String, Object>> properties = new ArrayList<>();
                ids.add(instance.getIds().get(0));
                newElements.add(false);
                properties.add(instance.getProperties().get(0));
                ids.add(instance.getIds().get(1));
                newElements.add(false);
                properties.add(instance.getProperties().get(1));
                ids.add(instance.getIds().get(2));
                newElements.add(false);
                properties.add(instance.getProperties().get(2));
                ids.add((int) presentID);
                newElements.add(true);
                properties.add(generatePropertiesInstance(result.getRelationships().get(1), ids.get(3)));
                presentID++;
                ids.add((int) presentID);
                newElements.add(true);
                properties.add(generatePropertiesInstance(result.getNodes().get(2), ids.get(4)));
                presentID++;

                childInstance.setIds(ids);
                childInstance.addParent(instance);
                childInstance.setProperties(properties);
                resultTreeNode.addInstance(childInstance);

                results.add(generateMerge(result, ids, properties, newElements));
            }

            return resultTreeNode;
        } else if (randNum < 60) {
            result.addNode(nodes.get(1));
            result.addNode(nodes.get(2));
            AbstractNode newNode = randomColorNode();
            result.addNode(newNode);
            result.addRelationship(parent.getRelationships().get(1));
            result.addRelationship(connectNodesRandom(nodes.get(2), newNode));

            SubgraphTreeNode resultTreeNode = new SubgraphTreeNode(result);
            resultTreeNode.addParent(parentNode);

            List<SubgraphTreeNodeInstance> instances = new ArrayList<>(parentNode.getInstances());
            Collections.shuffle(instances, new Random(Randomly.THREAD_SEED.get()));
            instances = instances.subList(0, (int) limit);
            for (SubgraphTreeNodeInstance instance : instances) {
                SubgraphTreeNodeInstance childInstance = new SubgraphTreeNodeInstance();

                List<Integer> ids = new ArrayList<>();
                List<Boolean> newElements = new ArrayList<>();
                List<Map<String, Object>> properties = new ArrayList<>();
                ids.add(instance.getIds().get(2));
                newElements.add(false);
                properties.add(instance.getProperties().get(2));
                ids.add(instance.getIds().get(3));
                newElements.add(false);
                properties.add(instance.getProperties().get(3));
                ids.add(instance.getIds().get(4));
                newElements.add(false);
                properties.add(instance.getProperties().get(4));
                ids.add((int) presentID);
                newElements.add(true);
                properties.add(generatePropertiesInstance(result.getRelationships().get(1), ids.get(3)));
                presentID++;
                ids.add((int) presentID);
                newElements.add(true);
                properties.add(generatePropertiesInstance(result.getNodes().get(2), ids.get(4)));
                presentID++;

                results.add(generateMerge(result, ids, properties, newElements));

                childInstance.setIds(ids);
                childInstance.addParent(instance);
                childInstance.setProperties(properties);
                resultTreeNode.addInstance(childInstance);
            }


            return resultTreeNode;
        } else {
            if (randNum < 80 || parentNode.getAncestors().size() == 0) {
                result.addNode(nodes.get(0));
                AbstractNode newNode = randomColorNode();
                result.addNode(newNode);
                result.addNode(nodes.get(2));
                result.addRelationship(connectNodesRandom(nodes.get(0), newNode));
                result.addRelationship(connectNodesRandom(nodes.get(2), newNode));

                SubgraphTreeNode resultTreeNode = new SubgraphTreeNode(result);
                resultTreeNode.addParent(parentNode);

                List<SubgraphTreeNodeInstance> instances = new ArrayList<>(parentNode.getInstances());
                Collections.shuffle(instances, new Random(Randomly.THREAD_SEED.get()));
                instances = instances.subList(0, (int) limit);
                for (SubgraphTreeNodeInstance instance : instances) {
                    SubgraphTreeNodeInstance childInstance = new SubgraphTreeNodeInstance();

                    List<Integer> ids = new ArrayList<>();
                    List<Boolean> newElements = new ArrayList<>();
                    List<Map<String, Object>> properties = new ArrayList<>();
                    ids.add(instance.getIds().get(0));
                    newElements.add(false);
                    properties.add(instance.getProperties().get(0));
                    ids.add((int) presentID);
                    newElements.add(true);
                    properties.add(generatePropertiesInstance(result.getRelationships().get(0), ids.get(1)));
                    presentID++;
                    ids.add((int) presentID);
                    newElements.add(true);
                    properties.add(generatePropertiesInstance(result.getNodes().get(1), ids.get(2)));
                    presentID++;
                    ids.add((int) presentID);
                    newElements.add(true);
                    properties.add(generatePropertiesInstance(result.getRelationships().get(1), ids.get(3)));
                    presentID++;
                    ids.add(instance.getIds().get(4));
                    newElements.add(false);
                    properties.add(instance.getProperties().get(4));

                    results.add(generateMerge(result, ids, properties, newElements));

                    childInstance.setIds(ids);
                    childInstance.addParent(instance);
                    childInstance.setProperties(properties);
                    resultTreeNode.addInstance(childInstance);
                }

                return resultTreeNode;
            } else {
                List<SubgraphTreeNode> ancestors = new ArrayList<>(parentNode.getAncestors());
                Collections.shuffle(ancestors, new Random(Randomly.THREAD_SEED.get()));
                SubgraphTreeNode ancestorTreeNode = ancestors.get(0);
                Subgraph ancestor = ancestorTreeNode.getSubgraph();
                long parentSelectedPos = randomly.getInteger(0, nodes.size());
                long ancestorSelectedPos = randomly.getInteger(0, ancestor.getNodes().size());
                AbstractNode parentSelectedNode = nodes.get((int) parentSelectedPos);
                AbstractNode ancestorSelectedNode = ancestor.getNodes().get((int) ancestorSelectedPos);
                result.addNode(parentSelectedNode);
                AbstractNode newNode = randomColorNode();
                result.addNode(newNode);
                result.addNode(ancestorSelectedNode);
                result.addRelationship(connectNodesRandom(parentSelectedNode, newNode));
                result.addRelationship(connectNodesRandom(ancestorSelectedNode, newNode));

                SubgraphTreeNode resultTreeNode = new SubgraphTreeNode(result);
                resultTreeNode.addParent(parentNode);
                resultTreeNode.addParent(ancestors.get(0));

                List<SubgraphTreeNodeInstance> instances = new ArrayList<>(parentNode.getInstances());
                Collections.shuffle(instances, new Random(Randomly.THREAD_SEED.get()));
                instances = instances.subList(0, (int) limit);
                for (SubgraphTreeNodeInstance instance : instances) {
                    List<SubgraphTreeNodeInstance> candidates = ancestorTreeNode.getInstances().stream().filter(i -> instance.getAncestors().contains(i))
                            .collect(Collectors.toList());
                    if (candidates.size() != 1) {
                        throw new RuntimeException();
                    }
                    SubgraphTreeNodeInstance ancestorInstance = candidates.get(0);
                    SubgraphTreeNodeInstance childInstance = new SubgraphTreeNodeInstance();

                    List<Integer> ids = new ArrayList<>();
                    List<Boolean> newElements = new ArrayList<>();
                    List<Map<String, Object>> properties = new ArrayList<>();
                    ids.add(instance.getIds().get((int) parentSelectedPos * 2));
                    newElements.add(false);
                    properties.add(instance.getProperties().get((int) parentSelectedPos * 2));
                    ids.add((int) presentID);
                    newElements.add(true);
                    properties.add(generatePropertiesInstance(result.getRelationships().get(0), ids.get(1)));
                    presentID++;
                    ids.add((int) presentID);
                    newElements.add(true);
                    properties.add(generatePropertiesInstance(result.getNodes().get(1), ids.get(2)));
                    presentID++;
                    ids.add((int) presentID);
                    newElements.add(true);
                    properties.add(generatePropertiesInstance(result.getRelationships().get(1), ids.get(3)));
                    presentID++;
                    ids.add(ancestorInstance.getIds().get((int) ancestorSelectedPos * 2));
                    newElements.add(false);
                    properties.add(ancestorInstance.getProperties().get((int) ancestorSelectedPos * 2));

                    results.add(generateMerge(result, ids, properties, newElements));

                    childInstance.setIds(ids);
                    childInstance.addParent(instance);
                    childInstance.addParent(ancestorInstance);
                    childInstance.setProperties(properties);
                    resultTreeNode.addInstance(childInstance);
                }


                return resultTreeNode;
            }

        }
    }

    private Subgraph generateThreeNodesGraph() {
        Subgraph subgraph = new Subgraph();
        AbstractNode nodeA = new AbstractNode(), nodeB = new AbstractNode(), nodeC = new AbstractNode();
        List<AbstractNode> nodes = new ArrayList<>(Arrays.asList(nodeA, nodeB, nodeC));


        for (int i = 0; i < 3; i++) {
            List<ILabelInfo> labels = new ArrayList<>(schema.getLabelInfos());
            Collections.shuffle(labels, new Random(Randomly.THREAD_SEED.get()));
            long labelNum = getRandomLabelNum(labels.size());
            List<ILabelInfo> selectedLabels = new ArrayList<>();
            for (int j = 0; j < labelNum; j++) {
                selectedLabels.add(labels.get(j));
            }
            nodes.get(i).setLabelInfos(selectedLabels);
            subgraph.addNode(nodes.get(i));
        }
        subgraph.addRelationship(connectNodesRandom(nodeA, nodeB));
        subgraph.addRelationship(connectNodesRandom(nodeB, nodeC));

        return subgraph;
    }

    private AbstractRelationship randomColorRelationship() {
        AbstractRelationship relationship = new AbstractRelationship();
        List<IRelationTypeInfo> relationTypeInfos = new ArrayList<>(schema.getRelationshipTypeInfos());
        Collections.shuffle(relationTypeInfos, new Random(Randomly.THREAD_SEED.get()));
        IRelationTypeInfo relationTypeInfo = null;
        if (relationTypeInfos.size() != 0) {
            relationTypeInfo = relationTypeInfos.get(0);
        }
        relationship.setType(relationTypeInfo);
        return relationship;
    }

    private AbstractNode randomColorNode() {
        List<ILabelInfo> labels = new ArrayList<>(schema.getLabelInfos());
        AbstractNode node = new AbstractNode();
        Collections.shuffle(labels, new Random(Randomly.THREAD_SEED.get()));
        long labelNum = getRandomLabelNum(labels.size());
        List<ILabelInfo> selectedLabels = new ArrayList<>();
        for (int j = 0; j < labelNum; j++) {
            selectedLabels.add(labels.get(j));
        }
        node.setLabelInfos(selectedLabels);

        return node;
    }

    private AbstractRelationship connectNodesRandom(AbstractNode a, AbstractNode b) {
        AbstractRelationship relationship = randomColorRelationship();
        if (randomly.getInteger(0, 100) < 50) {
            relationship.setFrom(a);
            relationship.setTo(b);
        } else {
            relationship.setFrom(b);
            relationship.setTo(a);
        }
        return relationship;
    }

    private Subgraph generateTwoNodesGraph() {
        Subgraph subgraph = new Subgraph();
        AbstractNode nodeA = new AbstractNode(), nodeB = new AbstractNode();
        List<AbstractNode> nodes = new ArrayList<>(Arrays.asList(nodeA, nodeB));


        for (int i = 0; i < 2; i++) {
            List<ILabelInfo> labels = new ArrayList<>(schema.getLabelInfos());
            Collections.shuffle(labels, new Random(Randomly.THREAD_SEED.get()));
            long labelNum = getRandomLabelNum(labels.size());
            List<ILabelInfo> selectedLabels = new ArrayList<>();
            for (int j = 0; j < labelNum; j++) {
                selectedLabels.add(labels.get(j));
            }
            nodes.get(i).setLabelInfos(selectedLabels);
            subgraph.addNode(nodes.get(i));
        }
        subgraph.addRelationship(connectNodesRandom(nodeA, nodeB));

        long randNum = randomly.getInteger(0, 1 << 3);
        if (randNum >= 4 && randNum <= 5) {
            subgraph.addRelationship(connectNodesRandom(nodeA, nodeB));
        }

        return subgraph;
    }

    private Subgraph generateOneNodeGraph() {
        Subgraph subgraph = new Subgraph();
        List<ILabelInfo> labels = new ArrayList<>(schema.getLabelInfos());
        AbstractNode node = new AbstractNode();
        Collections.shuffle(labels, new Random(Randomly.THREAD_SEED.get()));
        long labelNum = getRandomLabelNum(labels.size());
        List<ILabelInfo> selectedLabels = new ArrayList<>();
        for (int j = 0; j < labelNum; j++) {
            selectedLabels.add(labels.get(j));
        }
        node.setLabelInfos(selectedLabels);
        subgraph.addNode(node);

        return subgraph;
    }

    private long getRandomLabelNum(long labelsSize) {
        long maxNum = 1 << maxNodeColor;
        long randNum = randomly.getInteger(0, maxNum);
        long labelNum = maxNodeColor + 1;
        if (randNum == 0) {
            labelNum = 0;
        } else {
            while (randNum > 0) {
                randNum = randNum >> 1;
                labelNum--;
            }
        }
        return Math.min(options.getLabelNum(), Math.min(labelsSize, labelNum));
    }

    public List<SubgraphTreeNode> getTreeNodes() {
        List<SubgraphTreeNode> result = new ArrayList<>(root.getDescendants());
        result.add(root);
        return result;
    }
}
