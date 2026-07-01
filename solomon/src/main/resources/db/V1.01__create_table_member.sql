CREATE TABLE IF NOT EXISTS member (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    
    email VARCHAR(255) UNIQUE NOT NULL,
    picture VARCHAR(255) NOT NULL,
    role VARCHAR(255) NOT NULL,
    
    balance INT NOT NULL,
    last_free_awarded_at DATETIME NOT NULL,
    last_ad_awarded_at DATETIME NOT NULL,
    
    created_at DATETIME NOT NULL,
    updated_at DATETIME NOT NULL
);