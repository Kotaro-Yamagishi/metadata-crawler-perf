# metadata-crawler-perf

Java 21 + Spring Boot 3 で実装する、企業内 DB メタデータ同期クローラーの段階的パフォーマンスチューニング再現リポジトリ。

## 概要

複数の MySQL データベース（サブシステム単位）の `information_schema` をクロールし、メタデータ（スキーマ・テーブル・カラム・FK・インデックス）を H2 のカタログ DB に同期する。

Step 0（ナイーブ実装）から Step 5（非同期 + Webhook）まで、段階的にチューニングを適用し、改善効果を計測する。

## 技術スタック

- Java 21
- Spring Boot 3.3+
- Gradle Kotlin DSL
- H2 Database（カタログ DB）
- MySQL 8（ソース DB、Docker コンテナ）
- HikariCP
- Guava
- Spring Retry

## 動かし方

実装が進んだら追記。

## ステップ別ブランチ

| Branch | 内容 |
|--------|------|
| step-0-naive | ナイーブ実装（N+1 / 同期 / 小プール） |
| step-1-batch | IN句・JOIN で N+1 解消 |
| step-2-parallel | ExecutorService + CompletableFuture で並列化 |
| step-3-pool | HikariCP プールサイズ・タイムアウトチューニング |
| step-4-cache | Guava Cache + Visited Set でグラフ訪問抑制 |
| step-5-async | 非同期 API + Webhook + Spring Retry |
| main | 最終形（step-5-async と同等） |

## 計測結果

Step 0 完成後に Before/After 比較表を追記。
