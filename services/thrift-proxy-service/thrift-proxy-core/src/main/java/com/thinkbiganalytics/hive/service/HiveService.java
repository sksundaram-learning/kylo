package com.thinkbiganalytics.hive.service;


import com.thinkbiganalytics.db.model.query.QueryResult;
import com.thinkbiganalytics.db.model.query.QueryResultColumn;
import com.thinkbiganalytics.db.model.schema.Field;
import com.thinkbiganalytics.db.model.schema.TableSchema;
import com.thinkbiganalytics.schema.DBSchemaParser;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Service;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;
import javax.sql.DataSource;

/**
 * Created by sr186054 on 2/11/16.
 */
@Service("hiveService")
public class HiveService {

    @Inject
    @Qualifier("hiveJdbcTemplate")
    private JdbcTemplate jdbcTemplate;

    private DBSchemaParser schemaParser = null;

    public DataSource getDataSource(){
        return jdbcTemplate.getDataSource();
    }
    public DBSchemaParser getDBSchemaParser(){
        if(schemaParser == null) {
            schemaParser = new DBSchemaParser(getDataSource());
        }
        return schemaParser;
    }

    public List<String> getSchemaNames() {
        return getDBSchemaParser().listSchemas();
    }

    public List<String> getTables(String schema) {
       return getDBSchemaParser().listTables(schema);
    }


    /**
     * returns a list of schemanName.TableName
     * @return
     */
    public List<String> getAllTables(){
        List<String> allTables = new ArrayList<>();
        List<String> schemas = getSchemaNames();
        if(schemas != null) {
            for (String schema : schemas) {
                List<String> tables = getTables(schema);
                if(tables != null) {
                    for (String table :tables) {
                        allTables.add(schema+"."+table);
                    }
                }
            }
        }
        return allTables;
    }

    /**
     * returns a list of populated TableSchema objects
     * @return
     */
    public List<TableSchema> getAllTableSchemas(){
        List<TableSchema> allTables = new ArrayList<>();
        List<String> schemas = getSchemaNames();
        if(schemas != null) {
            for (String schema : schemas) {
                List<String> tables = getTables(schema);
                if(tables != null) {
                    for (String table :tables) {
                        allTables.add(getTableSchema(schema,table));
                    }
                }
            }
        }
        return allTables;
    }

    public TableSchema getTableSchema(String schema, String table) {
        return  getDBSchemaParser().describeTable(schema, table);
    }

    public List<Field> getFields(String schema, String table) {
        TableSchema tableSchema = getTableSchema(schema, table);
        if(tableSchema != null) {
           return tableSchema.getFields();
        }
        return null;
    }



    public  QueryResult browse(String schema, String table, String where, Integer limit) throws DataAccessException{

        if(where == null){
            where = "";
        }
        String query = "SELECT * from "+schema+"."+table+" "+where+" LIMIT "+limit;
        return browse(query);
    }



    public QueryResult browse(String query) throws DataAccessException{
        return query(query);

    }


    public QueryResult query(String query) throws DataAccessException{
        Connection conn = null;
        Statement stmt = null;
       final QueryResult queryResult = new QueryResult(query);
        final List<QueryResultColumn> columns = new ArrayList<>();
        final Map<String,Integer> displayNameMap = new HashMap<>();

        jdbcTemplate.query(query, new RowMapper<Map<String,Object>>() {
            @Override
            public Map<String,Object> mapRow(ResultSet rs, int rowNum) throws SQLException {
                if(columns.isEmpty()){
                    ResultSetMetaData rsMetaData = rs.getMetaData();
                    for(int i = 1; i<=rsMetaData.getColumnCount(); i++){
                        String colName = rsMetaData.getColumnName(i);
                        QueryResultColumn column = new QueryResultColumn();
                        column.setField(rsMetaData.getColumnName(i));
                        String displayName = rsMetaData.getColumnLabel(i);
                        column.setHiveColumnLabel(displayName);
                        //remove the table name if it exists
                        displayName = StringUtils.substringAfterLast(displayName,".");
                        Integer count =0;
                        if(displayNameMap.containsKey(displayName)){
                            count =  displayNameMap.get(displayName);
                            count++;
                        }
                        displayNameMap.put(displayName,count);
                        column.setDisplayName(displayName + "" + (count > 0 ? count : ""));


                        //HiveResultSetMetaData object doesnt support accesss to the rsMetadata.getSchemaName or getTableName())
                        //    column.setDatabaseName(rsMetaData.getSchemaName(i));
                        column.setTableName(StringUtils.substringAfterLast(rsMetaData.getColumnName(i),"."));
                        column.setDataType(Field.sqlTypeToDataType(rsMetaData.getColumnType(i)));
                        columns.add(column);
                    }
                    queryResult.setColumns(columns);
                }
                Map<String,Object> row = new LinkedHashMap<>();
                for(QueryResultColumn column: columns){
                    row.put(column.getDisplayName(), rs.getObject(column.getHiveColumnLabel()));
                }
                queryResult.addRow(row);
                return row;
            }
        });


        System.out.println("Return "+queryResult.getRows().size());
        return queryResult;

    }


    public class SchemaTable {
        private String schema;
        private String table;

        public SchemaTable(String schema, String table) {
            this.schema = schema;
            this.table = table;
        }

        public String getSchema() {
            return schema;
        }

        public void setSchema(String schema) {
            this.schema = schema;
        }

        public String getTable() {
            return table;
        }

        public void setTable(String table) {
            this.table = table;
        }
    }

}