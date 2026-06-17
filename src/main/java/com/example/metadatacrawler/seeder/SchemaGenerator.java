package com.example.metadatacrawler.seeder;

import java.util.*;

/**
 * SchemaDefinition から TableSpec のリストを生成するクラス。
 *
 * <p>生成アルゴリズム:
 * <ol>
 *   <li>rootTables をベーステーブルとして先頭に配置</li>
 *   <li>targetTableCount に満たない場合は接尾辞バリエーションで補完</li>
 *   <li>各テーブルに共通カラム + ドメインカラム(5〜25個)を付与</li>
 *   <li>特徴テーブル: FK多め(最初5個), カラム多め(末尾3個)</li>
 *   <li>自己参照FK・相互参照FKを仕込む</li>
 * </ol>
 *
 * <p>再現性のため Random は固定シード(42L)を使用する。
 */
public final class SchemaGenerator {

    private SchemaGenerator() {
    }

    private static final List<String> SUFFIXES = List.of(
            "_history", "_audit", "_log", "_archive",
            "_v2", "_2024", "_temp", "_backup"
    );

    /** 自己参照FKを張るカラム名パターン */
    private static final Set<String> SELF_REF_COLUMNS = Set.of(
            "parent_id", "parent_task_id", "parent_category_id", "manager_id", "reply_to_id"
    );

    /** FK多めテーブルの件数（スキーマ先頭N個） */
    private static final int FK_HEAVY_COUNT = 5;

    /** カラム多めテーブルの件数（ベーステーブルの末尾N個） */
    private static final int COLUMN_HEAVY_COUNT = 3;

    /** FK多めテーブルのFKカラム数 */
    private static final int FK_HEAVY_MIN = 8;
    private static final int FK_HEAVY_MAX = 15;

    /** カラム多めテーブルのカラム数 */
    private static final int COLUMN_HEAVY_MIN = 80;
    private static final int COLUMN_HEAVY_MAX = 120;

    /** 通常テーブルのドメインカラム数 */
    private static final int DOMAIN_COL_MIN = 5;
    private static final int DOMAIN_COL_MAX = 25;

    public static List<TableSpec> generate(SchemaDefinition def) {
        Random rng = new Random(42L);

        // --- 1. テーブル名リストを決定 ---
        List<String> tableNames = buildTableNames(def, rng);

        // カラム多めにするテーブルのインデックスを決定
        // ベーステーブルの末尾3個をカラム多めにする（ベーステーブルが3未満の場合は全部）
        int rootSize = def.rootTables().size();
        int colHeavyStart = Math.max(0, rootSize - COLUMN_HEAVY_COUNT);
        Set<Integer> colHeavyIndexes = new HashSet<>();
        for (int i = colHeavyStart; i < rootSize && colHeavyIndexes.size() < COLUMN_HEAVY_COUNT; i++) {
            colHeavyIndexes.add(i);
        }

        // --- 2. 各テーブルのColumnSpecを生成（FKは後で追加） ---
        List<TableSpec> tables = new ArrayList<>();
        for (int i = 0; i < tableNames.size(); i++) {
            String tableName = tableNames.get(i);
            boolean isColHeavy = colHeavyIndexes.contains(i);
            List<ColumnSpec> columns = buildColumns(tableName, def.domainColumns(), isColHeavy, rng);
            // foreignKeys/indexes は後で埋める
            tables.add(new TableSpec(tableName, columns, new ArrayList<>(), new ArrayList<>(), buildComment(tableName)));
        }

        // --- 3. FK生成 ---
        applyForeignKeys(tables, def, rng);

        // --- 4. 相互参照FK（特定スキーマのみ） ---
        applyMutualForeignKeys(tables, def.schemaName());

        // --- 5. インデックス生成 ---
        applyIndexes(tables, rng);

        return tables;
    }

    // ---- テーブル名生成 ----

