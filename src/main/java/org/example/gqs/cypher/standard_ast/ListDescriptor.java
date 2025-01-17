package org.example.gqs.cypher.standard_ast;

import org.example.gqs.cypher.ast.analyzer.ICypherTypeDescriptor;
import org.example.gqs.cypher.ast.analyzer.IListDescriptor;

import java.util.List;

public class ListDescriptor implements IListDescriptor {
    boolean isListSizeUnknown;
    List<ICypherTypeDescriptor> memberTypes;
    ICypherTypeDescriptor sameType;
    boolean isMembersWithSameType;

    public ListDescriptor(List<ICypherTypeDescriptor> memberTypes){
        this.memberTypes = memberTypes;
        isListSizeUnknown = false;
        sameType = null;
        isMembersWithSameType = false;
    }

    public ListDescriptor(ICypherTypeDescriptor sameType){
        this.sameType = sameType;
        memberTypes = null;
        isListSizeUnknown = true;
        isMembersWithSameType = true;
    }

    public ListDescriptor(){
        isListSizeUnknown = true;
        isMembersWithSameType = false;
        sameType = null;
        memberTypes = null;
    }

    @Override
    public boolean isListLengthUnknown() {
        return isListSizeUnknown;
    }

    @Override
    public List<ICypherTypeDescriptor> getListMemberTypes() {
        return memberTypes;
    }

    @Override
    public boolean isMembersWithSameType() {
        return isMembersWithSameType;
    }

    @Override
    public ICypherTypeDescriptor getSameMemberType() {
        return sameType;
    }
}
