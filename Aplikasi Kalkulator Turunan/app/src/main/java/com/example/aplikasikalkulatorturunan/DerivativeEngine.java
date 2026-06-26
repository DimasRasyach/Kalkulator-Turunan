// DerivativeEngine.java
// Mesin penurunan simbolik: parsing ekspresi -> AST -> turunkan sesuai
// teorema (Power Rule, Product Rule, Quotient Rule, Chain Rule, dst),
// sambil mencatat setiap langkah beserta nama teorema yang dipakai.
//
// CATATAN PENTING (perbaikan Quotient Rule):
// Untuk DIV, pembilang dan penyebut SENGAJA tidak disederhanakan secara
// agresif/diekspansi. Pembilang dibiarkan dalam bentuk
// SUB(MUL(f',g), MUL(f,g')) dan penyebut dalam bentuk POW(g,2), supaya
// hasil akhirnya selalu terbaca seperti contoh dosen:
//      y' = [2x(x+1) - (x^2+1)] / (x+1)^2
// bukan diekspansi/diacak menjadi bentuk lain.
//
// Letakkan di package yang sama dengan TurunanFragment.java

package com.example.aplikasikalkulatorturunan;

import java.util.ArrayList;
import java.util.List;

public class DerivativeEngine {

    // =========================================================
    // MODEL: satu langkah penyelesaian
    // =========================================================
    public static class Step {
        public String theorem;
        public String formulaRule;
        public String expression;
        public String explanation;

        public Step(String theorem, String formulaRule, String expression, String explanation) {
            this.theorem = theorem;
            this.formulaRule = formulaRule;
            this.expression = expression;
            this.explanation = explanation;
        }
    }

    public static class Result {
        public Node derivative;
        public String derivativeStr;
        public List<Step> steps;

        public Result(Node derivative, String derivativeStr, List<Step> steps) {
            this.derivative = derivative;
            this.derivativeStr = derivativeStr;
            this.steps = steps;
        }
    }

    // =========================================================
    // AST NODE
    // =========================================================
    public enum NodeType {
        CONST, VAR, ADD, SUB, MUL, DIV, POW,
        SIN, COS, TAN, SEC, CSC, COT,
        EXP, LN, LOG, SQRT, NEG
    }

    public static class Node {
        NodeType type;
        double value;
        Node left, right;

        Node(NodeType type) { this.type = type; }

        static Node constant(double v) { Node n = new Node(NodeType.CONST); n.value = v; return n; }
        static Node variable() { return new Node(NodeType.VAR); }

        static Node bin(NodeType t, Node l, Node r) { Node n = new Node(t); n.left = l; n.right = r; return n; }
        static Node un(NodeType t, Node l) { Node n = new Node(t); n.left = l; return n; }

        boolean isConst() { return type == NodeType.CONST; }
        boolean isZero()  { return isConst() && Math.abs(value) < 1e-12; }
        boolean isOne()   { return isConst() && Math.abs(value - 1) < 1e-12; }
    }

    // =========================================================
    // PARSER: ekspresi string -> AST
    // =========================================================
    private String expr;
    private int pos;
    private char ch;

    public Node parse(String input) {
        expr = input.replaceAll("\\s+", "")
                .replace("×", "*")
                .replace("÷", "/")
                .replace("²", "^2")
                .replace("³", "^3")
                .replace("√(", "sqrt(")
                .replace("π", "pi");
        pos = -1;
        nextChar();
        Node n = parseExpr();
        if (pos < expr.length()) throw new RuntimeException("Unexpected char: " + ch);
        return n;
    }

    private void nextChar() { pos++; ch = (pos < expr.length()) ? expr.charAt(pos) : '\0'; }

    private boolean eat(char c) {
        if (ch == c) { nextChar(); return true; }
        return false;
    }

    private Node parseExpr() {
        Node node = parseTerm();
        for (;;) {
            if (eat('+')) node = Node.bin(NodeType.ADD, node, parseTerm());
            else if (eat('-')) node = Node.bin(NodeType.SUB, node, parseTerm());
            else return node;
        }
    }

    private Node parseTerm() {
        Node node = parseImplicitMul();
        for (;;) {
            if (eat('*')) node = Node.bin(NodeType.MUL, node, parseImplicitMul());
            else if (eat('/')) node = Node.bin(NodeType.DIV, node, parseImplicitMul());
            else return node;
        }
    }

    private Node parseImplicitMul() {
        Node node = parsePow();
        while (ch == 'x' || ch == '(' || Character.isLetter(ch) || Character.isDigit(ch)) {
            if (ch == '\0' || ch == '+' || ch == '-' || ch == '*' || ch == '/' || ch == '^' || ch == ')') break;
            Node next = parsePow();
            node = Node.bin(NodeType.MUL, node, next);
        }
        return node;
    }

