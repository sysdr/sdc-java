package com.example.logindexing.search;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface LogDocumentRepository extends JpaRepository<LogDocument, String> {
}
