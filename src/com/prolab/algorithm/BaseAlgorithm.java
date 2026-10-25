package com.prolab.algorithm;

import com.prolab.model.UserRecord;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

/*
  KNN ve DecisionTree'nin ortak işlevlerini barındıran soyut temel sınıf
  Inheritance (Kalıtım) prensibinin uygulaması
 */
public abstract class BaseAlgorithm implements IClassifier {

    protected List<UserRecord> trainingData = new ArrayList<>();

    /*
      Test listesindeki tahminlerle gerçek etiketleri karşılaştırarak
      doğruluk oranı hesaplar.
     
     */
    public double calculateAccuracy(List<UserRecord> testData) {
        if (testData == null || testData.isEmpty()) return 0.0;
        int correct = 0;
        for (UserRecord record : testData) {
            String predicted = predict(record);
            if (predicted != null && predicted.equals(record.getCategory())) {
                correct++;
            }
        }
        return (double) correct / testData.size();
    }

    /*
      Veri setini rastgele karıştırarak trainRatio oranda eğitim,
      kalanını test olarak döner.
     
      data       Tüm kayıtlar
      trainRatio Eğitim oranı (örn 0.8)
      seed       Tekrar üretilebilirlik için seed
     
     */
    public static List<List<UserRecord>> splitData(List<UserRecord> data,
                                                   double trainRatio,
                                                   long seed) {
        List<UserRecord> shuffled = new ArrayList<>(data);
        Collections.shuffle(shuffled, new Random(seed));

        int splitIdx = (int) (shuffled.size() * trainRatio);
        List<UserRecord> train = new ArrayList<>(shuffled.subList(0, splitIdx));
        List<UserRecord> test  = new ArrayList<>(shuffled.subList(splitIdx, shuffled.size()));

        List<List<UserRecord>> result = new ArrayList<>();
        result.add(train);
        result.add(test);
        return result;
    }

    /*
      Verilen kayıt listesindeki belirli bir kategorinin frekansını döner.
     */
    protected int countCategory(List<UserRecord> records, String category) {
        int count = 0;
        for (UserRecord r : records) {
            if (category.equals(r.getCategory())) count++;
        }
        return count;
    }

    /*
      Kayıt listesindeki en sık geçen kategoriyi (çoğunluk etiketi) döner.
     */
    protected String majorityVote(List<UserRecord> records) {
        java.util.HashMap<String, Integer> freq = new java.util.HashMap<>();
        for (UserRecord r : records) {
            freq.merge(r.getCategory(), 1, Integer::sum);
        }
        return freq.entrySet().stream()
                .max(java.util.Map.Entry.comparingByValue())
                .map(java.util.Map.Entry::getKey)
                .orElse("Unknown");
    }
}