    private Node parsePow() {
        Node node = parseUnary();
        if (eat('^')) {
            Node exp = parseUnary();
            node = Node.bin(NodeType.POW, node, exp);
        }
        return node;
    }

    private Node parseUnary() {
        if (eat('-')) return Node.un(NodeType.NEG, parseUnary());
        if (eat('+')) return parseUnary();
        return parseAtom();
    }

    private Node parseAtom() {
        if (eat('(')) {
            Node n = parseExpr();
            eat(')');
            return n;
        }
        if (Character.isDigit(ch) || ch == '.') {
            int start = pos;
            while (Character.isDigit(ch) || ch == '.') nextChar();
            return Node.constant(Double.parseDouble(expr.substring(start, pos)));
        }
        if (ch == 'x') { nextChar(); return Node.variable(); }
        if (Character.isLetter(ch)) {
            int start = pos;
            while (Character.isLetter(ch)) nextChar();
            String name = expr.substring(start, pos);
            switch (name) {
                case "pi": return Node.constant(Math.PI);
                case "e":  return Node.constant(Math.E);
                case "sqrt": { eat('('); Node a = parseExpr(); eat(')'); return Node.un(NodeType.SQRT, a); }
                case "sin": { Node a = parseFnArg(); return Node.un(NodeType.SIN, a); }
                case "cos": { Node a = parseFnArg(); return Node.un(NodeType.COS, a); }
                case "tan": { Node a = parseFnArg(); return Node.un(NodeType.TAN, a); }
                case "sec": { Node a = parseFnArg(); return Node.un(NodeType.SEC, a); }
                case "csc": { Node a = parseFnArg(); return Node.un(NodeType.CSC, a); }
                case "cot": { Node a = parseFnArg(); return Node.un(NodeType.COT, a); }
                case "exp": { Node a = parseFnArg(); return Node.un(NodeType.EXP, a); }
                case "ln":  { Node a = parseFnArg(); return Node.un(NodeType.LN, a); }
                case "log": { Node a = parseFnArg(); return Node.un(NodeType.LOG, a); }
                default: throw new RuntimeException("Unknown identifier: " + name);
            }
        }
        throw new RuntimeException("Unexpected char: " + ch);
    }

    private Node parseFnArg() {
        if (eat('(')) { Node a = parseExpr(); eat(')'); return a; }
        return parseUnary();
    }

    // =========================================================
    // DIFERENSIASI SIMBOLIK + PENCATATAN LANGKAH
    // =========================================================
    private List<Step> steps;

    public Result differentiate(String input) {
        steps = new ArrayList<>();
        Node tree = parse(input);

        steps.add(new Step(
                "Analisis Fungsi",
                "f(x) = " + input,
                "f(x) = " + toStr(tree),
                "Fungsi dianalisis untuk menentukan aturan turunan yang tepat (Notasi Aksen f'(x) atau Leibniz dy/dx)."
        ));

        Node d = diff(tree);
        Node expanded = expand(d);
        Node simplified = simplify(expanded);

        steps.add(new Step(
                "Hasil Akhir",
                "dy/dx = f'(x)",
                "f'(x) = " + toStr(simplified),
                "Proses perhitungan selesai. Hasil turunan sudah dalam bentuk akhir."
        ));

        return new Result(simplified, toStr(simplified), steps);
    }

