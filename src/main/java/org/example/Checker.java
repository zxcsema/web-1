package org.example;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Map;

public class Checker {
    private final Map<String, String> data;
    private int x;  // Изменено на int
    private BigDecimal y;
    private int r;  // Изменено на int

    // Параметры для деления (точность)
    private static final int SCALE = 15;
    private static final RoundingMode RM = RoundingMode.HALF_UP;
    private static final BigDecimal TWO = BigDecimal.valueOf(2);

    public Checker(Map<String, String> data) {
        this.data = data;
    }


    public boolean validate() {
        try {
            if (data.get("x") == null || data.get("y") == null || data.get("r") == null) {
                return false;
            }
            this.x = Integer.parseInt(data.get("x").trim());
            this.y = new BigDecimal(data.get("y").trim());
            this.r = Integer.parseInt(data.get("r").trim());

            // радиус должен быть положительным
            return r > 0;
        } catch (NumberFormatException | NullPointerException e) {
            return false;
        }
    }

    public boolean isHit() {
        return inTopLeftTriangle() || inTopRightQuarterCircle() || inBottomRightRectangle();
    }

    private boolean inTopLeftTriangle() {
        BigDecimal xDec = BigDecimal.valueOf(x);
        BigDecimal rDec = BigDecimal.valueOf(r);

        BigDecimal halfR = rDec.divide(TWO, SCALE, RM);
        if (x <= 0
                && xDec.compareTo(halfR.negate()) >= 0
                && y.compareTo(BigDecimal.ZERO) >= 0) {
            BigDecimal rhs = xDec.add(halfR); // x + R/2
            return y.compareTo(rhs) <= 0;
        }
        return false;
    }

    private boolean inTopRightQuarterCircle() {
        if (x >= 0 && y.compareTo(BigDecimal.ZERO) >= 0) {
            BigDecimal xDec = BigDecimal.valueOf(x);
            BigDecimal rDec = BigDecimal.valueOf(r);
            BigDecimal halfR = rDec.divide(TWO, SCALE, RM);
            BigDecimal left = xDec.multiply(xDec).add(y.multiply(y));
            BigDecimal right = halfR.multiply(halfR);
            return left.compareTo(right) <= 0;
        }
        return false;
    }

    // Правый нижний прямоугольник: 0 <= x <= R/2 и -R <= y <= 0
    private boolean inBottomRightRectangle() {
        BigDecimal halfR = BigDecimal.valueOf(r).divide(TWO, SCALE, RM);
        if (x >= 0
                && BigDecimal.valueOf(x).compareTo(halfR) <= 0
                && y.compareTo(BigDecimal.ZERO) <= 0
                && y.compareTo(BigDecimal.valueOf(-r)) >= 0) {
            return true;
        }
        return false;
    }
}