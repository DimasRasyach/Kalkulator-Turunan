package com.example.aplikasikalkulatorturunan;

// TurunanFragment.java — versi refactor
// Perubahan vs versi lama:
//   1. Keyboard sticky: padding bawah NestedScrollView = tinggi keyboardPanel
//   2. Langkah penyelesaian pakai RecyclerView + StepAdapter (bukan 4 card statis)
//   3. resetAll() satu tempat untuk clear semua state
//   4. Selebihnya (eval, turunan, grafik) tidak diubah — copy dari kode lama kamu

import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.widget.NestedScrollView;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;

import java.util.ArrayList;
import java.util.List;

public class TurunanFragment extends Fragment {

    // Views
    private EditText         inputFunction;
    private TextView txtHasil;
    private Button           btnHitungBottom;
    private ImageButton      btnClear;
    private LineChart        lineChart;
    private NestedScrollView nestedScrollView;
    private View             keyboardPanel;


    // RecyclerView langkah
    private RecyclerView rvSteps;
    private StepAdapter  stepAdapter;

    // =========================================================
    // Lifecycle
    // =========================================================
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
        setupRecyclerView();
        setupKeyboardSticky();
        setupSystemInsets(view);
        setupKeyboard(view);
        setupChart(view);
        setupButtons();
    }

    // =========================================================
    // Bind views
    // =========================================================
    private void bindViews(View view) {
        inputFunction    = view.findViewById(R.id.inputFunction);
        txtHasil         = view.findViewById(R.id.txtHasil);
        btnHitungBottom  = view.findViewById(R.id.btnHitungBottom);
        btnClear         = view.findViewById(R.id.btnClear);
        lineChart        = view.findViewById(R.id.lineChart);
        nestedScrollView = view.findViewById(R.id.nestedScrollView);
        keyboardPanel    = view.findViewById(R.id.keyboardPanel);
        rvSteps          = view.findViewById(R.id.rvSteps);

        inputFunction.setFocusable(true);
        inputFunction.setFocusableInTouchMode(true);
        inputFunction.setHorizontallyScrolling(true);
        inputFunction.setSingleLine(true);
    }

    // =========================================================
    // RecyclerView setup
    // =========================================================
    private void setupRecyclerView() {
        stepAdapter = new StepAdapter();
        rvSteps.setAdapter(stepAdapter);
        rvSteps.setLayoutManager(new LinearLayoutManager(requireContext()));
        // Nonaktifkan scroll sendiri — biar NestedScrollView yang handle
        rvSteps.setNestedScrollingEnabled(false);
        rvSteps.setHasFixedSize(false);
    }

    // =========================================================
    // Keyboard sticky: beri padding bawah pada NestedScrollView
    // sebesar tinggi keyboardPanel supaya konten tidak tertutup
    // =========================================================
    private void setupKeyboardSticky() {
        keyboardPanel.post(() -> {
            int kbHeight = keyboardPanel.getHeight();
            nestedScrollView.setPadding(
                    nestedScrollView.getPaddingLeft(),
                    nestedScrollView.getPaddingTop(),
                    nestedScrollView.getPaddingRight(),
                    kbHeight
            );
        });
    }

    // =========================================================
    // System insets — hindari gesture bar HP
    // =========================================================
    private void setupSystemInsets(View view) {
        ViewCompat.setOnApplyWindowInsetsListener(view, (v, insets) -> {
            Insets navInsets = insets.getInsets(WindowInsetsCompat.Type.navigationBars());
            // Tambah padding bawah keyboardPanel sesuai gesture bar
            keyboardPanel.setPadding(
                    keyboardPanel.getPaddingLeft(),
                    keyboardPanel.getPaddingTop(),
                    keyboardPanel.getPaddingRight(),
                    navInsets.bottom
            );
            // Update padding scroll area setelah insets diterapkan
            keyboardPanel.post(() -> {
                int kbHeight = keyboardPanel.getHeight();
                nestedScrollView.setPadding(
                        nestedScrollView.getPaddingLeft(),
                        nestedScrollView.getPaddingTop(),
                        nestedScrollView.getPaddingRight(),
                        kbHeight
                );
            });
            return insets;
        });
    }

    // =========================================================
    // Keyboard — sama persis dengan kode lama kamu
    // =========================================================
    private void setupKeyboard(View view) {
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

        // Tombol =
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
        if (btnAC != null) btnAC.setOnClickListener(v -> resetAll());

        // Clear X
        btnClear.setOnClickListener(v -> inputFunction.setText(""));
    }

    private void appendToInput(String text) {
        String cur = inputFunction.getText().toString();
        int pos    = inputFunction.getSelectionStart();
        if (pos < 0) pos = cur.length();
        String newText = cur.substring(0, pos) + text + cur.substring(pos);
        inputFunction.setText(newText);
        inputFunction.setSelection(pos + text.length());
    }

    // =========================================================
    // Hitung turunan — sama dengan kode lama kamu
    // Perbedaan: updateSteps() sekarang pakai RecyclerView
    // =========================================================
    private void setupButtons() {
        btnHitungBottom.setOnClickListener(v -> hitungTurunan());
    }

    private void hitungTurunan() {
        String input = inputFunction.getText().toString().trim();
        if (input.isEmpty()) return;

        try {
            String hasil = turunkanPolinomial(input);
            txtHasil.setText("f'(x) = " + hasil);

            // Update langkah via RecyclerView
            updateSteps(input, hasil);

            // Grafik
            plotGrafik(input, hasil);

            // Legend
            View v = getView();
            if (v != null) {
                TextView lFx  = v.findViewById(R.id.legendFx);
                TextView lDfx = v.findViewById(R.id.legendDfx);
                if (lFx  != null) lFx.setText("f(x) = "  + input);
                if (lDfx != null) lDfx.setText("f'(x) = " + hasil);
            }

            // Scroll ke bagian langkah
            nestedScrollView.post(() ->
                    nestedScrollView.smoothScrollTo(0, rvSteps.getTop())
            );

        } catch (Exception e) {
            txtHasil.setText("f'(x) = Error");
        }
    }

    // =========================================================
    // Update langkah — sekarang pakai StepAdapter
    // =========================================================
    private void updateSteps(String input, String hasil) {
        List<Stepmodel> steps = new ArrayList<>();
        steps.add(new Stepmodel(1,
                "Identifikasi fungsi",
                "Analisis",
                "Fungsi yang diinputkan: f(x) = " + input));
        steps.add(new Stepmodel(2,
                "Gunakan aturan turunan",
                "Power Rule",
                "d/dx(xⁿ) = nxⁿ⁻¹,  d/dx(ax) = a,  d/dx(c) = 0"));
        steps.add(new Stepmodel(3,
                "Turunkan tiap suku",
                "Hitung",
                "Turunkan setiap suku satu per satu sesuai aturan"));
        steps.add(new Stepmodel(4,
                "Hasil akhir",
                "Selesai",
                "f'(x) = " + hasil));

        stepAdapter.submitList(steps);
    }

    // =========================================================
    // Reset semua ke kondisi awal
    // =========================================================
    private void resetAll() {
        inputFunction.setText("");
        txtHasil.setText("");
        lineChart.clear();
        lineChart.invalidate();
        stepAdapter.submitList(new ArrayList<>());

        View v = getView();
        if (v != null) {
            View legend = v.findViewById(R.id.chartLegend);
            if (legend != null) legend.setVisibility(View.GONE);
        }
    }

    // =========================================================
    // Evaluasi ekspresi numerik (tombol =)
    // Sama persis dengan kode lama kamu
    // =========================================================
    private void evaluasiEkspresi() {
        String raw = inputFunction.getText().toString().trim();
        if (raw.isEmpty()) return;
        try {
            double hasil    = eval(normalizeExpr(raw));
            String hasilStr = formatHasil(hasil);
            txtHasil.setText("= " + hasilStr);

            stepAdapter.submitList(new ArrayList<>());
        } catch (Exception e) {
            txtHasil.setText("Error");
        }
    }

    // =========================================================
    // TURUNAN POLINOMIAL — tidak diubah dari kode lama kamu
    // =========================================================
    private String turunkanPolinomial(String expr) {
        String e = expr.replaceAll("\\s+", "")
                .replace("×", "*")
                .replace("÷", "/")
                .replace("²", "^2")
                .replace("π", String.valueOf(Math.PI));

        if (!e.startsWith("-") && !e.startsWith("+")) e = "+" + e;

        List<String> sukuList  = new ArrayList<>();
        List<String> tandaList = new ArrayList<>();

        int start = 0;
        for (int i = 1; i < e.length(); i++) {
            char c = e.charAt(i);
            if ((c == '+' || c == '-') && e.charAt(i - 1) != '^') {
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

    private String turunkanSuku(String suku, String sgn) {
        double sign = sgn.equals("-") ? -1 : 1;
        suku = suku.trim();
        if (suku.isEmpty()) return null;

        if (suku.contains("x^")) {
            String[] parts = suku.split("x\\^");
            double koef    = parseKoef(parts[0]) * sign;
            double exp     = Double.parseDouble(parts[1]);
            return formatSuku(koef * exp, exp - 1);
        }
        if (suku.contains("x")) {
            String kStr = suku.replace("x", "").replace("*", "");
            return formatDouble(parseKoef(kStr) * sign);
        }
        return null; // konstanta → 0
    }

    private double parseKoef(String s) {
        s = s.trim();
        if (s.isEmpty() || s.equals("+")) return 1.0;
        if (s.equals("-"))                return -1.0;
        if (s.endsWith("*")) s = s.substring(0, s.length() - 1);
        try { return Double.parseDouble(s); } catch (Exception e) { return 1.0; }
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
        if (Double.isNaN(d))      return "Tidak terdefinisi";
        if (Double.isInfinite(d)) return d > 0 ? "∞" : "-∞";
        if (d == Math.floor(d) && !Double.isInfinite(d))
            return String.valueOf((long) d);
        return String.format("%.10f", d).replaceAll("0+$", "").replaceAll("\\.$", "");
    }

    // =========================================================
    // EVAL NUMERIK — tidak diubah dari kode lama kamu
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

            boolean eat(int c) {
                while (ch == ' ') nextChar();
                if (ch == c) { nextChar(); return true; }
                return false;
            }

            double parse() {
                nextChar();
                double v = parseExpr();
                if (pos < expr.length()) throw new RuntimeException("Unexpected: " + (char) ch);
                return v;
            }

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
                    v = Double.parseDouble(expr.substring(startPos, this.pos));
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
                } else throw new RuntimeException("Unexpected: " + (char) ch);
                if (eat('^')) v = Math.pow(v, parseFactor());
                return v;
            }
        }.parse();
    }

    // =========================================================
    // GRAFIK — tidak diubah dari kode lama kamu
    // =========================================================
    private void setupChart(View view) {

        lineChart.getDescription().setEnabled(false);

        // TAMBAHAN
        lineChart.setDrawGridBackground(false);
        lineChart.setBackgroundColor(Color.WHITE);

        lineChart.setTouchEnabled(true);
        lineChart.setDragEnabled(true);
        lineChart.setScaleEnabled(true);
        lineChart.setPinchZoom(true);

        lineChart.getLegend().setEnabled(false);

        lineChart.setNoDataText("Masukkan fungsi lalu tekan Hitung Turunan");

        // X Axis
        XAxis xAxis = lineChart.getXAxis();
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);

        xAxis.setDrawGridLines(true);
        xAxis.setGridColor(Color.parseColor("#EEEEEE"));

        xAxis.setAxisLineColor(Color.parseColor("#9CA3AF"));
        xAxis.setTextColor(Color.GRAY);

        // TAMBAHAN
        xAxis.setTextSize(11f);

        // Y Axis kiri
        YAxis leftAxis = lineChart.getAxisLeft();

        leftAxis.setDrawGridLines(true);
        leftAxis.setGridColor(Color.parseColor("#EEEEEE"));

        leftAxis.setAxisLineColor(Color.parseColor("#9CA3AF"));
        leftAxis.setTextColor(Color.GRAY);

        // TAMBAHAN
        leftAxis.setTextSize(11f);

        // RANGE BIAR GRAFIK ENAK
        leftAxis.setAxisMinimum(-20f);
        leftAxis.setAxisMaximum(40f);

        lineChart.getAxisRight().setEnabled(false);

        // TAMBAHAN
        lineChart.setExtraOffsets(8f, 8f, 8f, 16f);

        Button btnZoomIn  = view.findViewById(R.id.btnZoomIn);
        Button btnZoomOut = view.findViewById(R.id.btnZoomOut);

        if (btnZoomIn != null)
            btnZoomIn.setOnClickListener(v -> lineChart.zoomIn());

        if (btnZoomOut != null)
            btnZoomOut.setOnClickListener(v -> lineChart.zoomOut());
    }

    private void plotGrafik(String fxExpr, String dfxExpr) {
        ArrayList<Entry> fxPts  = new ArrayList<>();
        ArrayList<Entry> dfxPts = new ArrayList<>();

        String fBase  = normalizeExpr(fxExpr);
        String dfBase = normalizeExpr(dfxExpr);

        for (int xi = -60; xi <= 60; xi++) {
            double x    = xi / 10.0;
            String xStr = (x < 0) ? "(" + x + ")" : String.valueOf(x);

            try {
                double fy = eval(fBase.replace("x", xStr));
                if (!Double.isNaN(fy) && !Double.isInfinite(fy) && Math.abs(fy) < 1000)
                    fxPts.add(new Entry((float) x, (float) fy));
            } catch (Exception ignored) {}

            try {
                double dfy = eval(dfBase.replace("x", xStr));
                if (!Double.isNaN(dfy) && !Double.isInfinite(dfy) && Math.abs(dfy) < 1000)
                    dfxPts.add(new Entry((float) x, (float) dfy));
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
        lineChart.getAxisLeft().setSpaceTop(10f);
        lineChart.getAxisLeft().setSpaceBottom(10f);
        lineChart.animateX(500);
        lineChart.invalidate();

        View v = getView();
        if (v != null) {
            View legend = v.findViewById(R.id.chartLegend);
            if (legend != null) legend.setVisibility(View.VISIBLE);
        }
    }
}