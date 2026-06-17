package com.example.metadatacrawler.seeder;

import java.util.List;

/**
 * インデックスを表すデータクラス。
 *
 * @param indexName   インデックス名（例: "idx_users_email"）
 * @param columnNames 対象カラム名のリスト（複合インデックスも対応）
 * @param unique      true=UNIQUE INDEX
 */
public record IndexSpec(
        String indexName,
        List<String> columnNames,
        boolean unique
) {
}
