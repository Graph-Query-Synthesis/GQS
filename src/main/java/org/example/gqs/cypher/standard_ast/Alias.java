package org.example.gqs.cypher.standard_ast;

import org.example.gqs.MainOptions;
import org.example.gqs.cypher.dsl.IIdentifierBuilder;
import org.example.gqs.cypher.ast.IAlias;
import org.example.gqs.cypher.ast.ICypherType;
import org.example.gqs.cypher.ast.IExpression;
import org.example.gqs.cypher.ast.IIdentifier;
import org.example.gqs.cypher.standard_ast.expr.*;

import java.util.Set;

public class Alias implements IAlias {
    protected String name;
    protected IExpression expression;
    public boolean isDistinct = false;

    public static Alias createIdentifierRef(IIdentifier alias) {
        throw new RuntimeException("Alias.createIdentifierRef(IIdentifier alias) is not implemented");
    }
    public static Alias createIdentifierRef(IIdentifier alias, IExpression expression){
        return new Alias(alias.getName(), expression);
    }
    public Set<IIdentifier> reliedContent()
    {
        return ((CypherExpression)expression).reliedContent();
    }

    public static Alias createExpressionAlias(IExpression expression, IIdentifierBuilder identifierBuilder){
        return new Alias(identifierBuilder.getNewAliasName(), expression);
    }

    public Alias(String name, IExpression expression){
        this.name = name;
        this.expression = expression;
    }

    Alias(String name, IExpression expression, boolean isDistinct){
        this.name = name;
        this.expression = expression;
        this.isDistinct = isDistinct;
    }

    @Override
    public String getName() {
        return name;
    }
    public void setName(String n) {
        name = n;
    }

    @Override
    public ICypherType getType() {
        return CypherType.UNKNOWN;
    }

    @Override
    public boolean equals(IIdentifier i2) {
        if(i2 instanceof Alias){
            if(this.name.equals(i2.getName()) && this.getExpression().equals(((Alias)i2).getExpression())){
                return true;
            }
        }
        return false;
    }

    @Override
    public IIdentifier getCopy() {
        Alias alias;
        if(expression != null){
            alias = new Alias(name, expression.getCopy(), isDistinct);
        }
        else {
            alias = new Alias(name, null, isDistinct);
        }
        return alias;
    }

    @Override
    public IExpression getExpression() {
        return expression;
    }
    public IExpression setExpression(IExpression expression) {
        this.expression = expression;
        return expression;
    }

    @Override
    public void toTextRepresentation(StringBuilder sb) {
        if(expression != null){
            expression.toTextRepresentation(sb);
            sb.append(" AS ");
        }
        sb.append(name);
    }

    @Override
    public boolean equals(Object o){
        if(!(o instanceof Alias)){
            return false;
        }
        if(getName().equals(((Alias)o).getName()) && getExpression().equals(((Alias)o).getExpression())){
            if(getExpression() instanceof CallExpression && MainOptions.isAggregateFunction(((CallExpression)getExpression()).functionName))
            {
                if(((CallExpression)getExpression()).getElementValue()!= null && (((CallExpression)getExpression()).getElementValue()).equals(((CallExpression)((Alias) o).getExpression()).getElementValue()))
                {
                    return true;
                }
                else if (((CallExpression)getExpression()).getElementValue() == null && ((CallExpression)((Alias) o).getExpression()).getElementValue() == null)
                    return true;
                else
                    return false;
            }
            else
                return true;
        }
        return false;
    }

    @Override
    public int hashCode(){
        return getName().hashCode();
    }

    public Object getValue()
    {
        IIdentifier identifier = this;
        if(identifier instanceof Alias && ((Alias)(identifier)).getExpression() instanceof CallExpression){
           return ((CallExpression)((Alias)(identifier)).getExpression()).getValue();
        }
        else if (identifier instanceof Alias && ((Alias) identifier).getExpression() instanceof GetPropertyExpression)
        {
            return ((GetPropertyExpression) ((Alias) identifier).getExpression()).getValue();
        }
        else if (identifier instanceof Alias && ((Alias) identifier).getExpression() instanceof ConstExpression)
        {
            return ((ConstExpression) ((Alias) identifier).getExpression()).getValue();
        }
        else if (identifier instanceof Alias && ((Alias) identifier).getExpression() instanceof CreateListExpression)
        {
            return ((CreateListExpression) ((Alias) identifier).getExpression()).getValue();
        }
        else if (identifier instanceof Alias  && ((Alias) identifier).getExpression() instanceof IdentifierExpression)
        {
            IIdentifier inner = ((IdentifierExpression) ((Alias) identifier).getExpression()).getIdentifier();
            if(inner instanceof NodeIdentifier)
            {
                return ((NodeIdentifier) inner).actualNode;
            }
            else if(inner instanceof RelationIdentifier)
            {
                return ((RelationIdentifier) inner).actualRelationship;
            }
            else if (inner instanceof Alias)
            {
                return ((Alias) inner).getValue();
            }
            else
            {
                throw new RuntimeException("Alias expression is not a valid expression");
            }

        }
        else
        {
            throw new RuntimeException("Alias expression is not a valid expression");
        }
    }

}
