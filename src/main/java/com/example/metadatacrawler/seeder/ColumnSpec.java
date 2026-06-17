package com.example.metadatacrawler.seeder;

/**
 * テーブルの1カラムを表すデータクラス。
 *
 * @param name            カラム名
 * @param dataType        MySQLデータ型文字列（例: "VARCHAR(255)", "BIGINT", "TIMESTAMP" 等）
 * @param nullable        true=NULL許容, false=NOT NULL
 * @param comment         カラムコメント（空文字も可）
 * @param isPrimaryKey    PRIMARY KEY かどうか
 * @param ordinalPosition CREATE TABLE 内の順序（1始まり）
 */
public record ColumnSpec(
        String name,
        String dataType,
        boolean nullable,
        String comment,
        boolean isPrimaryKey,
        int ordinalPosition
) {
}
