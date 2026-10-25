package com.prolab.model;

/*
  Veri setindeki her bir satırı (satış işlemini) temsil eden model sınıfı.
 
  Özellik vektörü (5 özellik):
    [0] encodedGender
    [1] normalizedPrice      — birim fiyat (ürün fiyat segmenti)
    [2] normalizedAmount     — alım miktarı (alım davranışı)
    [3] normalizedNetTotal   — toplam harcama
    [4] encodedBrandCode
 */
public class UserRecord {

    // Ham veriler
    private final int    clientCode;
    private final String gender;
    private final double amount;
    private final double price;
    private final double lineNetTotal;
    private final String brand;
    private final int    brandCode;
    private final String category;

    // Ön işleme sonrası değerler
    private double encodedGender;
    private double normalizedPrice;
    private double normalizedAmount;
    private double normalizedNetTotal;
    private double encodedBrandCode;

    public UserRecord(int clientCode, String gender,
                      double amount, double price, double lineNetTotal,
                      String brand, int brandCode, String category) {
        this.clientCode   = clientCode;
        this.gender       = gender;
        this.amount       = amount;
        this.price        = price;
        this.lineNetTotal = lineNetTotal;
        this.brand        = brand;
        this.brandCode    = brandCode;
        this.category     = category;
    }

    // Ham getter'lar
    public int    getClientCode()   { return clientCode; }
    public String getGender()       { return gender; }
    public double getAmount()       { return amount; }
    public double getPrice()        { return price; }
    public double getLineNetTotal() { return lineNetTotal; }
    public String getBrand()        { return brand; }
    public int    getBrandCode()    { return brandCode; }
    public String getCategory()     { return category; }

    // Encode/Normalize getter & setter'lar
    public double getEncodedGender()              { return encodedGender; }
    public void   setEncodedGender(double v)      { this.encodedGender = v; }

    public double getNormalizedPrice()             { return normalizedPrice; }
    public void   setNormalizedPrice(double v)     { this.normalizedPrice = v; }

    public double getNormalizedAmount()            { return normalizedAmount; }
    public void   setNormalizedAmount(double v)    { this.normalizedAmount = v; }

    public double getNormalizedNetTotal()          { return normalizedNetTotal; }
    public void   setNormalizedNetTotal(double v)  { this.normalizedNetTotal = v; }

    public double getEncodedBrandCode()            { return encodedBrandCode; }
    public void   setEncodedBrandCode(double v)    { this.encodedBrandCode = v; }

    /*
      KNN ve DT için özellik vektörü (5 özellik).
     */
    public double[] getFeatureVector() {
        return new double[]{
            encodedGender,
            normalizedPrice,
            normalizedAmount,
            normalizedNetTotal,
            encodedBrandCode
        };
    }

    @Override
    public String toString() {
        return String.format(
            "UserRecord{client=%d, gender=%s, price=%.2f, amount=%.0f, netTotal=%.2f, brand=%s, category=%s}",
            clientCode, gender, price, amount, lineNetTotal, brand, category);
    }
}