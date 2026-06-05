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
import androidx.fragment.app.Fragment;

import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;

import java.util.ArrayList;
import java.util.List;

public class GrafikFragment extends Fragment {

    private EditText inputFunction, activeInput;
    private TextView txtHasilTurunan, txtLegendFx, txtLegendDfx;
    private TextView txtInfoDomain, txtInfoRange, txtInfoSumbuX, txtInfoSumbuY;
    private TextView txtInfoTitikKritis, txtInfoNolTurunan, txtInfoNegatif, txtInfoPositif;
    private TextView txtKritis1, txtKritis2, txtKritis3;
    private LinearLayout containerTitikKritis, keyboardPanel;
    private View btnHideKeyboard;
    private LineChart lineChart;
    private Button btnHitungBottom, btnZoomIn, btnZoomOut;
    private ImageButton btnClear;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_grafik2, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        bindViews(view);
        setupKeyboard(view);
        setupChart();
        setupListeners();
        fixInsets(view);
    }

    private void bindViews(View v) {
        inputFunction = v.findViewById(R.id.inputFunction);

        // Mematikan keyboard bawaan sistem Android agar custom keyboard berjalan mulus
        inputFunction.setShowSoftInputOnFocus(false);

        txtHasilTurunan = v.findViewById(R.id.txtHasilTurunan);
        txtLegendFx = v.findViewById(R.id.txtLegendFx);
        txtLegendDfx = v.findViewById(R.id.txtLegendDfx);
        lineChart = v.findViewById(R.id.lineChart);
        btnHitungBottom = v.findViewById(v.getContext().getResources().getIdentifier("btnHitungBottom", "id", v.getContext().getPackageName()));
        // fallback binding aman jika ada redundansi ID di setupKeyboard
        btnHitungBottom = v.findViewById(R.id.btnHitungBottom);
        btnClear = v.findViewById(R.id.btnClear);
        btnZoomIn = v.findViewById(R.id.btnZoomIn);
        btnZoomOut = v.findViewById(R.id.btnZoomOut);

        keyboardPanel = v.findViewById(R.id.keyboardPanel);
        btnHideKeyboard = v.findViewById(R.id.btnHideKeyboard);
        containerTitikKritis = v.findViewById(R.id.containerTitikKritis);
        txtKritis1 = v.findViewById(R.id.txtKritis1);
        txtKritis2 = v.findViewById(R.id.txtKritis2);
        txtKritis3 = v.findViewById(R.id.txtKritis3);
        txtInfoDomain = v.findViewById(R.id.txtInfoDomain);
        txtInfoRange = v.findViewById(R.id.txtInfoRange);
        txtInfoSumbuX = v.findViewById(R.id.txtInfoSumbuX);
        txtInfoSumbuY = v.findViewById(R.id.txtInfoSumbuY);
        txtInfoTitikKritis = v.findViewById(R.id.txtInfoTitikKritis);
        txtInfoNolTurunan = v.findViewById(R.id.txtInfoNolTurunan);
        txtInfoNegatif = v.findViewById(R.id.txtInfoNegatif);
        txtInfoPositif = v.findViewById(R.id.txtInfoPositif);
    }

    private void setupChart() {
        lineChart.getDescription().setEnabled(false);
        lineChart.getLegend().setEnabled(false); // Menggunakan kustom TextView legend bawaan fragment_grafik2
        lineChart.setDrawGridBackground(false);
        lineChart.setBackgroundColor(Color.WHITE);

        lineChart.setTouchEnabled(true);
        lineChart.setDragEnabled(true);
        lineChart.setScaleEnabled(true);
        lineChart.setPinchZoom(true);

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
    }

    private void setupListeners() {
        inputFunction.setOnClickListener(v -> showKeyboard());
        btnHitungBottom.setOnClickListener(v -> hitungSemua());
        btnClear.setOnClickListener(v -> resetUI());
        btnHideKeyboard.setOnClickListener(v -> hideKeyboard());

        btnZoomIn.setOnClickListener(v -> lineChart.zoomIn());
        btnZoomOut.setOnClickListener(v -> lineChart.zoomOut());
    }

    private void hitungSemua() {
        String raw = inputFunction.getText().toString().trim();
        if (TextUtils.isEmpty(raw)) return;

        try {
            String derivative = cleanExpression(computeDerivative(raw));

            txtHasilTurunan.setText("f′(x) = " + derivative);
            txtHasilTurunan.setTextColor(Color.parseColor("#1D4FFF"));

            txtLegendFx.setText("— f(x) = " + raw);
            txtLegendDfx.setText("— f′(x) = " + derivative);

            plotGrafik(raw, derivative);
            updateAnalisis(raw, derivative);
            hideKeyboard();
        } catch (Exception e) {
            txtHasilTurunan.setText("Error");
            txtHasilTurunan.setTextColor(Color.RED);
        }
    }

    private String computeDerivative(String expr) {
        String f = expr.replace(" ", "").replace("−", "-");

        if (f.contains("/") || f.contains("÷")) {
            String[] parts = f.split("[/÷]");
            if (parts.length == 2) {
                String u = parts[0].replaceAll("^\\((.*)\\)$", "$1");
                String v = parts[1].replaceAll("^\\((.*)\\)$", "$1");

                String du = computePolynomialDerivative(u);
                String dv = computePolynomialDerivative(v);

                return "((" + du + ")*(" + v + ")-(" + u + ")*(" + dv + "))/((" + v + ")^2)";
            }
        }

        return computePolynomialDerivative(f);
    }

    private String computePolynomialDerivative(String expr) {
        String f = expr;
        if (!f.startsWith("-") && !f.startsWith("+")) f = "+" + f;
        StringBuilder res = new StringBuilder();
        String[] terms = f.split("(?=[+-])");
        for (String t : terms) {
            if (t.isEmpty()) continue;
            String d = deriveSuku(t.substring(1), t.substring(0, 1));
            if (d != null && !d.equals("0")) {
                if (res.length() > 0 && !d.startsWith("-")) res.append("+");
                res.append(d);
            }
        }
        return res.length() == 0 ? "0" : res.toString();
    }

    private void updateAnalisis(String fx, String dfx) {
        String fNorm = normalizeExpr(fx);
        String dfNorm = normalizeExpr(dfx);

        // --- 1. HITUNG DOMAIN & ASIMTOT TEGAK ---
        String domain = "ℝ";
        double asimtotX = Double.NaN;

        if (fx.contains("/") || fx.contains("÷")) {
            String[] parts = fx.split("[/÷]");
            if (parts.length == 2) {
                String penyebut = parts[1].replace("(", "").replace(")", "");
                for (double x = -10; x <= 10; x += 0.05) {
                    if (Math.abs(evalDenganX(normalizeExpr(penyebut), x)) < 0.01) {
                        asimtotX = Math.round(x * 10.0) / 10.0;
                        domain = "ℝ, x ≠ " + formatNum(asimtotX);
                        break;
                    }
                }
            }
        } else if (fx.contains("√")) {
            domain = "{x | x ≥ 0}";
        } else if (fx.contains("ln") || fx.contains("log")) {
            domain = "{x | x > 0}";
        }
        txtInfoDomain.setText("• Domain: " + domain);

        // --- 2. HITUNG SUMBU-Y ---
        double y0 = evalDenganX(fNorm, 0);
        txtInfoSumbuY.setText("• Sumbu-Y: " + (Double.isNaN(y0) || Double.isInfinite(y0) ? "−" : "(0, " + formatNum(y0) + ")"));

        // --- 3. HITUNG SUMBU-X ---
        List<Double> zerosX = findZeros(fNorm, -6, 6);
        txtInfoSumbuX.setText("• Sumbu-X: " + (zerosX.isEmpty() ? "−" : formatPoints(zerosX)));

        // --- 4. HITUNG RANGE ---
        double minY = Double.MAX_VALUE;
        double maxY = -Double.MAX_VALUE;
        boolean hasValidPoint = false;

        for (double x = -6; x <= 6; x += 0.1) {
            if (!Double.isNaN(asimtotX) && Math.abs(x - asimtotX) < 0.2) continue;
            double y = evalDenganX(fNorm, x);
            if (!Double.isNaN(y) && !Double.isInfinite(y) && Math.abs(y) < 100) {
                if (y < minY) minY = y;
                if (y > maxY) maxY = y;
                hasValidPoint = true;
            }
        }

        String range = "ℝ";
        if (hasValidPoint) {
            if (fx.contains("/") || fx.contains("÷")) {
                double yLimit = evalDenganX(fNorm, 1000);
                if (!Double.isNaN(yLimit)) range = "ℝ, y ≠ " + formatNum(Math.round(yLimit));
            } else if (minY > -30 && maxY <= 40) {
                range = "{y | y ≥ " + formatNum(minY) + "}";
            } else if (minY <= -40 && maxY < 30) {
                range = "{y | y ≤ " + formatNum(maxY) + "}";
            }
        }
        txtInfoRange.setText("• Range: " + range);

        // --- 5. HITUNG TITIK KRITIS & f'(x) = 0 ---
        List<Double> kritis = new ArrayList<>();
        if (!fx.contains("/") && !fx.contains("÷")) {
            kritis = findZeros(dfNorm, -5, 5);
        }
        txtInfoTitikKritis.setText("• Titik kritis: " + (kritis.isEmpty() ? "−" : formatXList(kritis)));
        txtInfoNolTurunan.setText("• f′(x)=0: " + (kritis.isEmpty() ? "−" : formatXList(kritis)));

        // --- 6. DETEKSI INTERVAL FUNGSI NAIK / TURUN ---
        boolean selaluNaik = true;
        boolean selaluTurun = true;

        for (double x = -5; x <= 5; x += 0.2) {
            if (!Double.isNaN(asimtotX) && Math.abs(x - asimtotX) < 0.3) continue;
            double dy = evalDenganX(dfNorm, x);
            if (!Double.isNaN(dy) && !Double.isInfinite(dy)) {
                if (dy > 0) selaluTurun = false;
                if (dy < 0) selaluNaik = false;
            }
        }

        String naikInterval = "−";
        String turunInterval = "−";

        if (selaluNaik) {
            naikInterval = "Seluruh daerah asal";
        } else if (selaluTurun) {
            turunInterval = "Seluruh daerah asal";
        } else if (!kritis.isEmpty()) {
            double k = kritis.get(0);
            double tester = evalDenganX(dfNorm, k - 0.5);
            if (tester > 0) {
                naikInterval = "x < " + formatNum(k);
                turunInterval = "x > " + formatNum(k);
            } else {
                naikInterval = "x > " + formatNum(k);
                turunInterval = "x < " + formatNum(k);
            }
        }

        txtInfoPositif.setText("• f′(x)>0: " + naikInterval);
        txtInfoNegatif.setText("• f′(x)<0: " + turunInterval);

        updateKritisCards(kritis);
    }

    private void updateKritisCards(List<Double> list) {
        containerTitikKritis.setVisibility(list.isEmpty() ? View.GONE : View.VISIBLE);
        TextView[] views = {txtKritis1, txtKritis2, txtKritis3};
        for (int i = 0; i < 3; i++) {
            if (i < list.size()) {
                views[i].setVisibility(View.VISIBLE);
                views[i].setText("Titik kritis\nx = " + formatNum(list.get(i)));
            } else views[i].setVisibility(View.GONE);
        }
    }

    private void plotGrafik(String fx, String dfx) {
        ArrayList<Entry> fxEntries = new ArrayList<>();
        ArrayList<Entry> dfxEntries = new ArrayList<>();
        String fn = normalizeExpr(fx);
        String dfn = normalizeExpr(dfx);

        for (float x = -5f; x <= 5f; x += 0.1f) {
            double y = evalDenganX(fn, x);
            double dy = evalDenganX(dfn, x);
            if (!Double.isNaN(y) && Math.abs(y) < 50) fxEntries.add(new Entry(x, (float) y));
            if (!Double.isNaN(dy) && Math.abs(dy) < 50) dfxEntries.add(new Entry(x, (float) dy));
        }

        LineDataSet set1 = new LineDataSet(fxEntries, "f(x)");
        set1.setColor(0xFF1D4FFF);
        set1.setLineWidth(2.5f);
        set1.setDrawCircles(false);
        set1.setDrawValues(false); // Mematikan nilai angka di setiap koordinat titik agar grafik bersih
        set1.setMode(LineDataSet.Mode.CUBIC_BEZIER); // Membuat lekukan kurva f(x) mulus sama seperti TurunanFragment

        LineDataSet set2 = new LineDataSet(dfxEntries, "f'(x)");
        set2.setColor(0xFFEF4444);
        set2.setLineWidth(2.5f);
        set2.setDrawCircles(false);
        set2.setDrawValues(false); // Mematikan nilai angka di setiap koordinat titik agar grafik bersih
        set2.setMode(LineDataSet.Mode.CUBIC_BEZIER); // Membuat lekukan kurva f'(x) mulus sama seperti TurunanFragment

        lineChart.setData(new LineData(set1, set2));
        lineChart.animateX(500);
        lineChart.invalidate();
    }

    private String deriveSuku(String s, String sgn) {
        double sign = sgn.equals("-") ? -1 : 1;
        if (!s.contains("x")) return "0";
        double k, e;
        if (s.contains("x^")) {
            String[] p = s.split("x\\^");
            k = p[0].isEmpty() ? 1 : Double.parseDouble(p[0]);
            e = Double.parseDouble(p[1]);
        } else if (s.contains("x³")) { k = s.replace("x³","").isEmpty() ? 1 : Double.parseDouble(s.replace("x³","")); e = 3; }
        else if (s.contains("x²")) { k = s.replace("x²","").isEmpty() ? 1 : Double.parseDouble(s.replace("x²","")); e = 2; }
        else { k = s.replace("x","").isEmpty() ? 1 : Double.parseDouble(s.replace("x","")); e = 1; }
        double nk = k * e * sign; double ne = e - 1;
        if (ne == 0) return formatNum(nk);
        if (ne == 1) return formatNum(nk) + "x";
        return formatNum(nk) + "x^" + formatNum(ne);
    }

    private String cleanExpression(String expr) {
        if (expr == null) return "0";
        return expr.replace(".0x", "x")
                .replace(".0+", "+")
                .replace(".0-", "-")
                .replaceAll("1x", "x");
    }

    private List<Double> findZeros(String expr, double start, double end) {
        List<Double> results = new ArrayList<>();
        double prev = evalDenganX(expr, start);
        for (double x = start + 0.1; x <= end; x += 0.1) {
            double curr = evalDenganX(expr, x);
            if (prev * curr <= 0) results.add(Math.round((x - 0.05) * 10.0) / 10.0);
            prev = curr;
            if (results.size() >= 3) break;
        }
        return results;
    }

    private void setupKeyboard(View v) {
        activeInput = inputFunction;
        int[] ids = {R.id.btn0, R.id.btn1, R.id.btn2, R.id.btn3, R.id.btn4, R.id.btn5, R.id.btn6, R.id.btn7, R.id.btn8, R.id.btn9,
                R.id.btnDot, R.id.btnPlus, R.id.btnMinus, R.id.btnMultiply, R.id.btnDivide, R.id.btnCaret, R.id.btnOpenParen, R.id.btnCloseParen,
                R.id.btnX, R.id.btnPi, R.id.btnE, R.id.btnSqrt, R.id.btnSquare, R.id.btnPow, R.id.btnSin, R.id.btnCos, R.id.btnTan, R.id.btnLn, R.id.btnLog};

        String[] vals = {"0","1","2","3","4","5","6","7","8","9",".","+","-","*","/","^","(",")","x","π","e","√","²","^","sin(","cos(","tan(","ln(","log("};

        for (int i = 0; i < ids.length; i++) {
            final String s = vals[i];
            View btn = v.findViewById(ids[i]);
            if (btn != null) {
                btn.setOnClickListener(v1 -> {
                    int start = activeInput.getSelectionStart();
                    activeInput.getText().insert(start, s);
                });
            }
        }
        View backspace = v.findViewById(R.id.btnBackspace);
        if (backspace != null) {
            backspace.setOnClickListener(v1 -> {
                String s = activeInput.getText().toString();
                if (!s.isEmpty()) { activeInput.setText(s.substring(0, s.length()-1)); activeInput.setSelection(activeInput.length()); }
            });
        }
        View ac = v.findViewById(R.id.btnAC);
        if (ac != null) ac.setOnClickListener(v1 -> resetUI());
    }

    private double evalDenganX(String expr, double x) {
        return evalEkspresi(expr.replaceAll("(?<![a-z])x(?![a-z])", "(" + x + ")"));
    }

    private String normalizeExpr(String e) {
        String res = e.replace("×", "*").replace("÷", "/").replace("−", "-").replace("²", "^2").replace("³", "^3").replace("√", "sqrt")
                .replace("π", String.valueOf(Math.PI)).replaceAll("(?<![a-zA-Z])e(?![a-zA-Z])", String.valueOf(Math.E));
        res = res.replaceAll("(\\d)(x)", "$1*$2").replaceAll("(\\d)(\\()", "$1*$2").replaceAll("(\\))(x)", "$1*$2").replaceAll("(\\))(\\()", "$1*$2");
        return res;
    }

    private double evalEkspresi(String expr) {
        try {
            return new Object() {
                int pos = -1, ch;
                void nextChar() { ch = (++pos < expr.length()) ? expr.charAt(pos) : -1; }
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
        } catch (Exception e) { return Double.NaN; }
    }

    private void fixInsets(View v) {
        ViewCompat.setOnApplyWindowInsetsListener(v, (v1, insets) -> {
            Insets nav = insets.getInsets(WindowInsetsCompat.Type.navigationBars());
            keyboardPanel.setPadding(keyboardPanel.getPaddingLeft(), keyboardPanel.getPaddingTop(), keyboardPanel.getPaddingRight(), nav.bottom + 12);
            return insets;
        });
    }

    private void resetUI() {
        inputFunction.setText("");

        txtHasilTurunan.setText("f′(x) = Tampil Di Sini");
        txtHasilTurunan.setTextColor(Color.parseColor("#9CA3AF"));

        txtLegendFx.setText("— f(x)");
        txtLegendDfx.setText("— f′(x)");

        lineChart.clear();

        containerTitikKritis.setVisibility(View.GONE);
        txtInfoDomain.setText("• Domain: −");
        txtInfoRange.setText("• Range: −");
        txtInfoSumbuX.setText("• Sumbu-X: −");
        txtInfoSumbuY.setText("• Sumbu-Y: −");
        txtInfoTitikKritis.setText("• Titik kritis: −");
        txtInfoNolTurunan.setText("• f′(x)=0: −");
        txtInfoPositif.setText("• f′(x)>0: −");
        txtInfoNegatif.setText("• f′(x)<0: −");
    }

    private void showKeyboard() { keyboardPanel.setVisibility(View.VISIBLE); }
    private void hideKeyboard() { keyboardPanel.setVisibility(View.GONE); }
    private String formatNum(double d) { if(Double.isNaN(d)) return "−"; if(d==(long)d) return String.valueOf((long)d); return String.format("%.2f", d).replaceAll("0+$","").replaceAll("\\.$",""); }
    private String formatPoints(List<Double> list) { StringBuilder sb = new StringBuilder(); for(int i=0; i<list.size(); i++) { sb.append("(").append(formatNum(list.get(i))).append(",0)"); if(i<list.size()-1) sb.append(", "); } return sb.toString(); }
    private String formatXList(List<Double> list) { StringBuilder sb = new StringBuilder(); for(int i=0; i<list.size(); i++) { sb.append("x=").append(formatNum(list.get(i))); if(i<list.size()-1) sb.append(", "); } return sb.toString(); }
}