package com.prolab.evaluation;

import com.prolab.algorithm.IClassifier;
import com.prolab.model.UserRecord;
import java.util.*;

/*
  İki algoritmanın doğruluk ve hız sonuçlarını karşılaştıran değerlendirici sınıf.
 
  Hesaplanan metrikler:
   - Accuracy (Doğruluk)
   - Confusion Matrix (Hata Matrisi)
   - Çalışma süresi (ms)
   - Bellek kullanımı (MB)
 */
public class Evaluator {


    /**
     * Verilen sınıflandırıcıyı test verisi üzerinde çalıştırır ve
     * EvaluationResult döner.
     *
     * @param classifier Değerlendirilecek model
     * @param testData   Test kayıtları
     * @return Detaylı sonuç nesnesi
     */
    public EvaluationResult evaluate(IClassifier classifier,
                                     List<UserRecord> testData) {
        if (testData == null || testData.isEmpty()) {
            throw new IllegalArgumentException("Test verisi boş olamaz.");
        }

        Runtime runtime = Runtime.getRuntime();
        runtime.gc(); // GC'yi tetikle — daha doğru bellek ölçümü için
        long memBefore = runtime.totalMemory() - runtime.freeMemory();
        long timeBefore = System.currentTimeMillis();

        // Tahminler
        List<String> predicted = new ArrayList<>();
        List<String> actual    = new ArrayList<>();

        for (UserRecord rec : testData) {
            predicted.add(classifier.predict(rec));
            actual.add(rec.getCategory());
        }

        long timeAfter = System.currentTimeMillis();
        long memAfter  = runtime.totalMemory() - runtime.freeMemory();

        long   elapsedMs     = timeAfter - timeBefore;
        double memUsedMB     = (memAfter - memBefore) / (1024.0 * 1024.0);

        // Doğruluk
        double accuracy = computeAccuracy(actual, predicted);

        // Confusion matrix
        Map<String, Map<String, Integer>> confMatrix = buildConfusionMatrix(actual, predicted);

        return new EvaluationResult(
                classifier.getName(),
                accuracy,
                elapsedMs,
                memUsedMB,
                confMatrix,
                actual,
                predicted
        );
    }

    /**
     * Eğitim süresini ayrıca ölçer.
     *
     * @param classifier Eğitilecek model
     * @param trainData  Eğitim verisi
     * @return Eğitim süresi (ms)
     */
    public long measureTrainTime(IClassifier classifier,
                                 List<UserRecord> trainData) {
        long t0 = System.currentTimeMillis();
        classifier.train(trainData);
        return System.currentTimeMillis() - t0;
    }

    // ---------------------------------------------------------------
    // Metrik hesapları
    // ---------------------------------------------------------------

    private double computeAccuracy(List<String> actual, List<String> predicted) {
        int correct = 0;
        for (int i = 0; i < actual.size(); i++) {
            if (actual.get(i).equals(predicted.get(i))) correct++;
        }
        return (double) correct / actual.size();
    }

    /**
     * İki boyutlu confusion matrix oluşturur.
     * confMatrix.get(actualCategory).get(predictedCategory) → sayı
     */
    private Map<String, Map<String, Integer>> buildConfusionMatrix(
            List<String> actual, List<String> predicted) {

        Set<String> labels = new TreeSet<>(actual);
        labels.addAll(predicted);

        Map<String, Map<String, Integer>> matrix = new LinkedHashMap<>();
        for (String lbl : labels) {
            Map<String, Integer> row = new LinkedHashMap<>();
            for (String lbl2 : labels) row.put(lbl2, 0);
            matrix.put(lbl, row);
        }

        for (int i = 0; i < actual.size(); i++) {
            matrix.get(actual.get(i)).merge(predicted.get(i), 1, Integer::sum);
        }

        return matrix;
    }

    // ---------------------------------------------------------------
    // İç sonuç sınıfı
    // ---------------------------------------------------------------

    public static class EvaluationResult {

        private final String algorithmName;
        private final double accuracy;
        private final long   elapsedMs;
        private final double memUsedMB;
        private final Map<String, Map<String, Integer>> confusionMatrix;
        private final List<String> actual;
        private final List<String> predicted;