    private static List<String> buildTableNames(SchemaDefinition def, Random rng) {
        List<String> names = new ArrayList<>(def.rootTables());
        Set<String> used = new HashSet<>(names);

        outer:
        for (String root : def.rootTables()) {
            for (String suffix : SUFFIXES) {
                if (names.size() >= def.targetTableCount()) break outer;
                String candidate = root + suffix;
                if (!used.contains(candidate)) {
                    names.add(candidate);
                    used.add(candidate);
                }
            }
        }

        // まだ足りない場合: rootTables を周回して別接尾辞の組み合わせを試みる
        // 接尾辞が尽きたら _extra_N で補完
        if (names.size() < def.targetTableCount()) {
            int extra = 1;
            while (names.size() < def.targetTableCount()) {
                String candidate = def.rootTables().get(extra % def.rootTables().size()) + "_extra_" + extra;
                if (!used.contains(candidate)) {
                    names.add(candidate);
                    used.add(candidate);
                }
                extra++;
            }
        }

        return names;
    }

    // ---- カラム生成 ----

    private static List<ColumnSpec> buildColumns(
            String tableName,
            List<String> domainColumns,
            boolean isColHeavy,
            Random rng
    ) {
        List<ColumnSpec> cols = new ArrayList<>();
        int position = 1;

        // 共通カラム(必須)
        cols.add(new ColumnSpec("id", "BIGINT NOT NULL AUTO_INCREMENT", false, "Primary key", true, position++));
        cols.add(new ColumnSpec("created_at", "TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP", false, "作成日時", false, position++));
        cols.add(new ColumnSpec("updated_at", "TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP", false, "更新日時", false, position++));

        // ドメインカラム選択数
        int domainCount;
        if (isColHeavy) {
            domainCount = COLUMN_HEAVY_MIN + rng.nextInt(COLUMN_HEAVY_MAX - COLUMN_HEAVY_MIN + 1);
        } else {
            domainCount = DOMAIN_COL_MIN + rng.nextInt(DOMAIN_COL_MAX - DOMAIN_COL_MIN + 1);
        }

        // domainColumns をシャッフルして選ぶ（足りない場合は繰り返し利用し番号付け）
        List<String> shuffled = new ArrayList<>(domainColumns);
        Collections.shuffle(shuffled, rng);

        Set<String> usedColNames = new HashSet<>();
        // 既存の共通カラム名を登録
        usedColNames.add("id");
        usedColNames.add("created_at");
        usedColNames.add("updated_at");

        int added = 0;
        int repeatCount = 1;
        while (added < domainCount) {
            for (String colName : shuffled) {
                if (added >= domainCount) break;
                String finalName = usedColNames.contains(colName) ? colName + "_" + repeatCount : colName;
                // 同名が再度重複する可能性があるのでループで一意化
                while (usedColNames.contains(finalName)) {
                    repeatCount++;
                    finalName = colName + "_" + repeatCount;
                }
                usedColNames.add(finalName);
                boolean nullable = rng.nextBoolean(); // 半数をNULLABLE
                cols.add(new ColumnSpec(
                        finalName,
                        inferDataType(finalName),
                        nullable,
                        "",
                        false,
                        position++
                ));
                added++;
            }
            repeatCount++;
            // 次の周回でシャッフルし直す（同じ順序の繰り返しを避ける）
            Collections.shuffle(shuffled, rng);
        }

        return cols;
    }

