package com.prolab.ui;

import com.prolab.algorithm.*;
import com.prolab.data.*;
import com.prolab.evaluation.Evaluator;
import com.prolab.model.UserRecord;

import javax.swing.*;
import javax.swing.border.*;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.*;
import java.util.*;
import java.util.List;

/**
 * ProLab-II Ana Arayüzü — Java Swing
 *
 * Paneller:
 *  1. Veri Yükleme Paneli
 *  2. Model Seçimi ve Parametre Paneli
 *  3. Performans Metrik Paneli
 *  4. Confusion Matrix Paneli
 *  5. Grafik Paneli (Bar Chart — sıfırdan çizilmiş)
 */
public class MainUI extends JFrame {

    // ------------------------------------------------------------------
    // Renkler & Sabitler
    // ------------------------------------------------------------------
    private static final Color BG_DARK    = new Color(18, 18, 30);
    private static final Color BG_PANEL   = new Color(28, 28, 45);
    private static final Color BG_CARD    = new Color(38, 38, 58);
    private static final Color ACCENT     = new Color(99, 179, 237);
    private static final Color ACCENT2    = new Color(154, 117, 234);
    private static final Color SUCCESS    = new Color(72, 199, 142);
    private static final Color WARNING    = new Color(253, 196, 78);
    private static final Color TEXT_MAIN  = new Color(226, 232, 240);
    private static final Color TEXT_SUB   = new Color(148, 163, 184);
    private static final Font  FONT_TITLE = new Font("Segoe UI", Font.BOLD, 20);
    private static final Font  FONT_LABEL = new Font("Segoe UI", Font.PLAIN, 13);
    private static final Font  FONT_BOLD  = new Font("Segoe UI", Font.BOLD, 13);
    private static final Font  FONT_MONO  = new Font("Courier New", Font.PLAIN, 12);

    // ------------------------------------------------------------------
    // State
    // ------------------------------------------------------------------
    private List<UserRecord> allData    = new ArrayList<>();
    private List<UserRecord> trainData  = new ArrayList<>();
    private List<UserRecord> testData   = new ArrayList<>();
    private PreProcessor     preprocessor;
    private Evaluator        evaluator  = new Evaluator();

    private Evaluator.EvaluationResult knnResult = null;
    private Evaluator.EvaluationResult dtResult  = null;
    private long knnTrainMs = 0, dtTrainMs = 0;

    // ------------------------------------------------------------------
    // UI Bileşenleri
    // ------------------------------------------------------------------
    private JLabel   lblDataStatus;
    private JLabel   lblTrainCount, lblTestCount;
    private JSpinner spnTrainRatio;

    // Algorithm selection
    private JCheckBox chkKNN, chkDT;
    private JSpinner  spnK, spnMaxDepth, spnMinSamples;

    // Results
    private JLabel lblKnnAcc, lblDtAcc, lblKnnTime, lblDtTime, lblKnnMem, lblDtMem;
    private JLabel lblKnnErr, lblDtErr;
    private JTextArea taConfMatrix;
    private ChartPanel chartPanel;
    private JButton btnRun;
    private JProgressBar progressBar;
    private JTextArea taLog;

    // ------------------------------------------------------------------
    // Constructor
    // ------------------------------------------------------------------
    public MainUI() {
        setTitle("ProLab-II — KNN vs Decision Tree | Kocaeli Üniversitesi");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(1200, 820);
        setMinimumSize(new Dimension(900, 650));
        setLocationRelativeTo(null);
        getContentPane().setBackground(BG_DARK);

        buildUI();
        setVisible(true);
    }

    // UI İnşası
    
