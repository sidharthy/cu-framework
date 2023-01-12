// Compilation Units Framework: a very generic & powerful data driven programming framework.
// Copyright (c) 2019 Sidharth Yadav, sidharth_08@yahoo.com
// All rights reserved.
//
// Redistribution and use in source and binary forms, with or without
// modification, are permitted provided that the following conditions
// are met:
// 1. Redistributions of source code must retain the above copyright
//    notice, this list of conditions and the following disclaimer.
// 2. Redistributions in binary form must reproduce the above copyright
//    notice, this list of conditions and the following disclaimer in the
//    documentation and/or other materials provided with the distribution.
// 3. Neither the name of the copyright holders nor the names of its
//    contributors may be used to endorse or promote products derived from
//    this software without specific prior written permission.
//
// THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
// AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
// IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
// ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE
// LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
// CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
// SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
// INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
// CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
// ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF
// THE POSSIBILITY OF SUCH DAMAGE.

package org.cuframework.util.cu;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;

import org.cuframework.core.CompilationUnits.IExecutable;
import org.cuframework.core.CompilationUnits.HeadlessExecutableGroup;

/**
 * @author Sidharth Yadav
 */
public class RdbmsIO extends HeadlessExecutableGroup implements IExecutable {
    public static final String TAG_NAME = "rdbms";

    //cu input parameters
    private static final String PARAM_OPERATION = "operation";  //e.g. open, prepare, execute, read, close
    private static final String PARAM_DB_DRIVER = "db-driver";  //database driver class
    private static final String PARAM_DB_URL = "db-url";  //database url
    private static final String PARAM_USERNAME = "username";  //connection username
    private static final String PARAM_PASSWORD = "password";  //connection password
    private static final String PARAM_CONNECTION = "connection";  //connection object to use to execute statements
    private static final String PARAM_QUERY = "query";  //query to execute
    private static final String PARAM_QUERY_TYPE = "query-type";  //e.g. select, insert, update, delete
    private static final String PARAM_RUN_AS_TRANSACTION = "transaction";  //true or false
    private static final String PARAM_RESULT_SET = "result-set";  //query result set
    private static final String PARAM_CLOSEABLE = "closeable";  //an object that can be closed e.g. connection, statement, result set

    @Override
    public String getTagName() {
        return RdbmsIO.TAG_NAME;
    }

    //the result or outcome of the execution should be set inside requestContext as a map and the name of the map key should be returned as the value of the function.
    @Override
    protected String doExecute(Map<String, Object> requestContext) {
        String operation = (String) requestContext.get(PARAM_OPERATION);  //possible values - open, prepare, execute, read, close
        if (operation == null) {
            return null;
        }
        String resultMapKeyName = "-rdbms-io-result-";
        String idOrElse = getIdOrElse();  //using the non computed version of idOrElse
        String resultKeyName = idOrElse == null? "result": idOrElse;
        Map<String, Object> resultMap = new HashMap<>();
        try {
            switch(operation.toUpperCase().trim()) {
                case "OPEN": resultMap.put(resultKeyName, openConnection(requestContext)); break;
                case "PREPARE": resultMap.put(resultKeyName, prepareStatement(requestContext)); break;
                case "EXECUTE": resultMap.put(resultKeyName, executeStatement(requestContext)); break;
                case "READ": resultMap.put(resultKeyName, read(requestContext)); break;
                case "CLOSE": close(requestContext); break;
                default: throw new UnsupportedOperationException("Unsupported io operation: " + operation);
            }
            requestContext.put(resultMapKeyName, resultMap);
        } catch(Exception e) {
            //any custom processing needed?
            throw new RuntimeException(e);
        }
        return resultMapKeyName;
    }

    private Object openConnection(Map<String, Object> requestContext) throws ClassNotFoundException, SQLException {
        String driver = (String) requestContext.get(PARAM_DB_DRIVER);
        String dburl = (String) requestContext.get(PARAM_DB_URL);
        String username = (String) requestContext.get(PARAM_USERNAME);
        String password = (String) requestContext.get(PARAM_PASSWORD);

        Class.forName(driver);
        return DriverManager.getConnection(dburl, username, password);
    }

    private Object prepareStatement(Map<String, Object> requestContext) throws SQLException {
        throw new UnsupportedOperationException("Prepared statement support is yet to be provided");  //TODO
    }

