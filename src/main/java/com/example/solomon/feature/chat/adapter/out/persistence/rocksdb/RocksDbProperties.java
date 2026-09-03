package com.example.solomon.feature.chat.adapter.out.persistence.rocksdb;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "rocksdb")
public record RocksDbProperties(String path) {
}
