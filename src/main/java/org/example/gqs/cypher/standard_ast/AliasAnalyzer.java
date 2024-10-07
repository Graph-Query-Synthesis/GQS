package org.example.gqs.cypher.standard_ast;

import org.example.gqs.cypher.ICypherSchema;
import org.example.gqs.cypher.ast.IAlias;
import org.example.gqs.cypher.ast.IExpression;
import org.example.gqs.cypher.ast.analyzer.IAliasAnalyzer;
import org.example.gqs.cypher.ast.analyzer.IContextInfo;
import org.example.gqs.cypher.ast.analyzer.ICypherTypeDescriptor;

public class AliasAnalyzer extends Alias implements IAliasAnalyzer {
    IAliasAnalyzer formerDef = null;
    IContextInfo contextInfo;
    IExpression sourceExpression;
    IAlias source;

    AliasAnalyzer(IAlias alias, IContextInfo contextInfo){
        this(alias, contextInfo, null);
    }

    AliasAnalyzer(IAlias alias, IContextInfo contextInfo, IExpression sourceExpression){
        super(alias.getName(), alias.getExpression(), ((Alias)alias).isDistinct);
        this.source = alias;
        this.contextInfo = contextInfo;
        this.sourceExpression = sourceExpression;
    }


    @Override
    public IAliasAnalyzer getFormerDef() {
        return formerDef;
    }

    @Override
    public void setFormerDef(IAliasAnalyzer formerDef) {
        this.formerDef = formerDef;
    }

    @Override
    public IExpression getAliasDefExpression() {
        if(formerDef == null){
            return this.expression;
        }
        return formerDef.getAliasDefExpression();
    }

    @Override
    public IAlias getSource() {
        return source;
    }

    @Override
    public ICypherTypeDescriptor analyzeType(ICypherSchema cypherSchema) {
        IExpression expression = getAliasDefExpression();
        if(expression != null){
            return expression.analyzeType(cypherSchema, contextInfo.getIdentifiers());
        }
        return new CypherTypeDescriptor(CypherType.UNKNOWN);
    }


    @Override
    public IExpression getSourceRefExpression() {
        return sourceExpression;
    }

    @Override
    public IContextInfo getContextInfo() {
        return contextInfo;
    }
}