    /**
     * カラム名のヒューリスティクスでMySQLデータ型を推定する。
     */
    static String inferDataType(String colName) {
        if (colName.equals("id") || colName.endsWith("_id")) {
            return "BIGINT";
        }
        if (colName.endsWith("_at") || colName.endsWith("_date")) {
            return "TIMESTAMP";
        }
        if (colName.startsWith("is_") || colName.endsWith("_enabled")
                || colName.endsWith("_active") || colName.endsWith("_verified")
                || colName.endsWith("_completed") || colName.endsWith("_archived")) {
            return "BOOLEAN";
        }
        if (colName.endsWith("_count") || colName.endsWith("_quantity")
                || colName.endsWith("_score") || colName.endsWith("_days")
                || colName.endsWith("_position") || colName.endsWith("_level")) {
            return "INT";
        }
        if (colName.endsWith("_amount") || colName.endsWith("_value")
                || colName.endsWith("_price") || colName.endsWith("_rate")
                || colName.endsWith("_revenue") || colName.endsWith("_salary")
                || colName.endsWith("_threshold") || colName.endsWith("_percent")) {
            return "DECIMAL(10,2)";
        }
        if (colName.endsWith("_json")) {
            return "JSON";
        }
        if (colName.endsWith("_html") || colName.endsWith("_text")
                || colName.endsWith("_body") || colName.endsWith("_description")) {
            return "TEXT";
        }
        if (colName.endsWith("_code")) {
            return "VARCHAR(64)";
        }
        return "VARCHAR(255)";
    }

    // ---- FK生成 ----

    private static void applyForeignKeys(List<TableSpec> tables, SchemaDefinition def, Random rng) {
        // テーブル名→インデックスのマップ（参照先テーブルの存在確認用）
        Map<String, Integer> tableIndex = new HashMap<>();
        for (int i = 0; i < tables.size(); i++) {
            tableIndex.put(tables.get(i).tableName(), i);
        }

        for (int i = 0; i < tables.size(); i++) {
            TableSpec table = tables.get(i);
            List<ForeignKeySpec> fks = (List<ForeignKeySpec>) table.foreignKeys();
            List<ColumnSpec> columns = table.columns();

            // A. FK多めテーブル: 先頭5個は 8〜15 個のFKを持つ
            if (i < FK_HEAVY_COUNT) {
                int fkCount = FK_HEAVY_MIN + rng.nextInt(FK_HEAVY_MAX - FK_HEAVY_MIN + 1);
                for (int f = 0; f < fkCount; f++) {
                    // 参照先テーブルをランダム選択（自分以外）
                    String refTable = pickRandomTable(tables, table.tableName(), rng);
                    if (refTable == null) break;
                    String fkColName = refTable.replaceAll("[^a-zA-Z0-9]", "_") + "_ref_" + f + "_id";
                    String constraintName = "fk_" + abbreviate(table.tableName()) + "_" + abbreviate(refTable) + "_" + f;
                    fks.add(new ForeignKeySpec(constraintName, fkColName, refTable, "id"));
                    // 対応するカラムをテーブルに追加
                    columns.add(new ColumnSpec(fkColName, "BIGINT", true, "FK ref to " + refTable, false, columns.size() + 1));
                }
            }

            // B. 通常の _id カラムにFKを張る（同スキーマ内テーブルへ）
            for (ColumnSpec col : List.copyOf(columns)) {
                if (!col.name().endsWith("_id") || col.isPrimaryKey()) continue;
                // 既にFKが登録されているカラムはスキップ
                boolean alreadyHasFk = fks.stream().anyMatch(fk -> fk.fromColumn().equals(col.name()));
                if (alreadyHasFk) continue;

                // C. 自己参照FK
                if (SELF_REF_COLUMNS.contains(col.name())) {
                    String constraintName = "fk_" + abbreviate(table.tableName()) + "_self_" + col.name();
                    fks.add(new ForeignKeySpec(constraintName, col.name(), table.tableName(), "id"));
                    continue;
                }

                // カラム名からテーブル名を推定（"user_id" → "users", "assignee_id" → テーブル候補から探す）
                String refTable = resolveRefTable(col.name(), tables, table.tableName(), rng);
                if (refTable != null) {
                    String constraintName = "fk_" + abbreviate(table.tableName()) + "_" + abbreviate(col.name().replace("_id", ""));
                    fks.add(new ForeignKeySpec(constraintName, col.name(), refTable, "id"));
                }
            }
        }
    }