    private Node diff(Node n) {
        switch (n.type) {
            case CONST:
                addStep("Theorem A: Constant Function Rule",
                        "Dx(k) = 0",
                        "Dx(" + toStr(n) + ") = 0",
                        "Turunan dari konstanta selalu bernilai 0, karena konstanta tidak berubah terhadap x.");
                return Node.constant(0);

            case VAR:
                addStep("Theorem B: Identity Function Rule",
                        "Dx(x) = 1",
                        "Dx(x) = 1",
                        "Turunan dari fungsi identitas f(x) = x adalah 1.");
                return Node.constant(1);

            case NEG:
                return Node.un(NodeType.NEG, diff(n.left));

            case ADD: {
                Node dl = diff(n.left);
                Node dr = diff(n.right);
                addStep("Theorem E: Sum Rule",
                        "Dx[f(x) + g(x)] = f'(x) + g'(x)",
                        "Dx[" + toStr(n.left) + " + " + toStr(n.right) + "] = " + toStr(dl) + " + " + toStr(dr),
                        "Turunan dari penjumlahan dua fungsi adalah penjumlahan dari turunan masing-masing fungsi.");
                return Node.bin(NodeType.ADD, dl, dr);
            }

            case SUB: {
                Node dl = diff(n.left);
                Node dr = diff(n.right);
                addStep("Theorem F: Difference Rule",
                        "Dx[f(x) - g(x)] = f'(x) - g'(x)",
                        "Dx[" + toStr(n.left) + " - " + toStr(n.right) + "] = " + toStr(dl) + " - " + toStr(dr),
                        "Turunan dari pengurangan dua fungsi adalah pengurangan dari turunan masing-masing fungsi.");
                return Node.bin(NodeType.SUB, dl, dr);
            }

            case MUL: {
                if (n.left.isConst()) {
                    Node dr = diff(n.right);
                    addStep("Theorem D: Constant Multiple Rule",
                            "Dx[k * f(x)] = k * f'(x)",
                            "Dx[" + toStr(n.left) + " * " + toStr(n.right) + "] = " + toStr(n.left) + " * " + toStr(dr),
                            "Konstanta pengali dapat dikeluarkan dari operator turunan: k dikalikan langsung dengan f'(x).");
                    return Node.bin(NodeType.MUL, n.left, dr);
                }
                if (n.right.isConst()) {
                    Node dl = diff(n.left);
                    addStep("Theorem D: Constant Multiple Rule",
                            "Dx[k * f(x)] = k * f'(x)",
                            "Dx[" + toStr(n.right) + " * " + toStr(n.left) + "] = " + toStr(n.right) + " * " + toStr(dl),
                            "Konstanta pengali dapat dikeluarkan dari operator turunan: k dikalikan langsung dengan f'(x).");
                    return Node.bin(NodeType.MUL, n.right, dl);
                }
                Node dl = diff(n.left);
                Node dr = diff(n.right);
                addStep("Theorem G: Product Rule",
                        "Dx[f(x)g(x)] = f(x)Dx g(x) + g(x)Dx f(x)",
                        "Dx[" + toStr(n.left) + " * " + toStr(n.right) + "] = (" + toStr(dl) + ")(" + toStr(n.right)
                                + ") + (" + toStr(n.left) + ")(" + toStr(dr) + ")",
                        "Untuk perkalian dua fungsi f(x) dan g(x), gunakan Product Rule: turunan suku pertama dikali fungsi kedua, ditambah fungsi pertama dikali turunan suku kedua."
                );
                return Node.bin(NodeType.ADD,
                        Node.bin(NodeType.MUL, dl, n.right),
                        Node.bin(NodeType.MUL, n.left, dr));
            }

            case DIV: {
                Node dl = diff(n.left);
                Node dr = diff(n.right);
                addStep("Theorem H: Quotient Rule",
                        "Dx[f(x)/g(x)] = [g(x)Dx f(x) - f(x)Dx g(x)] / g(x)^2",
                        "Dx[" + toStr(n.left) + " / " + toStr(n.right) + "] = [(" + toStr(dl) + ")(" + toStr(n.right)
                                + ") - (" + toStr(n.left) + ")(" + toStr(dr) + ")] / (" + toStr(n.right) + ")^2",
                        "Untuk pembagian dua fungsi, gunakan Quotient Rule: (turunan pembilang × penyebut − pembilang × turunan penyebut) dibagi kuadrat penyebut."
                );
                Node numerator = Node.bin(NodeType.SUB,
                        Node.bin(NodeType.MUL, dl, n.right),
                        Node.bin(NodeType.MUL, n.left, dr));
                Node denominator = Node.bin(NodeType.POW, n.right, Node.constant(2));
                return Node.bin(NodeType.DIV, numerator, denominator);
            }

            case POW: {
                if (n.left.type == NodeType.VAR && n.right.isConst()) {
                    double expn = n.right.value;
                    addStep("Theorem C: Power Rule",
                            "Dx(x^n) = n x^(n-1)",
                            "Dx(x^" + fmt(expn) + ") = " + fmt(expn) + "x^" + fmt(expn - 1),
                            "Aturan pangkat (Power Rule): turunan dari x^n adalah n dikali x^(n-1)."
                    );
                    Node coeff = Node.constant(expn);
                    Node newPow = Node.bin(NodeType.POW, Node.variable(), Node.constant(expn - 1));
                    return Node.bin(NodeType.MUL, coeff, newPow);
                }
                Node dg = diff(n.left);
                addStep("Chain Rule + Power Rule",
                        "Dx[g(x)^n] = n*g(x)^(n-1) * Dx g(x)",
                        "Dx[(" + toStr(n.left) + ")^" + toStr(n.right) + "] = " + toStr(n.right) + "*(" + toStr(n.left)
                                + ")^(" + toStr(n.right) + "-1) * (" + toStr(dg) + ")",
                        "Karena basisnya bukan x murni melainkan fungsi g(x), gunakan Chain Rule: turunan luar (Power Rule) dikalikan turunan dalam g'(x)."
                );
                Node newExp = Node.bin(NodeType.SUB, n.right, Node.constant(1));
                Node outer = Node.bin(NodeType.MUL, n.right, Node.bin(NodeType.POW, n.left, newExp));
                return Node.bin(NodeType.MUL, outer, dg);
            }

            case SIN: {
                Node dg = diff(n.left);
                boolean simpleX = n.left.type == NodeType.VAR;
                addStep(simpleX ? "Theorem A: Trig Derivative" : "Chain Rule + Trig Derivative",
                        "Dx(sin x) = cos x",
                        simpleX
                                ? "Dx(sin x) = cos x"
                                : "Dx(sin(" + toStr(n.left) + ")) = cos(" + toStr(n.left) + ") * (" + toStr(dg) + ")",
                        simpleX
                                ? "Turunan dari sin x adalah cos x."
                                : "Karena argumen sinus bukan x murni, gunakan Chain Rule: turunan luar cos(u) dikalikan turunan dalam u'."
                );
                Node cosPart = Node.un(NodeType.COS, n.left);
                return simpleX ? cosPart : Node.bin(NodeType.MUL, cosPart, dg);
            }

            case COS: {
                Node dg = diff(n.left);
                boolean simpleX = n.left.type == NodeType.VAR;
                addStep(simpleX ? "Theorem A: Trig Derivative" : "Chain Rule + Trig Derivative",
                        "Dx(cos x) = -sin x",
                        simpleX
                                ? "Dx(cos x) = -sin x"
                                : "Dx(cos(" + toStr(n.left) + ")) = -sin(" + toStr(n.left) + ") * (" + toStr(dg) + ")",
                        simpleX
                                ? "Turunan dari cos x adalah -sin x."
                                : "Karena argumen cosinus bukan x murni, gunakan Chain Rule: turunan luar -sin(u) dikalikan turunan dalam u'."
                );
                Node sinPart = Node.un(NodeType.NEG, Node.un(NodeType.SIN, n.left));
                return simpleX ? sinPart : Node.bin(NodeType.MUL, sinPart, dg);
            }

            case TAN: {
                Node dg = diff(n.left);
                boolean simpleX = n.left.type == NodeType.VAR;
                addStep(simpleX ? "Theorem B: Trig Derivative" : "Chain Rule + Trig Derivative",
                        "Dx(tan x) = sec^2 x",
                        simpleX
                                ? "Dx(tan x) = sec^2 x"
                                : "Dx(tan(" + toStr(n.left) + ")) = sec^2(" + toStr(n.left) + ") * (" + toStr(dg) + ")",
                        simpleX
                                ? "Turunan dari tan x adalah sec^2 x."
                                : "Karena argumen tangen bukan x murni, gunakan Chain Rule: turunan luar sec^2(u) dikalikan turunan dalam u'."
                );
                Node secPart = Node.bin(NodeType.POW, Node.un(NodeType.SEC, n.left), Node.constant(2));
                return simpleX ? secPart : Node.bin(NodeType.MUL, secPart, dg);
            }

            case SEC: {
                Node dg = diff(n.left);
                addStep("Theorem B: Trig Derivative",
                        "Dx(sec x) = sec x tan x",
                        "Dx(sec(" + toStr(n.left) + ")) = sec(" + toStr(n.left) + ")tan(" + toStr(n.left) + ") * (" + toStr(dg) + ")",
                        "Turunan dari sec x adalah sec x tan x, dikalikan turunan argumen bila bukan x murni."
                );
                Node part = Node.bin(NodeType.MUL, Node.un(NodeType.SEC, n.left), Node.un(NodeType.TAN, n.left));
                return Node.bin(NodeType.MUL, part, dg);
            }

            case CSC: {
                Node dg = diff(n.left);
                addStep("Theorem B: Trig Derivative",
                        "Dx(csc x) = -csc x cot x",
                        "Dx(csc(" + toStr(n.left) + ")) = -csc(" + toStr(n.left) + ")cot(" + toStr(n.left) + ") * (" + toStr(dg) + ")",
                        "Turunan dari csc x adalah -csc x cot x, dikalikan turunan argumen bila bukan x murni."
                );
                Node part = Node.un(NodeType.NEG, Node.bin(NodeType.MUL, Node.un(NodeType.CSC, n.left), Node.un(NodeType.COT, n.left)));
                return Node.bin(NodeType.MUL, part, dg);
            }

            case COT: {
                Node dg = diff(n.left);
                addStep("Theorem B: Trig Derivative",
                        "Dx(cot x) = -csc^2 x",
                        "Dx(cot(" + toStr(n.left) + ")) = -csc^2(" + toStr(n.left) + ") * (" + toStr(dg) + ")",
                        "Turunan dari cot x adalah -csc^2 x, dikalikan turunan argumen bila bukan x murni."
                );
                Node part = Node.un(NodeType.NEG, Node.bin(NodeType.POW, Node.un(NodeType.CSC, n.left), Node.constant(2)));
                return Node.bin(NodeType.MUL, part, dg);
            }

            case EXP: {
                Node dg = diff(n.left);
                boolean simpleX = n.left.type == NodeType.VAR;
                addStep("Exponential Rule",
                        "Dx(e^x) = e^x",
                        simpleX
                                ? "Dx(e^x) = e^x"
                                : "Dx(e^(" + toStr(n.left) + ")) = e^(" + toStr(n.left) + ") * (" + toStr(dg) + ")",
                        simpleX
                                ? "Turunan dari e^x adalah e^x itu sendiri."
                                : "Gunakan Chain Rule: turunan e^u adalah e^u dikalikan turunan dalam u'."
                );
                Node expPart = Node.un(NodeType.EXP, n.left);
                return simpleX ? expPart : Node.bin(NodeType.MUL, expPart, dg);
            }

            case LN: {
                Node dg = diff(n.left);
                boolean simpleX = n.left.type == NodeType.VAR;
                addStep("Logarithm Rule",
                        "Dx(ln x) = 1/x",
                        simpleX
                                ? "Dx(ln x) = 1/x"
                                : "Dx(ln(" + toStr(n.left) + ")) = (" + toStr(dg) + ") / (" + toStr(n.left) + ")",
                        simpleX
                                ? "Turunan dari ln x adalah 1/x."
                                : "Gunakan Chain Rule: turunan ln(u) adalah u'/u."
                );
                return simpleX
                        ? Node.bin(NodeType.DIV, Node.constant(1), Node.variable())
                        : Node.bin(NodeType.DIV, dg, n.left);
            }

            case LOG: {
                Node dg = diff(n.left);
                addStep("Logarithm Rule (basis 10)",
                        "Dx(log x) = 1/(x ln 10)",
                        "Dx(log(" + toStr(n.left) + ")) = (" + toStr(dg) + ") / ((" + toStr(n.left) + ") * ln10)",
                        "Turunan dari log basis 10 adalah 1 dibagi (x dikali ln 10), dikalikan turunan dalam bila bukan x murni."
                );
                Node denom = Node.bin(NodeType.MUL, n.left, Node.constant(Math.log(10)));
                return Node.bin(NodeType.DIV, dg, denom);
            }

            case SQRT: {
                Node asPow = Node.bin(NodeType.POW, n.left, Node.constant(0.5));
                return diff(asPow);
            }

            default:
                throw new RuntimeException("Tidak didukung: " + n.type);
        }
    }

