package org.example.gqs.cypher.standard_ast;

import org.example.gqs.cypher.ast.*;
import org.example.gqs.cypher.dsl.IIdentifierBuilder;

public class Ret implements IRet {
    private boolean isAll;
    public IExpression expression = null;
    private IIdentifier identifier = null;

    public static Ret createAliasRef(IAlias alias){
        return new Ret(new Alias(alias.getName(), alias.getExpression(), ((Alias)alias).isDistinct));
    }

    public static Ret createNodeRef(INodeIdentifier node){
        return new Ret(NodeIdentifier.createNodeRef(node));
    }

    public static Ret createRelationRef(IRelationIdentifier relation){
        if(relation instanceof RelationAnalyzer)
            return new Ret(RelationIdentifier.createRelationRef(relation, Direction.NONE, 1, 1, ((RelationIdentifier)((RelationAnalyzer) relation).getSource()).actualRelationship));
        else
            return new Ret(RelationIdentifier.createRelationRef(relation, Direction.NONE, 1, 1, ((RelationIdentifier) relation).actualRelationship));
    }

    public static Ret createNewExpressionAlias(IIdentifierBuilder identifierBuilder, IExpression expression){
        String resultName = identifierBuilder.getNewAliasName();
        return new Ret(expression, resultName);
    }

    public static Ret createNewExpressionReturnVal(IExpression expression){
        return new Ret(expression);
    }

    public static Ret createStar(){
        return new Ret();
    }

    Ret(IIdentifier identifier){
        this.identifier = identifier;
        isAll = false;
    }

    public Ret(IExpression expression, String name){
        this.expression = expression;
        this.identifier = new Alias(name, expression, false);
        isAll = false;
    }

    public Ret(IExpression expression){
        this.expression = expression;
        this.identifier = null;
        isAll = false;
    }

    Ret(){
        isAll = true;
    }


    @Override
    public boolean isAll() {
        return isAll;
    }

    @Override
    public void setAll(boolean isAll) {
        this.isAll = isAll;
    }

    @Override
    public boolean isNodeIdentifier() {
        return identifier instanceof INodeIdentifier;
    }

    @Override
    public boolean isRelationIdentifier() {
        return identifier instanceof IRelationIdentifier;
    }

    @Override
    public boolean isAnonymousExpression() {
        return expression != null;
    }

    @Override
    public boolean isAlias() {
        return identifier instanceof IAlias;
    }

    @Override
    public IExpression getExpression() {
        if(identifier == null)
            return expression;
        if(identifier instanceof IAlias)
            return ((IAlias) identifier).getExpression();
        return null;
    }

    public IExpression setExpression(IExpression expression){
        if(identifier == null)
            return this.expression = expression;
        if(identifier instanceof IAlias) {
            this.expression = expression;
            return ((Alias) identifier).setExpression(expression);
        }
        return null;
    }


    @Override
    public IIdentifier getIdentifier() {
        return identifier;
    }

    @Override
    public IRet getCopy() {
        Ret returnVal = new Ret();
        returnVal.isAll = isAll;
        returnVal.expression = null;
        returnVal.identifier = null;
        if(expression != null){
            returnVal.expression = expression.getCopy();
        }
        if(identifier != null){
            returnVal.identifier = identifier.getCopy();
        }
        return returnVal;
    }

    @Override
    public void toTextRepresentation(StringBuilder sb) {
        if(isAll()){
            sb.append("*");
            return;
        }
        if(expression != null){
            expression.toTextRepresentation(sb);
            if(identifier == null) {
                return;
            }
            sb.append(" AS ");
        }
        sb.append(identifier.getName());
    }

    private boolean sameExpression(Ret ret){
        if(expression != null){
            return expression.equals(ret.expression);
        }
        return ret.expression == null;
    }

    private boolean sameIdentifier(Ret ret){
        if(identifier != null){
            return identifier.equals(ret.identifier);
        }
        return ret.identifier == null;
    }


    @Override
    public boolean equals(Object o){
        if(!(o instanceof Ret)){
            return false;
        }
        return sameExpression((Ret)o) && sameIdentifier((Ret)o) && isAll == ((Ret) o).isAll;
    }
}
