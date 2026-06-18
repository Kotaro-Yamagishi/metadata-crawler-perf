package com.example.metadatacrawler.seeder;

import java.util.List;

/**
 * 8スキーマ全ての定義を保持するカタログクラス。
 * rootTables と domainColumns は仕様書から固定値として設定。
 */
public final class SchemaDefinitions {

    private SchemaDefinitions() {
    }

    public static final List<SchemaDefinition> ALL = List.of(

            new SchemaDefinition(
                    "user_mgmt",
                    150,
                    List.of(
                            "users", "user_profiles", "roles", "permissions", "role_permissions",
                            "user_roles", "sessions", "login_attempts", "password_resets",
                            "two_factor_secrets", "api_tokens"
                    ),
                    List.of(
                            "email", "username", "password_hash", "first_name", "last_name",
                            "phone_number", "avatar_url", "locale", "timezone", "role_name",
                            "permission_key", "scope", "last_login_at", "is_active",
                            "email_verified_at", "two_factor_enabled", "ip_address",
                            "user_agent", "session_token", "expires_at"
                    )
            ),

            new SchemaDefinition(
                    "project_mgmt",
                    250,
                    List.of(
                            "projects", "project_members", "tasks", "subtasks", "milestones",
                            "comments", "attachments", "task_assignments", "task_dependencies",
                            "labels", "project_labels", "task_labels", "boards", "lists",
                            "cards", "checklists", "checklist_items", "activities",
                            "notifications", "watchers"
                    ),
                    List.of(
                            "title", "description", "status", "priority", "due_date",
                            "estimated_hours", "actual_hours", "assignee_id", "reporter_id",
                            "tag_name", "milestone_name", "board_name", "position",
                            "color_code", "is_completed", "completed_at", "is_archived",
                            "parent_task_id", "depends_on_task_id", "mentioned_user_id"
                    )
            ),

            new SchemaDefinition(
                    "crm",
                    160,
                    List.of(
                            "customers", "customer_addresses", "customer_contacts", "leads",
                            "opportunities", "deals", "activities", "campaigns",
                            "campaign_members", "email_templates", "segments", "tags",
                            "notes", "tasks", "meetings"
                    ),
                    List.of(
                            "company_name", "industry", "annual_revenue", "employee_count",
                            "lead_source", "lead_status", "opportunity_stage", "deal_value",
                            "close_date", "win_probability", "campaign_name", "campaign_type",
                            "subject", "body_html", "sent_at", "opened_at", "clicked_at",
                            "segment_name", "lifecycle_stage", "contact_owner"
                    )
            ),

            new SchemaDefinition(
                    "billing",
                    130,
                    List.of(
                            "customers", "subscriptions", "subscription_items", "products",
                            "prices", "invoices", "invoice_items", "payments", "payment_methods",
                            "refunds", "credits", "tax_rates", "coupons", "discounts"
                    ),
                    List.of(
                            "product_name", "sku", "price_amount", "currency_code",
                            "billing_cycle", "subscription_status", "trial_end_date",
                            "next_billing_date", "invoice_number", "due_date", "paid_at",
                            "payment_status", "payment_method_type", "card_brand",
                            "card_last4", "refund_reason", "refund_amount", "tax_rate",
                            "coupon_code", "discount_percent"
                    )
            ),

            new SchemaDefinition(
                    "analytics",
                    170,
                    List.of(
                            "events", "event_properties", "sessions", "page_views",
                            "user_journeys", "funnels", "funnel_steps", "experiments",
                            "experiment_variants", "kpi_definitions", "kpi_values",
                            "reports", "dashboards", "alerts"
                    ),
                    List.of(
                            "event_name", "event_category", "event_value", "session_id",
                            "page_url", "referrer_url", "device_type", "browser_name",
                            "os_name", "country_code", "experiment_name", "variant_name",
                            "conversion_rate", "kpi_name", "kpi_value", "metric_name",
                            "dimension_name", "alert_threshold", "alert_triggered_at",
                            "report_name"
                    )
            ),

            new SchemaDefinition(
                    "integration",
                    110,
                    List.of(
                            "webhooks", "webhook_deliveries", "api_keys", "oauth_clients",
                            "oauth_tokens", "external_connections", "sync_jobs",
                            "sync_logs", "rate_limits"
                    ),
                    List.of(
                            "webhook_url", "secret_token", "event_type", "payload_json",
                            "delivery_status", "http_status_code", "response_body",
                            "api_key_value", "api_key_scopes", "client_id", "client_secret",
                            "redirect_uri", "access_token", "refresh_token", "external_id",
                            "sync_status", "last_synced_at", "rate_limit_per_minute",
                            "current_count", "reset_at"
                    )
            ),

            new SchemaDefinition(
                    "inventory",
                    180,
                    List.of(
                            "products", "product_variants", "warehouses", "locations",
                            "stocks", "stock_movements", "purchase_orders",
                            "purchase_order_items", "suppliers", "shipments", "shipment_items",
                            "stock_takes", "stock_take_items", "transfers", "transfer_items"
                    ),
                    List.of(
                            "sku", "barcode", "product_name", "variant_name",
                            "warehouse_name", "location_code", "quantity", "reserved_quantity",
                            "available_quantity", "reorder_point", "supplier_name",
                            "purchase_price", "movement_type", "movement_reason",
                            "shipment_tracking", "shipment_status", "stock_take_date",
                            "counted_quantity", "variance", "transfer_status"
                    )
            ),

            new SchemaDefinition(
                    "hr",
                    150,
                    List.of(
                            "employees", "departments", "positions", "employments",
                            "evaluations", "goals", "feedback", "skills", "employee_skills",
                            "certifications", "leaves", "attendances", "salaries",
                            "bonuses", "trainings", "training_enrollments"
                    ),
                    List.of(
                            "employee_number", "first_name", "last_name", "email",
                            "department_name", "position_title", "hire_date",
                            "employment_type", "manager_id", "evaluation_score",
                            "goal_description", "feedback_text", "skill_name",
                            "skill_level", "certification_name", "leave_type", "leave_days",
                            "attendance_status", "base_salary", "bonus_amount"
                    )
            )
    );
}
