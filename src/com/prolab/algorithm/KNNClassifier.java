package com.prolab.algorithm;

import com.prolab.model.UserRecord;
import java.util.*;

/*
  K-En Yakın Komşu (K-Nearest Neighbours) sınıflandırıcısı
  IClassifier arayüzünü BaseAlgorithm üzerinden implemente ede
  Mesafe formülü: Öklid uzaklığı d = sqrt( Σ (xi - yi)² )
  
 */
public class KNNClassifier extends BaseAlgorithm {

    private int k;

    public KNNClassifier(int k) {
        if (k < 1) throw new IllegalArgumentException("K değeri en az 1 olmalıdır.");
        this.k = k;
    }

    @Override
    public String getName() {
        return "KNN (k=" + k + ")";
    }

    /* eğitim verisini hafızaya alır */
    @Override
    public void train(List<UserRecord> trainingData) {
        this.trainingData = new ArrayList<>(trainingData);
    }

    /*
      Yeni kaydın kategorisini tahmin eder
      PriorityQueue kullanarak k en yakın komşuyu bulur,
      ardından çoğunluk oylaması yapar
     */
    @Override
    public String predict(UserRecord user) {
        if (trainingData.isEmpty()) {
            throw new IllegalStateException("Model henüz eğitilmedi. Önce train() çağrılmalıdır.");
        }

        // küçük mesafeli önde
        PriorityQueue<double[]> pq = new PriorityQueue<>(
                Comparator.comparingDouble(a -> a[0])
        );

        double[] userFeatures = user.getFeatureVector();

        for (int i = 0; i < trainingData.size(); i++) {
            double dist = euclideanDistance(userFeatures,
                    trainingData.get(i).getFeatureVector());
            pq.offer(new double[]{dist, i});
        }

        // k komşuyu topla
        Map<String, Integer> voteMap = new HashMap<>();
        for (int i = 0; i < k && !pq.isEmpty(); i++) {
            double[] entry = pq.poll();
            int idx = (int) entry[1];
            String cat = trainingData.get(idx).getCategory();
            voteMap.merge(cat, 1, Integer::sum);
        }

        // En fazla oy alan kategori
        return voteMap.entrySet().stream()
                .max(Map.Entry.comparingByValue())
                .map(Map.Entry::getKey)
                .orElse("Unknown");
    }

    /*
      İki özellik vektörü arasındaki Öklid mesafesini hesaplar
     */
    private double euclideanDistance(double[] a, double[] b) {
        double sum = 0.0;
        int len = Math.min(a.length, b.length);
        for (int i = 0; i < len; i++) {
            double diff = a[i] - b[i];
            sum += diff * diff;
        }
        return Math.sqrt(sum);
    }

    public int getK() { return k; }
    public void setK(int k) {
        if (k < 1) throw new IllegalArgumentException("K değeri en az 1 olmalıdır.");
        this.k = k;
    }
}