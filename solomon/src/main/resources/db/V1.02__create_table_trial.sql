CREATE TABLE IF NOT EXISTS trial (
    id BINARY(16) PRIMARY KEY,
    issue_title VARCHAR(255) NOT NULL,
    stage VARCHAR(255) NOT NULL,

    last_message TEXT,
    last_message_seq INT,

    created_at DATETIME NOT NULL,
    updated_at DATETIME NOT NULL,

    INDEX idx_updated_at (updated_at DESC)
);