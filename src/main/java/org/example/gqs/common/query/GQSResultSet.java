package org.example.gqs.common.query;


import com.alibaba.fastjson.JSONArray;
import com.kuzudb.KuzuFlatTuple;
import com.kuzudb.KuzuObjectRefDestroyedException;
import com.kuzudb.KuzuQueryResult;
import org.neo4j.driver.Record;
import org.neo4j.driver.Result;

import java.io.Closeable;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.*;
import java.util.stream.Collectors;


public class GQSResultSet implements Closeable {

    public long resultRowNum;
    public List<Map<String, Object>> result;

    public List<Map<String, Object>> getResult() {
        return result;
    }

    public GQSResultSet() {
        result = new ArrayList<>();
        resultRowNum = 0;
    }

    public void add(String key, Object value) {
        if (result.size() == 0) {
            Map<String, Object> newMap = new HashMap<>();
            result.add(newMap);
            resultRowNum++;
        }
        result.get(0).put(key, value);
    }

    private String getMapAsString(Map<String, Object> m) {
        String s = "{";
        TreeSet<String> ts = new TreeSet<>(m.keySet());
        for (String key : ts) {
            if (m.get(key) == null || m.get(key).toString().contains("null")) {
                s += key + ":null,";
            } else {
                if (!(m.get(key) instanceof Number) && !(m.get(key) instanceof Boolean) && !(m.get(key) instanceof String) && !(m.get(key) instanceof List)) {
                    System.out.println(m.get(key).getClass());
                    System.out.println(m.get(key).toString());
                }

                if (m.get(key) instanceof Number) {
                    m.put(key, ((Number) m.get(key)).longValue());
                }
                if (m.get(key) instanceof List) {
                    List<Long> currentList = (List) m.get(key);
                    List<Long> intElements = currentList;
                    boolean flag = false;
                    String newString = "[";
                    if (intElements.size() > 0) {
                        Long firstElement = intElements.get(0);
                        for (int i = 0; i < intElements.size(); i++) {
                            Long element = intElements.get(i);
                            if (i == intElements.size() - 1)
                                newString += element.toString();
                            else
                                newString += element.toString() + ", ";
                        }


                    }
                    newString += "]";
                    m.put(key, newString);
                }

                s += key + ":" + m.get(key).toString() + ",";
            }
        }
        s += "}";
        return s;
    }

    public List<String> resultToStringList() {
        List<String> l = new ArrayList<>();
        for (int i = 0; i < result.size(); ++i) {
            l.add(getMapAsString(result.get(i)));
        }
        return l;
    }

    public void resolveFloat() {
        List<Map<String, Object>> newList = new ArrayList<>();
        for (Map<String, Object> map : result) {
            Map<String, Object> newMap = new HashMap<>();
            for (Map.Entry<String, Object> entry : map.entrySet()) {
                if (entry.getValue() instanceof Number) {
                    long value = ((Number) entry.getValue()).longValue();
                    newMap.put(entry.getKey(), value);
                } else if (entry.getValue() instanceof List) {
                    List list = (List) entry.getValue();
                    if (list.size() == 0) {
                        newMap.put(entry.getKey(), entry.getValue());
                    } else {
                        newMap.put(entry.getKey(), list.stream().map(
                                e -> {
                                    if (e instanceof Number) {
                                        long value = ((Number) e).longValue();
                                        return value;
                                    } else {
                                        return e;
                                    }
                                }
                        ).collect(Collectors.toList()));
                    }
                } else {
                    newMap.put(entry.getKey(), entry.getValue());
                }
            }
            newList.add(newMap);
        }
        this.result = newList;
    }

    public boolean compare(GQSResultSet secondGQSResultSet, boolean withOrder) {

        List<String> firstSortList = new ArrayList<>(resultToStringList());
        List<String> secondSortList = new ArrayList<>(secondGQSResultSet.resultToStringList());

        if (!withOrder) {
            Collections.sort(firstSortList);
            Collections.sort(secondSortList);
        }
        return firstSortList.equals(secondSortList);
    }

    public boolean compareWithOneRow(GQSResultSet secondGQSResultSet) {
        GQSResultSet firstGQSResultSet = new GQSResultSet();
        firstGQSResultSet.resultRowNum = 1;
        GQSResultSet secondGQSResultSetOneRow = new GQSResultSet();
        secondGQSResultSetOneRow.result.add(new HashMap<String, Object>());
        for (Map<String, Object> curRes : secondGQSResultSet.result) {
            for (Map.Entry<String, Object> entry : curRes.entrySet()) {
                if (secondGQSResultSetOneRow.result.get(0).containsKey(entry.getKey())) {
                    if (secondGQSResultSetOneRow.result.get(0).get(entry.getKey()).toString().compareTo(entry.getValue().toString()) < 0) {
                        secondGQSResultSetOneRow.result.get(0).put(entry.getKey(), entry.getValue());
                    }
                } else {
                    secondGQSResultSetOneRow.result.get(0).put(entry.getKey(), entry.getValue());
                }

            }
        }
        secondGQSResultSetOneRow.resultRowNum = 1;
        return firstGQSResultSet.compareWithOutOrder(secondGQSResultSetOneRow);
    }

