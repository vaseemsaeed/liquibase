package liquibase.sqlgenerator;

import liquibase.database.*;
import liquibase.database.structure.Column;
import liquibase.database.structure.Table;
import liquibase.exception.ValidationErrors;
import liquibase.sql.Sql;
import liquibase.sql.UnparsedSql;
import liquibase.statement.AddColumnStatement;

public class AddColumnGeneratorDefaultClauseBeforeNotNull extends AddColumnGenerator {
    public int getPriority() {
        return PRIORITY_DATABASE;
    }

    public boolean supports(AddColumnStatement statement, Database database) {
        return database instanceof OracleDatabase
                || database instanceof HsqlDatabase
                || database instanceof DerbyDatabase
                || database instanceof DB2Database
                || database instanceof FirebirdDatabase
                || database instanceof InformixDatabase;
    }

    @Override
    public ValidationErrors validate(AddColumnStatement statement, Database database) {
        ValidationErrors validationErrors = super.validate(statement, database);
        if (database instanceof DerbyDatabase && statement.isAutoIncrement()) {
            validationErrors.addError("Cannot add an identity column to a database");
        }
        return validationErrors;
    }

    public Sql[] generateSql(AddColumnStatement statement, Database database) {
        String alterTable = "ALTER TABLE " + database.escapeTableName(statement.getSchemaName(), statement.getTableName()) + " ADD " + database.escapeColumnName(statement.getSchemaName(), statement.getTableName(), statement.getColumnName()) + " " + database.getColumnType(statement.getColumnType(), statement.isAutoIncrement());

        alterTable += getDefaultClause(statement, database);

        if (primaryKeyBeforeNotNull(database)) {
            if (statement.isPrimaryKey()) {
                alterTable += " PRIMARY KEY";
            }
        }

        if (statement.isAutoIncrement()) {
            alterTable += " " + database.getAutoIncrementClause();
        }

        if (!statement.isNullable()) {
            alterTable += " NOT NULL";
        }

        if (!primaryKeyBeforeNotNull(database)) {
            if (statement.isPrimaryKey()) {
                alterTable += " PRIMARY KEY";
            }
        }

        return new Sql[]{
                new UnparsedSql(alterTable, new Column()
                        .setTable(new Table(statement.getTableName()).setSchema(statement.getSchemaName()))
                        .setName(statement.getColumnName()))
        };
    }


    private String getDefaultClause(AddColumnStatement statement, Database database) {
        String clause = "";
        if (statement.getDefaultValue() != null) {
            clause += " DEFAULT " + database.convertJavaObjectToString(statement.getDefaultValue());
        }
        return clause;
    }

    private boolean primaryKeyBeforeNotNull(Database database) {
        return !(database instanceof HsqlDatabase || database instanceof H2Database);
    }


}