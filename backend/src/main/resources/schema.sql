CREATE TABLE IF NOT EXISTS problems (
    slug TEXT PRIMARY KEY,
    title TEXT NOT NULL,
    difficulty TEXT,
    tags_json TEXT,
    topics_json TEXT
);

CREATE TABLE IF NOT EXISTS submissions (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    problem_slug TEXT NOT NULL,
    status TEXT NOT NULL,
    lang TEXT,
    submitted_at TEXT
);

CREATE TABLE IF NOT EXISTS problem_states (
    problem_slug TEXT PRIMARY KEY,
    mastery_level INTEGER NOT NULL DEFAULT 0,
    ac_count INTEGER NOT NULL DEFAULT 0,
    attempt_count INTEGER NOT NULL DEFAULT 0,
    is_mistake INTEGER NOT NULL DEFAULT 0,
    mistake_type TEXT,
    last_review_at TEXT,
    next_review_at TEXT,
    review_count INTEGER NOT NULL DEFAULT 0,
    first_ac_at TEXT
);

CREATE TABLE IF NOT EXISTS problem_lists (
    id TEXT PRIMARY KEY,
    name TEXT NOT NULL,
    source TEXT NOT NULL
);

CREATE TABLE IF NOT EXISTS problem_list_items (
    list_id TEXT NOT NULL,
    problem_slug TEXT NOT NULL,
    position INTEGER NOT NULL,
    PRIMARY KEY (list_id, problem_slug)
);

CREATE TABLE IF NOT EXISTS topics (
    id TEXT PRIMARY KEY,
    name TEXT NOT NULL,
    category TEXT
);

CREATE TABLE IF NOT EXISTS problem_topics (
    problem_slug TEXT NOT NULL,
    topic_id TEXT NOT NULL,
    PRIMARY KEY (problem_slug, topic_id)
);

CREATE TABLE IF NOT EXISTS mistakes (
    problem_slug TEXT PRIMARY KEY,
    error_type TEXT,
    stuck_point TEXT,
    lesson TEXT,
    similar_problems TEXT,
    created_at TEXT
);

CREATE TABLE IF NOT EXISTS reviews (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    problem_slug TEXT NOT NULL,
    reviewed_at TEXT,
    passed INTEGER,
    notes TEXT
);

CREATE TABLE IF NOT EXISTS daily_plans (
    plan_date TEXT PRIMARY KEY,
    core_tasks_json TEXT,
    bonus_tasks_json TEXT,
    completed INTEGER NOT NULL DEFAULT 0
);

CREATE TABLE IF NOT EXISTS user_goals (
    id INTEGER PRIMARY KEY CHECK (id = 1),
    active_list_id TEXT,
    target_type TEXT,
    target INTEGER
);

CREATE TABLE IF NOT EXISTS recommendations (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    problem_slug TEXT,
    reason TEXT,
    feedback TEXT,
    created_at TEXT
);

CREATE TABLE IF NOT EXISTS insights (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    type TEXT,
    generated_at TEXT,
    content_json TEXT
);

CREATE TABLE IF NOT EXISTS stuck_events (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    problem_slug TEXT,
    happened_at TEXT,
    source TEXT,
    hint_level INTEGER
);

CREATE TABLE IF NOT EXISTS notes (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    problem_slug TEXT UNIQUE,
    content TEXT,
    created_at TEXT,
    updated_at TEXT
);

CREATE TABLE IF NOT EXISTS app_settings (
    key TEXT PRIMARY KEY,
    value TEXT
);

CREATE TABLE IF NOT EXISTS problem_titles (
    slug TEXT PRIMARY KEY,
    title_cn TEXT
);
