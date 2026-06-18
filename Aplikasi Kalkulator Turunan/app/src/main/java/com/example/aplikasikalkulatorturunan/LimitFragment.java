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
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;

import java.util.ArrayList;

public class LimitFragment extends Fragment {

    private EditText inputFunction, inputLimitValue, activeInput;
    private TextView txtHasil, txtHasilAkhir, txtLimitLabel;
    private Button btnHitung;
    private ImageButton btnClear;
    private LineChart lineChart;
    private LinearLayout keyboardPanel;
    private View btnHideKeyboard;

    private LinearLayout[] stepHeaders = new LinearLayout[3];
    private TextView[]     stepTitles   = new TextView[3];
    private TextView[]     stepSubtitles= new TextView[3];
    private TextView[]     stepFormulas = new TextView[3];
    private TextView[]     stepDetails  = new TextView[3];
    private ImageView[]    stepArrows   = new ImageView[3];
    private boolean[]      stepExpanded = {false, false, false};

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_limit, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        bindViews(view);
        setupKeyboard(view);
        setupChart();
        setupButtons(view);
        fixInsets(view);
        setupStepAccordion();
    }

    private void bindViews(View v) {
        inputFunction   = v.findViewById(R.id.inputFunction);
        inputLimitValue = v.findViewById(R.id.inputLimitValue);

        // KUNCI MATI KEYBOARD BAWAAN HP
        inputFunction.setShowSoftInputOnFocus(false);
        inputLimitValue.setShowSoftInputOnFocus(false);

        txtHasil        = v.findViewById(R.id.txtHasil);
        txtHasilAkhir   = v.findViewById(R.id.txtHasilAkhir);
        txtLimitLabel   = v.findViewById(R.id.txtLimitLabel);
        btnHitung       = v.findViewById(R.id.btnHitung);
        btnClear        = v.findViewById(R.id.btnClear);
        lineChart       = v.findViewById(R.id.lineChart);
        keyboardPanel   = v.findViewById(R.id.keyboardPanel);
        btnHideKeyboard = v.findViewById(R.id.btnHideKeyboard);

        for (int i = 0; i < 3; i++) {
            int n = i + 1;
            stepHeaders[i]  = v.findViewById(getResources().getIdentifier("step"+n+"Header", "id", getContext().getPackageName()));
            stepTitles[i]   = v.findViewById(getResources().getIdentifier("step"+n+"Title", "id", getContext().getPackageName()));
            stepSubtitles[i]= v.findViewById(getResources().getIdentifier("step"+n+"Subtitle", "id", getContext().getPackageName()));
            stepFormulas[i] = v.findViewById(getResources().getIdentifier("step"+n+"Formula", "id", getContext().getPackageName()));
            stepDetails[i]  = v.findViewById(getResources().getIdentifier("step"+n+"Detail", "id", getContext().getPackageName()));
            stepArrows[i]   = v.findViewById(getResources().getIdentifier("step"+n+"Arrow", "id", getContext().getPackageName()));
        }
    }

    private void setupButtons(View root) {
        btnHitung.setOnClickListener(v -> prosesHitungCerdas());
        View btnBottom = root.findViewById(R.id.btnHitungBottom);
        if (btnBottom != null) btnBottom.setOnClickListener(v -> prosesHitungCerdas());
        btnClear.setOnClickListener(v -> resetAll());
    }

    private void prosesHitungCerdas() {
        String fx = inputFunction.getText().toString().trim();
        String aVal = inputLimitValue.getText().toString().trim();
        if (fx.isEmpty()) return;

        if (TextUtils.isEmpty(aVal)) {
            hitungLimitTurunan(fx);
        } else {
            hitungLimitAljabar(fx, aVal);
        }
        hideCustomKeyboard();
    }

    private void hitungLimitTurunan(String fx) {
        try {
            String dfx = hitungHasilTurunan(fx);
            txtHasil.setText("f'(x) = " + dfx);
            txtHasilAkhir.setText(dfx);
            txtLimitLabel.setText("h→0");

            String fx_h = fx.replace("x", "(x+h)");
            String[] t = {"Definisi Turunan", "Substitusi (x+h)", "Hasil Akhir (h→0)"};
            String[] s = {"Gambar 1", "f(x+h) - f(x)", "Diferensial Aljabar"};
            String[] f = {"Rumus Gambar 10", "lim [ "+fx_h+" - f(x) ] / h", "= " + dfx};
            String[] d = {"Langkah awal menggunakan rumus turunan melalui limit sesuai Gambar 10.",
                          "Ganti x dengan (x+h), jabarkan perkalian, kemudian kurangi dengan f(x).",
                          "Suku f(x) habis. Bagi sisa pembilang dengan h, lalu masukkan h = 0."};
            setStepData(t, s, f, d);
            plotGrafik(normalizeExpr(fx), 0, dfx);
        } catch (Exception e) { txtHasil.setText("Error"); }
    }

    private void hitungLimitAljabar(String fx, String aStr) {
        try {
            double a = evalEkspresi(normalizeExpr(aStr));
            String fNorm = normalizeExpr(fx);
            
            // Cek 0/0
            double yDirect = evalDenganX(fNorm, a);
            String hasil;

            if (Double.isNaN(yDirect) || Math.abs(yDirect) < 1e-6 && fx.contains("/")) {
                double lim = (evalDenganX(fNorm, a + 0.0001) + evalDenganX(fNorm, a - 0.0001)) / 2.0;
                hasil = formatHasil(lim);
                if (fx.contains("√")) {
                    String[] t={"Bentuk Akar", "Rasionalisasi", "Hasil Limit"};
                    String[] s={"Gambar 12", "Kali Sekawan", "Evaluasi x="+aStr};
                    String[] f={"√f - √g", "f(x) * sekawan", "= "+hasil};
                    String[] d={"Substitusi langsung 0/0. Gunakan Metode Gambar 12 Metode B.", "Kalikan pembilang & penyebut dengan akar sekawan.", "Sederhanakan dan masukkan x="+aStr+"."};
                    setStepData(t, s, f, d);
                } else {
                    String[] t={"Bentuk Pangkat", "Faktorisasi", "Hasil Limit"};
                    String[] s={"Gambar 12", "Coret (x-a)", "Evaluasi x="+aStr};
                    String[] f={"0 / 0", "lim H(x)/P(x)", "= "+hasil};
                    String[] d={"Substitusi langsung 0/0. Gunakan Metode Gambar 12 Metode A.", "Faktorkan pembilang/penyebut untuk mencoret faktor nol.", "Dapatkan hasil limit "+hasil+"."};
                    setStepData(t, s, f, d);
                }
            } else {
                hasil = formatHasil(yDirect);
                String[] t={"Teorema Limit", "Substitusi", "Hasil Akhir"};
                String[] s={"Gambar 5", "Masukkan x="+aStr, "Selesai"};
                String[] f={"lim f(x)=f(a)", "f("+aStr+")", "= "+hasil};
                String[] d={"Limit kontinu. Berdasarkan Gambar 5, cukup substitusi langsung.", "Ganti x dengan "+aStr+" ke fungsi asli.", "Didapatkan hasil akhir "+hasil+"."};
                setStepData(t, s, f, d);
            }

            txtHasil.setText("lim x→" + aStr + " (" + fx + ") = " + hasil);
            txtHasilAkhir.setText(hasil);
            txtLimitLabel.setText("x→" + aStr);
            plotGrafik(fNorm, a, hasil);
        } catch (Exception e) { txtHasil.setText("Error"); }
    }

    private void setupChart() {
        lineChart.getDescription().setEnabled(false);
        lineChart.getLegend().setEnabled(false);
        lineChart.setDrawGridBackground(false);
        lineChart.setBackgroundColor(Color.WHITE);
        XAxis xAxis = lineChart.getXAxis();
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        xAxis.setDrawGridLines(true);
        xAxis.setGridColor(Color.parseColor("#F3F4F6"));
        lineChart.getAxisLeft().setDrawGridLines(true);
        lineChart.getAxisLeft().setGridColor(Color.parseColor("#F3F4F6"));
        lineChart.getAxisRight().setEnabled(false);
    }

    private void plotGrafik(String f, double a, String resStr) {
        ArrayList<Entry> pts = new ArrayList<>();
        for (int i = -30; i <= 30; i++) {
            float x = (float)a + (i / 10f);
            double y = evalDenganX(f, x);
            if (!Double.isNaN(y) && Math.abs(y) < 500) pts.add(new Entry(x, (float)y));
        }
        LineDataSet ds = new LineDataSet(pts, "f(x)");
        ds.setColor(0xFF1D4FFF); ds.setLineWidth(3f); ds.setDrawCircles(false); ds.setDrawValues(false);
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

    private void setStepData(String[] t, String[] s, String[] f, String[] d) {
        for (int i = 0; i < 3; i++) {
            stepTitles[i].setText(t[i]); stepSubtitles[i].setText(s[i]);
            stepFormulas[i].setText(f[i]); stepDetails[i].setText(d[i]);
            stepDetails[i].setVisibility(View.GONE); stepExpanded[i] = false;
            stepArrows[i].setRotation(0f);
        }
    }

    private void setupKeyboard(View v) {
        activeInput = inputFunction;
        inputFunction.setOnClickListener(v1 -> { activeInput = inputFunction; keyboardPanel.setVisibility(View.VISIBLE); });
        inputLimitValue.setOnClickListener(v1 -> { activeInput = inputLimitValue; keyboardPanel.setVisibility(View.VISIBLE); });
        btnHideKeyboard.setOnClickListener(v1 -> keyboardPanel.setVisibility(View.GONE));
        int[] ids = {R.id.btnSin, R.id.btnCos, R.id.btnTan, R.id.btnLn, R.id.btnLog, R.id.btnSqrt, R.id.btnSquare, R.id.btnPow, R.id.btnPi, R.id.btnE,
                R.id.btn7, R.id.btn8, R.id.btn9, R.id.btnDivide, R.id.btn4, R.id.btn5, R.id.btn6, R.id.btnMultiply, R.id.btnCaret, R.id.btn1, R.id.btn2, R.id.btn3, R.id.btnMinus, R.id.btnOpenParen,
                R.id.btn0, R.id.btnDot, R.id.btnCloseParen, R.id.btnPlus, R.id.btnX};
        String[] vals = {"sin(", "cos(", "tan(", "ln(", "log(", "√(", "²", "^", "π", "e", "7", "8", "9", "/", "4", "5", "6", "*", "/", "1", "2", "3", "-", "(", "0", ".", ")", "+", "x"};
        for (int i = 0; i < ids.length; i++) {
            final String val = vals[i];
            v.findViewById(ids[i]).setOnClickListener(v1 -> { activeInput.getText().insert(activeInput.getSelectionStart(), val); });
        }
        v.findViewById(R.id.btnEquals).setOnClickListener(v1 -> prosesHitungCerdas());
        v.findViewById(R.id.btnBackspace).setOnClickListener(v1 -> { String t = activeInput.getText().toString(); if (!t.isEmpty()) { activeInput.setText(t.substring(0, t.length()-1)); activeInput.setSelection(activeInput.length()); } });
        v.findViewById(R.id.btnAC).setOnClickListener(v1 -> resetAll());
    }

    private void resetAll() {
        inputFunction.setText(""); inputLimitValue.setText(""); txtHasil.setText(""); txtHasilAkhir.setText("");
        lineChart.clear(); txtLimitLabel.setText("x→a");
        for(int i=0; i<3; i++) { stepTitles[i].setText(""); stepSubtitles[i].setText(""); stepFormulas[i].setText(""); stepDetails[i].setText(""); stepDetails[i].setVisibility(View.GONE); }
    }

    private void setupStepAccordion() {
        for (int i = 0; i < 3; i++) {
            final int idx = i;
            if (stepHeaders[i] != null) {
                stepHeaders[i].setOnClickListener(v -> {
                    stepExpanded[idx] = !stepExpanded[idx];
                    stepDetails[idx].setVisibility(stepExpanded[idx] ? View.VISIBLE : View.GONE);
                    stepArrows[idx].setRotation(stepExpanded[idx] ? 180f : 0f);
                });
            }
        }
    }

    private void hideCustomKeyboard() { keyboardPanel.setVisibility(View.GONE); }
    private void fixInsets(View v) { ViewCompat.setOnApplyWindowInsetsListener(v, (v1, in) -> { Insets nav = in.getInsets(WindowInsetsCompat.Type.navigationBars()); keyboardPanel.setPadding(keyboardPanel.getPaddingLeft(), keyboardPanel.getPaddingTop(), keyboardPanel.getPaddingRight(), nav.bottom + 12); return in; }); }
    private String hitungHasilTurunan(String expr) {
        String e = expr.replace(" ", "").replace("−", "-").replace("²", "^2");
        if (!e.startsWith("-") && !e.startsWith("+")) e = "+" + e;
        StringBuilder res = new StringBuilder();
        String[] terms = e.split("(?=[+-])");
        for (String t : terms) {
            if (!t.contains("x")) continue;
            double sign = t.startsWith("-") ? -1 : 1;
            String s = t.substring(1);
            double k, p;
            if (s.contains("x^2") || s.contains("x²")) { k = (s.replace("x^2","").replace("x²","").isEmpty())?1:Double.parseDouble(s.replace("x^2","").replace("x²","")); p=2; }
            else { k = s.replace("x","").isEmpty()?1:Double.parseDouble(s.replace("x","")); p=1; }
            double nk = k * p * sign; double np = p - 1;
            String d = (np==0) ? formatHasil(nk) : formatHasil(nk)+"x";
            if (res.length()>0 && !d.startsWith("-")) res.append("+");
            res.append(d);
        }
        return res.toString();
    }
    private double evalDenganX(String e, double x) { return evalEkspresi(e.replaceAll("(?<![a-z])x(?![a-z])", "("+x+")")); }
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
