package com.example.logindexing.indexer;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.stream.Collectors;

@Component
@Slf4j
public class InvertedIndex {
    
    // Term -> Set of Document IDs
    private final Map<String, Set<String>> index = new ConcurrentHashMap<>();
    
    // Document ID -> Terms (for deletion/update)
    private final Map<String, Set<String>> documentTerms = new ConcurrentHashMap<>();
    
    // Segment metadata
    private volatile int segmentVersion = 0;
    private volatile int documentsInSegment = 0;
    
    public void addDocument(String docId, String text) {
        Set<String> terms = tokenize(text);
        
        // Update inverted index
        for (String term : terms) {
            index.computeIfAbsent(term, k -> new CopyOnWriteArraySet<>())
                 .add(docId);
        }
        
        // Track document terms for updates/deletes
        documentTerms.put(docId, terms);
        documentsInSegment++;
        
        log.debug("Indexed document {} with {} terms", docId, terms.size());
    }
    
    public void removeDocument(String docId) {
        Set<String> terms = documentTerms.remove(docId);
        if (terms != null) {
            for (String term : terms) {
                Set<String> docIds = index.get(term);
                if (docIds != null) {
                    docIds.remove(docId);
                    if (docIds.isEmpty()) {
                        index.remove(term);
                    }
                }
            }
            documentsInSegment--;
        }
    }
    
    public Set<String> search(String query) {
        Set<String> terms = tokenize(query);
        
        if (terms.isEmpty()) {
            return Collections.emptySet();
        }
        
        // Get posting lists for all terms
        List<Set<String>> postingLists = terms.stream()
            .map(term -> index.getOrDefault(term, Collections.emptySet()))
            .collect(Collectors.toList());
        
        // Intersect all posting lists
        if (postingLists.isEmpty()) {
            return Collections.emptySet();
        }
        
        Set<String> result = new HashSet<>(postingLists.get(0));
        for (int i = 1; i < postingLists.size(); i++) {
            result.retainAll(postingLists.get(i));
        }
        
        log.debug("Search for '{}' returned {} documents", query, result.size());
        return result;
    }
    
    public int getDocumentCount() {
        return documentsInSegment;
    }
    
    public int getTermCount() {
        return index.size();
    }
    
    public int getSegmentVersion() {
        return segmentVersion;
    }
    
    public void incrementSegmentVersion() {
        segmentVersion++;
    }
    
    public void clear() {
        index.clear();
        documentTerms.clear();
        documentsInSegment = 0;
        log.info("Cleared inverted index");
    }
    
    private Set<String> tokenize(String text) {
        if (text == null || text.isBlank()) {
            return Collections.emptySet();
        }
        
        // Simple tokenization: lowercase, split on whitespace and punctuation
        return Arrays.stream(text.toLowerCase()
                    .split("[\\s.,;:!?()\\[\\]{}\"']+"))
                .filter(token -> !token.isEmpty() && token.length() > 2)
                .collect(Collectors.toSet());
    }
}
