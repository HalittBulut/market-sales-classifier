package com.prolab.data;

import com.prolab.model.UserRecord;
import java.io.*;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

/*
 .xlsx / .csv dosyasını okuyarak UserRecord listesi oluşturur.
 
  Sütun sırası (0-indexed):
    5:  AMOUNT          
    6:  PRICE           
   7:  LINENETTTOTAL
    9:  CLIENTCODE
   10: BRANDCODE
    11: BRAND
   12: CATEGORY_NAME  
  17: GENDER
 */
public class DataLoader {

    private static final int COL_CLIENTCODE    = 9;
    private static final int COL_GENDER        = 17;
    private static final int COL_AMOUNT        = 5;
    private static final int COL_PRICE         = 6;
    private static final int COL_LINENETTTOTAL = 7;
    private static final int COL_BRAND         = 11;
    private static final int COL_BRANDCODE     = 10;
    private static final int COL_CATEGORY      = 12;

    public List<UserRecord> loadFromXlsx(String filePath) throws IOException {
        List<UserRecord> records = new ArrayList<>();
        try {
            Class<?> wbFactory = Class.forName("org.apache.poi.ss.usermodel.WorkbookFactory");
            Object workbook = wbFactory.getMethod("create", File.class).invoke(null, new File(filePath));

            Class<?> dfClass = Class.forName("org.apache.poi.ss.usermodel.DataFormatter");
            Object df = dfClass.getDeclaredConstructor().newInstance();
            Method fmtMethod = dfClass.getMethod("formatCellValue",
                    Class.forName("org.apache.poi.ss.usermodel.Cell"));

            Class<?> wbClass = Class.forName("org.apache.poi.ss.usermodel.Workbook");
            Object sheet = wbClass.getMethod("getSheetAt", int.class).invoke(workbook, 0);

            Class<?> sheetClass = Class.forName("org.apache.poi.ss.usermodel.Sheet");
            java.util.Iterator<?> rowIter =
                    (java.util.Iterator<?>) sheetClass.getMethod("iterator").invoke(sheet);

            Class<?> rowClass = Class.forName("org.apache.poi.ss.usermodel.Row");
            Method getCell = rowClass.getMethod("getCell", int.class);

            boolean firstRow = true;
            int lineNum = 0;

            while (rowIter.hasNext()) {
                Object row = rowIter.next();
                lineNum++;
                if (firstRow) { firstRow = false; continue; }
                try {
                    String clientCodeStr = fmt(df, fmtMethod, getCell.invoke(row, COL_CLIENTCODE));
                    String gender        = fmt(df, fmtMethod, getCell.invoke(row, COL_GENDER));
                    String amountStr     = fmt(df, fmtMethod, getCell.invoke(row, COL_AMOUNT));
                    String priceStr      = fmt(df, fmtMethod, getCell.invoke(row, COL_PRICE));
                    String lineNetStr    = fmt(df, fmtMethod, getCell.invoke(row, COL_LINENETTTOTAL));
                    String brand         = fmt(df, fmtMethod, getCell.invoke(row, COL_BRAND));
                    String brandCodeStr  = fmt(df, fmtMethod, getCell.invoke(row, COL_BRANDCODE));
                    String category      = fmt(df, fmtMethod, getCell.invoke(row, COL_CATEGORY));

                    if (gender.isEmpty() || category.isEmpty()) continue;

                    records.add(new UserRecord(
                            (int) parseDouble(clientCodeStr),
                            gender.trim(),
                            parseDouble(amountStr),
                            parseDouble(priceStr),
                            parseDouble(lineNetStr),
                            brand.trim(),
                            (int) parseDouble(brandCodeStr),
                            category.trim()));
                } catch (Exception e) {
                    Throwable c = e.getCause() != null ? e.getCause() : e;
                    System.err.printf("[DataLoader] Satır %d atlandı: %s%n", lineNum, c.getMessage());
                }
            }
            wbClass.getMethod("close").invoke(workbook);

        } catch (ClassNotFoundException e) {
            throw new IOException("Apache POI bulunamadı. poi-ooxml-5.x.jar ekleyin.", e);
        } catch (java.lang.reflect.InvocationTargetException e) {
            Throwable real = e.getCause() != null ? e.getCause() : e;
            throw new IOException("XLSX okuma hatası: " + real.getMessage(), real);
        
        } catch (Exception e) {
            throw new IOException("Beklenmedik hata: " + e.getMessage(), e);
        }
        System.out.printf("[DataLoader] %d kayıt yüklendi (XLSX).%n", records.size());
        return records;
    }

    public List<UserRecord> loadFromCsv(String filePath) throws IOException {
        List<UserRecord> records = new ArrayList<>();
        try (BufferedReader br = new BufferedReader(
                new InputStreamReader(new FileInputStream(filePath), "UTF-8"))) {
            String line;
            boolean first = true;
            int lineNum = 0;
            while ((line = br.readLine()) != null) {
                lineNum++;
                if (first) { first = false; continue; }
                line = line.trim();
                if (line.isEmpty()) continue;
                try {
                    String[] p = splitCsvLine(line);
                    if (p.length <= COL_GENDER) continue;
                    String gender   = p[COL_GENDER].trim();
                    String category = p[COL_CATEGORY].trim();
                    if (gender.isEmpty() || category.isEmpty()) continue;
                    records.add(new UserRecord(
                            (int) parseDouble(p[COL_CLIENTCODE].trim()),
                            gender,
                            parseDouble(p[COL_AMOUNT].trim()),
                            parseDouble(p[COL_PRICE].trim()),
                            parseDouble(p[COL_LINENETTTOTAL].trim()),
                            p[COL_BRAND].trim(),
                            (int) parseDouble(p[COL_BRANDCODE].trim()),
                            category));
                } catch (NumberFormatException e) {
                    System.err.printf("[DataLoader] Satır %d sayısal hata: %s%n", lineNum, e.getMessage());
                }
            }
        }
        System.out.printf("[DataLoader] %d kayıt yüklendi (CSV).%n", records.size());
        return records;
    }

    private String fmt(Object df, Method m, Object cell) throws Exception {
        if (cell == null) return "";
        String v = (String) m.invoke(df, cell);
        return v == null ? "" : v.trim();
    }

    private double parseDouble(String s) {
        if (s == null || s.isEmpty()) return 0.0;
        if (s.contains(",") && s.contains(".")) s = s.replace(".", "").replace(",", ".");
        else if (s.contains(",")) s = s.replace(",", ".");
        s = s.replaceAll("[^0-9.\\-]", "");
        return s.isEmpty() ? 0.0 : Double.parseDouble(s);
    }

    private String[] splitCsvLine(String line) {
        List<String> t = new ArrayList<>();
        StringBuilder sb = new StringBuilder();
        boolean inQ = false;
        for (char c : line.toCharArray()) {
            if (c == '"') inQ = !inQ;
            else if (c == ',' && !inQ) { t.add(sb.toString()); sb.setLength(0); }
            else sb.append(c);
        }
        t.add(sb.toString());
        return t.toArray(new String[0]);
    }
}