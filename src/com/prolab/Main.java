package com.prolab;

import com.prolab.algorithm.*;
import com.prolab.data.*;
import com.prolab.evaluation.Evaluator;
import com.prolab.model.UserRecord;
import com.prolab.ui.MainUI;

import java.util.*;

/*
    AKIŞ:
   1. Veri yükleme (xlsx veya csv)
   2. Ön işleme (encoding + normalization)
   3. Train/Test ayrımı (%80/%20)
   4. KNN ve DecisionTree eğitimi
   5. Değerlendirme ve raporlama
   6. GUI başlatma (opsiyonel --gui bayrağı ile)
 */
public class Main {

    public static void main(String[] args) {

        System.out.println("========================================");
        System.out.println("  ProLab-II: KNN vs Decision Tree");
        System.out.println("  Kocaeli Üniversitesi — Bilgisayar Müh.");
        System.out.println("========================================\n");

        // GUI modunu kontrol et
        boolean guiMode = args.length > 0 && args[0].equalsIgnoreCase("--gui");

        if (guiMode) {
            System.out.println("[Main] GUI modu başlatılıyor...");
            javax.swing.SwingUtilities.invokeLater(MainUI::new);
            return;
        }


        String filePath = args.length > 0 ? args[0] : "MarketSalesKocaeli.csv";

        List<UserRecord> allData;
        DataLoader loader = new DataLoader();

        try {
            if (filePath.endsWith(".xlsx") || filePath.endsWith(".xls")) {
                allData = loader.loadFromXlsx(filePath);
            } else {
                allData = loader.loadFromCsv(filePath);
            }
        } catch (Exception e) {
            System.err.println("[HATA] Veri yüklenemedi: " + e.getMessage());
            System.err.println("Lütfen dosya yolunu kontrol edin.");
            e.printStackTrace();
            return;
        }

        if (allData.isEmpty()) {
            System.err.println("[HATA] Veri seti boş döndü.");
            return;
        }

        // Train / Test ayrımı (%80 / %20)
        
        List<List<UserRecord>> split = BaseAlgorithm.splitData(allData, 0.80, 42L);
        List<UserRecord> trainData = split.get(0);
        List<UserRecord> testData  = split.get(1);

        System.out.printf("Toplam: %d kayıt → Eğitim: %d | Test: %d%n%n",
                allData.size(), trainData.size(), testData.size());


        PreProcessor preprocessor = new PreProcessor();
        preprocessor.fitTransform(trainData);   // fit + transform train
        preprocessor.transform(testData);        // sadece transform test

        
        // Algoritmaları Polymorphism ile listede tut
        
        int k = 5;
        int maxDepth = 10;
        int minSamples = 5;

        List<IClassifier> classifiers = new ArrayList<>();
        classifiers.add(new KNNClassifier(k));
        classifiers.add(new DecisionTreeClassifier(maxDepth, minSamples));

        Evaluator evaluator = new Evaluator();

        for (IClassifier clf : classifiers) {
            System.out.println(">>> Eğitim: " + clf.getName());
            long trainMs = evaluator.measureTrainTime(clf, trainData);
            System.out.printf("    Eğitim süresi: %d ms%n", trainMs);

            System.out.println(">>> Test: " + clf.getName());
            Evaluator.EvaluationResult result = evaluator.evaluate(clf, testData);
            System.out.println(result.toSummaryString());
        }

        System.out.println("\n[Main] Tüm değerlendirmeler tamamlandı.");
        System.out.println("[Main] GUI için: java -jar prolab.jar --gui");
    }
}