    /**
     * カラム名から参照先テーブルを推定する。
     * "user_id" → "users", "assignee_id" → 候補から探す。
     */
    private static String resolveRefTable(String colName, List<TableSpec> tables, String selfTable, Random rng) {
        // コアワード抽出: "assignee_id" → "assignee"
        String core = colName.substring(0, colName.length() - 3); // "_id" を除く

        // 完全一致（複数形）
        for (TableSpec t : tables) {
            if (t.tableName().equals(selfTable)) continue;
            if (t.tableName().equals(core) || t.tableName().equals(core + "s")
                    || t.tableName().equals(core + "es")) {
                return t.tableName();
            }
        }

        // 前方一致（"project" → "projects", "project_members" 等）
        for (TableSpec t : tables) {
            if (t.tableName().equals(selfTable)) continue;
            if (t.tableName().startsWith(core)) {
                return t.tableName();
            }
        }

        // 見つからなければランダム選択（50%の確率でFKを張らない）
        if (rng.nextBoolean()) {
            return pickRandomTable(tables, selfTable, rng);
        }
        return null;
    }

    /**
     * 仕様書で指定された相互参照FK。特定スキーマにのみ適用する。
     */
    private static void applyMutualForeignKeys(List<TableSpec> tables, String schemaName) {
        Map<String, TableSpec> byName = new HashMap<>();
        for (TableSpec t : tables) {
            byName.put(t.tableName(), t);
        }

        switch (schemaName) {
            case "billing" -> {
                // payments.refund_id <-> refunds.payment_id
                TableSpec payments = byName.get("payments");
                TableSpec refunds = byName.get("refunds");
                if (payments != null && refunds != null) {
                    addColumnIfAbsent(payments, "refund_id", "BIGINT");
                    addFkIfAbsent(payments, "fk_payments_refund", "refund_id", "refunds", "id");
                    addColumnIfAbsent(refunds, "payment_id", "BIGINT");
                    addFkIfAbsent(refunds, "fk_refunds_payment", "payment_id", "payments", "id");
                }
            }
            case "project_mgmt" -> {
                // tasks.depends_on_task_id <-> subtasks.parent_task_id
                TableSpec tasks = byName.get("tasks");
                TableSpec subtasks = byName.get("subtasks");
                if (tasks != null && subtasks != null) {
                    addColumnIfAbsent(tasks, "depends_on_task_id", "BIGINT");
                    addFkIfAbsent(tasks, "fk_tasks_depends_on", "depends_on_task_id", "subtasks", "id");
                    addColumnIfAbsent(subtasks, "parent_task_id", "BIGINT");
                    addFkIfAbsent(subtasks, "fk_subtasks_parent_task", "parent_task_id", "tasks", "id");
                }
            }
            case "user_mgmt" -> {
                // users.last_session_id <-> sessions.user_id
                TableSpec users = byName.get("users");
                TableSpec sessions = byName.get("sessions");
                if (users != null && sessions != null) {
                    addColumnIfAbsent(users, "last_session_id", "BIGINT");
                    addFkIfAbsent(users, "fk_users_last_session", "last_session_id", "sessions", "id");
                    addColumnIfAbsent(sessions, "user_id", "BIGINT");
                    addFkIfAbsent(sessions, "fk_sessions_user", "user_id", "users", "id");
                }
            }
        }
    }

    private static void addColumnIfAbsent(TableSpec table, String colName, String dataType) {
        boolean exists = table.columns().stream().anyMatch(c -> c.name().equals(colName));
        if (!exists) {
            table.columns().add(new ColumnSpec(colName, dataType, true, "", false, table.columns().size() + 1));
        }
    }

    private static void addFkIfAbsent(TableSpec table, String constraintName, String fromCol, String toTable, String toCol) {
        boolean exists = table.foreignKeys().stream().anyMatch(fk -> fk.constraintName().equals(constraintName));
        if (!exists) {
            ((List<ForeignKeySpec>) table.foreignKeys()).add(new ForeignKeySpec(constraintName, fromCol, toTable, toCol));
        }
    }

