package com.example.aplikasikalkulatorturunan;

// TurunanFragment.java — versi dengan DerivativeEngine (penurunan simbolik penuh)
// Perubahan vs versi sebelumnya:
//   1. turunkanPolinomial() diganti DerivativeEngine.differentiate()
//      -> mendukung Power, Constant Multiple, Sum/Difference, Product,
//         Quotient, Chain Rule, Trig, Exponential, Logarithm
//   2. updateSteps() menampilkan SETIAP langkah dari engine (nama teorema +
//      formula + ekspresi + penjelasan natural), bukan 4 langkah generik
//   3. Grafik memakai DerivativeEngine.evaluate() untuk f(x) dan f'(x)
//   4. Sisanya (keyboard, sticky panel, insets) tidak diubah

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
    private Button            btnHitungBottom;
    private ImageButton      btnClear;
    private LineChart        lineChart;
    private NestedScrollView nestedScrollView;
    private View             keyboardPanel, btnHideKeyboard;

    // RecyclerView langkah
    private RecyclerView rvSteps;
    private StepAdapter  stepAdapter;

    // Engine penurunan simbolik
    private final DerivativeEngine engine = new DerivativeEngine();

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
        inputFunction.setShowSoftInputOnFocus(false);

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

    // =========================================================
    // RecyclerView setup
    // =========================================================
    private void setupRecyclerView() {
        stepAdapter = new StepAdapter();
        rvSteps.setAdapter(stepAdapter);
        rvSteps.setLayoutManager(new LinearLayoutManager(requireContext()));
        rvSteps.setNestedScrollingEnabled(false);
        rvSteps.setHasFixedSize(false);
    }

    // =========================================================
    // Keyboard sticky
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
    // System insets
    // =========================================================
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

    // =========================================================
    // Keyboard
    // =========================================================
    private void setupKeyboard(View view) {
        inputFunction.setOnClickListener(v -> showCustomKeyboard());

        if (btnHideKeyboard != null) {
            btnHideKeyboard.setOnClickListener(v -> hideCustomKeyboard());
        }

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
                "7",    "8",    "9",    "/",
                "4",    "5",    "6",    "×",    "/",
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

    // =========================================================
    // HITUNG TURUNAN — pakai DerivativeEngine
    // =========================================================
    private void setupButtons() {
        btnHitungBottom.setOnClickListener(v -> hitungTurunan());
    }

    private String lastInput = "";
    private DerivativeEngine.Node lastDerivativeNode = null;

    private void hitungTurunan() {
        String input = inputFunction.getText().toString().trim();
        if (input.isEmpty()) return;

        try {
            DerivativeEngine.Result result = engine.differentiate(input);
            String hasil = result.derivativeStr;

            txtHasil.setText("f'(x) = " + hasil);

            lastInput = input;
            lastDerivativeNode = result.derivative;

            updateSteps(result);
            plotGrafik(input, result.derivative);

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
            txtHasil.setText("f'(x) = Error: periksa penulisan fungsi");
        }
    }

    // =========================================================
    // UPDATE LANGKAH — tiap Step dari engine jadi satu Stepmodel
    // =========================================================
    private void updateSteps(DerivativeEngine.Result result) {
        List<Stepmodel> steps = new ArrayList<>();
        int idx = 1;
        for (DerivativeEngine.Step s : result.steps) {
            steps.add(new Stepmodel(
                    idx++,
                    s.theorem,
                    s.formulaRule,
                    s.explanation + "\n" + s.expression
            ));
        }
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
        lastInput = "";
        lastDerivativeNode = null;

        View v = getView();
        if (v != null) {
            View legend = v.findViewById(R.id.chartLegend);
            if (legend != null) legend.setVisibility(View.GONE);
        }
    }

    // =========================================================
    // GRAFIK — pakai DerivativeEngine.evaluate untuk f(x) & f'(x)
    // =========================================================
    private void setupChart(View view) {
        lineChart.getDescription().setEnabled(false);
        lineChart.setDrawGridBackground(false);
        lineChart.setBackgroundColor(Color.WHITE);
        lineChart.setTouchEnabled(true);
        lineChart.setDragEnabled(true);
        lineChart.setScaleEnabled(true);
        lineChart.setPinchZoom(true);
        lineChart.getLegend().setEnabled(false);
        lineChart.setNoDataText("Masukkan fungsi lalu tekan Hitung Turunan");

        XAxis xAxis = lineChart.getXAxis();
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        xAxis.setDrawGridLines(true);
        xAxis.setGridColor(Color.parseColor("#EEEEEE"));
        xAxis.setAxisLineColor(Color.parseColor("#9CA3AF"));
        xAxis.setTextColor(Color.GRAY);
        xAxis.setTextSize(11f);

        YAxis leftAxis = lineChart.getAxisLeft();
        leftAxis.setDrawGridLines(true);
        leftAxis.setGridColor(Color.parseColor("#EEEEEE"));
        leftAxis.setAxisLineColor(Color.parseColor("#9CA3AF"));
        leftAxis.setTextColor(Color.GRAY);
        leftAxis.setTextSize(11f);
        leftAxis.setAxisMinimum(-20f);
        leftAxis.setAxisMaximum(40f);

        lineChart.getAxisRight().setEnabled(false);
        lineChart.setExtraOffsets(8f, 8f, 8f, 16f);

        Button btnZoomIn  = view.findViewById(R.id.btnZoomIn);
        Button btnZoomOut = view.findViewById(R.id.btnZoomOut);
        if (btnZoomIn  != null) btnZoomIn.setOnClickListener(v -> lineChart.zoomIn());
        if (btnZoomOut != null) btnZoomOut.setOnClickListener(v -> lineChart.zoomOut());
    }

    private void plotGrafik(String fxExpr, DerivativeEngine.Node dfxNode) {
        ArrayList<Entry> fxPts  = new ArrayList<>();
        ArrayList<Entry> dfxPts = new ArrayList<>();

        DerivativeEngine.Node fxNode;
        try {
            fxNode = new DerivativeEngine().parse(fxExpr);
        } catch (Exception e) {
            lineChart.clear();
            lineChart.setNoDataText("Grafik tidak dapat ditampilkan untuk fungsi ini");
            lineChart.invalidate();
            return;
        }

        for (int xi = -60; xi <= 60; xi++) {
            double x = xi / 10.0;
            try {
                double fy = engine.evaluate(fxNode, x);
                if (!Double.isNaN(fy) && !Double.isInfinite(fy) && Math.abs(fy) < 1000)
                    fxPts.add(new Entry((float) x, (float) fy));
            } catch (Exception ignored) {}

            try {
                double dfy = engine.evaluate(dfxNode, x);
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