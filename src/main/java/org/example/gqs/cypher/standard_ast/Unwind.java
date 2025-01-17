package org.example.gqs.cypher.standard_ast;

import org.example.gqs.cypher.ast.*;
import org.example.gqs.cypher.ast.analyzer.IUnwindAnalyzer;
import org.example.gqs.cypher.standard_ast.expr.ConstExpression;
import org.example.gqs.cypher.standard_ast.expr.CreateListExpression;

import java.util.*;

public class Unwind extends CypherClause implements IUnwindAnalyzer {
    public Unwind() {
        super(true);
    }
    public long repeatTimes = 1;
    public boolean needExpand = true;

    @Override
    public IUnwindAnalyzer toAnalyzer() {
        return this;
    }

    public void updateProvideAndRequire() {
        provide = new HashSet<>();
        require = new HashSet<>();
        for (IRet ret : symtab.getAliasDefinitions()) {
            if (ret.getIdentifier() instanceof Alias) {
                provide.add((IIdentifier) ((Alias) ret.getIdentifier()));
            } else {
                throw new RuntimeException("In UNWIND updating provide: identifier is not an alias");
            }
        }
        for (IRet ret : symtab.getAliasDefinitions()) {
            if (ret.getExpression() instanceof CreateListExpression) {
                require.addAll((((CreateListExpression) ret.getExpression()).reliedContent()));
            } else {
                throw new RuntimeException("In UNWIND updating require: missing createListExpression");
            }
        }
    }

    public List<Map<String, Object>> getNamespace(List<Map<String, Object>> expandedNamespace)
    {
        Set<Alias> toExpand = new HashSet<>();
        for(IRet ret : symtab.getAliasDefinitions())
        {
            if(ret.getIdentifier() instanceof Alias)
            {
                toExpand.add((Alias) ret.getIdentifier());
            }
            else
            {
                throw new RuntimeException("In UNWIND getNamespace: identifier is not an alias");
            }
        }
        for(Alias alias: toExpand) {
            List<Object> list = ((CreateListExpression) alias.getExpression()).getValue();
            List<Map<String, Object>> newNamespace = new ArrayList<>();
            for (int i = 0; i < expandedNamespace.size(); i++) {
                newNamespace.add(new HashMap<>(expandedNamespace.get(i)));
            }
            for (int j = 0; j < list.size() - 1; j++) {
                for (int k = 0; k < newNamespace.size(); k++) {
                    expandedNamespace.add(new HashMap<>(newNamespace.get(k)));
                }
            }

            int cnt = 0;
            for (int i = 0; i < expandedNamespace.size(); i++) {
                cnt = i / (expandedNamespace.size() / list.size());
                expandedNamespace.get(i).put(alias.getName(), new Alias(alias.getName(), new ConstExpression(list.get(cnt))));
            }
        }


        return expandedNamespace;
    }
    public Map<String, Object> getNamespace(Map<String, Object> namespace)
    {
        for(IRet ret : symtab.getAliasDefinitions())
        {
            if(ret.getIdentifier() instanceof Alias)
            {
                namespace.put(((Alias) ret.getIdentifier()).getName(), ((Alias) ret.getIdentifier()));
            }
            else
            {
                throw new RuntimeException("In UNWIND getNamespace: identifier is not an alias");
            }
            repeatTimes = ((List<Object>)(((Alias) ret.getIdentifier()).getValue())).size();
        }

        return namespace;
    }
    @Override
    public ICypherClause getCopy() {
        Unwind unwind = new Unwind();
        unwind.setListAsAliasRet(getListAsAliasRet().getCopy());
        if(require != null){
            unwind.require = new HashSet<>(require);
        }
        if(provide != null){
            unwind.provide = new HashSet<>(provide);
        }
        return unwind;
    }

    @Override
    public void toTextRepresentation(StringBuilder sb) {
        sb.append("UNWIND ");
        IRet listAsAlias = getListAsAliasRet();
        if(listAsAlias == null){
            sb.append("null");
        }
        else {
            listAsAlias.toTextRepresentation(sb);
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
    public IUnwind getSource() {
        return this;
    }

    @Override
    public IRet getListAsAliasRet() {
        if(symtab.getAliasDefinitions() == null || symtab.getAliasDefinitions().size() != 1){
            return null;
        }
        return symtab.getAliasDefinitions().get(0);
    }

    @Override
    public void setListAsAliasRet(IRet listAsAlias) {
        if(listAsAlias != null){
            symtab.setAliasDefinition(Arrays.asList(listAsAlias));
        }
        else {
            symtab.setAliasDefinition(new ArrayList<>());
        }

    }
}
