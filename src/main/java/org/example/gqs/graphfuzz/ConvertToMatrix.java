package org.example.gqs.graphfuzz;
import org.example.gqs.Randomly;
import org.example.gqs.cypher.gen.AbstractNode;
import org.example.gqs.cypher.gen.AbstractRelationship;
import org.example.gqs.cypher.schema.IPatternElementInfo;

import java.util.*;

class Route implements Comparable<Route>
{
    public List<Integer> localPathList = new ArrayList<>();
    public List<Integer> localEdgeList = new ArrayList<>();
    Route(){}
    Route(List<Integer> localPathList, List<Integer> localEdgeList)
    {
        this.localPathList.addAll(localPathList);
        this.localEdgeList.addAll(localEdgeList);
    }

    @Override
    public int compareTo(Route o) {
        return o.localPathList.size() - this.localPathList.size();
    }
}

public class ConvertToMatrix implements Cloneable {
    public ArrayList<ArrayList<Set<Integer>>> Matrix;
    public Map<Integer, Set<IPatternElementInfo>> labelMap = new HashMap<Integer, Set<IPatternElementInfo>>();
    public Map<Integer, HashMap<String, Object>> propertyMap = new HashMap<Integer, HashMap<String, Object>>();
    public Map<Integer, Object> idMap = new HashMap<Integer, Object>();
    List<Route> availableRoute = new ArrayList<>();
    public ConvertToMatrix(int numOfNode)
    {
        Matrix = new ArrayList<ArrayList<Set<Integer>>>(numOfNode);
        for(int i = 0; i<numOfNode; i++)
        {
            Matrix.add(new ArrayList<Set<Integer>>(numOfNode));
            for(int j = 0; j<numOfNode; j++)
            {
                Matrix.get(i).add(new HashSet<Integer>());
            }
        }
    }

    public void initialize(List<AbstractNode> nodes, List<AbstractRelationship> relationships)
    {
        try
        {
            for (AbstractRelationship curRelation : relationships)
            {
                Matrix.get(curRelation.getFrom().getId()).get(curRelation.getTo().getId()).add(curRelation.getId());
                labelMap.computeIfAbsent(curRelation.getId(), k -> new HashSet<>()).add((IPatternElementInfo)curRelation.getType());
                propertyMap.computeIfAbsent(curRelation.getId(), k -> new HashMap<>()).putAll(curRelation.getProperties());
                idMap.put(curRelation.getId(), curRelation);
            }
            for (AbstractNode curNode : nodes)
            {
                labelMap.computeIfAbsent(curNode.getId(), k -> new HashSet<>()).addAll(curNode.getLabelInfos());
                propertyMap.computeIfAbsent(curNode.getId(), k -> new HashMap<>()).putAll(curNode.getProperties());
                idMap.put(curNode.getId(), curNode);
            }
        }
        catch (Exception e)
        {
            System.out.println("Error in initialize");
            e.printStackTrace();
        }
    }


