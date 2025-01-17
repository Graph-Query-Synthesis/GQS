package org.example.gqs.cypher.standard_ast;

import org.example.gqs.cypher.ast.*;
import org.example.gqs.cypher.ast.analyzer.ICreateAnalyzer;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class Create extends CypherClause implements ICreateAnalyzer {

    public Create(){
        super(true);
    }

    @Override
    public IPattern getPattern() {
        return symtab.getPatterns().get(0);
    }

    @Override
    public void setPattern(IPattern pattern) {
        symtab.setPatterns(Arrays.asList(pattern));
    }

    @Override
    public ICreateAnalyzer toAnalyzer() {
        return this;
    }

    public void updateProvideAndRequire(){
        throw new RuntimeException("Not implemented yet for Create");
    }

    @Override
    public ICypherClause getCopy() {
        Create create = new Create();
        if(symtab != null){
            create.symtab.setPatterns(symtab.getPatterns().stream().map(p->p.getCopy()).collect(Collectors.toList()));
            create.symtab.setAliasDefinition(symtab.getAliasDefinitions().stream().map(a->a.getCopy()).collect(Collectors.toList()));
        }
        return create;
    }

    @Override
    public void toTextRepresentation(StringBuilder sb) {
        sb.append("CREATE ");
        List<INodeIdentifier> nodePatterns = new ArrayList<>();
        List<IRelationIdentifier> relationPatterns = new ArrayList<>();
        getPattern().toTextRepresentation(sb);
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
    public ICreate getSource() {
        return this;
    }
}
