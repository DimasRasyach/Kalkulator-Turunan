package com.example.aplikasikalkulatorturunan;

import android.graphics.Color;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
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

import java.util.ArrayList;
import java.util.List;

import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;

public class LimitFragment extends Fragment {

    private EditText inputFunction, inputLimitValue, activeInput;
    private TextView txtHasil, txtLimitLabel;
    private ImageButton btnClear;
    private LineChart lineChart;
    private LinearLayout keyboardPanel;
    private View btnHideKeyboard;
    private NestedScrollView nestedScrollView;

    private RecyclerView rvSteps;
    private StepAdapter  stepAdapter;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_limit, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        bindViews(view);
        setupRecyclerView();
        setupKeyboard(view);
        setupChart(view);          // <-- sekarang terima View untuk binding zoom button
        setupButtons(view);
        setupKeyboardSticky();
        fixInsets(view);
    }

    private void bindViews(View v) {
        inputFunction   = v.findViewById(R.id.inputFunction);
        inputLimitValue = v.findViewById(R.id.inputLimitValue);

        // KUNCI MATI KEYBOARD BAWAAN HP
        inputFunction.setShowSoftInputOnFocus(false);
        inputLimitValue.setShowSoftInputOnFocus(false);

        txtHasil        = v.findViewById(R.id.txtHasil);
        txtLimitLabel   = v.findViewById(R.id.txtLimitLabel);
        btnClear        = v.findViewById(R.id.btnClear);
        lineChart       = v.findViewById(R.id.lineChart);
        keyboardPanel   = v.findViewById(R.id.keyboardPanel);
        btnHideKeyboard = v.findViewById(R.id.btnHideKeyboard);
        nestedScrollView= v.findViewById(R.id.nestedScrollView);
        rvSteps         = v.findViewById(R.id.rvSteps);
    }

    private void setupButtons(View root) {
        View btnBottom = root.findViewById(R.id.btnHitungBottom);
        if (btnBottom != null) btnBottom.setOnClickListener(v -> prosesHitungCerdas());
        btnClear.setOnClickListener(v -> resetAll());
    }

    private void prosesHitungCerdas() {
        String fx = inputFunction.getText().toString().trim();
        String aVal = inputLimitValue.getText().toString().trim();
        if (fx.isEmpty()) return;

        if (TextUtils.isEmpty(aVal)) {
            hitungLimitTurunan(fx);       // x→a kosong → turunan fungsi f'(x)
        } else {
            hitungTurunanDiTitik(fx, aVal); // x→a diisi → turunkan dulu, baru substitusi
        }
        hideCustomKeyboard();
    }

    // METHOD BARU — ganti hitungLimitAljabar
    private void hitungTurunanDiTitik(String fx, String aStr) {
        try {
            double a = evalEkspresi(normalizeExpr(aStr));

            // 1. Turunkan dulu
            DerivativeEngine engine = new DerivativeEngine();
            DerivativeEngine.Result result = engine.differentiate(fx);
            String dfx = result.derivativeStr;

            // 2. Substitusi x=a ke f'(x)
            String dfxNorm = normalizeExpr(dfx);
            double nilaiDfxA = evalDenganX(dfxNorm, a);
            String hasil = formatHasil(nilaiDfxA);

            // 3. Tampilkan hasil
            txtHasil.setText("f'(" + aStr + ") = " + hasil);
            txtLimitLabel.setText("x→" + aStr);

            // 4. Langkah penyelesaian
            List<Stepmodel> steps = new ArrayList<>();
            steps.add(new Stepmodel(1, "Turunkan f(x)",
                    "f(x) = " + fx,
                    "Turunkan fungsi menggunakan aturan turunan."));
            steps.add(new Stepmodel(2, "Hasil Turunan",
                    "f'(x) = " + dfx,
                    "Hasil turunan dari f(x) = " + fx + "."));
            steps.add(new Stepmodel(3, "Substitusi x = " + aStr,
                    "f'(" + aStr + ") = " + dfx.replace("x", "(" + aStr + ")"),
                    "Masukkan x = " + aStr + " ke dalam f'(x)."));
            steps.add(new Stepmodel(4, "Hasil Akhir",
                    "f'(" + aStr + ") = " + hasil,
                    "Nilai turunan di titik x = " + aStr + " adalah " + hasil + "."));
            stepAdapter.submitList(steps);

            // 5. Plot grafik f(x) dan f'(x)
            plotGrafik(normalizeExpr(fx), a, hasil, true, false);

        } catch (Exception e) { txtHasil.setText("Error"); }
    }

    private void hitungLimitTurunan(String fx) {
        try {
            String dfx = hitungHasilTurunan(fx);
            txtHasil.setText("f'(x) = " + dfx);
            txtLimitLabel.setText("h→0");

            String fx_h = fx.replace("x", "(x+h)");

            List<Stepmodel> steps = new ArrayList<>();
            steps.add(new Stepmodel(1, "Definisi Turunan",
                    "f'(x) = lim(h→0) [f(x+h) − f(x)] / h",
                    "Langkah awal menggunakan definisi turunan melalui limit. Ganti f(x) dengan fungsi yang diberikan."));
            steps.add(new Stepmodel(2, "Substitusi (x+h)",   "lim [ " + fx_h + " - f(x) ] / h",
                    "Ganti x dengan (x+h), jabarkan perkalian, kemudian kurangi dengan f(x)."));
            steps.add(new Stepmodel(3, "Hasil Akhir (h→0)",  "= " + dfx,
                    "Suku f(x) habis. Bagi sisa pembilang dengan h, lalu masukkan h = 0."));
            stepAdapter.submitList(steps);

            plotGrafik(normalizeExpr(fx), 0, dfx);
        } catch (Exception e) { txtHasil.setText("Error"); }
    }

    private void hitungLimitAljabar(String fx, String aStr) {
        try {
            double a = evalEkspresi(normalizeExpr(aStr));
            String fNorm = normalizeExpr(fx);

            double yDirect = evalDenganX(fNorm, a);
            String hasil;
            List<Stepmodel> steps = new ArrayList<>();
            boolean isIndeterminate = false;

            if (Double.isNaN(yDirect) || Double.isInfinite(yDirect) || (Math.abs(yDirect) < 1e-6 && fx.contains("/"))) {
                isIndeterminate = true;
                double lim = (evalDenganX(fNorm, a + 0.0001) + evalDenganX(fNorm, a - 0.0001)) / 2.0;
                hasil = formatHasil(lim);
                if (fx.contains("√")) {
                    steps.add(new Stepmodel(1, "Bentuk Akar",    "0/0 → Kalikan Sekawan",         "Substitusi langsung menghasilkan 0/0. Gunakan metode rasionalisasi."));
                    steps.add(new Stepmodel(2, "Rasionalisasi",  "f(x)·sekawan / g(x)·sekawan",   "Kalikan pembilang & penyebut dengan akar sekawan."));
                    steps.add(new Stepmodel(3, "Hasil Limit",    "lim x→"+aStr+" = "+hasil,       "Sederhanakan dan masukkan x="+aStr+"."));
                } else {
                    steps.add(new Stepmodel(1, "Bentuk Pangkat", "0/0 → Faktorkan",               "Substitusi langsung menghasilkan 0/0. Faktorkan untuk mengeliminasi nol."));
                    steps.add(new Stepmodel(2, "Faktorisasi",    "lim H(x)/P(x) → coret (x−a)",   "Faktorkan pembilang/penyebut untuk mencoret faktor nol."));
                    steps.add(new Stepmodel(3, "Hasil Limit",    "lim x→"+aStr+" = "+hasil,       "Dapatkan hasil limit "+hasil+"."));
                }
            } else {
                hasil = formatHasil(yDirect);
                steps.add(new Stepmodel(1, "Teorema Limit",  "lim x→a f(x) = f(a)",   "Limit kontinu. Fungsi kontinu di x="+aStr+", cukup substitusi langsung."));
                steps.add(new Stepmodel(2, "Substitusi",     "f("+aStr+")",            "Ganti x dengan "+aStr+" ke fungsi asli."));
                steps.add(new Stepmodel(3, "Hasil Akhir",    "lim x→"+aStr+" = "+hasil,"Didapatkan hasil akhir "+hasil+"."));
            }
            stepAdapter.submitList(steps);

            txtHasil.setText("lim x→" + aStr + " (" + fx + ") = " + hasil);
            txtLimitLabel.setText("x→" + aStr);
            plotGrafik(fNorm, a, hasil, true, isIndeterminate);
        } catch (Exception e) { txtHasil.setText("Error"); }
    }

    // =========================================================
    // GRAFIK — disamakan dengan TurunanFragment
    // + zoom button binding
    // + garis kartesius (axis line) di-bold (lineWidth lebih tebal)
    // =========================================================
    private void setupChart(View view) {
        lineChart.getDescription().setEnabled(false);
        lineChart.getLegend().setEnabled(false);

        lineChart.setDrawGridBackground(false);
        lineChart.setBackgroundColor(Color.WHITE);

        lineChart.setTouchEnabled(true);
        lineChart.setDragEnabled(true);
        lineChart.setScaleEnabled(true);
        lineChart.setPinchZoom(true);

        lineChart.setNoDataText("Tidak Ada Grafik");
        lineChart.setExtraOffsets(8f, 8f, 8f, 16f);

        // ---- X Axis ----
        XAxis xAxis = lineChart.getXAxis();
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        xAxis.setDrawGridLines(true);
        xAxis.setGridColor(Color.parseColor("#EEEEEE"));
        xAxis.setTextColor(Color.GRAY);
        xAxis.setTextSize(11f);

        // Bold garis kartesius sumbu X
        xAxis.setAxisLineColor(Color.parseColor("#374151"));
        xAxis.setAxisLineWidth(2f);           // <-- BOLD

        // ---- Y Axis kiri ----
        YAxis leftAxis = lineChart.getAxisLeft();
        leftAxis.setDrawGridLines(true);
        leftAxis.setGridColor(Color.parseColor("#EEEEEE"));
        leftAxis.setTextColor(Color.GRAY);
        leftAxis.setTextSize(11f);

        // Bold garis kartesius sumbu Y
        leftAxis.setAxisLineColor(Color.parseColor("#374151"));
        leftAxis.setAxisLineWidth(2f);        // <-- BOLD

        leftAxis.setAxisMinimum(-20f);
        leftAxis.setAxisMaximum(40f);

        lineChart.getAxisRight().setEnabled(false);

        // ---- Zoom Buttons (sama seperti TurunanFragment) ----
        Button btnZoomIn  = view.findViewById(R.id.btnZoomIn);
        Button btnZoomOut = view.findViewById(R.id.btnZoomOut);

        if (btnZoomIn  != null) btnZoomIn.setOnClickListener(v -> lineChart.zoomIn());
        if (btnZoomOut != null) btnZoomOut.setOnClickListener(v -> lineChart.zoomOut());
    }

    private void plotGrafik(String f, double a, String resStr) {
        plotGrafik(f, a, resStr, true, false);
    }

    // hollowDot=true  → titik berlubang (kasus 0/0, fungsi tidak terdefinisi di x=a)
    // hollowDot=false → titik solid merah (fungsi kontinu di x=a)
    private void plotGrafik(String f, double a, String resStr, boolean showDot, boolean hollowDot) {

        // === FIX 1: range X diperlebar ±10 dari titik a, step 0.1 ===
        ArrayList<Entry> pts = new ArrayList<>();
        for (int i = -100; i <= 100; i++) {
            double x = a + (i / 10.0);
            // skip tepat di titik a untuk kasus tidak terdefinisi
            if (hollowDot && Math.abs(x - a) < 1e-9) continue;
            double y = evalDenganX(f, x);
            if (!Double.isNaN(y) && !Double.isInfinite(y) && Math.abs(y) < 1000)
                pts.add(new Entry((float) x, (float) y));
        }

        LineDataSet ds = new LineDataSet(pts, "f(x)");
        ds.setColor(0xFF1D4FFF);
        ds.setLineWidth(2.5f);
        ds.setDrawCircles(false);
        ds.setDrawValues(false);
        ds.setMode(LineDataSet.Mode.CUBIC_BEZIER);

        LineData data = new LineData(ds);
        try {
            float resY = Float.parseFloat(resStr.replace(",", "."));
            ArrayList<Entry> dot = new ArrayList<>(); dot.add(new Entry((float)a, resY));
            LineDataSet dsDot = new LineDataSet(dot, "P");
            dsDot.setCircleColor(Color.RED); dsDot.setCircleRadius(6f); dsDot.setDrawValues(false);
            data.addDataSet(dsDot);
        } catch (Exception ignored) {}

        lineChart.setData(data);
        lineChart.animateX(600);
        lineChart.invalidate();
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

    private void setupKeyboard(View v) {
        activeInput = inputFunction;
        inputFunction.setOnClickListener(v1 -> { activeInput = inputFunction; showCustomKeyboard(); });
        inputLimitValue.setOnClickListener(v1 -> { activeInput = inputLimitValue; showCustomKeyboard(); });
        btnHideKeyboard.setOnClickListener(v1 -> hideCustomKeyboard());
        int[] ids = {R.id.btnSin, R.id.btnCos, R.id.btnTan, R.id.btnLn, R.id.btnLog, R.id.btnSqrt, R.id.btnSquare, R.id.btnPow, R.id.btnPi, R.id.btnE,
                R.id.btn7, R.id.btn8, R.id.btn9, R.id.btnDivide, R.id.btn4, R.id.btn5, R.id.btn6, R.id.btnMultiply, R.id.btnCaret, R.id.btn1, R.id.btn2, R.id.btn3, R.id.btnMinus, R.id.btnOpenParen,
                R.id.btn0, R.id.btnDot, R.id.btnCloseParen, R.id.btnPlus, R.id.btnX};
        String[] vals = {"sin(", "cos(", "tan(", "ln(", "log(", "√(", "²", "^", "π", "e", "7", "8", "9", "/", "4", "5", "6", "×", "/", "1", "2", "3", "-", "(", "0", ".", ")", "+", "x"};
        for (int i = 0; i < ids.length; i++) {
            final String val = vals[i];
            v.findViewById(ids[i]).setOnClickListener(v1 -> { activeInput.getText().insert(activeInput.getSelectionStart(), val); });
        }
        v.findViewById(R.id.btnEquals).setOnClickListener(v1 -> prosesHitungCerdas());
        v.findViewById(R.id.btnBackspace).setOnClickListener(v1 -> { String t = activeInput.getText().toString(); if (!t.isEmpty()) { activeInput.setText(t.substring(0, t.length()-1)); activeInput.setSelection(activeInput.length()); } });
        v.findViewById(R.id.btnAC).setOnClickListener(v1 -> resetAll());
    }

    private void resetAll() {
        inputFunction.setText("");
        inputLimitValue.setText("");
        txtHasil.setText("");
        lineChart.clear();
        txtLimitLabel.setText("x→a");
        stepAdapter.submitList(new ArrayList<>());
    }

    private void showCustomKeyboard() {
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

    private void hideCustomKeyboard() {
        keyboardPanel.setVisibility(View.GONE);
        nestedScrollView.setPadding(
                nestedScrollView.getPaddingLeft(),
                nestedScrollView.getPaddingTop(),
                nestedScrollView.getPaddingRight(),
                0
        );
    }

    private void fixInsets(View v) {
        ViewCompat.setOnApplyWindowInsetsListener(v, (v1, in) -> {
            Insets nav = in.getInsets(WindowInsetsCompat.Type.navigationBars());
            keyboardPanel.setPadding(keyboardPanel.getPaddingLeft(), keyboardPanel.getPaddingTop(), keyboardPanel.getPaddingRight(), nav.bottom + 12);
            keyboardPanel.post(() -> {
                int kbHeight = keyboardPanel.getHeight();
                nestedScrollView.setPadding(nestedScrollView.getPaddingLeft(), nestedScrollView.getPaddingTop(), nestedScrollView.getPaddingRight(), kbHeight);
            });
            return in;
        });
    }

    private String hitungHasilTurunan(String fx) {
        DerivativeEngine engine = new DerivativeEngine();
        DerivativeEngine.Result result = engine.differentiate(fx);
        return result.derivativeStr;
    }

    private double evalDenganX(String fNorm, double a) {
        DerivativeEngine engine = new DerivativeEngine();
        DerivativeEngine.Node tree = engine.parse(fNorm);
        return engine.evaluate(tree, a);
    }

    private String normalizeExpr(String e) {
        String res = e.replace("×", "*").replace("÷", "/").replace("−", "-").replace("²", "^2").replace("³", "^3").replace("√(", "sqrt(").replace("√", "sqrt").replace("π", String.valueOf(Math.PI)).replaceAll("(?<![a-zA-Z])e(?![a-zA-Z])", String.valueOf(Math.E));
        return res.replaceAll("(\\d)(x)", "$1*$2").replaceAll("(\\d)(\\()", "$1*$2").replaceAll("(\\))(x)", "$1*$2").replaceAll("(\\))(\\()", "$1*$2");
    }

    private double evalEkspresi(String expr) {
        return new Object() {
            int pos = -1, ch; void nextChar() { ch = (++pos < expr.length()) ? expr.charAt(pos) : -1; }
            boolean eat(int c) { while (ch == ' ') nextChar(); if (ch == c) { nextChar(); return true; } return false; }
            double parse() { nextChar(); return parseExpr(); }
            double parseExpr() { double v = parseTerm(); for (;;) { if (eat('+')) v += parseTerm(); else if (eat('-')) v -= parseTerm(); else return v; } }
            double parseTerm() { double v = parseFactor(); for (;;) { if (eat('*')) v *= parseFactor(); else if (eat('/')) v /= parseFactor(); else return v; } }
            double parseFactor() {
                if (eat('+')) return +parseFactor(); if (eat('-')) return -parseFactor();
                double v; int startPos = this.pos;
                if (eat('(')) { v = parseExpr(); eat(')'); }
                else if ((ch >= '0' && ch <= '9') || ch == '.') { while ((ch >= '0' && ch <= '9') || ch == '.') nextChar(); v = Double.parseDouble(expr.substring(startPos, this.pos)); }
                else if (ch >= 'a' && ch <= 'z') {
                    while (ch >= 'a' && ch <= 'z') nextChar(); String fn = expr.substring(startPos, this.pos);
                    if (eat('(')) { v = parseExpr(); eat(')'); } else v = parseFactor();
                    switch (fn) { case "sqrt": v = Math.sqrt(v); break; case "sin": v = Math.sin(Math.toRadians(v)); break; case "cos": v = Math.cos(Math.toRadians(v)); break; case "tan": v = Math.tan(Math.toRadians(v)); break; case "ln": v = Math.log(v); break; case "log": v = Math.log10(v); break; default: return 0; }
                } else return 0;
                if (eat('^')) v = Math.pow(v, parseFactor()); return v;
            }
        }.parse();
    }

    private String formatHasil(double d) { if(Double.isNaN(d)) return "−"; if(d==(long)d) return String.valueOf((long)d); return String.format("%.2f", d).replaceAll("0+$","").replaceAll("\\.$",""); }
}