    // Gabungkan suku-suku sejenis dalam ekspresi ADD, misal 2x^2 + x^2 → 3x^2
    private Node combineAddTerms(Node n) {
        List<Node> terms = new ArrayList<>();
        collectAddTerms(n, terms);

        // Map: kunci basis term → koefisien total
        java.util.LinkedHashMap<String, Double> coeffMap = new java.util.LinkedHashMap<>();
        java.util.Map<String, Node> baseMap = new java.util.HashMap<>();
        List<Node> nonLikeTerms = new ArrayList<>();

        for (Node t : terms) {
            // Ekstrak koefisien dan "bentuk" term
            double coeff = 1.0;
            Node base = t;

            if (t.isConst()) {
                // konstanta murni: kunci = "__const__"
                String key = "__const__";
                coeffMap.merge(key, t.value, Double::sum);
                baseMap.putIfAbsent(key, Node.constant(1));
                continue;
            }
            if (t.type == NodeType.NEG && t.left.isConst()) {
                coeff = -t.left.value;
                base = Node.constant(1);
                String key = "__const__";
                coeffMap.merge(key, coeff, Double::sum);
                baseMap.putIfAbsent(key, Node.constant(1));
                continue;
            }
            // k * something
            if (t.type == NodeType.MUL && t.left.isConst()) {
                coeff = t.left.value;
                base = t.right;
            } else if (t.type == NodeType.MUL && t.right.isConst()) {
                coeff = t.right.value;
                base = t.left;
            } else if (t.type == NodeType.NEG) {
                coeff = -1.0;
                base = t.left;
            }

            String key = toStr(base);
            coeffMap.merge(key, coeff, Double::sum);
            baseMap.putIfAbsent(key, base);
        }

        // Rekonstruksi dari map
        List<Node> resultTerms = new ArrayList<>();
        for (String key : coeffMap.keySet()) {
            double c = coeffMap.get(key);
            if (Math.abs(c) < 1e-12) continue; // suku hilang
            Node b = baseMap.get(key);
            Node term;
            if (key.equals("__const__")) {
                term = Node.constant(c);
            } else if (Math.abs(c - 1.0) < 1e-12) {
                term = b;
            } else if (Math.abs(c + 1.0) < 1e-12) {
                term = Node.un(NodeType.NEG, b);
            } else {
                term = Node.bin(NodeType.MUL, Node.constant(c), b);
            }
            resultTerms.add(term);
        }

        if (resultTerms.isEmpty()) return Node.constant(0);
        Node result = resultTerms.get(0);
        for (int i = 1; i < resultTerms.size(); i++) {
            Node t = resultTerms.get(i);
            // Jika koefisiennya negatif, pakai SUB supaya tampilan lebih bersih
            if (t.type == NodeType.NEG) {
                result = Node.bin(NodeType.SUB, result, t.left);
            } else if (t.isConst() && t.value < 0) {
                result = Node.bin(NodeType.SUB, result, Node.constant(-t.value));
            } else {
                result = Node.bin(NodeType.ADD, result, t);
            }
        }
        return result;
    }

