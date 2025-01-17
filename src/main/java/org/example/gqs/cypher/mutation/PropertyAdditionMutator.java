package org.example.gqs.cypher.mutation;

import org.example.gqs.Randomly;
import org.example.gqs.cypher.ast.IClauseSequence;
import org.example.gqs.cypher.ast.IMatch;
import org.example.gqs.cypher.ast.analyzer.IIdentifierAnalyzer;
import org.example.gqs.cypher.ast.analyzer.INodeAnalyzer;
import org.example.gqs.cypher.ast.analyzer.IRelationAnalyzer;
import org.example.gqs.cypher.dsl.ClauseVisitor;
import org.example.gqs.cypher.dsl.IContext;
import org.example.gqs.cypher.mutation.PropertyAdditionMutator.PropertyAdditionMutatorContext;
import org.example.gqs.cypher.schema.CypherSchema;
import org.example.gqs.cypher.schema.IPropertyInfo;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Deprecated
public class PropertyAdditionMutator<S extends CypherSchema<?,?>> extends ClauseVisitor<PropertyAdditionMutatorContext> implements IClauseMutator {

    private List<IIdentifierAnalyzer> patternElements = new ArrayList<>();
    private S schema;

    public PropertyAdditionMutator(IClauseSequence clauseSequence, S schema) {
        super(clauseSequence, new PropertyAdditionMutatorContext());
        this.schema = schema;
    }

    @Override
    public void mutate() {
        startVisit();
    }

    public static class PropertyAdditionMutatorContext implements IContext {

    }

    @Override
    public void visitMatch(IMatch matchClause, PropertyAdditionMutatorContext context) {
        matchClause.toAnalyzer().getLocalNodeIdentifiers().stream().filter(node-> {
            return node.getAllPropertiesAvailable(schema).stream().anyMatch(
                    property -> {
                        return node.getAllPropertiesInDefChain().stream().noneMatch(
                                declaredProperty -> declaredProperty.getKey().equals(property.getKey())
                        );
                    }
            );
        }).forEach(node-> patternElements.add(node));

        matchClause.toAnalyzer().getLocalRelationIdentifiers().stream().filter(relation-> {
            return relation.getAllPropertiesAvailable(schema).stream().anyMatch(
                    property -> {
                        return relation.getAllPropertiesInDefChain().stream().noneMatch(
                                declaredProperty -> declaredProperty.getKey().equals(property.getKey())
                        );
                    }
            );
        }).forEach(relation-> patternElements.add(relation));
    }


    @Override
    public void postProcessing(PropertyAdditionMutatorContext context) {
        Randomly randomly = new Randomly();
        if(patternElements.size() == 0){
            return;
        }

        IIdentifierAnalyzer identifier = patternElements.get(randomly.getInteger(0, patternElements.size()));

        if(identifier instanceof INodeAnalyzer) {
            INodeAnalyzer nodeAnalyzer = (INodeAnalyzer) identifier;
            List<IPropertyInfo> possiblePropertyInfo = nodeAnalyzer.getAllPropertiesAvailable(schema).stream().filter(
                    property -> {
                        return nodeAnalyzer.getAllPropertiesInDefChain().stream().noneMatch(
                                declaredProperty -> declaredProperty.getKey().equals(property.getKey())
                        );
                    }
            ).collect(Collectors.toList());
            if (possiblePropertyInfo.size() == 0) {
                throw new RuntimeException();
            }
            IPropertyInfo selectedPropertyInfo = possiblePropertyInfo.get(randomly.getInteger(0, possiblePropertyInfo.size()));
            return;
        }
        else if(identifier instanceof IRelationAnalyzer) {
            IRelationAnalyzer relationAnalyzer = (IRelationAnalyzer) identifier;
            List<IPropertyInfo> possiblePropertyInfo = relationAnalyzer.getAllPropertiesAvailable(schema).stream().filter(
                    property -> {
                        return relationAnalyzer.getAllPropertiesInDefChain().stream().noneMatch(
                                declaredProperty -> declaredProperty.getKey().equals(property.getKey())
                        );
                    }
            ).collect(Collectors.toList());
            if (possiblePropertyInfo.size() == 0) {
                throw new RuntimeException();
            }
            IPropertyInfo selectedPropertyInfo = possiblePropertyInfo.get(randomly.getInteger(0, possiblePropertyInfo.size()));
            return;
        }
        throw new RuntimeException();
    }
}
