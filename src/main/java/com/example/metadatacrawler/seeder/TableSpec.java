package com.example.metadatacrawler.seeder;

import java.util.List;

/**
 * テーブル全体のスペックを表すデータクラス。
 * SchemaGenerator が生成し、DdlBuilder が DDL に変換する。
 *
 * @param tableName    テーブル名（スキーマ修飾なし）
 * @param columns      カラム一覧（id/created_at/updated_at を含む全カラム）
 * @param foreignKeys  このテーブルが持つ外部キー一覧
 * @param indexes      このテーブルが持つインデックス一覧（PKを除く）
 * @param tableComment テーブルコメント
 */
public record TableSpec(
        String tableName,
        List<ColumnSpec> columns,
        List<ForeignKeySpec> foreignKeys,
        List<IndexSpec> indexes,
        String tableComment
) {
}