    public boolean compareWithOrder(GQSResultSet secondGQSResultSet) {
        return compare(secondGQSResultSet, true);
    }

    public boolean compareWithOutOrder(GQSResultSet secondGQSResultSet) {
        return compare(secondGQSResultSet, false);
    }

    public GQSResultSet(KuzuQueryResult rs) {
        resultRowNum = 0;
        result = new ArrayList<Map<String, Object>>();
        try {
            long size = rs.getNumColumns();

            while (rs.hasNext()) {
                Map<String, Object> row = new HashMap<String, Object>();
                KuzuFlatTuple kuzurow = rs.getNext();
                for (int i = 0; i < size; i++) {
                    row.put(rs.getColumnName(i), kuzurow.getValue(i).getValue().toString());
                }
                kuzurow.destroy();
                resultRowNum++;
                result.add(row);
            }
            rs.destroy();

        } catch (KuzuObjectRefDestroyedException e) {
            e.printStackTrace();
        } catch (Exception e) {
            e.printStackTrace();
        }
        System.out.println("result_size=" + resultRowNum);
    }

    public GQSResultSet(Map<String, Object> res) {
        resultRowNum = 1;
        result = new ArrayList<Map<String, Object>>();
        result.add(res);
    }

    public GQSResultSet(com.falkordb.ResultSet rs) {
        resultRowNum = 0;
        result = new ArrayList<Map<String, Object>>();
        Iterator<com.falkordb.Record> re = rs.iterator();
        while (re.hasNext()) {
            com.falkordb.Record r = re.next();
            Map<String, Object> row = new HashMap<String, Object>();
            for (String k : r.keys()) {
                row.put(k, r.getValue(k));
            }
            resultRowNum++;
            result.add(row);
        }
        System.out.println("result_size=" + resultRowNum);
    }

    public GQSResultSet(Result rs) {
        List<Record> resultList = rs.list();
        if (resultList.size() < 10000) {
            resultRowNum = resultList.size();
            result = new ArrayList<Map<String, Object>>();

            for (Record x : resultList) {
                Map<String, Object> m = x.asMap();
                result.add(m);
            }
            System.out.println("result_size=" + resultRowNum);
        } else {
            resultRowNum = -1;
            this.result = null;
        }

    }

    public GQSResultSet(org.neo4j.graphdb.Result rs) {
        List<Map<String, Object>> resultList = rs.stream().toList();
        if (resultList.size() < 10000) {
            resultRowNum = resultList.size();
            result = new ArrayList<Map<String, Object>>();

            for (Map<String, Object> x : resultList) {
                result.add(x);
            }
            System.out.println("result_size=" + resultRowNum);
        } else {
            resultRowNum = -1;
            this.result = null;
        }

    }

    public GQSResultSet(ResultSet rs) throws SQLException {
        resultRowNum = 0;
        result = new ArrayList<Map<String, Object>>();

        ResultSetMetaData md = rs.getMetaData();
        int columns = md.getColumnCount();
        while (rs.next()) {
            Map<String, Object> row = new HashMap<String, Object>(columns);
            for (int i = 1; i <= columns; ++i) {
                row.put(md.getColumnName(i), rs.getObject(i));
            }
            resultRowNum++;
            result.add(row);
        }
        System.out.println("result_size=" + resultRowNum);
    }

    public GQSResultSet(com.redislabs.redisgraph.ResultSet rs) throws SQLException {

        resultRowNum = 0;
        result = new ArrayList<Map<String, Object>>();
        while (rs.hasNext()) {
            com.redislabs.redisgraph.Record r = rs.next();
            Map<String, Object> row = new HashMap<String, Object>();
            for (String k : r.keys()) {
                row.put(k, r.getValue(k));
            }
            resultRowNum++;
            result.add(row);
        }
        System.out.println("result_size=" + resultRowNum);
    }

    public GQSResultSet(List<Map<String, Object>> gremlinResults) {
        resultRowNum = gremlinResults.size();
        result = gremlinResults;

        System.out.println("result_size=" + resultRowNum);
    }

    public GQSResultSet(String rs) {
        result = new ArrayList<>();
        if (rs.equals("null")) {
            resultRowNum = 0;
        } else {
            if (!(rs.startsWith("[") && rs.endsWith("]"))) {
                rs = "[" + rs + "]";
            }
            List<Map> maps = JSONArray.parseArray(rs, Map.class);
            resultRowNum = maps.size();

            for (Map m : maps) {
                Map<String, Object> row = new HashMap<String, Object>();
                for (Object k : m.keySet()) {
                    row.put(k.toString(), m.get(k.toString()));
                }
                result.add(row);
            }
        }
        System.out.println("result_size=" + resultRowNum);
    }

    public long getRowNum() {
        return resultRowNum;
    }

    @Override
    public void close() {

    }

    public void registerEpilogue(Runnable runnableEpilogue) {

    }

    public boolean next() throws SQLException {
        return true;
    }

    public String getString(long i) throws SQLException {
        return "zzz";
    }

    public long getLong(long i) throws SQLException {
        return 1;
    }

    public boolean isClosed() throws SQLException {
        return true;
    }

}
