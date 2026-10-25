package com.prolab.algorithm;

import com.prolab.model.UserRecord;
import java.util.*;

/*
  Karar Ağacı (Decision Tree) sınıflandırıcısı 
   Dallanma kriteri: Information Gain (Entropi azaltımı).
     5 özellik: Gender, Price, Amount, NetTotal, BrandCode
 */
public class DecisionTreeClassifier extends BaseAlgorithm {

    private int maxDepth;
    private int minSamplesSplit;
    private TreeNode root;

    private static final String[] FEATURE_NAMES = {
        "Gender(enc)",
        "Price(norm)",
        "Amount(norm)",
        "NetTotal(norm)",
        "BrandCode(norm)"
    };
    private static final int NUM_FEATURES = 5;

    public DecisionTreeClassifier(int maxDepth, int minSamplesSplit) {
        this.maxDepth        = maxDepth;
        this.minSamplesSplit = minSamplesSplit;
    }

    public DecisionTreeClassifier() { this(10, 5); }

    @Override
    public String getName() { return "Decision Tree (maxDepth=" + maxDepth + ")"; }

    @Override
    public void train(List<UserRecord> trainingData) {
        this.trainingData = new ArrayList<>(trainingData);
        this.root = buildTree(this.trainingData, 0);
    }

    @Override
    public String predict(UserRecord user) {
        if (root == null) throw new IllegalStateException("Model henüz eğitilmedi.");
        return traverse(root, user.getFeatureVector());
    }

    private TreeNode buildTree(List<UserRecord> data, int depth) {
        if (data.isEmpty()) return new TreeNode("Unknown");

        Set<String> uniqueCategories = new HashSet<>();
        for (UserRecord r : data) uniqueCategories.add(r.getCategory());

        if (uniqueCategories.size() == 1) return new TreeNode(data.get(0).getCategory());
        if (depth >= maxDepth || data.size() < minSamplesSplit) return new TreeNode(majorityLabel(data));

        BestSplit best = findBestSplit(data);
        if (best == null || best.informationGain <= 0) return new TreeNode(majorityLabel(data));

        List<UserRecord> leftData  = new ArrayList<>();
        List<UserRecord> rightData = new ArrayList<>();
        for (UserRecord r : data) {
            if (r.getFeatureVector()[best.featureIndex] <= best.threshold) leftData.add(r);
            else rightData.add(r);
        }

        if (leftData.isEmpty() || rightData.isEmpty()) return new TreeNode(majorityLabel(data));

        TreeNode node = new TreeNode(best.featureIndex, best.threshold);
        node.left  = buildTree(leftData,  depth + 1);
        node.right = buildTree(rightData, depth + 1);
        return node;
    }

    private BestSplit findBestSplit(List<UserRecord> data) {
        double baseEntropy = entropy(data);
        BestSplit best = null;

        for (int fi = 0; fi < NUM_FEATURES; fi++) {
            Set<Double> uniqueVals = new TreeSet<>();
            for (UserRecord r : data) uniqueVals.add(r.getFeatureVector()[fi]);

            Double prev = null;
            for (Double val : uniqueVals) {
                if (prev != null) {
                    double threshold = (prev + val) / 2.0;
                    List<UserRecord> left = new ArrayList<>(), right = new ArrayList<>();
                    for (UserRecord r : data) {
                        if (r.getFeatureVector()[fi] <= threshold) left.add(r);
                        else right.add(r);
                    }
                    if (left.isEmpty() || right.isEmpty()) { prev = val; continue; }

                    double wEntropy = ((double) left.size()  / data.size()) * entropy(left)
                                    + ((double) right.size() / data.size()) * entropy(right);
                    double gain = baseEntropy - wEntropy;
                    if (best == null || gain > best.informationGain)
                        best = new BestSplit(fi, threshold, gain);
                }
                prev = val;
            }
        }
        return best;
    }

    private double entropy(List<UserRecord> data) {
        if (data.isEmpty()) return 0.0;
        Map<String, Integer> freq = new HashMap<>();
        for (UserRecord r : data) freq.merge(r.getCategory(), 1, Integer::sum);
        double h = 0.0;
        int n = data.size();
        for (int count : freq.values()) {
            double p = (double) count / n;
            if (p > 0) h -= p * (Math.log(p) / Math.log(2));
        }
        return h;
    }

    private String traverse(TreeNode node, double[] features) {
        if (node.isLeaf()) return node.leafCategory;
        return features[node.featureIndex] <= node.threshold
                ? traverse(node.left, features) : traverse(node.right, features);
    }

    private String majorityLabel(List<UserRecord> data) {
        Map<String, Integer> freq = new HashMap<>();
        for (UserRecord r : data) freq.merge(r.getCategory(), 1, Integer::sum);
        return freq.entrySet().stream().max(Map.Entry.comparingByValue())
                .map(Map.Entry::getKey).orElse("Unknown");
    }

    public String printTree() {
        StringBuilder sb = new StringBuilder();
        printNode(root, 0, sb);
        return sb.toString();
    }

    private void printNode(TreeNode node, int depth, StringBuilder sb) {
        if (node == null) return;
        String indent = "  ".repeat(depth);
        if (node.isLeaf()) {
            sb.append(indent).append("LEAF: ").append(node.leafCategory).append("\n");
        } else {
            sb.append(indent).append("IF ").append(FEATURE_NAMES[node.featureIndex])
              .append(" <= ").append(String.format("%.4f", node.threshold)).append("\n");
            printNode(node.left,  depth + 1, sb);
            sb.append(indent).append("ELSE\n");
            printNode(node.right, depth + 1, sb);
        }
    }

    public int  getMaxDepth()             { return maxDepth; }
    public void setMaxDepth(int v)        { this.maxDepth = v; }
    public int  getMinSamplesSplit()      { return minSamplesSplit; }
    public void setMinSamplesSplit(int v) { this.minSamplesSplit = v; }

    private static class BestSplit {
        int featureIndex; double threshold; double informationGain;
        BestSplit(int fi, double thr, double ig) {
            featureIndex = fi; threshold = thr; informationGain = ig;
        }
    }
}