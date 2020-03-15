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

    //the result or outcome of the execution should be set inside requestContext as a map and the name of the map key should be returned as the value of the function.
    @Override
    protected String doExecute(Map<String, Object> requestContext) {
        String operation = (String) requestContext.get("operation");  //possible values - open, prepare, execute, read, close
        if (operation == null) {
            return null;
        }
        String resultMapKeyName = "-rdbms-io-result-";
        String resultKeyName = "result";
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
        String driver = (String) requestContext.get("db-driver");
        String dburl = (String) requestContext.get("db-url");
        String username = (String) requestContext.get("username");
        String password = (String) requestContext.get("password");

        Class.forName(driver);
        return DriverManager.getConnection(dburl, username, password);
    }

    private Object prepareStatement(Map<String, Object> requestContext) throws SQLException {
        throw new UnsupportedOperationException("Prepared statement support is yet to be provided");  //TODO
    }

    private Object executeStatement(Map<String, Object> requestContext) throws SQLException {
        Connection connection = (Connection) requestContext.get("connection");
        String query = (String) requestContext.get("query");
        String queryType = (String) requestContext.get("query-type");
        if (connection == null || query == null || queryType == null) {
            return null;
        }

        Statement statement = connection.createStatement();

        switch(queryType.toUpperCase().trim()) {
            case "SELECT": return statement.executeQuery(query);  //returns a ResultSet object
            case "INSERT":
            case "UPDATE":
            case "DELETE": return statement.executeUpdate(query);  //returns an int value
            default: throw new UnsupportedOperationException("Unsupported query type: " + queryType);
        }
    }

    private Object read(Map<String, Object> requestContext) throws SQLException {
        Object _resultSet = requestContext.get("result-set");
        if (!(_resultSet instanceof ResultSet)) {
            return null;
        }
        ResultSet resultSet = (ResultSet) _resultSet;
        Map<String, Map<String, Object>> returnMap = null;
        if (resultSet.next()) {
            returnMap = new HashMap<>();
            returnMap.put("values", new HashMap<String, Object>());
            returnMap.put("datatypes", new HashMap<String, Object>());
            ResultSetMetaData rsmd = resultSet.getMetaData();
            int colCount = rsmd.getColumnCount();
            for (int i = 1; i <= colCount; i++) {
                String colName = rsmd.getColumnName(i);
                String colDataType = rsmd.getColumnTypeName(i);
                Object colValue = resultSet.getObject(i);
                returnMap.get("values").put(colName, colValue);
                returnMap.get("datatypes").put(colName, colDataType);
            }
        }
        return returnMap;
    }

    private void close(Map<String, Object> requestContext) throws SQLException {
        Object closeable = requestContext.get("closeable");
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
