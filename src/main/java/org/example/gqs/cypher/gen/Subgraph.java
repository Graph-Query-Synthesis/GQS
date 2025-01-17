package org.example.gqs.cypher.gen;

import org.example.gqs.cypher.dsl.IIdentifierBuilder;
import org.example.gqs.cypher.schema.ILabelInfo;
import org.example.gqs.cypher.standard_ast.*;
import org.example.gqs.cypher.ast.Direction;
import org.example.gqs.cypher.ast.INodeIdentifier;
import org.example.gqs.cypher.ast.IPattern;
import org.example.gqs.cypher.ast.IRelationIdentifier;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class Subgraph {

    private List<AbstractNode> nodes = new ArrayList<>();
    private List<AbstractRelationship> relationships = new ArrayList<>();

    public List<AbstractNode> getNodes() {
        return nodes;
    }

    public void setNodes(List<AbstractNode> nodes) {
        this.nodes = nodes;
    }

    public List<AbstractRelationship> getRelationships() {
        return relationships;
    }

    public void setRelationships(List<AbstractRelationship> relationships) {
        this.relationships = relationships;
    }

    public void addNode(AbstractNode node){
        nodes.add(node);
    }

    public void addRelationship(AbstractRelationship relationship){
        relationships.add(relationship);
    }

    public IPattern translateMatch(IIdentifierBuilder identifierBuilder, Map<AbstractNode, INodeIdentifier> nodeToString,
                                   Map<AbstractRelationship, IRelationIdentifier> relationToString){
        Pattern.PatternBuilder patternBuilder = new Pattern.PatternBuilder(identifierBuilder);
        Pattern.PatternBuilder.OngoingRelation lastRelationship = null;

        List<AbstractNode> abstractNodes = new ArrayList<>();
        List<AbstractRelationship> abstractRelations = new ArrayList<>();


        for(int i = 0; i < nodes.size(); i++) {
            Pattern.PatternBuilder.OngoingNode ongoingNode = null;
            if (nodeToString.containsKey(nodes.get(i))) {
                if (lastRelationship != null) {
                    ongoingNode = lastRelationship.newNodeRef(nodeToString.get(nodes.get(i)));
                } else {
                    ongoingNode = patternBuilder.newRefDefinedNode(nodeToString.get(nodes.get(i)));
                }
            } else {
                if (lastRelationship != null) {
                    ongoingNode = lastRelationship.newNamedNode();
                } else {
                    ongoingNode = patternBuilder.newNamedNode();
                }
                for (ILabelInfo labelInfo : nodes.get(i).getLabelInfos()) {
                    ongoingNode.withLabels(new Label(labelInfo.getName()));
                }
            }
            abstractNodes.add(nodes.get(i));

            if (i == nodes.size() - 1) {
                IPattern pattern = ongoingNode.build();
                resolveAndAddMap(pattern, abstractNodes, abstractRelations, nodeToString, relationToString);
                return pattern;
            }

            Direction direction;
            if (relationships.get(i).getFrom() == nodes.get(i)) {
                direction = Direction.RIGHT;
            } else {
                direction = Direction.LEFT;
            }
            Pattern.PatternBuilder.OngoingRelation relation = null;
            {
                relation = ongoingNode.newNamedRelation().withDirection(direction);
                if (relationships.get(i).getType() != null) {
                    relation.withType(new RelationType(relationships.get(i).getType().getName()));
                }
            }
            abstractRelations.add(relationships.get(i));


            lastRelationship = relation;
        }
        throw new RuntimeException();
    }

    public IPattern translateMerge(IIdentifierBuilder identifierBuilder, Map<AbstractNode, INodeIdentifier> nodeToString,
                                   Map<AbstractRelationship, IRelationIdentifier> relationToString) {
        Pattern.PatternBuilder patternBuilder = new Pattern.PatternBuilder(identifierBuilder);
        Pattern.PatternBuilder.OngoingRelation lastRelationship = null;

        List<AbstractNode> abstractNodes = new ArrayList<>();
        List<AbstractRelationship> abstractRelations = new ArrayList<>();
        for (int i = 0; i < nodes.size(); i++) {
            Pattern.PatternBuilder.OngoingNode ongoingNode = null;

            if (i != nodes.size() - 1) {
                if (relationToString.containsKey(relationships.get(i))) {
                    continue;
                }
            }

            if (nodeToString.containsKey(nodes.get(i))) {
                if (lastRelationship != null) {
                    ongoingNode = lastRelationship.newNodeRef(nodeToString.get(nodes.get(i)));
                } else {
                    ongoingNode = patternBuilder.newRefDefinedNode(nodeToString.get(nodes.get(i)));
                }
            } else {
                if (lastRelationship != null) {
                    ongoingNode = lastRelationship.newNamedNode();
                } else {
                    ongoingNode = patternBuilder.newNamedNode();
                }
                for (ILabelInfo labelInfo : nodes.get(i).getLabelInfos()) {
                    ongoingNode.withLabels(new Label(labelInfo.getName()));
                }
            }

            abstractNodes.add(nodes.get(i));

            if (i == nodes.size() - 1) {
                IPattern pattern = ongoingNode.build();
                resolveAndAddMap(pattern, abstractNodes, abstractRelations, nodeToString, relationToString);
                return pattern;
            }

            Direction direction;
            if (relationships.get(i).getFrom() == nodes.get(i)) {
                direction = Direction.RIGHT;
            } else {
                direction = Direction.LEFT;
            }
            Pattern.PatternBuilder.OngoingRelation relation = null;

            if (relationToString.containsKey(relationships.get(i))) {
                relation = ongoingNode.newAnonymousRelation();
            } else {
                relation = ongoingNode.newNamedRelation().withDirection(direction);
                if (relationships.get(i).getType() != null) {
                    relation.withType(new RelationType(relationships.get(i).getType().getName()));
                }
            }

            abstractRelations.add(relationships.get(i));


            lastRelationship = relation;
        }
        throw new RuntimeException();
    }

    public static void resolveAndAddMap(IPattern pattern, List<AbstractNode> nodes, List<AbstractRelationship> relationships, Map<AbstractNode, INodeIdentifier> nodeToString,
                                        Map<AbstractRelationship, IRelationIdentifier> relationToString) {
        for (int i = 0; i < nodes.size(); i++) {
            if (nodes.get(i) == null)
                System.out.println("null discovered!!!!!");
            ((NodeIdentifier) pattern.getPatternElements().get(i * 2)).actualNode = nodes.get(i);
            if (!nodeToString.containsKey(nodes.get(i))) {
                nodeToString.put(nodes.get(i), (INodeIdentifier) pattern.getPatternElements().get(i * 2));
            }

        }
        for (int i = 0; i < relationships.size(); i++) {
            ((RelationIdentifier) pattern.getPatternElements().get(i * 2 + 1)).actualRelationship = relationships.get(i);
            if (!relationToString.containsKey(relationships.get(i))) {
                relationToString.put(relationships.get(i), (IRelationIdentifier) pattern.getPatternElements().get(i * 2 + 1));
            }

        }
    }

    public IPattern translateCreate(IIdentifierBuilder identifierBuilder){
        Pattern.PatternBuilder patternBuilder = new Pattern.PatternBuilder(identifierBuilder);
        Pattern.PatternBuilder.OngoingRelation lastRelationship = null;
        for(int i = 0; i < nodes.size(); i++){
            Pattern.PatternBuilder.OngoingNode ongoingNode = null;
            if(lastRelationship != null){
                ongoingNode = lastRelationship.newNamedNode();
            }
            else {
                ongoingNode = patternBuilder.newNamedNode();
            }
            for(ILabelInfo labelInfo : nodes.get(i).getLabelInfos()){
                ongoingNode.withLabels(new Label(labelInfo.getName()));
            }
            if(i == nodes.size() - 1){
                return ongoingNode.build();
            }

            Direction direction;
            if(relationships.get(i).getFrom() == nodes.get(i)){
                direction =  Direction.RIGHT;
            }
            else {
                direction = Direction.LEFT;
            }
            Pattern.PatternBuilder.OngoingRelation relation = ongoingNode.newNamedRelation().withDirection(direction);
            if(relationships.get(i).getType() != null){
                relation.withType(new RelationType(relationships.get(i).getType().getName()));
            }
            lastRelationship = relation;
        }
        throw new RuntimeException();
    }

    private List<List<Integer>> idLists = new ArrayList<>();

    public void putInstance(List<Integer> ids){
        idLists.add(ids);
    }

}
