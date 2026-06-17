package com.example.metadatacrawler.seeder;

/**
 * 外部キー制約を表すデータクラス。
 * ALTER TABLE ADD CONSTRAINT で後から追加される。
 *
 * @param constraintName 制約名（例: "fk_user_roles_user"）
 * @param fromColumn     参照元カラム名
 * @param toTable        参照先テーブル名（スキーマ修飾なし）
 * @param toColumn       参照先カラム名（通常 "id"）
 */
public record ForeignKeySpec(
        String constraintName,
        String fromColumn,
        String toTable,
        String toColumn
) {
}
