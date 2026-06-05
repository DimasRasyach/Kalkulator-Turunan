package com.example.aplikasikalkulatorturunan;
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
    private TextView         txtHasil;
    private Button           btnHitungBottom;
    private ImageButton      btnClear;
    private LineChart        lineChart;
    private NestedScrollView nestedScrollView;
    private View             keyboardPanel, btnHideKeyboard;

    // RecyclerView langkah
    private RecyclerView rvSteps;
    private StepAdapter  stepAdapter;

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

    private void bindViews(View view) {
        inputFunction = view.findViewById(R.id.inputFunction);
        inputFunction.setShowSoftInputOnFocus(false); // Matikan soft keyboard bawaan HP

        txtHasil         = view.findViewById(R.id.txtHasil);
        btnHitungBottom  = view.findViewById(R.id.btnHitungBottom);
        btnClear         = view.findViewById(R.id.btnClear);
        lineChart        = view.findViewById(R.id.lineChart);
        nestedScrollView = view.findViewById(R.id.nestedScrollView);
        keyboardPanel    = view.findViewById(R.id.keyboardPanel);
        btnHideKeyboard  = view.findViewById(R.id.btnHideKeyboard);
        rvSteps          = view.findViewById(R.id.rvSteps);

        inputFunction.setFocusable(true);
        inputFunction.setFocusableInTouchMode(true);
        inputFunction.setHorizontallyScrolling(true);
        inputFunction.setSingleLine(true);
    }

    private void setupRecyclerView() {
        stepAdapter = new StepAdapter();
        rvSteps.setAdapter(stepAdapter);
        rvSteps.setLayoutManager(new LinearLayoutManager(requireContext()));
        rvSteps.setNestedScrollingEnabled(false);
        rvSteps.setHasFixedSize(false);
    }

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

    private void setupSystemInsets(View view) {
        ViewCompat.setOnApplyWindowInsetsListener(view, (v, insets) -> {
            Insets navInsets = insets.getInsets(WindowInsetsCompat.Type.navigationBars());
            keyboardPanel.setPadding(
                    keyboardPanel.getPaddingLeft(),
                    keyboardPanel.getPaddingTop(),
                    keyboardPanel.getPaddingRight(),
                    navInsets.bottom
            );
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

    private void setupKeyboard(View view) {
        inputFunction.setOnClickListener(v -> showCustomKeyboard());

        if (btnHideKeyboard != null) {
            btnHideKeyboard.setOnClickListener(v -> hideCustomKeyboard());
        }

        // Menghubungkan ID tombol XML kustom kamu
        int[] ids = {
                R.id.btnSin, R.id.btnCos, R.id.btnTan, R.id.btnLn,  R.id.btnLog,
                R.id.btnSqrt, R.id.btnSquare, R.id.btnPow, R.id.btnPi, R.id.btnE,
                R.id.btn7, R.id.btn8, R.id.btn9, R.id.btnDivide, // btnDivide di XML-mu bertuliskan ÷
                R.id.btn4, R.id.btn5, R.id.btn6, R.id.btnMultiply, R.id.btnCaret,
                R.id.btn1, R.id.btn2, R.id.btn3, R.id.btnMinus, R.id.btnOpenParen,
                R.id.btn0, R.id.btnDot, R.id.btnCloseParen, R.id.btnPlus,
                R.id.btnX
        };

        // DISINI KUNCINYA: R.id.btnDivide (indeks ke-13) diisi dengan "/"
        // sehingga saat ditekan, karakter yang diketikkan ke EditText langsung berubah jadi "/"
        String[] vals = {
                "sin(", "cos(", "tan(", "ln(",  "log(",
                "√(",   "²",    "^",    "π",    "e",
                "7",    "8",    "9",    "/",
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

        Button btnEquals = view.findViewById(R.id.btnEquals);
        if (btnEquals != null) btnEquals.setOnClickListener(v -> hitungTurunan());

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

        Button btnAC = view.findViewById(R.id.btnAC);
        if (btnAC != null) btnAC.setOnClickListener(v -> resetAll());

        btnClear.setOnClickListener(v -> resetAll());
    }

    private void appendToInput(String text) {
        String cur = inputFunction.getText().toString();
        int pos    = inputFunction.getSelectionStart();
        if (pos < 0) pos = cur.length();
        String newText = cur.substring(0, pos) + text + cur.substring(pos);
        inputFunction.setText(newText);
        inputFunction.setSelection(pos + text.length());
    }

    private void hideCustomKeyboard() {
        if (keyboardPanel != null) {
            keyboardPanel.setVisibility(View.GONE);
            nestedScrollView.setPadding(
                    nestedScrollView.getPaddingLeft(),
                    nestedScrollView.getPaddingTop(),
                    nestedScrollView.getPaddingRight(),
                    0
            );
        }
    }

    private void showCustomKeyboard() {
        if (keyboardPanel != null) {
            keyboardPanel.setVisibility(View.VISIBLE);
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
    }

    private void setupButtons() {
        btnHitungBottom.setOnClickListener(v -> hitungTurunan());
    }

    private void hitungTurunan() {
        String input = inputFunction.getText().toString().trim();
        if (input.isEmpty()) return;

        try {
            String hasil = turunkanPolinomial(input);
            txtHasil.setText("f'(x) = " + hasil);

            hideCustomKeyboard();
            updateSteps(input, hasil);
            plotGrafik(input, hasil);

            View v = getView();
            if (v != null) {
                TextView lFx  = v.findViewById(R.id.legendFx);
                TextView lDfx = v.findViewById(R.id.legendDfx);
                if (lFx  != null) lFx.setText("f(x) = "  + input);
                if (lDfx != null) lDfx.setText("f'(x) = " + hasil);
            }

            nestedScrollView.post(() ->
                    nestedScrollView.smoothScrollTo(0, rvSteps.getTop())
            );

        } catch (Exception e) {
            txtHasil.setText("f'(x) = Error");
        }
    }

    private void updateSteps(String input, String hasil) {
        List<Stepmodel> steps = new ArrayList<>();

        steps.add(new Stepmodel(1,
                "Notasi Penulisan Turunan",
                "f'(x) = dy/dx = D[f(x)]",
                "Sebuah fungsi yang diturunkan dapat ditulis dalam Notasi Aksen f'(x), Notasi Leibniz dy/dx, atau Notasi Operator D."));

        String ruleName = "Theorem C: Power Rule";
        String ruleFormula = "Dₓ(xⁿ) = n·xⁿ⁻¹";

        if (input.contains("/")) {
            ruleName = "Theorem H: Quotient Rule (Aturan Pembagian)";
            ruleFormula = "Dₓ[u/v] = (u'v - uv') / v²";
        } else if (!input.contains("x")) {
            ruleName = "Theorem A: Constant Function Rule";
            ruleFormula = "Dₓ(k) = 0";
        } else if (input.trim().equals("x")) {
            ruleName = "Theorem B: Identity Function Rule";
            ruleFormula = "Dₓ(x) = 1";
        } else if (input.contains("+") || input.contains("-")) {
            ruleName = "Theorem E & F: Sum and Difference Rule";
            ruleFormula = "Dₓ[f(x) ± g(x)] = f'(x) ± g'(x)";
        }

        steps.add(new Stepmodel(2,
                ruleName,
                ruleFormula,
                "Menentukan aturan turunan aljabar yang sesuai berdasarkan Teorema Dasar Diferensial."));

        steps.add(new Stepmodel(3,
                "Proses Diferensiasi",
                "d/dx (" + input + ")",
                "Mengevaluasi dan menurunkan komponen fungsi aljabar sesuai dengan kaidah kalkulus turunan dasar."));

        steps.add(new Stepmodel(4,
                "Hasil Akhir",
                "f'(x) = " + hasil,
                "Proses perhitungan selesai. Turunan dari f(x) = " + input + " adalah f'(x) = " + hasil));

        stepAdapter.submitList(steps);
    }

    private void resetAll() {
        inputFunction.setText("");
        txtHasil.setText("");
        lineChart.clear();
        lineChart.invalidate();
        stepAdapter.submitList(new ArrayList<>());
        hideCustomKeyboard();

        View v = getView();
        if (v != null) {
            View legend = v.findViewById(R.id.chartLegend);
            if (legend != null) legend.setVisibility(View.GONE);
        }
    }

    // =========================================================
    // PARSER TURUNAN UTAMA (Mendukung Pembagian Dinamis + Simpel)
    // =========================================================
    private String turunkanPolinomial(String expr) {
        String e = expr.replaceAll("\\s+", "")
                .replace("×", "*")
                .replace("÷", "/")
                .replace("²", "^2")
                .replace("π", String.valueOf(Math.PI));

        // -----------------------------------------------------
        // PARSER OTOMATIS ATURAN PEMBAGIAN (QUOTIENT RULE)
        // -----------------------------------------------------
        if (e.contains("/")) {
            String[] parts = e.split("/");
            if (parts.length == 2) {
                String uRaw = parts[0];
                String vRaw = parts[1];

                if (uRaw.startsWith("(") && uRaw.endsWith(")")) {
                    uRaw = uRaw.substring(1, uRaw.length() - 1);
                }
                if (vRaw.startsWith("(") && vRaw.endsWith(")")) {
                    vRaw = vRaw.substring(1, vRaw.length() - 1);
                }

                String uDash = turunkanPolinomial(uRaw);
                String vDash = turunkanPolinomial(vRaw);

                String pembilangFinal = "";

                // Kasus Khusus 1: Jika u = x dan v = x + konstanta atau x - konstanta
                if (uDash.equals("1") && vDash.equals("1")) {
                    if (vRaw.contains("+")) {
                        pembilangFinal = vRaw.substring(vRaw.indexOf("+") + 1);
                    } else if (vRaw.contains("-")) {
                        pembilangFinal = "-" + vRaw.substring(vRaw.indexOf("-") + 1);
                    } else {
                        pembilangFinal = "0";
                    }
                }
                // Kasus Khusus 2: Jika u = x^2 dan v = x + konstanta (u'=2x, v'=1)
                else if (uRaw.equals("x^2") && vDash.equals("1")) {
                    if (vRaw.contains("+")) {
                        String c = vRaw.substring(vRaw.indexOf("+") + 1);
                        pembilangFinal = "x^2+2*" + c + "*x";
                    } else if (vRaw.contains("-")) {
                        String c = vRaw.substring(vRaw.indexOf("-") + 1);
                        pembilangFinal = "x^2-2*" + c + "*x";
                    }
                }
                // Standar Fallback Rumus Asli
                else {
                    pembilangFinal = "((" + uDash + ")*(" + vRaw + ")-(" + uRaw + ")*(" + vDash + "))";
                }

                String penyebutFinal = "(" + vRaw + ")^2";
                String hasilFinal = pembilangFinal + "/" + penyebutFinal;

                hasilFinal = hasilFinal.replace("*(1)", "")
                        .replace("(1)*", "")
                        .replace("2*1*x", "2*x")
                        .replace("2*1.0*x", "2*x");

                return hasilFinal;
            }
        }

        // Jalur Pemrosesan Polinomial Standar Penjumlahan/Pengurangan
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

        return hasil.toString().length() == 0 ? "0" : hasil.toString();
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
        return null;
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

    private void setupChart(View view) {
        lineChart.getDescription().setEnabled(false);
        lineChart.setDrawGridBackground(false);
        lineChart.setBackgroundColor(Color.WHITE);
        lineChart.setTouchEnabled(true);
        lineChart.setDragEnabled(true);
        lineChart.setScaleEnabled(true);
        lineChart.setPinchZoom(true);
        lineChart.getLegend().setEnabled(false);
        lineChart.setNoDataText("Tidak Ada Grafik");

        XAxis xAxis = lineChart.getXAxis();
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        xAxis.setDrawGridLines(true);
        xAxis.setGridColor(Color.parseColor("#EEEEEE"));
        xAxis.setAxisLineColor(Color.parseColor("#374151"));
        xAxis.setAxisLineWidth(2f);
        xAxis.setTextColor(Color.GRAY);
        xAxis.setTextSize(11f);

        YAxis leftAxis = lineChart.getAxisLeft();
        leftAxis.setDrawGridLines(true);
        leftAxis.setGridColor(Color.parseColor("#EEEEEE"));
        leftAxis.setAxisLineColor(Color.parseColor("#374151"));
        leftAxis.setAxisLineWidth(2f);
        leftAxis.setTextColor(Color.GRAY);
        leftAxis.setTextSize(11f);
        leftAxis.setAxisMinimum(-20f);
        leftAxis.setAxisMaximum(40f);

        lineChart.getAxisRight().setEnabled(false);
        lineChart.setExtraOffsets(8f, 8f, 8f, 16f);

        Button btnZoomIn  = view.findViewById(R.id.btnZoomIn);
        Button btnZoomOut = view.findViewById(R.id.btnZoomOut);

        if (btnZoomIn != null)  btnZoomIn.setOnClickListener(v -> lineChart.zoomIn());
        if (btnZoomOut != null) btnZoomOut.setOnClickListener(v -> lineChart.zoomOut());
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