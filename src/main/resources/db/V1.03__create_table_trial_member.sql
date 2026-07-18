CREATE TABLE IF NOT EXISTS trial_member (
    trial_id BINARY(16) NOT NULL,
    member_id BIGINT NOT NULL,
    nickname VARCHAR(255) NOT NULL,
    turn VARCHAR(255) NOT NULL,

    read_seq INT,
    
    created_at DATETIME NOT NULL,
    updated_at DATETIME NOT NULL,
    
    PRIMARY KEY (trial_id, member_id),

    INDEX idx_member_id (member_id),

    CONSTRAINT fk_trial_member_trial FOREIGN KEY (trial_id) REFERENCES trial (id) ON DELETE CASCADE,
    CONSTRAINT fk_trial_member_member FOREIGN KEY (member_id) REFERENCES member (id) ON DELETE CASCADE
);