    // ---- インデックス生成 ----

    private static void applyIndexes(List<TableSpec> tables, Random rng) {
        // ユニークインデックスを張りやすいカラム名
        Set<String> uniqueCandidates = Set.of("email", "username", "sku", "barcode",
                "invoice_number", "employee_number", "coupon_code", "api_key_value",
                "session_token", "access_token");

        for (TableSpec table : tables) {
            List<IndexSpec> indexes = (List<IndexSpec>) table.indexes();
            Set<String> indexedCols = new HashSet<>();

            // FKカラムには必ずインデックス
            for (ForeignKeySpec fk : table.foreignKeys()) {
                if (!indexedCols.contains(fk.fromColumn())) {
                    String idxName = "idx_" + abbreviate(table.tableName()) + "_" + abbreviate(fk.fromColumn());
                    indexes.add(new IndexSpec(idxName, List.of(fk.fromColumn()), false));
                    indexedCols.add(fk.fromColumn());
                }
            }

            // ユニークインデックス候補を探す（0〜1個）
            if (rng.nextBoolean()) {
                for (ColumnSpec col : table.columns()) {
                    if (uniqueCandidates.contains(col.name()) && !indexedCols.contains(col.name())) {
                        String idxName = "uk_" + abbreviate(table.tableName()) + "_" + col.name();
                        indexes.add(new IndexSpec(idxName, List.of(col.name()), true));
                        indexedCols.add(col.name());
                        break; // 1個だけ
                    }
                }
            }

            // 追加インデックス（全体で1〜3個になるよう調整）
            int targetIdx = 1 + rng.nextInt(3); // 1〜3
            for (ColumnSpec col : table.columns()) {
                if (indexes.size() >= targetIdx) break;
                if (col.isPrimaryKey() || indexedCols.contains(col.name())) continue;
                // TIMESTAMP, TEXT, JSON はインデックス対象外
                if (col.dataType().startsWith("TEXT") || col.dataType().equals("JSON")) continue;
                String idxName = "idx_" + abbreviate(table.tableName()) + "_" + col.name();
                indexes.add(new IndexSpec(idxName, List.of(col.name()), false));
                indexedCols.add(col.name());
            }
        }
    }

    // ---- ユーティリティ ----

    private static String pickRandomTable(List<TableSpec> tables, String excludeTable, Random rng) {
        List<String> candidates = tables.stream()
                .map(TableSpec::tableName)
                .filter(n -> !n.equals(excludeTable))
                .toList();
        if (candidates.isEmpty()) return null;
        return candidates.get(rng.nextInt(candidates.size()));
    }

    /**
     * インデックス名・FK名が長くなりすぎないよう、テーブル名・カラム名を短縮する。
     * MySQL の識別子は64文字まで。
     */
    static String abbreviate(String name) {
        // 先頭16文字まで、アンダースコア区切りで短縮
        if (name.length() <= 20) return name;
        String[] parts = name.split("_");
        StringBuilder sb = new StringBuilder();
        for (String part : parts) {
            if (part.isEmpty()) continue;
            sb.append(part.charAt(0));
        }
        // 短縮後も長ければ先頭16文字でカット
        String abbr = sb.toString();
        if (abbr.length() > 16) abbr = abbr.substring(0, 16);
        return abbr;
    }

    private static String buildComment(String tableName) {
        // 接尾辞からコメントを推定
        if (tableName.endsWith("_history")) return tableName + " の変更履歴";
        if (tableName.endsWith("_audit")) return tableName + " の監査ログ";
        if (tableName.endsWith("_log")) return tableName + " のログ";
        if (tableName.endsWith("_archive")) return tableName + " のアーカイブ";
        if (tableName.endsWith("_backup")) return tableName + " のバックアップ";
        if (tableName.endsWith("_temp")) return tableName + " の一時データ";
        return tableName;
    }
}
