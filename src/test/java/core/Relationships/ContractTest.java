package core.Relationships;

import org.junit.jupiter.api.Test;

import java.util.Date;
import java.util.GregorianCalendar;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Round-trip tests for {@link Contract}, the immutable tariff record (name, buyer,
 * price c/KWh, start/end dates) consumed by Arena billing. Pins the constructor ->
 * getter mapping so field wiring can't silently drift.
 */
public class ContractTest {

    private static Date date(int year, int month, int day) {
        return new GregorianCalendar(year, month - 1, day).getTime();
    }

    @Test
    void gettersReturnConstructorValues() {
        Date start = date(2021, 7, 1);
        Date end = date(2022, 6, 30);
        Contract c = new Contract("FlatDomestic", 77, 24.5f, start, end);

        assertEquals("FlatDomestic", c.getTariffName());
        assertEquals(77, c.getBuyerId());
        assertEquals(24.5f, c.getPricecKWh(), 1e-6f);
        assertEquals(start, c.getStart());
        assertEquals(end, c.getEnd());
    }

    @Test
    void startPrecedesEnd() {
        Contract c = new Contract("Feed-in", 3, 10.2f, date(2020, 1, 1), date(2020, 12, 31));
        assertTrue(c.getStart().before(c.getEnd()));
    }
}