    private void buildUI() {
        setLayout(new BorderLayout(8, 8));

        // Header
        add(buildHeader(), BorderLayout.NORTH);

        // Sol panel (kontroller)
        JPanel leftPanel = new JPanel();
        leftPanel.setLayout(new BoxLayout(leftPanel, BoxLayout.Y_AXIS));
        leftPanel.setBackground(BG_DARK);
        leftPanel.setPreferredSize(new Dimension(310, 0));
        leftPanel.setBorder(new EmptyBorder(0, 8, 8, 4));
        leftPanel.add(buildDataPanel());
        leftPanel.add(Box.createVerticalStrut(8));
        leftPanel.add(buildAlgorithmPanel());
        leftPanel.add(Box.createVerticalStrut(8));
        leftPanel.add(buildRunPanel());
        leftPanel.add(Box.createVerticalStrut(8));
        leftPanel.add(buildLogPanel());

        // Sağ panel (sonuçlar)
        JPanel rightPanel = new JPanel(new BorderLayout(8, 8));
        rightPanel.setBackground(BG_DARK);
        rightPanel.setBorder(new EmptyBorder(0, 4, 8, 8));

        JPanel metricsRow = new JPanel(new GridLayout(1, 2, 8, 0));
        metricsRow.setBackground(BG_DARK);
        metricsRow.add(buildMetricCard("KNN", true));
        metricsRow.add(buildMetricCard("DT", false));

        JSplitPane bottomSplit = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT,
                buildConfMatrixPanel(), buildChartPanel());
        bottomSplit.setResizeWeight(0.45);
        bottomSplit.setBackground(BG_DARK);
        bottomSplit.setBorder(null);
        bottomSplit.setDividerSize(6);

        rightPanel.add(metricsRow, BorderLayout.NORTH);
        rightPanel.add(bottomSplit, BorderLayout.CENTER);