    public void fetchPath(List<List<AbstractNode>> nodes, List<List<AbstractRelationship>> relationships) {
        boolean[] visited = new boolean[Matrix.size()];
        Set<Integer> visitedEdge = new HashSet<>();
        Set<Integer> backupEdge = new HashSet<>();
        for (int j = 0; j < Matrix.size(); j++) {
            List<Integer> pathList = new ArrayList<>();
            List<Integer> edgeList = new ArrayList<>();
            pathList.add(j);
            findAllPathsUtil(j, visited, pathList, edgeList);
        }
        List<Route> backupRoute = new ArrayList<>(availableRoute);
        Set<Route> aggregated = new HashSet<>();
        Set<Route> modifiedRoute = new HashSet<>();
        for (int i = 0; i < availableRoute.size(); i++) {
            Route route1 = availableRoute.get(i);
            for (int j = 0; j < availableRoute.size(); j++) {
                Route route2 = availableRoute.get(j);
                if (route1 == route2 || route2.localPathList.size() == 0 || route1.localPathList.size() == 0 || aggregated.contains(route1) || aggregated.contains(route2))
                    continue;
                if (Objects.equals(route1.localPathList.get(route1.localPathList.size() - 1), route2.localPathList.get(0))) {
                    aggregated.add(route1);
                    aggregated.add(route2);
                    modifiedRoute.add(route1);
                    backupRoute.get(i).localPathList.remove(backupRoute.get(i).localPathList.size() - 1);
                    backupRoute.get(i).localPathList.addAll(backupRoute.get(j).localPathList);
                    backupRoute.get(i).localEdgeList.addAll(backupRoute.get(j).localEdgeList);
                }
            }
        }
        for (Route route : aggregated) {
            if (backupRoute.contains(route) && !modifiedRoute.contains(route))
                backupRoute.remove(route);
        }
        availableRoute = backupRoute;
        if (availableRoute.size() > 100) {
            Randomly r = new Randomly();
            Set<Integer> selected = new HashSet<>();
            while (selected.size() < 100) {
                selected.add(r.getInteger(0, availableRoute.size() - 1));
            }
            List<Route> backup = new ArrayList<>(availableRoute);
            availableRoute.clear();
            for (Integer i : selected) {
                availableRoute.add(backup.get(i));
            }
        }
        for (Route route : availableRoute) {
            List<AbstractNode> curNodes = new ArrayList<>();
            List<AbstractRelationship> curRelations = new ArrayList<>();
            boolean notDuplicate = true;
            backupEdge = new HashSet<>(visitedEdge);
            for (int i = 0; i < route.localEdgeList.size(); i++) {
                if (visitedEdge.contains(route.localEdgeList.get(i))) {
                    notDuplicate = false;
                    visitedEdge = new HashSet<>(backupEdge);
                    break;
                }
                visitedEdge.add(route.localEdgeList.get(i));
            }
            if (!notDuplicate) {
                continue;
            }
            for (Integer integer : route.localEdgeList) {
                curRelations.add((AbstractRelationship) idMap.get(integer));
            }
            relationships.add(curRelations);
            for (Integer integer : route.localPathList) {
                curNodes.add((AbstractNode) idMap.get(integer));
            }
            nodes.add(curNodes);
        }

    }

    private void findAllPathsUtil(Integer u, boolean[] visited, List<Integer> localPathList, List<Integer> localEdgeList) {


        if(availableRoute.size() > 10000)
            return;
        visited[u] = true;
        boolean isDeadEnd = true;

        for (int i = 0; i < Matrix.size(); i++) {

            if (!Matrix.get(u).get(i).isEmpty() && !visited[i]) {
                isDeadEnd = false;
                for (Integer edgeId : Matrix.get(u).get(i))
                {

                    localPathList.add(i);
                    localEdgeList.add(edgeId);

                    findAllPathsUtil(i, visited, localPathList, localEdgeList);

                    localPathList.remove(localPathList.size()-1);
                    localEdgeList.remove(localEdgeList.size()-1);
                }
            }
        }

        if(isDeadEnd)
        {
            visited[u] = false;

            if(localPathList.size() > 1)
                availableRoute.add(new Route(localPathList, localEdgeList));
        }

        visited[u] = false;
    }

    @Override
    public ConvertToMatrix clone() {
        try {
            ConvertToMatrix clone = (ConvertToMatrix) super.clone();






            clone.Matrix = new ArrayList<ArrayList<Set<Integer>>>(Matrix.size());
            for(int i = 0; i<Matrix.size(); i++)
            {
                clone.Matrix.add(new ArrayList<Set<Integer>>(Matrix.get(i).size()));
                for(int j = 0; j<Matrix.get(i).size(); j++)
                {
                    clone.Matrix.get(i).add(new HashSet<Integer>());
                    clone.Matrix.get(i).get(j).addAll(Matrix.get(i).get(j));
                }
            }
            clone.labelMap = new HashMap<Integer, Set<IPatternElementInfo>>();
            for(Map.Entry<Integer, Set<IPatternElementInfo>> entry : labelMap.entrySet())
            {
                clone.labelMap.put(entry.getKey(), new HashSet<IPatternElementInfo>());
                clone.labelMap.get(entry.getKey()).addAll(entry.getValue());
            }
            clone.propertyMap = new HashMap<Integer, HashMap<String, Object>>();
            for(Map.Entry<Integer, HashMap<String, Object>> entry : propertyMap.entrySet())
            {
                clone.propertyMap.put(entry.getKey(), new HashMap<String, Object>());
                clone.propertyMap.get(entry.getKey()).putAll(entry.getValue());
            }
            clone.idMap = new HashMap<Integer, Object>();
            for(Map.Entry<Integer, Object> entry : idMap.entrySet())
            {
                clone.idMap.put(entry.getKey(), entry.getValue());
            }
            clone.availableRoute = new ArrayList<>();
            for(Route route : availableRoute)
            {
                clone.availableRoute.add(new Route(route.localPathList, route.localEdgeList));
            }
            return clone;
        } catch (CloneNotSupportedException e) {
            throw new AssertionError();
        }
    }
}
