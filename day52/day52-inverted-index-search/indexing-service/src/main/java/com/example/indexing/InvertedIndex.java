package com.example.indexing;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentSkipListSet;
import java.util.stream.Collectors;

@Component
@Slf4j
public class InvertedIndex {
    
    private final Map<String, ConcurrentSkipListSet<Long>> index = new ConcurrentHashMap<>();
    private final Map<Long, Set<String>> documentTerms = new ConcurrentHashMap<>();
    
    private static final Set<String> STOPWORDS = Set.of(
        "the", "is", "at", "which", "on", "a", "an", "and", "or", "but",
        "in", "with", "to", "for", "of", "as", "by", "from", "that", "this"
    );
    
    private final Counter indexedDocsCounter;
    private final Counter indexedTermsCounter;
    
    public InvertedIndex(MeterRegistry meterRegistry) {
        this.indexedDocsCounter = Counter.builder("inverted_index_documents")
            .description("Number of documents indexed")
            .register(meterRegistry);
        this.indexedTermsCounter = Counter.builder("inverted_index_terms")
            .description("Number of unique terms indexed")
            .register(meterRegistry);
    }
    
    public void addDocument(Long docId, String text) {
        Set<String> tokens = tokenize(text);
        documentTerms.put(docId, tokens);
        tokens.forEach(token -> {
            index.computeIfAbsent(token, k -> {
                indexedTermsCounter.increment();
                return new ConcurrentSkipListSet<>();
            }).add(docId);
        });
        indexedDocsCounter.increment();
        if (docId % 1000 == 0) {
            log.info("Indexed {} documents, {} unique terms", 
                documentTerms.size(), index.size());
        }
    }
    
    public void removeDocument(Long docId) {
        Set<String> terms = documentTerms.remove(docId);
        if (terms != null) {
            terms.forEach(term -> {
                Set<Long> postingList = index.get(term);
                if (postingList != null) {
                    postingList.remove(docId);
                    if (postingList.isEmpty()) {
                        index.remove(term);
                    }
                }
            });
        }
    }
    
    public Set<Long> search(String query) {
        Set<String> terms = tokenize(query);
        if (terms.isEmpty()) {
            return Collections.emptySet();
        }
        List<Set<Long>> postingLists = terms.stream()
            .map(index::get)
            .filter(Objects::nonNull)
            .sorted(Comparator.comparingInt(Set::size))
            .collect(Collectors.toList());
        if (postingLists.isEmpty()) {
            return Collections.emptySet();
        }
        Set<Long> result = new HashSet<>(postingLists.get(0));
        for (int i = 1; i < postingLists.size(); i++) {
            result.retainAll(postingLists.get(i));
            if (result.isEmpty()) {
                break;
            }
        }
        return result;
    }
    
    public List<ScoredDocument> searchWithScores(String query, int limit) {
        Set<String> terms = tokenize(query);
        if (terms.isEmpty()) {
            return Collections.emptyList();
        }
        Map<Long, Double> scores = new HashMap<>();
        long totalDocs = documentTerms.size();
        for (String term : terms) {
            Set<Long> postingList = index.get(term);
            if (postingList == null) continue;
            double idf = Math.log((double) totalDocs / postingList.size());
            for (Long docId : postingList) {
                double tf = 1.0;
                double score = tf * idf;
                scores.merge(docId, score, Double::sum);
            }
        }
        return scores.entrySet().stream()
            .sorted(Map.Entry.<Long, Double>comparingByValue().reversed())
            .limit(limit)
            .map(e -> new ScoredDocument(e.getKey(), e.getValue()))
            .collect(Collectors.toList());
    }
    
    public Set<String> tokenize(String text) {
        if (text == null || text.isEmpty()) {
            return Collections.emptySet();
        }
        return Arrays.stream(text.toLowerCase()
                .split("[^a-z0-9]+"))
            .filter(token -> token.length() > 2)
            .filter(token -> !STOPWORDS.contains(token))
            .collect(Collectors.toSet());
    }
    
    public int getIndexSize() {
        return index.size();
    }
    
    public int getDocumentCount() {
        return documentTerms.size();
    }
    
    public Map<String, Integer> getIndexStats() {
        return Map.of(
            "uniqueTerms", index.size(),
            "totalDocuments", documentTerms.size()
        );
    }
}
