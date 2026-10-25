# Market Sales Classifier

Market satış verilerinden müşteri kategorisini tahmin eden, **KNN** ve **Karar Ağacı** algoritmalarını hiçbir makine öğrenmesi kütüphanesi kullanmadan sıfırdan uygulayan bir Java masaüstü uygulaması. Programlama Laboratuvarı kapsamında iki kişilik takımla geliştirildi.

![Java](https://img.shields.io/badge/Java-ED8B00?logo=openjdk&logoColor=white)
![Swing](https://img.shields.io/badge/Swing-5382A1)

## Özellikler

- **İki algoritma, sıfırdan:** k-En Yakın Komşu ve Karar Ağacı (derinlik ve bölme sınırı ayarlanabilir)
- **Veri yükleme:** Excel (`.xlsx`) ve CSV
- **Ön işleme:** eğitim verisine göre ölçekleme, test verisine aynı dönüşümü uygulama
- **Değerlendirme:** doğruluk oranı, eğitim süresi ölçümü ve iki algoritmanın karşılaştırması
- Tekrar üretilebilir eğitim/test bölmesi (seed)
- Karar ağacını metin olarak yazdırma
- Swing arayüzü

## Mimari

Nesne yönelimli tasarım prensipleriyle kuruldu: iki algoritma ortak `IClassifier` arayüzünü ve `BaseAlgorithm` soyut sınıfını paylaşıyor. Yeni bir algoritma eklemek için yalnızca `train` ve `predict` yazmak yeterli.

| Paket | Görevi |
|---|---|
| `algorithm` | `KNNClassifier`, `DecisionTreeClassifier`, `TreeNode` |
| `data` | `DataLoader`, `PreProcessor` |
| `evaluation` | `Evaluator` |
| `model` | `UserRecord` |
| `ui` | `MainUI` |

## Çalıştırma

1. Projeyi NetBeans ile aç (Ant projesi).
2. Excel okuma için Apache POI 5.2.3 kütüphanesini projeye ekle.
3. `Main` sınıfını çalıştır, arayüzden veri dosyanı seç.

Ders kapsamında verilen veri seti bu depoda yer almıyor.

## Ekip

- [Halit Bulut](https://github.com/HalittBulut)
- Enes Canlı
