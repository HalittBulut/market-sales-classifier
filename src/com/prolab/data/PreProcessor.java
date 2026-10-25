package com.prolab.data;

import com.prolab.model.UserRecord;
import java.util.List;

/*
  Ham verileri algoritma girdisine uygun hale getiren ön işleme sınıfı.
 
 */
public class PreProcessor {

    private double minPrice,     maxPrice;
    private double minAmount,    maxAmount;
    private double minNetTotal,  maxNetTotal;
    private double minBrandCode, maxBrandCode;

    private boolean fitted = false;

    public void fit(List<UserRecord> trainingData) {
        if (trainingData == null || trainingData.isEmpty())
            throw new IllegalArgumentException("Eğitim verisi boş olamaz.");

        minPrice     = Double.MAX_VALUE; maxPrice     = -Double.MAX_VALUE;
        minAmount    = Double.MAX_VALUE; maxAmount    = -Double.MAX_VALUE;
        minNetTotal  = Double.MAX_VALUE; maxNetTotal  = -Double.MAX_VALUE;
        minBrandCode = Double.MAX_VALUE; maxBrandCode = -Double.MAX_VALUE;

        for (UserRecord r : trainingData) {
            if (r.getPrice()        < minPrice)     minPrice     = r.getPrice();
            if (r.getPrice()        > maxPrice)     maxPrice     = r.getPrice();
            if (r.getAmount()       < minAmount)    minAmount    = r.getAmount();
            if (r.getAmount()       > maxAmount)    maxAmount    = r.getAmount();
            if (r.getLineNetTotal() < minNetTotal)  minNetTotal  = r.getLineNetTotal();
            if (r.getLineNetTotal() > maxNetTotal)  maxNetTotal  = r.getLineNetTotal();
            if (r.getBrandCode()    < minBrandCode) minBrandCode = r.getBrandCode();
            if (r.getBrandCode()    > maxBrandCode) maxBrandCode = r.getBrandCode();
        }

        fitted = true;
        System.out.printf("[PreProcessor] Fit tamamlandı.%n");
        System.out.printf("  Price     : [%.2f, %.2f]%n", minPrice,     maxPrice);
        System.out.printf("  Amount    : [%.2f, %.2f]%n", minAmount,    maxAmount);
        System.out.printf("  NetTotal  : [%.2f, %.2f]%n", minNetTotal,  maxNetTotal);
        System.out.printf("  BrandCode : [%.0f, %.0f]%n", minBrandCode, maxBrandCode);
    }

    public void transform(List<UserRecord> data) {
        if (!fitted)
            throw new IllegalStateException("Önce fit() çağrılmalıdır.");

        for (UserRecord r : data) {
            r.setEncodedGender(encodeGender(r.getGender()));
            r.setNormalizedPrice(minMaxNormalize(r.getPrice(),        minPrice,     maxPrice));
            r.setNormalizedAmount(minMaxNormalize(r.getAmount(),      minAmount,    maxAmount));
            r.setNormalizedNetTotal(minMaxNormalize(r.getLineNetTotal(), minNetTotal, maxNetTotal));
            r.setEncodedBrandCode(minMaxNormalize(r.getBrandCode(),   minBrandCode, maxBrandCode));
        }
    }

    public void fitTransform(List<UserRecord> trainingData) {
        fit(trainingData);
        transform(trainingData);
    }

    private double encodeGender(String gender) {
        if (gender == null) return 0.5;
        switch (gender.trim().toLowerCase()) {
            case "female": case "kadin": case "kadın": case "f": return 0.0;
            case "male":   case "erkek": case "m":                return 1.0;
            default: return 0.5;
        }
    }

    private double minMaxNormalize(double value, double min, double max) {
        if (max == min) return 0.0;
        return Math.max(0.0, Math.min(1.0, (value - min) / (max - min)));
    }

    public double getMinPrice()     { return minPrice; }
    public double getMaxPrice()     { return maxPrice; }
    public double getMinAmount()    { return minAmount; }
    public double getMaxAmount()    { return maxAmount; }
    public double getMinNetTotal()  { return minNetTotal; }
    public double getMaxNetTotal()  { return maxNetTotal; }
    public double getMinBrandCode() { return minBrandCode; }
    public double getMaxBrandCode() { return maxBrandCode; }
    public boolean isFitted()       { return fitted; }
}