        public EvaluationResult(String algorithmName, double accuracy,
                                long elapsedMs, double memUsedMB,
                                Map<String, Map<String, Integer>> confusionMatrix,
                                List<String> actual, List<String> predicted) {
            this.algorithmName  = algorithmName;
            this.accuracy       = accuracy;
            this.elapsedMs      = elapsedMs;
            this.memUsedMB      = memUsedMB;
            this.confusionMatrix = confusionMatrix;
            this.actual         = actual;
            this.predicted      = predicted;
        }

        // ---- Getters ----
        public String getAlgorithmName()  { return algorithmName; }
        public double getAccuracy()        { return accuracy; }
        public long   getElapsedMs()       { return elapsedMs; }
        public double getMemUsedMB()       { return memUsedMB; }
        public Map<String, Map<String, Integer>> getConfusionMatrix() { return confusionMatrix; }
        public List<String> getActual()    { return actual; }
        public List<String> getPredicted() { return predicted; }
        public double getErrorRate()       { return 1.0 - accuracy; }

        // ---- Per-class precision, recall, f1 ----
        public Map<String, double[]> getPerClassMetrics() {
            Map<String, double[]> metrics = new LinkedHashMap<>();
            Set<String> labels = confusionMatrix.keySet();

            for (String lbl : labels) {
                int tp = confusionMatrix.getOrDefault(lbl, Collections.emptyMap())
                                        .getOrDefault(lbl, 0);
                int fpSum = 0;
                for (String a : labels) {
                    if (!a.equals(lbl)) {
                        fpSum += confusionMatrix.getOrDefault(a, Collections.emptyMap())
                                               .getOrDefault(lbl, 0);
                    }
                }
                int fnSum = 0;
                Map<String, Integer> row = confusionMatrix.getOrDefault(lbl, Collections.emptyMap());
                for (String p : labels) {
                    if (!p.equals(lbl)) fnSum += row.getOrDefault(p, 0);
                }

                double precision = (tp + fpSum) == 0 ? 0.0 : (double) tp / (tp + fpSum);
                double recall    = (tp + fnSum)  == 0 ? 0.0 : (double) tp / (tp + fnSum);
                double f1        = (precision + recall) == 0 ? 0.0
                        : 2 * precision * recall / (precision + recall);

                metrics.put(lbl, new double[]{precision, recall, f1});
            }
            return metrics;
        }

        /** Okunabilir özet rapor */
        public String toSummaryString() {
            StringBuilder sb = new StringBuilder();
            sb.append("=".repeat(55)).append("\n");
            sb.append("Algoritma : ").append(algorithmName).append("\n");
            sb.append(String.format("Doğruluk  : %.4f  (%.2f%%)%n", accuracy, accuracy * 100));
            sb.append(String.format("Hata Oranı: %.4f  (%.2f%%)%n", getErrorRate(), getErrorRate() * 100));
            sb.append(String.format("Süre      : %d ms%n", elapsedMs));
            sb.append(String.format("Bellek    : %.3f MB%n", memUsedMB));
            sb.append("\n--- Confusion Matrix ---\n");

            Set<String> labels = confusionMatrix.keySet();
            // Başlık satırı
            sb.append(String.format("%-18s", "Gerçek \\ Tahmin"));
            for (String lbl : labels) sb.append(String.format("%-14s", lbl));
            sb.append("\n");

            for (String actualLbl : labels) {
                sb.append(String.format("%-18s", actualLbl));
                for (String predLbl : labels) {
                    sb.append(String.format("%-14d",
                            confusionMatrix.get(actualLbl).getOrDefault(predLbl, 0)));
                }
                sb.append("\n");
            }

            sb.append("\n--- Sınıf Bazlı Metrikler ---\n");
            sb.append(String.format("%-18s %-10s %-10s %-10s%n",
                    "Kategori", "Precision", "Recall", "F1-Score"));
            Map<String, double[]> pcm = getPerClassMetrics();
            for (Map.Entry<String, double[]> e : pcm.entrySet()) {
                double[] m = e.getValue();
                sb.append(String.format("%-18s %-10.4f %-10.4f %-10.4f%n",
                        e.getKey(), m[0], m[1], m[2]));
            }

            sb.append("=".repeat(55)).append("\n");
            return sb.toString();
        }
    }
}