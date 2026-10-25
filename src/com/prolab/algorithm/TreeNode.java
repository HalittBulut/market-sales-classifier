package com.prolab.algorithm;

/*
  Karar ağacındaki tek bir düğümü temsil eder
  Recursive yapı: sol ve sağ alt ağaçlara işaret eden referansla
 */
public class TreeNode {

    // Karar düğümü alanları
    int featureIndex;   
    double threshold;   
    // Yaprak düğüm alanı
    String leafCategory; // null değilse bu bir yaprak düğümdür

    // Alt ağaçlar
    TreeNode left;   
    TreeNode right;  

    /* Karar düğümü için kurucu */
    public TreeNode(int featureIndex, double threshold) {
        this.featureIndex = featureIndex;
        this.threshold    = threshold;
        this.leafCategory = null;
    }

    /* Yaprak düğümü için kurucu */
    public TreeNode(String leafCategory) {
        this.leafCategory = leafCategory;
        this.featureIndex = -1;
    }

    public boolean isLeaf() {
        return leafCategory != null;
    }
}