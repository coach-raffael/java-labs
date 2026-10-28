package shelfy;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class LateFeeTest {

    @Test
    void theFirstThreeDaysAreFree() {
        assertEquals(0.0, LateFee.forBook(0), 0.001);
        assertEquals(0.0, LateFee.forBook(3), 0.001);
    }

    @ParameterizedTest(name = "{0} days overdue cost {1}")
    @CsvSource({"4, 0.5", "6, 1.5", "30, 10.0"})
    void everyDayAfterThatCostsFiftyCents(long daysOverdue, double expected) {
        assertEquals(expected, LateFee.forBook(daysOverdue), 0.001);
    }
}
