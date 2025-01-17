package org.example.gqs.cypher.standard_ast;

import org.example.gqs.cypher.ast.analyzer.ICypherTypeDescriptor;
import org.example.gqs.cypher.ast.analyzer.IMapDescriptor;

import java.util.Map;

public class MapDescriptor implements IMapDescriptor {
    boolean isMapSizeUnknown;
    Map<String, ICypherTypeDescriptor> memberTypes;
    ICypherTypeDescriptor sameType;
    boolean isMembersWithSameType;

    public MapDescriptor(Map<String, ICypherTypeDescriptor> memberTypes){
        this.memberTypes = memberTypes;
        isMapSizeUnknown = false;
        sameType = null;
        isMembersWithSameType = false;
    }

    public MapDescriptor(ICypherTypeDescriptor sameType){
        this.sameType = sameType;
        memberTypes = null;
        isMapSizeUnknown = true;
        isMembersWithSameType = true;
    }

    public MapDescriptor(){
        isMapSizeUnknown = true;
        isMembersWithSameType = false;
        sameType = null;
        memberTypes = null;
    }

    @Override
    public boolean isMapSizeUnknown() {
        return isMapSizeUnknown;
    }

    @Override
    public Map<String, ICypherTypeDescriptor> getMapMemberTypes() {
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
