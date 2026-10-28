package shelfy;

import java.util.Locale;

// Amounts always print with two decimals and a dot, whatever language the machine speaks (Module 01.6 box).
public final class Money {

    private Money() {
    }

    public static String format(double amount) {
        return String.format(Locale.ROOT, "%.2f", amount);
    }
}
