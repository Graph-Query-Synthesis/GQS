package org.example.gqs.cypher.standard_ast;


import org.example.gqs.MainOptions;
import org.example.gqs.cypher.ast.*;
import org.example.gqs.cypher.ast.analyzer.IMatchAnalyzer;
import org.example.gqs.cypher.standard_ast.expr.CypherExpression;

import java.util.*;
import java.util.stream.Collectors;

public class Match extends CypherClause implements IMatchAnalyzer {
    private boolean isOptional = false;
    public IExpression condition = null;
    public String prefix;
    public String indexNode, indexLabel, indexProperty;

    public Match(){
        super(true);
    }

    @Override
    public List<IPattern> getPatternTuple() {
        return symtab.getPatterns();
    }

    @Override
    public void setPatternTuple(List<IPattern> patternTuple) {
        symtab.setPatterns(patternTuple);
    }

    public void updateProvideAndRequire() {
        provide = new HashSet<>();
        require = new HashSet<>();
        for (IPattern pattern : getPatternTuple()) {
            for (IIdentifier identifier : pattern.getPatternElements()) {
                provide.add(identifier);
            }
        }
        require.addAll(provide);
        if (condition != null) {
            require.addAll(((CypherExpression) condition).reliedContent());
        }
    }

    @Override
    public boolean isOptional() {
        return isOptional;
    }

    @Override
    public void setOptional(boolean optional) {
        this.isOptional = optional;
    }

    @Override
    public IExpression getCondition() {
        return condition;
    }

    @Override
    public void setCondition(IExpression condition) {
        this.condition = condition;
    }

    @Override
    public IMatchAnalyzer toAnalyzer() {
        return this;
    }

    @Override
    public ICypherClause getCopy() {
        Match match = new Match();
        match.isOptional = isOptional;
        if(symtab != null){
            match.symtab.setPatterns(symtab.getPatterns().stream().map(p->p.getCopy()).collect(Collectors.toList()));
            match.symtab.setAliasDefinition(symtab.getAliasDefinitions().stream().map(a->a.getCopy()).collect(Collectors.toList()));
        }
        if(condition != null){
            match.condition = condition.getCopy();
        }
        else {
            match.condition = null;
        }
        if(require != null){
            match.require = new HashSet<>();
            for(IIdentifier identifier: require){
                match.require.add(identifier.getCopy());
            }
        }
        if(provide != null){
            match.provide = new HashSet<>();
            for(IIdentifier identifier: provide){
                match.provide.add(identifier.getCopy());
            }
        }
        return match;
    }

    @Override
    public void toTextRepresentation(StringBuilder sb) {
        if(prefix!=null)
            sb.append(prefix);
        if(isOptional()){
            sb.append("OPTIONAL ");
        }
        sb.append("MATCH ");
        List<IPattern> patterns = getPatternTuple();
        List<INodeIdentifier> nodePatterns = new ArrayList<>();
        List<IRelationIdentifier> relationPatterns = new ArrayList<>();
        boolean hintFlag = false;

        for(int i = 0; i < patterns.size(); i++){
            IPattern pattern = patterns.get(i);
            pattern.toTextRepresentation(sb);
            if(i != patterns.size() - 1){
                sb.append(", ");
            }
        }
        if(indexNode != null) {
            if (MainOptions.mode == "neo4j") {


            }
        }
        if(condition != null){
            sb.append(" WHERE ");
            condition.toTextRepresentation(sb);
        }

    }

    @Override
    public List<IPattern> getLocalPatternContainsIdentifier(IIdentifier identifier) {
        List<IPattern> patterns = symtab.getPatterns();
        List<IPattern> result = new ArrayList<>();
        for(IPattern pattern: patterns){
            for(IPatternElement element: pattern.getPatternElements()){
                if(element.equals(identifier)){
                    result.add(pattern);
                    break;
                }
            }
        }
        return result;
    }

    @Override
    public IMatch getSource() {
        return this;
    }

    public List<Map<String, Object>> getNamespace(List<Map<String, Object>> namespace)
    {
        for(IPattern pattern: getPatternTuple())
        {
            for(IIdentifier identifier: pattern.getPatternElements())
            {
                if(identifier instanceof NodeIdentifier)
                {
                    for(int i = 0; i < namespace.size(); i++)
                        namespace.get(i).put(identifier.getName(), ((NodeIdentifier) identifier).actualNode);
                }
                else if(identifier instanceof RelationIdentifier)
                {
                    for(int i = 0; i < namespace.size(); i++)
                        namespace.get(i).put(identifier.getName(), ((RelationIdentifier) identifier).actualRelationship);
                }
            }
        }
        return namespace;

    }

    public Map<String, Object> getNamespace(Map<String, Object> namespace)
    {
        for(IPattern pattern: getPatternTuple())
        {
            for(IIdentifier identifier: pattern.getPatternElements())
            {
                if(identifier instanceof NodeIdentifier)
                {
                    namespace.put(identifier.getName(), ((NodeIdentifier) identifier).actualNode);
                }
                else if(identifier instanceof IRelationIdentifier)
                {
                    namespace.put(identifier.getName(), ((RelationIdentifier)identifier).actualRelationship);
                }
            }
        }
        return namespace;
    }
}
