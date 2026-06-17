package com.example.metadatacrawler.seeder;

/**
 * TableSpec から DDL 文字列を生成するビルダークラス。
 *
 * <p>MySQL 向け:
 * <ul>
 *   <li>{@link #buildCreateTable} — CREATE TABLE（FK なし）</li>
 *   <li>{@link #buildCreateIndex} — CREATE [UNIQUE] INDEX</li>
 *   <li>{@link #buildAddForeignKey} — ALTER TABLE ADD CONSTRAINT FOREIGN KEY</li>
 * </ul>
 *
 * <p>テーブル名・スキーマ名はバッククォートでエスケープする。
 */
public final class DdlBuilder {

    private DdlBuilder() {
    }

    /**
     * CREATE TABLE 文を生成する。FK は含まない。
     *
     * @param schemaName スキーマ名
     * @param table      テーブルスペック
     * @return CREATE TABLE SQL 文字列
     */
    public static String buildCreateTable(String schemaName, TableSpec table) {
        StringBuilder sb = new StringBuilder();
        sb.append("CREATE TABLE `").append(schemaName).append("`.`").append(table.tableName()).append("` (\n");

        for (int i = 0; i < table.columns().size(); i++) {
            ColumnSpec col = table.columns().get(i);
            sb.append("  ").append(buildColumnDef(col));
            // 最後のカラムにカンマを付けない
            if (i < table.columns().size() - 1) {
                sb.append(",");
            }
            sb.append("\n");
        }

        sb.append(")");

        if (table.tableComment() != null && !table.tableComment().isBlank()) {
            sb.append(" COMMENT '").append(escapeComment(table.tableComment())).append("'");
        }

        sb.append(" ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci");
        sb.append(";");

        return sb.toString();
    }

    /**
     * CREATE [UNIQUE] INDEX 文を生成する。
     *
     * @param schemaName スキーマ名
     * @param tableName  テーブル名
     * @param index      インデックススペック
     * @return CREATE INDEX SQL 文字列
     */
    public static String buildCreateIndex(String schemaName, String tableName, IndexSpec index) {
        StringBuilder sb = new StringBuilder();
        if (index.unique()) {
            sb.append("CREATE UNIQUE INDEX");
        } else {
            sb.append("CREATE INDEX");
        }
        sb.append(" `").append(index.indexName()).append("`");
        sb.append(" ON `").append(schemaName).append("`.`").append(tableName).append("` (");

        for (int i = 0; i < index.columnNames().size(); i++) {
            sb.append("`").append(index.columnNames().get(i)).append("`");
            if (i < index.columnNames().size() - 1) {
                sb.append(", ");
            }
        }

        sb.append(");");
        return sb.toString();
    }

    /**
     * ALTER TABLE ADD CONSTRAINT FOREIGN KEY 文を生成する。
     *
     * @param schemaName スキーマ名
     * @param tableName  参照元テーブル名
     * @param fk         外部キースペック
     * @return ALTER TABLE SQL 文字列
     */
    public static String buildAddForeignKey(String schemaName, String tableName, ForeignKeySpec fk) {
        return "ALTER TABLE `" + schemaName + "`.`" + tableName + "`" +
                " ADD CONSTRAINT `" + fk.constraintName() + "`" +
                " FOREIGN KEY (`" + fk.fromColumn() + "`)" +
                " REFERENCES `" + schemaName + "`.`" + fk.toTable() + "` (`" + fk.toColumn() + "`)" +
                " ON DELETE SET NULL ON UPDATE CASCADE;";
    }

    // ---- 内部ヘルパー ----

    private static String buildColumnDef(ColumnSpec col) {
        StringBuilder sb = new StringBuilder();
        sb.append("`").append(col.name()).append("` ");
        sb.append(col.dataType());

        if (col.isPrimaryKey()) {
            sb.append(" PRIMARY KEY");
        } else {
            // dataType に NOT NULL / NULL 指示が既に含まれている場合はそちらを優先
            // （共通カラムは dataType 内に NOT NULL DEFAULT を含んでいる）
            boolean hasNullabilityInType = col.dataType().contains("NOT NULL")
                    || col.dataType().contains("DEFAULT");
            if (!hasNullabilityInType) {
                sb.append(col.nullable() ? " NULL" : " NOT NULL");
            }
        }

        if (col.comment() != null && !col.comment().isBlank()) {
            sb.append(" COMMENT '").append(escapeComment(col.comment())).append("'");
        }

        return sb.toString();
    }

    /** コメント文字列内のシングルクォートをエスケープする。 */
    private static String escapeComment(String comment) {
        return comment.replace("'", "\\'");
    }
}
