package com.example.aplikasikalkulatorturunan;

// Stepmodel.java
// Model data untuk satu baris langkah penyelesaian di RecyclerView (rvSteps)

public class Stepmodel {
    public int number;       // nomor urut langkah (1, 2, 3, ...)
    public String title;     // nama teorema, contoh: "Theorem C: Power Rule"
    public String formula;   // rumus umum, contoh: "Dx(x^n) = n x^(n-1)"
    public String detail;    // ekspresi konkret + penjelasan natural

    public Stepmodel(int number, String title, String formula, String detail) {
        this.number = number;
        this.title = title;
        this.formula = formula;
        this.detail = detail;
    }
}