    private void collectAddTerms(Node n, List<Node> out) {
        if (n.type == NodeType.ADD) {
            collectAddTerms(n.left, out);
            collectAddTerms(n.right, out);
        } else if (n.type == NodeType.SUB) {
            collectAddTerms(n.left, out);
            // Sisi kanan SUB → negatifkan
            out.add(negateTerm(n.right));
        } else {
            out.add(n);
        }
    }

    private Node negateTerm(Node n) {
        if (n.isConst()) return Node.constant(-n.value);
        if (n.type == NodeType.NEG) return n.left;
        if (n.type == NodeType.MUL && n.left.isConst())
            return Node.bin(NodeType.MUL, Node.constant(-n.left.value), n.right);
        return Node.un(NodeType.NEG, n);
    }

    private void addStep(String theorem, String formulaRule, String expression, String explanation) {
        steps.add(new Step(theorem, formulaRule, expression, explanation));
    }

    // =========================================================
    // SIMPLIFIKASI
    // =========================================================
    private Node simplify(Node n) {
        if (n == null) return null;
        switch (n.type) {
            case ADD: {
                Node l = simplify(n.left), r = simplify(n.right);
                if (l.isZero()) return r;
                if (r.isZero()) return l;
                if (l.isConst() && r.isConst()) return Node.constant(l.value + r.value);
                Node combined = combineAddTerms(Node.bin(NodeType.ADD, l, r));
                return combined;
            }
            case SUB: {
                Node l = simplify(n.left), r = simplify(n.right);
                if (r.isZero()) return l;
                if (l.isConst() && r.isConst()) return Node.constant(l.value - r.value);
                return Node.bin(NodeType.SUB, l, r);
            }
            case MUL:
                return simplifyMul(n);
            case DIV: {
                Node l = simplify(n.left);
                Node r = simplify(n.right);
                if (l.isZero()) return Node.constant(0);
                if (r.isOne()) return l;
                return Node.bin(NodeType.DIV, l, r);
            }
            case POW: {
                Node l = simplify(n.left), r = simplify(n.right);
                if (r.isZero()) return Node.constant(1);
                if (r.isOne()) return l;
                if (l.isConst() && r.isConst()) return Node.constant(Math.pow(l.value, r.value));
                return Node.bin(NodeType.POW, l, r);
            }
            case NEG: {
                Node l = simplify(n.left);
                if (l.isConst()) return Node.constant(-l.value);
                return Node.un(NodeType.NEG, l);
            }
            case SIN: case COS: case TAN: case SEC: case CSC: case COT:
            case EXP: case LN: case LOG: case SQRT: {
                Node l = simplify(n.left);
                return Node.un(n.type, l);
            }
            default:
                return n;
        }
    }

