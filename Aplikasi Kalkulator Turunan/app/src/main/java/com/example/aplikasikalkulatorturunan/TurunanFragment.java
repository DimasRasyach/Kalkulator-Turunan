// BiasaFragment.java
// Ganti "com.example.kalkulatorturunan" dengan package name project kamu

package com.example.aplikasikalkulatorturunan;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowInsets;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.fragment.app.Fragment;

import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;

import java.util.ArrayList;

public class TurunanFragment extends Fragment {

    private EditText inputFunction;
    private TextView txtHasil, txtHasilAkhir;
    private Button btnHitung, btnHitungBottom;
    private ImageButton btnClear;
    private LineChart lineChart;
    private LinearLayout rootLayout;

    private LinearLayout[] stepHeaders  = new LinearLayout[4];
    private TextView[]     stepDetails  = new TextView[4];
    private ImageView[]    stepArrows   = new ImageView[4];
    private boolean[]      stepExpanded = {false, false, false, false};

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_turunan, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        bindViews(view);
        fixSystemWindowInsets(view);
        setupStepAccordion();
        setupKeyboard(view);
        setupChart(view);
        setupButtons();
    }

    // =========================================================
    // BIND
    // =========================================================
    private void bindViews(View view) {
        inputFunction   = view.findViewById(R.id.inputFunction);
        txtHasil        = view.findViewById(R.id.txtHasil);
        txtHasilAkhir   = view.findViewById(R.id.txtHasilAkhir);
        btnHitung       = view.findViewById(R.id.btnHitung);
        btnHitungBottom = view.findViewById(R.id.btnHitungBottom);
        btnClear        = view.findViewById(R.id.btnClear);
        lineChart       = view.findViewById(R.id.lineChart);
        rootLayout      = view.findViewById(R.id.rootLinearLayout);

        stepHeaders[0] = view.findViewById(R.id.step1Header);
        stepHeaders[1] = view.findViewById(R.id.step2Header);
        stepHeaders[2] = view.findViewById(R.id.step3Header);
        stepHeaders[3] = view.findViewById(R.id.step4Header);

        stepDetails[0] = view.findViewById(R.id.step1Detail);
        stepDetails[1] = view.findViewById(R.id.step2Detail);
        stepDetails[2] = view.findViewById(R.id.step3Detail);
        stepDetails[3] = view.findViewById(R.id.step4Detail);

        stepArrows[0] = view.findViewById(R.id.step1Arrow);
        stepArrows[1] = view.findViewById(R.id.step2Arrow);
        stepArrows[2] = view.findViewById(R.id.step3Arrow);
        stepArrows[3] = view.findViewById(R.id.step4Arrow);

        // EditText bisa dilihat & discroll horizontal
        inputFunction.setFocusable(true);
        inputFunction.setFocusableInTouchMode(true);
        inputFunction.setHorizontallyScrolling(true);
        inputFunction.setSingleLine(true);
    }

    // =========================================================
    // FIX: keyboard tidak ketimpah navbar HP
    // =========================================================
    private void fixSystemWindowInsets(View view) {
        ViewCompat.setOnApplyWindowInsetsListener(view, (v, insets) -> {
            Insets navInsets = insets.getInsets(WindowInsetsCompat.Type.navigationBars());
            if (rootLayout != null) {
                rootLayout.setPadding(
                        rootLayout.getPaddingLeft(),
                        rootLayout.getPaddingTop(),
                        rootLayout.getPaddingRight(),
                        navInsets.bottom + 16
                );
            }
            return insets;
        });
    }

    // =========================================================
    // ACCORDION
    // =========================================================
    private void setupStepAccordion() {
        for (int i = 0; i < 4; i++) {
            final int idx = i;
            if (stepHeaders[i] != null) {
                stepHeaders[i].setOnClickListener(v -> {
                    stepExpanded[idx] = !stepExpanded[idx];
                    if (stepDetails[idx] != null)
                        stepDetails[idx].setVisibility(stepExpanded[idx] ? View.VISIBLE : View.GONE);
                    if (stepArrows[idx] != null)
                        stepArrows[idx].setRotation(stepExpanded[idx] ? 180f : 0f);
                });
            }
        }
    }

    // =========================================================
    // KEYBOARD
    // Simbol yang tampil di input sudah diperbaiki:
    //   sqrt( → √(    *  → ×    ^2 → ²   dll
    // =========================================================
    private void setupKeyboard(View view) {

        // { id tombol, teks yang diappend ke input }
        int[]    ids  = {
                R.id.btnSin, R.id.btnCos, R.id.btnTan, R.id.btnLn,  R.id.btnLog,
                R.id.btnSqrt, R.id.btnSquare, R.id.btnPow, R.id.btnPi, R.id.btnE,
                R.id.btn7, R.id.btn8, R.id.btn9, R.id.btnDivide,
                R.id.btn4, R.id.btn5, R.id.btn6, R.id.btnMultiply, R.id.btnCaret,
                R.id.btn1, R.id.btn2, R.id.btn3, R.id.btnMinus, R.id.btnOpenParen,
                R.id.btn0, R.id.btnDot, R.id.btnCloseParen, R.id.btnPlus,
                R.id.btnX
        };
        String[] vals = {
                "sin(", "cos(", "tan(", "ln(",  "log(",
                "√(",   "²",    "^",    "π",    "e",
                "7",    "8",    "9",    "÷",
                "4",    "5",    "6",    "×",    "^",
                "1",    "2",    "3",    "-",    "(",
                "0",    ".",    ")",    "+",
                "x"
        };

        for (int i = 0; i < ids.length; i++) {
            final String val = vals[i];
            Button btn = view.findViewById(ids[i]);
            if (btn != null) btn.setOnClickListener(v -> appendToInput(val));
        }

        // Tombol = → evaluasi ekspresi
        Button btnEquals = view.findViewById(R.id.btnEquals);
        if (btnEquals != null) btnEquals.setOnClickListener(v -> evaluasiEkspresi());

        // Backspace
        Button btnBackspace = view.findViewById(R.id.btnBackspace);
        if (btnBackspace != null) {
            btnBackspace.setOnClickListener(v -> {
                String t = inputFunction.getText().toString();
                if (!t.isEmpty()) {
                    inputFunction.setText(t.substring(0, t.length() - 1));
                    inputFunction.setSelection(inputFunction.getText().length());
                }
            });
        }

        // AC
        Button btnAC = view.findViewById(R.id.btnAC);
        if (btnAC != null) btnAC.setOnClickListener(v -> {
            inputFunction.setText("");
            txtHasil.setText("");
            txtHasilAkhir.setText("");
            lineChart.clear();
            lineChart.invalidate();
            resetSteps();
        });

        // Clear X
        btnClear.setOnClickListener(v -> inputFunction.setText(""));
    }

    private void appendToInput(String text) {
        String cur = inputFunction.getText().toString();
        int pos = inputFunction.getSelectionStart();
        if (pos < 0) pos = cur.length();
        String newText = cur.substring(0, pos) + text + cur.substring(pos);
        inputFunction.setText(newText);
        inputFunction.setSelection(pos + text.length());
    }

    // =========================================================
    // EVALUASI EKSPRESI UMUM (bukan hanya turunan)
    // Mendukung: +, -, ×/*, ÷//, ^, √(), sin, cos, tan, ln, log, π, e
    // =========================================================
    private void evaluasiEkspresi() {
        String raw = inputFunction.getText().toString().trim();
        if (raw.isEmpty()) return;
        try {
            double hasil = eval(normalizeExpr(raw));
            String hasilStr = formatHasil(hasil);
            txtHasil.setText("= " + hasilStr);
            txtHasilAkhir.setText("= " + hasilStr);
            resetSteps();
        } catch (Exception e) {
            txtHasil.setText("Error");
            txtHasilAkhir.setText("Error");
        }
    }

    private void setupButtons() {
        btnHitung.setOnClickListener(v -> hitungTurunan());
        btnHitungBottom.setOnClickListener(v -> hitungTurunan());
    }

    // =========================================================
    // HITUNG TURUNAN (polinomial sederhana)
    // =========================================================
    private void hitungTurunan() {
        String input = inputFunction.getText().toString().trim();
        if (input.isEmpty()) return;

        try {
            String hasil = turunkanPolinomial(input);
            txtHasil.setText("f'(x) = " + hasil);
            txtHasilAkhir.setText("f'(x) = " + hasil);
            updateSteps(input, hasil);
            
            // Format ulang hasil untuk evaluasi grafik (hilangkan f'(x) =)
            plotGrafik(input, hasil);

            // Update legend
            View v = getView();
            if (v != null) {
                TextView lFx  = v.findViewById(R.id.legendFx);
                TextView lDfx = v.findViewById(R.id.legendDfx);
                if (lFx  != null) lFx.setText("f(x) = "  + input);
                if (lDfx != null) lDfx.setText("f'(x) = " + hasil);
            }
        } catch (Exception e) {
            txtHasil.setText("f'(x) = Error");
            txtHasilAkhir.setText("Error");
        }
    }

    // =========================================================
    // TURUNAN POLINOMIAL
    // Mendukung: ax^n, ax, a (konstanta), penjumlahan/pengurangan suku
    // =========================================================
    private String turunkanPolinomial(String expr) {
        // Normalisasi: hilangkan spasi, ganti simbol tampilan → simbol kalkulasi
        String e = expr.replaceAll("\\s+", "")
                .replace("×", "*")
                .replace("÷", "/")
                .replace("²", "^2")
                .replace("π", String.valueOf(Math.PI));

        // Pisah per suku (split +/-)
        // Tambah + di depan jika suku pertama positif agar mudah di-split
        if (!e.startsWith("-") && !e.startsWith("+")) e = "+" + e;
        
        java.util.List<String> sukuList = new ArrayList<>();
        java.util.List<String> tandaList = new ArrayList<>();
        
        int start = 0;
        for (int i = 1; i < e.length(); i++) {
            char c = e.charAt(i);
            // Cari + atau - yang bukan merupakan bagian dari eksponen (setelah ^)
            if ((c == '+' || c == '-') && e.charAt(i-1) != '^') {
                tandaList.add(String.valueOf(e.charAt(start)));
                sukuList.add(e.substring(start + 1, i));
                start = i;
            }
        }
        tandaList.add(String.valueOf(e.charAt(start)));
        sukuList.add(e.substring(start + 1));

        StringBuilder hasil = new StringBuilder();
        for (int i = 0; i < sukuList.size(); i++) {
            String s   = sukuList.get(i);
            String sgn = tandaList.get(i);
            String d   = turunkanSuku(s, sgn);
            if (d != null && !d.isEmpty() && !d.equals("0")) {
                if (hasil.length() > 0 && !d.startsWith("-")) hasil.append("+");
                hasil.append(d);
            }
        }
        return hasil.length() == 0 ? "0" : hasil.toString();
    }

    // Turunkan satu suku: sgn adalah "+" atau "-"
    private String turunkanSuku(String suku, String sgn) {
        double sign = sgn.equals("-") ? -1 : 1;
        suku = suku.trim();
        if (suku.isEmpty()) return null;

        // Bentuk ax^n
        if (suku.contains("x^")) {
            String[] parts = suku.split("x\\^");
            double koef = parseKoef(parts[0]) * sign;
            double exp  = Double.parseDouble(parts[1]);
            double newKoef = koef * exp;
            double newExp  = exp - 1;
            return formatSuku(newKoef, newExp);
        }
        // Bentuk ax (pangkat 1)
        if (suku.contains("x")) {
            String kStr = suku.replace("x", "").replace("*", "");
            double koef = parseKoef(kStr) * sign;
            return formatDouble(koef);
        }
        // Konstanta
        return null;
    }

    private double parseKoef(String s) {
        s = s.trim();
        if (s.isEmpty() || s.equals("+")) return 1.0;
        if (s.equals("-")) return -1.0;
        // Hapus tanda perkalian jika ada, misal "3*"
        if (s.endsWith("*")) s = s.substring(0, s.length() - 1);
        try {
            return Double.parseDouble(s);
        } catch (Exception e) {
            return 1.0;
        }
    }

    private String formatSuku(double koef, double exp) {
        if (koef == 0) return null;
        String koefStr = (koef == 1) ? "" : (koef == -1) ? "-" : formatDouble(koef);
        if (exp == 0) return formatDouble(koef);
        if (exp == 1) return koefStr + "x";
        return koefStr + "x^" + formatDouble(exp);
    }

    private String formatDouble(double d) {
        if (d == Math.floor(d) && !Double.isInfinite(d))
            return String.valueOf((long) d);
        return String.valueOf(d);
    }

    private String formatHasil(double d) {
        if (Double.isNaN(d)) return "Tidak terdefinisi";
        if (Double.isInfinite(d)) return d > 0 ? "∞" : "-∞";
        if (d == Math.floor(d) && !Double.isInfinite(d))
            return String.valueOf((long) d);
        // Bulatkan 10 desimal
        return String.format("%.10f", d).replaceAll("0+$","").replaceAll("\\.$","");
    }

    // =========================================================
    // EVALUASI EKSPRESI NUMERIK
    // =========================================================
    private String normalizeExpr(String e) {
        return e.replace("×", "*")
                .replace("÷", "/")
                .replace("²", "^2")
                .replace("√", "sqrt")
                .replace("π", String.valueOf(Math.PI))
                .replace("e", String.valueOf(Math.E))
                .replaceAll("(\\d)(x)", "$1*$2")
                .replaceAll("(x)(\\d)", "$1*$2");
    }

    private double eval(String expr) {
        return new Object() {
            int pos = -1, ch;

            void nextChar() { ch = (++pos < expr.length()) ? expr.charAt(pos) : -1; }

            boolean eat(int charToEat) {
                while (ch == ' ') nextChar();
                if (ch == charToEat) { nextChar(); return true; }
                return false;
            }

            double parse() { nextChar(); double v = parseExpr(); if (pos < expr.length()) throw new RuntimeException("Unexpected: " + (char)ch); return v; }

            double parseExpr() {
                double v = parseTerm();
                for (;;) {
                    if      (eat('+')) v += parseTerm();
                    else if (eat('-')) v -= parseTerm();
                    else return v;
                }
            }

            double parseTerm() {
                double v = parseFactor();
                for (;;) {
                    if      (eat('*')) v *= parseFactor();
                    else if (eat('/')) v /= parseFactor();
                    else return v;
                }
            }

            double parseFactor() {
                if (eat('+')) return +parseFactor();
                if (eat('-')) return -parseFactor();
                double v;
                int startPos = this.pos;
                if (eat('(')) { v = parseExpr(); eat(')'); }
                else if ((ch >= '0' && ch <= '9') || ch == '.') {
                    while ((ch >= '0' && ch <= '9') || ch == '.') nextChar();
                    String numStr = expr.substring(startPos, this.pos);
                    v = Double.parseDouble(numStr);
                } else if (ch >= 'a' && ch <= 'z') {
                    while (ch >= 'a' && ch <= 'z') nextChar();
                    String fn = expr.substring(startPos, this.pos);
                    if (eat('(')) { v = parseExpr(); eat(')'); } else { v = parseFactor(); }
                    switch (fn) {
                        case "sqrt": v = Math.sqrt(v); break;
                        case "sin":  v = Math.sin(Math.toRadians(v)); break;
                        case "cos":  v = Math.cos(Math.toRadians(v)); break;
                        case "tan":  v = Math.tan(Math.toRadians(v)); break;
                        case "ln":   v = Math.log(v); break;
                        case "log":  v = Math.log10(v); break;
                        case "abs":  v = Math.abs(v); break;
                        default: throw new RuntimeException("Unknown fn: " + fn);
                    }
                } else throw new RuntimeException("Unexpected: " + (char)ch);
                if (eat('^')) v = Math.pow(v, parseFactor());
                return v;
            }
        }.parse();
    }

    // =========================================================
    // UPDATE LANGKAH PENYELESAIAN
    // =========================================================
    private void updateSteps(String input, String hasil) {
        String[] labels  = {"Identifikasi fungsi", "Gunakan aturan turunan", "Turunkan tiap suku", "Sederhanakan"};
        String[] details = {
                "Fungsi yang diinputkan: f(x) = " + input,
                "Gunakan aturan: d/dx(xⁿ) = nxⁿ⁻¹, d/dx(ax) = a, d/dx(c) = 0",
                "Turunkan setiap suku satu per satu",
                "Hasil turunan: f'(x) = " + hasil
        };

        View v = getView();
        if (v == null) return;

        int[] headerIds = {R.id.step1Header, R.id.step2Header, R.id.step3Header, R.id.step4Header};
        int[] detailIds = {R.id.step1Detail, R.id.step2Detail, R.id.step3Detail, R.id.step4Detail};

        for (int i = 0; i < 4; i++) {
            LinearLayout header = v.findViewById(headerIds[i]);
            TextView     detail = v.findViewById(detailIds[i]);
            if (header != null) {
                // TextView kedua di header = label
                TextView lbl = (TextView) header.getChildAt(1);
                if (lbl != null) lbl.setText(labels[i]);
            }
            if (detail != null) detail.setText(details[i]);
        }
    }

    private void resetSteps() {
        View v = getView();
        if (v == null) return;
        int[] headerIds = {R.id.step1Header, R.id.step2Header, R.id.step3Header, R.id.step4Header};
        int[] detailIds = {R.id.step1Detail, R.id.step2Detail, R.id.step3Detail, R.id.step4Detail};
        for (int i = 0; i < 4; i++) {
            LinearLayout header = v.findViewById(headerIds[i]);
            TextView detail = v.findViewById(detailIds[i]);
            if (header != null) { TextView lbl = (TextView) header.getChildAt(1); if (lbl != null) lbl.setText(""); }
            if (detail != null) { detail.setText(""); detail.setVisibility(View.GONE); }
            stepExpanded[i] = false;
            if (stepArrows[i] != null) stepArrows[i].setRotation(0f);
        }
    }

    // =========================================================
    // GRAFIK
    // =========================================================
    private void setupChart(View view) {
        lineChart.getDescription().setEnabled(false);
        lineChart.getLegend().setEnabled(false);
        lineChart.setTouchEnabled(true);
        lineChart.setPinchZoom(true);
        lineChart.setDrawGridBackground(false);
        lineChart.setNoDataText("Masukkan fungsi lalu tekan Hitung Turunan");

        XAxis xAxis = lineChart.getXAxis();
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        xAxis.setDrawGridLines(true);
        xAxis.setGridColor(0xFFE5E7EB);
        xAxis.setAxisLineColor(0xFF111827);
        xAxis.setTextColor(0xFF6B7280);

        lineChart.getAxisLeft().setDrawGridLines(true);
        lineChart.getAxisLeft().setGridColor(0xFFE5E7EB);
        lineChart.getAxisLeft().setAxisLineColor(0xFF111827);
        lineChart.getAxisLeft().setTextColor(0xFF6B7280);
        lineChart.getAxisRight().setEnabled(false);

        // Tombol zoom
        Button btnZoomIn = view.findViewById(R.id.btnZoomIn);
        Button btnZoomOut = view.findViewById(R.id.btnZoomOut);
        if (btnZoomIn  != null) btnZoomIn.setOnClickListener(v2 -> lineChart.zoomIn());
        if (btnZoomOut != null) btnZoomOut.setOnClickListener(v2 -> lineChart.zoomOut());
    }

    private void plotGrafik(String fxExpr, String dfxExpr) {
        ArrayList<Entry> fxPts  = new ArrayList<>();
        ArrayList<Entry> dfxPts = new ArrayList<>();

        // Normalisasi dasar sekali saja
        String fBase = normalizeExpr(fxExpr);
        String dfBase = normalizeExpr(dfxExpr);

        for (int xi = -60; xi <= 60; xi++) {
            double x = xi / 10.0;
            // Gunakan string pengganti yang aman: jika x negatif, bungkus kurung
            String xStr = (x < 0) ? "(" + x + ")" : String.valueOf(x);

            try {
                // f(x)
                String fEval = fBase.replace("x", xStr);
                double fy = eval(fEval);
                if (!Double.isNaN(fy) && !Double.isInfinite(fy) && Math.abs(fy) < 1000) {
                    fxPts.add(new Entry((float)x, (float) fy));
                }
            } catch (Exception ignored) {}

            try {
                // f'(x)
                String dfEval = dfBase.replace("x", xStr);
                double dfy = eval(dfEval);
                if (!Double.isNaN(dfy) && !Double.isInfinite(dfy) && Math.abs(dfy) < 1000) {
                    dfxPts.add(new Entry((float)x, (float) dfy));
                }
            } catch (Exception ignored) {}
        }

        if (fxPts.isEmpty() && dfxPts.isEmpty()) {
            lineChart.clear();
            lineChart.setNoDataText("Grafik tidak dapat ditampilkan untuk fungsi ini");
            lineChart.invalidate();
            return;
        }

        LineData lineData = new LineData();

        if (!fxPts.isEmpty()) {
            LineDataSet dsFx = new LineDataSet(fxPts, "f(x)");
            dsFx.setColor(0xFF3B6FFF);
            dsFx.setLineWidth(2.5f);
            dsFx.setDrawCircles(false);
            dsFx.setDrawValues(false);
            dsFx.setMode(LineDataSet.Mode.CUBIC_BEZIER);
            lineData.addDataSet(dsFx);
        }

        if (!dfxPts.isEmpty()) {
            LineDataSet dsDfx = new LineDataSet(dfxPts, "f'(x)");
            dsDfx.setColor(0xFFFF4444);
            dsDfx.setLineWidth(2.5f);
            dsDfx.setDrawCircles(false);
            dsDfx.setDrawValues(false);
            dsDfx.setMode(LineDataSet.Mode.CUBIC_BEZIER);
            lineData.addDataSet(dsDfx);
        }

        lineChart.setData(lineData);
        lineChart.animateX(500);
        
        // Atur range sumbu Y agar tidak terlalu zoom out jika ada nilai ekstrim
        lineChart.getAxisLeft().setSpaceTop(10f);
        lineChart.getAxisLeft().setSpaceBottom(10f);
        
        lineChart.invalidate();

        View v = getView();
        if (v != null) {
            View legend = v.findViewById(R.id.chartLegend);
            if (legend != null) legend.setVisibility(View.VISIBLE);
        }
    }

    private void updateChart(String fxExpr, String dfxExpr) {
        plotGrafik(fxExpr, dfxExpr);
    }
}