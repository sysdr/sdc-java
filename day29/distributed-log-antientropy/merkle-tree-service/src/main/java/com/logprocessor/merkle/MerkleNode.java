package com.logprocessor.merkle;

import java.util.ArrayList;
import java.util.List;

public class MerkleNode {
    private String hash;
    private MerkleNode left;
    private MerkleNode right;
    private boolean isLeaf;
    private Long startVersion;
    private Long endVersion;
    private String partitionId;
    
    public MerkleNode(String hash) {
        this.hash = hash;
        this.isLeaf = true;
    }
    
    public MerkleNode(MerkleNode left, MerkleNode right, String hash) {
        this.left = left;
        this.right = right;
        this.hash = hash;
        this.isLeaf = false;
    }
    
    // Getters and setters
    public String getHash() { return hash; }
    public void setHash(String hash) { this.hash = hash; }
    
    public MerkleNode getLeft() { return left; }
    public void setLeft(MerkleNode left) { this.left = left; }
    
    public MerkleNode getRight() { return right; }
    public void setRight(MerkleNode right) { this.right = right; }
    
    public boolean isLeaf() { return isLeaf; }
    public void setLeaf(boolean leaf) { isLeaf = leaf; }
    
    public Long getStartVersion() { return startVersion; }
    public void setStartVersion(Long startVersion) { this.startVersion = startVersion; }
    
    public Long getEndVersion() { return endVersion; }
    public void setEndVersion(Long endVersion) { this.endVersion = endVersion; }
    
    public String getPartitionId() { return partitionId; }
    public void setPartitionId(String partitionId) { this.partitionId = partitionId; }
}