    private Node expand(Node n) {
        if (n == null) return null;
        switch (n.type) {
            case MUL: {
                Node l = expand(n.left);
                Node r = expand(n.right);
                // a * (b + c) → ab + ac
                if (r.type == NodeType.ADD)
                    return expand(Node.bin(NodeType.ADD,
                            Node.bin(NodeType.MUL, l, r.left),
                            Node.bin(NodeType.MUL, l, r.right)));
                if (r.type == NodeType.SUB)
                    return expand(Node.bin(NodeType.SUB,
                            Node.bin(NodeType.MUL, l, r.left),
                            Node.bin(NodeType.MUL, l, r.right)));
                // (a + b) * c → ac + bc
                if (l.type == NodeType.ADD)
                    return expand(Node.bin(NodeType.ADD,
                            Node.bin(NodeType.MUL, l.left, r),
                            Node.bin(NodeType.MUL, l.right, r)));
                if (l.type == NodeType.SUB)
                    return expand(Node.bin(NodeType.SUB,
                            Node.bin(NodeType.MUL, l.left, r),
                            Node.bin(NodeType.MUL, l.right, r)));
                return Node.bin(NodeType.MUL, l, r);
            }
            case ADD: return Node.bin(NodeType.ADD, expand(n.left), expand(n.right));
            case SUB: return Node.bin(NodeType.SUB, expand(n.left), expand(n.right));
            default:  return n;
        }
    }

