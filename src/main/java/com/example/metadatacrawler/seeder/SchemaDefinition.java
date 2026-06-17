package com.example.metadatacrawler.seeder;

import java.util.List;

/**
 * 1スキーマの生成仕様を表すデータクラス。
 *
 * @param schemaName       MySQLデータベース名（例: "user_mgmt"）
 * @param targetTableCount 生成するテーブル総数
 * @param rootTables       ベーステーブル名のリスト（必ずこの名前で先に作成される）
 * @param domainColumns    このスキーマ固有のドメインカラム名候補
 */
public record SchemaDefinition(
        String schemaName,
        int targetTableCount,
        List<String> rootTables,
        List<String> domainColumns
) {
}