        JSplitPane mainSplit = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, leftPanel, rightPanel);
        mainSplit.setResizeWeight(0.0);
        mainSplit.setDividerSize(4);
        mainSplit.setBackground(BG_DARK);
        mainSplit.setBorder(null);

        add(mainSplit, BorderLayout.CENTER);
    }

    private JPanel buildHeader() {
        JPanel p = new JPanel(new BorderLayout());
        p.setBackground(BG_PANEL);
        p.setBorder(new EmptyBorder(14, 20, 14, 20));

        JLabel title = new JLabel("ProLab-II  ·  Makine Öğrenmesi Karşılaştırma Platformu");
        title.setFont(FONT_TITLE);
        title.setForeground(ACCENT);

        JLabel subtitle = new JLabel("KNN  vs  Decision Tree  —  Kocaeli Üniversitesi Bilgisayar Mühendisliği");
        subtitle.setFont(FONT_LABEL);
        subtitle.setForeground(TEXT_SUB);

        JPanel textPanel = new JPanel();
        textPanel.setBackground(BG_PANEL);
        textPanel.setLayout(new BoxLayout(textPanel, BoxLayout.Y_AXIS));
        textPanel.add(title);
        textPanel.add(Box.createVerticalStrut(3));
        textPanel.add(subtitle);

        p.add(textPanel, BorderLayout.WEST);
        return p;
    }

    //Veri Yükleme Paneli 
    private JPanel buildDataPanel() {
        JPanel card = createCard("📂  Veri Yükleme");

        JButton btnLoad = createStyledButton("Dosya Seç (.xlsx / .csv)", ACCENT);
        btnLoad.addActionListener(e -> loadDataAction());

        lblDataStatus = new JLabel("Dosya seçilmedi");
        lblDataStatus.setFont(FONT_LABEL);
        lblDataStatus.setForeground(TEXT_SUB);

        JPanel ratioRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        ratioRow.setBackground(BG_CARD);
        ratioRow.add(styledLabel("Eğitim Oranı: "));
        spnTrainRatio = new JSpinner(new SpinnerNumberModel(0.80, 0.50, 0.95, 0.05));
        styleSpinner(spnTrainRatio);
        ratioRow.add(spnTrainRatio);

        lblTrainCount = styledLabel("Eğitim: —");
        lblTestCount  = styledLabel("Test: —");

        card.add(btnLoad);
        card.add(Box.createVerticalStrut(6));
        card.add(lblDataStatus);
        card.add(Box.createVerticalStrut(8));
        card.add(ratioRow);
        card.add(Box.createVerticalStrut(4));
        card.add(lblTrainCount);
        card.add(lblTestCount);
        return card;
    }

    //Algoritma Paneli
    private JPanel buildAlgorithmPanel() {
        JPanel card = createCard("⚙️  Model Seçimi & Parametreler");

        chkKNN = new JCheckBox("K-En Yakın Komşu (KNN)", true);
        chkDT  = new JCheckBox("Karar Ağacı (Decision Tree)", true);
        styleCheckBox(chkKNN);
        styleCheckBox(chkDT);

        // KNN params
        JPanel knnParams = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 0));
        knnParams.setBackground(BG_CARD);
        knnParams.add(styledLabel("  K değeri: "));
        spnK = new JSpinner(new SpinnerNumberModel(5, 1, 50, 1));
        styleSpinner(spnK);
        knnParams.add(spnK);

        // DT params
        JPanel dtParams = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 0));
        dtParams.setBackground(BG_CARD);
        dtParams.add(styledLabel("  Max Derinlik: "));
        spnMaxDepth = new JSpinner(new SpinnerNumberModel(10, 1, 30, 1));
        styleSpinner(spnMaxDepth);
        dtParams.add(spnMaxDepth);
        dtParams.add(styledLabel("  Min Örnekler: "));
        spnMinSamples = new JSpinner(new SpinnerNumberModel(5, 1, 50, 1));
        styleSpinner(spnMinSamples);
        dtParams.add(spnMinSamples);

        card.add(chkKNN);
        card.add(knnParams);
        card.add(Box.createVerticalStrut(8));
        card.add(chkDT);
        card.add(dtParams);
        return card;
    }

    // Çalıştır Paneli 
    private JPanel buildRunPanel() {
        JPanel card = createCard("▶  Çalıştır");
        btnRun = createStyledButton("Modelleri Eğit & Değerlendir", SUCCESS);
        btnRun.addActionListener(e -> runModelsAction());

        progressBar = new JProgressBar(0, 100);
        progressBar.setStringPainted(true);
        progressBar.setString("Hazır");
        progressBar.setForeground(ACCENT);
        progressBar.setBackground(BG_DARK);
        progressBar.setFont(FONT_LABEL);
        progressBar.setBorder(null);

        card.add(btnRun);
        card.add(Box.createVerticalStrut(6));
        card.add(progressBar);
        return card;
    }

    // Log Paneli
    private JPanel buildLogPanel() {
        JPanel card = createCard("📋  Konsol");
        taLog = new JTextArea(6, 20);
        taLog.setFont(FONT_MONO);
        taLog.setBackground(BG_DARK);
        taLog.setForeground(new Color(180, 210, 180));
        taLog.setEditable(false);
        taLog.setCaretColor(ACCENT);
        JScrollPane sp = new JScrollPane(taLog);
        sp.setBorder(null);
        sp.setBackground(BG_DARK);
        card.add(sp);
        return card;
    }

    // Metrik Kartı
    private JPanel buildMetricCard(String algo, boolean isKnn) {
        JPanel card = createCard((isKnn ? "🔵" : "🟣") + "  " + (isKnn ? "KNN Sonuçları" : "Decision Tree Sonuçları"));

        JPanel grid = new JPanel(new GridLayout(4, 2, 6, 6));
        grid.setBackground(BG_CARD);

        if (isKnn) {
            lblKnnAcc  = metricLabel("—"); lblKnnTime = metricLabel("—");
            lblKnnErr  = metricLabel("—"); lblKnnMem  = metricLabel("—");
            grid.add(metricKeyLabel("Doğruluk (Accuracy):")); grid.add(lblKnnAcc);
            grid.add(metricKeyLabel("Hata Oranı:")); grid.add(lblKnnErr);
            grid.add(metricKeyLabel("Tahmin Süresi:")); grid.add(lblKnnTime);
            grid.add(metricKeyLabel("Bellek Kullanımı:")); grid.add(lblKnnMem);
        } else {
            lblDtAcc   = metricLabel("—"); lblDtTime  = metricLabel("—");
            lblDtErr   = metricLabel("—"); lblDtMem   = metricLabel("—");
            grid.add(metricKeyLabel("Doğruluk (Accuracy):")); grid.add(lblDtAcc);
            grid.add(metricKeyLabel("Hata Oranı:")); grid.add(lblDtErr);
            grid.add(metricKeyLabel("Tahmin Süresi:")); grid.add(lblDtTime);
            grid.add(metricKeyLabel("Bellek Kullanımı:")); grid.add(lblDtMem);
        }

        card.add(grid);
        return card;
    }

    //Confusion Matrix Paneli 
    private JPanel buildConfMatrixPanel() {
        JPanel card = createCard("🗂  Confusion Matrix");
        taConfMatrix = new JTextArea();
        taConfMatrix.setFont(FONT_MONO);
        taConfMatrix.setBackground(BG_DARK);
        taConfMatrix.setForeground(TEXT_MAIN);
        taConfMatrix.setEditable(false);
        taConfMatrix.setText("Henüz hesaplanmadı.\nModel çalıştırmak için sol\npaneli kullanın.");

        JScrollPane sp = new JScrollPane(taConfMatrix);
        sp.setBorder(null);
        sp.setBackground(BG_DARK);
        card.add(sp);
        return card;
    }

    // Grafik Paneli
    private JPanel buildChartPanel() {
        JPanel card = createCard("📊  Karşılaştırma Grafikleri");
        chartPanel = new ChartPanel();
        card.add(chartPanel);
        return card;
    }

    // Aksiyonlar
    private void loadDataAction() {
        JFileChooser fc = new JFileChooser();
        fc.setFileFilter(new FileNameExtensionFilter("Veri Dosyaları (*.xlsx, *.csv)", "xlsx", "csv"));
        int result = fc.showOpenDialog(this);
        if (result != JFileChooser.APPROVE_OPTION) return;

        String path = fc.getSelectedFile().getAbsolutePath();
        lblDataStatus.setForeground(WARNING);
        lblDataStatus.setText("Yükleniyor...");

        SwingWorker<List<UserRecord>, Void> worker = new SwingWorker<>() {
            @Override
            protected List<UserRecord> doInBackground() throws Exception {
                DataLoader loader = new DataLoader();
                if (path.endsWith(".xlsx") || path.endsWith(".xls")) {
                    return loader.loadFromXlsx(path);
                } else {
                    return loader.loadFromCsv(path);
                }
            }

            @Override
            protected void done() {
                try {
                    allData = get();
                    lblDataStatus.setForeground(SUCCESS);
                    lblDataStatus.setText("✔ " + fc.getSelectedFile().getName()
                            + " (" + allData.size() + " kayıt)");
                    updateSplitLabels();
                    log("Veri yüklendi: " + allData.size() + " satır.");
                } catch (Exception ex) {
                    lblDataStatus.setForeground(Color.RED);
                    lblDataStatus.setText("HATA: " + ex.getCause().getMessage());
                    log("HATA: " + ex.getMessage());
                }
            }
        };
        worker.execute();
    }

    private void updateSplitLabels() {
        double ratio = (double) spnTrainRatio.getValue();
        int trainN = (int) (allData.size() * ratio);
        int testN  = allData.size() - trainN;
        lblTrainCount.setText("Eğitim: " + trainN + " kayıt");
        lblTestCount.setText("Test:    " + testN  + " kayıt");
    }

    private void runModelsAction() {
        if (allData.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Önce veri yükleyin!", "Uyarı", JOptionPane.WARNING_MESSAGE);
            return;
        }
        if (!chkKNN.isSelected() && !chkDT.isSelected()) {
            JOptionPane.showMessageDialog(this, "En az bir algoritma seçin!", "Uyarı", JOptionPane.WARNING_MESSAGE);
            return;
        }

        btnRun.setEnabled(false);
        progressBar.setValue(0);
        progressBar.setString("Başlıyor...");
        knnResult = null;
        dtResult  = null;

        int k          = (int) spnK.getValue();
        int maxDepth   = (int) spnMaxDepth.getValue();
        int minSamples = (int) spnMinSamples.getValue();
        double ratio   = (double) spnTrainRatio.getValue();
        boolean runKNN = chkKNN.isSelected();
        boolean runDT  = chkDT.isSelected();

        SwingWorker<Void, String> worker = new SwingWorker<>() {
            @Override
            protected Void doInBackground() {
                publish("Veri ayrılıyor (%80-%20)...");
                List<List<UserRecord>> split = BaseAlgorithm.splitData(allData, ratio, 42L);
                trainData = split.get(0);
                testData  = split.get(1);

                publish("Ön işleme yapılıyor...");
                preprocessor = new PreProcessor();
                preprocessor.fitTransform(trainData);
                preprocessor.transform(testData);
                setProgress(20);

                if (runKNN) {
                    publish("KNN eğitiliyor (k=" + k + ")...");
                    KNNClassifier knn = new KNNClassifier(k);
                    knnTrainMs = evaluator.measureTrainTime(knn, trainData);
                    setProgress(40);

                    publish("KNN test ediliyor...");
                    knnResult = evaluator.evaluate(knn, testData);
                    setProgress(60);
                    publish(String.format("KNN tamamlandı. Doğruluk: %.2f%%", knnResult.getAccuracy() * 100));
                }

                if (runDT) {
                    publish("Decision Tree eğitiliyor (maxDepth=" + maxDepth + ")...");
                    DecisionTreeClassifier dt = new DecisionTreeClassifier(maxDepth, minSamples);
                    dtTrainMs = evaluator.measureTrainTime(dt, trainData);
                    setProgress(80);

                    publish("Decision Tree test ediliyor...");
                    dtResult = evaluator.evaluate(dt, testData);
                    setProgress(95);
                    publish(String.format("DT tamamlandı. Doğruluk: %.2f%%", dtResult.getAccuracy() * 100));
                }

                setProgress(100);
                return null;
            }

            @Override
            protected void process(List<String> chunks) {
                for (String msg : chunks) {
                    log(msg);
                    progressBar.setString(msg.length() > 40 ? msg.substring(0, 40) + "..." : msg);
                }
            }

            @Override
            protected void done() {
                try {
                    get();
                } catch (Exception ex) {
                    log("HATA: " + ex.getMessage());
                    ex.printStackTrace();
                }
                updateResults();
                btnRun.setEnabled(true);
                progressBar.setString("Tamamlandı ✔");
            }
        };
        worker.addPropertyChangeListener(e -> {
            if ("progress".equals(e.getPropertyName())) {
                progressBar.setValue((int) e.getNewValue());
            }
        });
        worker.execute();
    }

    private void updateResults() {
        if (knnResult != null) {
            lblKnnAcc.setText(String.format("%.2f%%", knnResult.getAccuracy() * 100));
            lblKnnAcc.setForeground(accuracyColor(knnResult.getAccuracy()));
            lblKnnErr.setText(String.format("%.2f%%", knnResult.getErrorRate() * 100));
            lblKnnTime.setText(knnResult.getElapsedMs() + " ms");
            lblKnnMem.setText(String.format("%.2f MB", knnResult.getMemUsedMB()));
        }
        if (dtResult != null) {
            lblDtAcc.setText(String.format("%.2f%%", dtResult.getAccuracy() * 100));
            lblDtAcc.setForeground(accuracyColor(dtResult.getAccuracy()));
            lblDtErr.setText(String.format("%.2f%%", dtResult.getErrorRate() * 100));
            lblDtTime.setText(dtResult.getElapsedMs() + " ms");
            lblDtMem.setText(String.format("%.2f MB", dtResult.getMemUsedMB()));
        }

        // Confusion matrix
        StringBuilder sb = new StringBuilder();
        if (knnResult != null) {
            sb.append("===== KNN Confusion Matrix =====\n");
            sb.append(formatConfMatrix(knnResult)).append("\n");
        }
        if (dtResult != null) {
            sb.append("===== Decision Tree Confusion Matrix =====\n");
            sb.append(formatConfMatrix(dtResult));
        }
        taConfMatrix.setText(sb.toString());
        taConfMatrix.setCaretPosition(0);

        // Grafik
        chartPanel.setResults(knnResult, dtResult, knnTrainMs, dtTrainMs);
        chartPanel.repaint();
    }

    private String formatConfMatrix(Evaluator.EvaluationResult res) {
        Map<String, Map<String, Integer>> cm = res.getConfusionMatrix();
        Set<String> labels = cm.keySet();
        StringBuilder sb = new StringBuilder();
        sb.append(String.format("%-16s", "Ger.\\ Tah."));
        for (String l : labels) sb.append(String.format("%-13s", shorten(l, 12)));
        sb.append("\n");
        for (String a : labels) {
            sb.append(String.format("%-16s", shorten(a, 15)));
            for (String p : labels) sb.append(String.format("%-13d", cm.get(a).getOrDefault(p, 0)));
            sb.append("\n");
        }
        return sb.toString();
    }

    private String shorten(String s, int max) {
        return s.length() > max ? s.substring(0, max - 1) + "." : s;
    }

    private Color accuracyColor(double acc) {
        if (acc >= 0.75) return SUCCESS;
        if (acc >= 0.55) return WARNING;
        return Color.RED;
    }

    // Yardımcı UI Bileşeni Oluşturucular

    private JPanel createCard(String title) {
        JPanel p = new JPanel();
        p.setLayout(new BoxLayout(p, BoxLayout.Y_AXIS));
        p.setBackground(BG_CARD);
        p.setBorder(BorderFactory.createCompoundBorder(
                new LineBorder(new Color(60, 60, 90), 1, true),
                new EmptyBorder(12, 14, 12, 14)
        ));

        JLabel lbl = new JLabel(title);
        lbl.setFont(FONT_BOLD);
        lbl.setForeground(ACCENT);
        lbl.setBorder(new EmptyBorder(0, 0, 8, 0));
        lbl.setAlignmentX(Component.LEFT_ALIGNMENT);
        p.add(lbl);
        p.setAlignmentX(Component.LEFT_ALIGNMENT);
        p.setMaximumSize(new Dimension(Integer.MAX_VALUE, Integer.MAX_VALUE));
        return p;
    }

    private JButton createStyledButton(String text, Color color) {
        JButton btn = new JButton(text);
        btn.setFont(FONT_BOLD);
        btn.setBackground(color.darker());
        btn.setForeground(Color.WHITE);
        btn.setFocusPainted(false);
        btn.setBorder(BorderFactory.createCompoundBorder(
                new LineBorder(color, 1, true),
                new EmptyBorder(7, 14, 7, 14)
        ));
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btn.setAlignmentX(Component.LEFT_ALIGNMENT);
        btn.setMaximumSize(new Dimension(Integer.MAX_VALUE, 36));
        btn.addMouseListener(new MouseAdapter() {
            public void mouseEntered(MouseEvent e) { btn.setBackground(color); }
            public void mouseExited(MouseEvent e)  { btn.setBackground(color.darker()); }
        });
        return btn;
    }

    private JLabel styledLabel(String text) {
        JLabel l = new JLabel(text);
        l.setFont(FONT_LABEL);
        l.setForeground(TEXT_MAIN);
        return l;
    }

    private JLabel metricLabel(String text) {
        JLabel l = new JLabel(text);
        l.setFont(new Font("Segoe UI", Font.BOLD, 15));
        l.setForeground(ACCENT);
        return l;
    }

    private JLabel metricKeyLabel(String text) {
        JLabel l = new JLabel(text);
        l.setFont(FONT_LABEL);
        l.setForeground(TEXT_SUB);
        return l;
    }

    private void styleSpinner(JSpinner sp) {
        sp.setFont(FONT_LABEL);
        sp.getEditor().getComponent(0).setBackground(BG_DARK);
        ((JSpinner.DefaultEditor) sp.getEditor()).getTextField().setForeground(TEXT_MAIN);
        sp.setBackground(BG_DARK);
        sp.setForeground(TEXT_MAIN);
    }

    private void styleCheckBox(JCheckBox cb) {
        cb.setFont(FONT_LABEL);
        cb.setForeground(TEXT_MAIN);
        cb.setBackground(BG_CARD);
        cb.setAlignmentX(Component.LEFT_ALIGNMENT);
    }

    private void log(String msg) {
        SwingUtilities.invokeLater(() -> {
            taLog.append("[" + new java.text.SimpleDateFormat("HH:mm:ss").format(new Date()) + "] " + msg + "\n");
            taLog.setCaretPosition(taLog.getDocument().getLength());
        });
    }

    // İç Grafik Paneli (sıfırdan çizilmiş, kütüphanesiz)

    static class ChartPanel extends JPanel {

        private Evaluator.EvaluationResult knnRes, dtRes;
        private long knnTrain, dtTrain;

        ChartPanel() {
            setBackground(BG_DARK);
            setPreferredSize(new Dimension(400, 300));
        }

        void setResults(Evaluator.EvaluationResult knn, Evaluator.EvaluationResult dt,
                        long knnTrain, long dtTrain) {
            this.knnRes   = knn;
            this.dtRes    = dt;
            this.knnTrain = knnTrain;
            this.dtTrain  = dtTrain;
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2 = (Graphics2D) g;
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            int w = getWidth(), h = getHeight();

            if (knnRes == null && dtRes == null) {
                g2.setColor(TEXT_SUB);
                g2.setFont(FONT_LABEL);
                String msg = "Grafik için modelleri çalıştırın.";
                FontMetrics fm = g2.getFontMetrics();
                g2.drawString(msg, (w - fm.stringWidth(msg)) / 2, h / 2);
                return;
            }

            // İki grafik: Accuracy ve Süre
            int halfW = w / 2;
            drawBarChart(g2, 0, 0, halfW, h,
                    "Doğruluk (%)",
                    new String[]{"KNN", "DT"},
                    new double[]{
                            knnRes != null ? knnRes.getAccuracy() * 100 : 0,
                            dtRes  != null ? dtRes.getAccuracy()  * 100 : 0
                    },
                    100,
                    new Color[]{ACCENT, ACCENT2});

            drawBarChart(g2, halfW, 0, halfW, h,
                    "Tahmin Süresi (ms)",
                    new String[]{"KNN", "DT"},
                    new double[]{
                            knnRes != null ? knnRes.getElapsedMs() : 0,
                            dtRes  != null ? dtRes.getElapsedMs()  : 0
                    },
                    -1, // otomatik max
                    new Color[]{new Color(253, 196, 78), new Color(72, 199, 142)});
        }

        private void drawBarChart(Graphics2D g2, int x, int y, int w, int h,
                                   String title,
                                   String[] labels, double[] values, double maxVal,
                                   Color[] colors) {
            int pad    = 30;
            int chartX = x + pad + 20;
            int chartY = y + 45;
            int chartW = w - pad * 2 - 20;
            int chartH = h - 80;

            // Arka plan
            g2.setColor(BG_PANEL);
            g2.fillRoundRect(x + 4, y + 4, w - 8, h - 8, 12, 12);

            // Başlık
            g2.setColor(ACCENT);
            g2.setFont(FONT_BOLD);
            FontMetrics fm = g2.getFontMetrics();
            g2.drawString(title, x + (w - fm.stringWidth(title)) / 2, y + 22);

            // Eksenler
            g2.setColor(new Color(80, 80, 110));
            g2.drawLine(chartX, chartY, chartX, chartY + chartH);
            g2.drawLine(chartX, chartY + chartH, chartX + chartW, chartY + chartH);

            // Y ekseni çizgileri
            double max = maxVal > 0 ? maxVal : Arrays.stream(values).max().orElse(1.0) * 1.2;
            if (max == 0) max = 1;
            g2.setFont(new Font("Segoe UI", Font.PLAIN, 10));
            g2.setColor(new Color(80, 80, 110));
            for (int i = 0; i <= 4; i++) {
                int yy = chartY + chartH - (int) ((double) i / 4 * chartH);
                g2.drawLine(chartX - 3, yy, chartX + chartW, yy);
                g2.setColor(TEXT_SUB);
                String valStr = String.format("%.0f", max * i / 4);
                g2.drawString(valStr, x + 2, yy + 4);
                g2.setColor(new Color(80, 80, 110));
            }

            // Barlar
            int barCount = labels.length;
            int totalBarW = chartW - 20;
            int barW = totalBarW / (barCount * 2 + 1);
            int gap  = barW;

            for (int i = 0; i < barCount; i++) {
                double val = values[i];
                int barH   = (int) ((val / max) * chartH);
                int bx     = chartX + gap + i * (barW + gap);
                int by     = chartY + chartH - barH;

                // Gölge
                g2.setColor(new Color(0, 0, 0, 60));
                g2.fillRoundRect(bx + 3, by + 3, barW, barH, 6, 6);

                // Bar
                GradientPaint gp = new GradientPaint(
                        bx, by, colors[i % colors.length].brighter(),
                        bx, by + barH, colors[i % colors.length].darker());
                g2.setPaint(gp);
                g2.fillRoundRect(bx, by, barW, barH, 6, 6);

                // Değer etiketi
                g2.setColor(Color.WHITE);
                g2.setFont(new Font("Segoe UI", Font.BOLD, 11));
                String valStr = String.format("%.1f", val);
                FontMetrics fm2 = g2.getFontMetrics();
                g2.drawString(valStr, bx + (barW - fm2.stringWidth(valStr)) / 2, by - 4);

                // X ekseni etiketi
                g2.setColor(TEXT_MAIN);
                g2.setFont(FONT_LABEL);
                FontMetrics fm3 = g2.getFontMetrics();
                g2.drawString(labels[i], bx + (barW - fm3.stringWidth(labels[i])) / 2,
                        chartY + chartH + 16);
            }
        }
    }
}