    private Node simplifyMul(Node n) {
        List<Node> rawFactors = new ArrayList<>();
        collectMulFactors(n, rawFactors);

        List<Node> flat = new ArrayList<>();
        for (Node f : rawFactors) {
            Node sf = simplify(f);
            collectMulFactors(sf, flat);
        }

        double coeff = 1.0;
        List<Node> others = new ArrayList<>();
        for (Node f : flat) {
            if (f.isZero()) return Node.constant(0);
            if (f.isConst()) {
                coeff *= f.value;
            } else if (f.type == NodeType.NEG && f.left.isConst()) {
                coeff *= -f.left.value;
            } else if (!f.isOne()) {
                others.add(f);
            }
        }

        if (coeff == 0) return Node.constant(0);

        java.util.LinkedHashMap<String, Double> powerGroups = new java.util.LinkedHashMap<>();
        java.util.Map<String, Node> baseNodes = new java.util.HashMap<>();
        List<Node> nonPowerOthers = new ArrayList<>();

        for (Node o : others) {
            Node base; double exp;
            if (o.type == NodeType.VAR) {
                base = o; exp = 1;
            } else if (o.type == NodeType.POW && o.right.isConst()) {
                base = o.left; exp = o.right.value;
            } else {
                nonPowerOthers.add(o);
                continue;
            }
            String key = toStr(base); // identitas basis
            powerGroups.merge(key, exp, Double::sum);
            baseNodes.putIfAbsent(key, base);
        }

        List<Node> rest = new ArrayList<>(nonPowerOthers);
        for (String key : powerGroups.keySet()) {
            double totalExp = powerGroups.get(key);
            Node base = baseNodes.get(key);
            if (totalExp == 1) rest.add(base);
            else rest.add(Node.bin(NodeType.POW, base, Node.constant(totalExp)));
        }

        boolean hasCoeff = Math.abs(coeff - 1.0) > 1e-12;
        if (rest.isEmpty()) return Node.constant(coeff);

        List<Node> ordered = new ArrayList<>();
        if (hasCoeff) ordered.add(Node.constant(coeff));
        ordered.addAll(rest);

        Node result = ordered.get(0);
        for (int i = 1; i < ordered.size(); i++) {
            result = Node.bin(NodeType.MUL, result, ordered.get(i));
        }
        return result;
    }

    private void collectMulFactors(Node n, List<Node> out) {
        if (n.type == NodeType.MUL) {
            collectMulFactors(n.left, out);
            collectMulFactors(n.right, out);
        } else {
            out.add(n);
        }
    }

    // =========================================================
    // KONVERSI AST -> STRING
    // =========================================================
    public String toStr(Node n) {
        if (n == null) return "";
        switch (n.type) {
            case CONST: return fmt(n.value);
            case VAR:   return "x";
            case NEG:   return "-" + wrap(n.left);
            case ADD:   return toStr(n.left) + " + " + toStr(n.right);
            case SUB: {
                String leftStr = toStr(n.left);
                String rightStr = wrapIfAddSub(n.right);
                if (rightStr.startsWith("-")) {
                    return leftStr + " + " + rightStr.substring(1).trim();
                }
                return leftStr + " - " + rightStr;
            }
            case MUL:   return joinMul(n.left, n.right);
            case DIV:   return wrapForDiv(n.left) + "/" + wrapForDiv(n.right);
            case POW:   return wrap(n.left) + "^" + wrap(n.right);
            case SIN:   return "sin(" + toStr(n.left) + ")";
            case COS:   return "cos(" + toStr(n.left) + ")";
            case TAN:   return "tan(" + toStr(n.left) + ")";
            case SEC:   return "sec(" + toStr(n.left) + ")";
            case CSC:   return "csc(" + toStr(n.left) + ")";
            case COT:   return "cot(" + toStr(n.left) + ")";
            case EXP:   return "e^(" + toStr(n.left) + ")";
            case LN:    return "ln(" + toStr(n.left) + ")";
            case LOG:   return "log(" + toStr(n.left) + ")";
            case SQRT:  return "sqrt(" + toStr(n.left) + ")";
            default:    return "?";
        }
    }

