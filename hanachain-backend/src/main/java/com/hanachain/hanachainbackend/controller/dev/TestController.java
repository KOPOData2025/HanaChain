package com.hanachain.hanachainbackend.controller.dev;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/test")
public class TestController {

    @Autowired
    private DataSource dataSource;

    @GetMapping("/tables")
    public ResponseEntity<?> getTables() {
        try (Connection connection = dataSource.getConnection()) {
            DatabaseMetaData metaData = connection.getMetaData();
            ResultSet tables = metaData.getTables(null, null, "%", new String[]{"TABLE"});
            
            List<String> tableNames = new ArrayList<>();
            while (tables.next()) {
                String tableName = tables.getString("TABLE_NAME");
                tableNames.add(tableName);
            }
            
            return ResponseEntity.ok().body(tableNames);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body("Error: " + e.getMessage());
        }
    }

    @GetMapping("/sequences")
    public ResponseEntity<?> getSequences() {
        try (Connection connection = dataSource.getConnection()) {
            Statement statement = connection.createStatement();
            ResultSet resultSet = statement.executeQuery(
                "SELECT sequence_name FROM user_sequences ORDER BY sequence_name"
            );
            
            List<String> sequenceNames = new ArrayList<>();
            while (resultSet.next()) {
                sequenceNames.add(resultSet.getString("sequence_name"));
            }
            
            return ResponseEntity.ok().body(sequenceNames);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body("Error: " + e.getMessage());
        }
    }

    @GetMapping("/check-organization")
    public ResponseEntity<?> checkOrganizationTable() {
        try (Connection connection = dataSource.getConnection()) {
            Statement statement = connection.createStatement();
            
            // 테이블 존재 확인
            ResultSet tables = statement.executeQuery(
                "SELECT table_name FROM user_tables WHERE table_name = 'ORGANIZATIONS'"
            );
            
            boolean tableExists = tables.next();
            
            if (tableExists) {
                // 테이블이 존재하면 레코드 수 확인
                ResultSet countResult = statement.executeQuery(
                    "SELECT COUNT(*) as count FROM organizations"
                );
                int count = 0;
                if (countResult.next()) {
                    count = countResult.getInt("count");
                }
                
                return ResponseEntity.ok().body(Map.of(
                    "tableExists", true,
                    "recordCount", count
                ));
            } else {
                return ResponseEntity.ok().body(Map.of(
                    "tableExists", false,
                    "recordCount", 0
                ));
            }
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body("Error: " + e.getMessage());
        }
    }
}