    private Object executeStatement(Map<String, Object> requestContext) throws SQLException {
        Connection connection = (Connection) requestContext.get(PARAM_CONNECTION);
        Object query = requestContext.get(PARAM_QUERY);
        String queryType = (String) requestContext.get(PARAM_QUERY_TYPE);
        if (connection == null || query == null) {
            return null;
        }

        if (!(query instanceof String || query.getClass().isArray())) {
            throw new IllegalArgumentException("Query object should be a String or an array object. Instead found: " + query.getClass().getName());
        }

        boolean isBatch = query.getClass().isArray();
        boolean asTransaction = isBatch &&  //we would attempt to run as a transaction only if a batch of queries is provided.
                                requestContext.get(PARAM_RUN_AS_TRANSACTION) != null?
                                                "true".equalsIgnoreCase(requestContext.get(PARAM_RUN_AS_TRANSACTION).toString()):
                                                false;

        Object result = null;
        if (!isBatch) {
            result = executeSingleQuery(connection, (String) query, queryType);
        } else {
            result = executeQueryBatch(connection, (Object[]) query, asTransaction);
        }
        return result;
    }

    private Object executeSingleQuery(Connection connection, String query, String queryType) throws SQLException {
        final String GENERIC = "_GENERIC";
        if (queryType == null) {
            queryType = GENERIC;
        }

        Statement statement = connection.createStatement();
        Object result = null;

        switch(queryType.toUpperCase().trim()) {
            case "SELECT": result = statement.executeQuery(query); break;  //result will be a ResultSet object
            case "INSERT":
            case "UPDATE":
            case "DELETE": result = statement.executeUpdate(query); break;  //result will be an int value
            case GENERIC: {
                              boolean isResultSet = statement.execute(query);  //in rare cases this may return multiple result sets
                              if (isResultSet) {
                                  result = statement.getResultSet();  //we will return only the first result set
                              } else {
                                  result = statement.getUpdateCount();
                              }
                              break;
                          }
            default: throw new UnsupportedOperationException("Unsupported query type: " + queryType);
        }
        return result;
    }

    private Object executeQueryBatch(Connection connection, Object[] queries, boolean asTransaction) throws SQLException {
        if (queries.length == 0) {
            return null;
        }

        Object result = null;
        boolean savedAutoCommitStatus = connection.getAutoCommit();

        if (asTransaction) {
            connection.setAutoCommit(false);
        }
        try {
            Statement statement = connection.createStatement();
            boolean atleastOneQueryFound = false;
            for (Object query: queries) {
                if (query instanceof String) {
                    atleastOneQueryFound = true;
                    statement.addBatch((String) query);
                }
            }
            if (atleastOneQueryFound) {
                int[] updateCounts = statement.executeBatch();
                if (asTransaction) {
                    //if it was a transaction then auto commit would have been disabled. We now need to explicitly commit.
                    connection.commit();
                }
                result = updateCounts;
            }
        } catch(SQLException sqle) {
            try {
                if (asTransaction) {
                    //if it was a transaction then auto commit would have been disabled. We now need to rollback as something went wrong.
                    connection.rollback();
                }
            } catch (SQLException sqle2) {
                //ignore. log?
            }
            throw sqle;
        } finally {
            try {
                if (asTransaction) {
                    connection.setAutoCommit(savedAutoCommitStatus);  //revert to the original auto commit status
                }
            } catch(SQLException sqle) {
                //ignore. log?
            }
        }
        return result;
    }

    private Object read(Map<String, Object> requestContext) throws SQLException {
        Object _resultSet = requestContext.get(PARAM_RESULT_SET);
        if (!(_resultSet instanceof ResultSet)) {
            return null;
        }

        String COLUMN_VALUES = "values";
        String COLUMN_DATATYPES = "datatypes";

        ResultSet resultSet = (ResultSet) _resultSet;
        Map<String, Map<String, Object>> returnMap = null;
        if (resultSet.next()) {
            returnMap = new HashMap<>();
            returnMap.put(COLUMN_VALUES, new LinkedHashMap<String, Object>());
            returnMap.put(COLUMN_DATATYPES, new LinkedHashMap<String, Object>());
            ResultSetMetaData rsmd = resultSet.getMetaData();
            int colCount = rsmd.getColumnCount();
            for (int i = 1; i <= colCount; i++) {
                String colName = rsmd.getColumnLabel(i);  //using getColumnLabel as it supports alias name(s)
                String colDataType = rsmd.getColumnTypeName(i);
                Object colValue = resultSet.getObject(i);
                returnMap.get(COLUMN_VALUES).put(colName, colValue);
                returnMap.get(COLUMN_DATATYPES).put(colName, colDataType);
            }
        }
        return returnMap;
    }

    private void close(Map<String, Object> requestContext) throws SQLException {
        Object closeable = requestContext.get(PARAM_CLOSEABLE);
        if (closeable instanceof Connection) {
            ((Connection) closeable).close();
        }
        else if (closeable instanceof ResultSet) {
            ((ResultSet) closeable).close();
        }
        else if (closeable instanceof Statement) {
            ((Statement) closeable).close();
        }
    }

    //overridden method to support cloning
    @Override
    protected RdbmsIO newInstance() {
        return new RdbmsIO();
    }
}