    private String wrap(Node n) {
        if (n.type == NodeType.ADD || n.type == NodeType.SUB || n.type == NodeType.MUL || n.type == NodeType.DIV)
            return "(" + toStr(n) + ")";
        return toStr(n);
    }

    private String wrapIfAddSub(Node n) {
        if (n.type == NodeType.ADD || n.type == NodeType.SUB) return "(" + toStr(n) + ")";
        return toStr(n);
    }

    private String wrapForDiv(Node n) {
        if (n.type == NodeType.ADD || n.type == NodeType.SUB || n.type == NodeType.DIV)
            return "(" + toStr(n) + ")";
        if (n.type == NodeType.MUL) return "(" + toStr(n) + ")";
        return toStr(n);
    }

    private String joinMul(Node left, Node right) {
        // Handle: CONST * NEG(something) → "-coeff * something"
        if (left.isConst() && right.type == NodeType.NEG) {
            double newCoeff = -left.value;
            Node inner = right.left;
            String coeffStr = fmt(newCoeff);
            boolean innerIsPureVar = inner.type == NodeType.VAR;
            boolean innerIsVarPower = inner.type == NodeType.POW && inner.left.type == NodeType.VAR;
            if (innerIsPureVar || innerIsVarPower) {
                return coeffStr + wrapForMul(inner); // "-8x" atau "-8x^2"
            }
            return coeffStr + "*" + wrapForMul(inner); // "-8*sin(4x)"
        }

        // Handle: NEG(something) * CONST → "-coeff * something"
        if (left.type == NodeType.NEG && right.isConst()) {
            double newCoeff = -right.value;
            Node inner = left.left;
            String coeffStr = fmt(newCoeff);
            return coeffStr + "*" + wrapForMul(inner);
        }

        String l = wrapForMul(left);
        String r = wrapForMul(right);

        boolean leftIsNumber = left.isConst();
        boolean rightIsPureVar = right.type == NodeType.VAR;
        boolean rightIsVarPower = right.type == NodeType.POW && right.left.type == NodeType.VAR;

        if (leftIsNumber && (rightIsPureVar || rightIsVarPower)) {
            return l + r;
        }
        if (leftIsNumber || right.isConst()) {
            return l + "*" + r;
        }
        if (left.type == NodeType.POW || right.type == NodeType.POW) {
            if (startsWithDigitLike(r)) return l + "*" + r;
        }
        return l + r;
    }

    private boolean startsWithDigitLike(String s) {
        return !s.isEmpty() && (Character.isDigit(s.charAt(0)) || s.charAt(0) == '-');
    }

    private String wrapForMul(Node n) {
        if (n.type == NodeType.ADD || n.type == NodeType.SUB)
            return "(" + toStr(n) + ")";
        if (n.isConst() && n.value < 0)
            return "(" + toStr(n) + ")";
        return toStr(n);
    }

    private String fmt(double d) {
        if (d == Math.floor(d) && !Double.isInfinite(d)) return String.valueOf((long) d);
        return String.valueOf(d);
    }

    // =========================================================
    // EVALUASI NUMERIK (untuk plotting grafik)
    // =========================================================
    public double evaluate(Node n, double xVal) {
        switch (n.type) {
            case CONST: return n.value;
            case VAR:   return xVal;
            case NEG:   return -evaluate(n.left, xVal);
            case ADD:   return evaluate(n.left, xVal) + evaluate(n.right, xVal);
            case SUB:   return evaluate(n.left, xVal) - evaluate(n.right, xVal);
            case MUL:   return evaluate(n.left, xVal) * evaluate(n.right, xVal);
            case DIV:   return evaluate(n.left, xVal) / evaluate(n.right, xVal);
            case POW:   return Math.pow(evaluate(n.left, xVal), evaluate(n.right, xVal));
            case SIN:   return Math.sin(evaluate(n.left, xVal));
            case COS:   return Math.cos(evaluate(n.left, xVal));
            case TAN:   return Math.tan(evaluate(n.left, xVal));
            case SEC:   return 1.0 / Math.cos(evaluate(n.left, xVal));
            case CSC:   return 1.0 / Math.sin(evaluate(n.left, xVal));
            case COT:   return 1.0 / Math.tan(evaluate(n.left, xVal));
            case EXP:   return Math.exp(evaluate(n.left, xVal));
            case LN:    return Math.log(evaluate(n.left, xVal));
            case LOG:   return Math.log10(evaluate(n.left, xVal));
            case SQRT:  return Math.sqrt(evaluate(n.left, xVal));
            default:    return Double.NaN;
        }
    }
}