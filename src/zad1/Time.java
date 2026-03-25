package zad1;


import java.text.DecimalFormat;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.Locale;

public class Time {
    public static String passed(String from, String to) {
        Locale.setDefault(new Locale("pl", "PL"));

        boolean hasTimeFrom = from.contains("T");
        boolean hasTimeTo = to.contains("T");

        DateTimeFormatter dateFormatter = DateTimeFormatter.ISO_LOCAL_DATE;
        DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ISO_LOCAL_DATE_TIME;

        try {

            if (hasTimeFrom && hasTimeTo) {
                LocalDateTime dateFrom = LocalDateTime.parse(from, dateTimeFormatter);
                LocalDateTime dateTo = LocalDateTime.parse(to, dateTimeFormatter);

                ZonedDateTime dateFromZoned = ZonedDateTime.of(dateFrom, ZoneId.of("Europe/Warsaw"));
                ZonedDateTime dateToZoned = ZonedDateTime.of(dateTo, ZoneId.of("Europe/Warsaw"));

                StringBuilder resultText = new StringBuilder(getHeader(dateFromZoned, dateToZoned, true));

                PassedDaysAndWeeks passedDW = getPassedDaysAndWeeks(dateFromZoned, dateToZoned);

                PassedHoursAndMinutes passedHM = getPassedHoursAndMinutes(dateFromZoned, dateToZoned);

                resultText.append(" - mija: ").append(passedDW.days).append(passedDW.days == 1 ? " dzień, tygodni " : " dni, tygodni ")
                        .append(passedDW.weeks)
                        .append("\n - godzin: ").append(passedHM.hoursPeriod).append(", minut: ").append(passedHM.minutesPeriod);

                PassedTimeInCalendarNotation passedTimeCalendar = getPassedTimeInCalendarNotation(dateFromZoned, dateToZoned);

                if (passedDW.days >= 1) {
                    boolean addComma = false;

                    resultText.append("\n - kalendarzowo: ");
                    if (passedTimeCalendar.yearsPeriod >= 1) {
                        String years = adjustPluralForm(passedTimeCalendar.yearsPeriod, "rok", "lata", "lat");
                        resultText.append(years);
                        addComma = true;
                    }
                    if (passedTimeCalendar.monthsPeriod >= 1) {
                        if (addComma) resultText.append(", ");
                        String months = adjustPluralForm(passedTimeCalendar.monthsPeriod, "miesiąc", "miesiące", "miesięcy");
                        resultText.append(months);
                        addComma = true;
                    }
                    if (passedTimeCalendar.daysPeriod >= 1) {
                        if (addComma) resultText.append(", ");
                        String days = adjustPluralForm(passedTimeCalendar.daysPeriod, "dzień", "dni", "dni");
                        resultText.append(days);
                    }

                }

                return resultText.toString();
            } else if (!hasTimeFrom && !hasTimeTo) {
                LocalDate dateFrom = LocalDate.parse(from, dateFormatter);
                LocalDate dateTo = LocalDate.parse(to, dateFormatter);

                ZonedDateTime dateFromZoned = ZonedDateTime.of(dateFrom.atStartOfDay(), ZoneId.of("Europe/Warsaw"));
                ZonedDateTime dateToZoned = ZonedDateTime.of(dateTo.atStartOfDay(), ZoneId.of("Europe/Warsaw"));

                StringBuilder resultText = new StringBuilder(getHeader(dateFromZoned, dateToZoned, false));

                PassedDaysAndWeeks passedDW = getPassedDaysAndWeeks(dateFromZoned, dateToZoned);

                resultText.append(" - mija: ").append(passedDW.days).append(passedDW.days == 1 ? " dzień, tygodni " : " dni, tygodni ")
                        .append(passedDW.weeks);

                PassedTimeInCalendarNotation passedTimeCalendar = getPassedTimeInCalendarNotation(dateFromZoned, dateToZoned);

                if (passedDW.days >= 1) {
                    resultText.append("\n - kalendarzowo: ");
                    if (passedTimeCalendar.yearsPeriod >= 1) {
                        String years = adjustPluralForm(passedTimeCalendar.yearsPeriod, "rok", "lata", "lat");
                        resultText.append(years);
                    }
                    if (passedTimeCalendar.monthsPeriod >= 1) {
                        String months = adjustPluralForm(passedTimeCalendar.monthsPeriod, "miesiąc", "miesiące", "miesięcy");
                        resultText.append(", ").append(months);
                    }
                    if (passedTimeCalendar.daysPeriod >= 1) {
                        String dayss = adjustPluralForm(passedTimeCalendar.daysPeriod, "dzień", "dni", "dni");
                        resultText.append(", ").append(dayss);
                    }

                }

                return resultText.toString();
            } else {
                return "*** Błąd: niezgodne formaty dat (tylko jedna z dat określa godzinę)";
            }

        } catch (java.time.format.DateTimeParseException e) {
            return "*** " + e;
        }
    }

    private static String getHeader(ZonedDateTime dateFromZoned, ZonedDateTime dateToZoned, boolean withHour) {
        StringBuilder resultText = new StringBuilder();
        String dayMonthYearFrom = DateTimeFormatter.ofPattern("d MMMM y").format(dateFromZoned);
        String dayOfWeekFrom = DateTimeFormatter.ofPattern("eeee").format(dateFromZoned);
        String dayMonthYearTo = DateTimeFormatter.ofPattern("d MMMM y").format(dateToZoned);
        String dayOfWeekTo = DateTimeFormatter.ofPattern("eeee").format(dateToZoned);
        String hourFrom = DateTimeFormatter.ofPattern("H:mm").format(dateFromZoned);
        String hourTo = DateTimeFormatter.ofPattern("H:mm").format(dateToZoned);

        if (withHour) {
            resultText.append("Od ").append(dayMonthYearFrom).append(" (").append(dayOfWeekFrom).append(") godz. ")
                    .append(hourFrom)
                    .append(" do ").append(dayMonthYearTo).append(" (").append(dayOfWeekTo).append(") godz. ")
                    .append(hourTo).append("\n");
        } else {
            resultText.append("Od ").append(dayMonthYearFrom).append(" (").append(dayOfWeekFrom).append(") ")
                    .append("do ").append(dayMonthYearTo).append(" (").append(dayOfWeekTo).append(")\n");
        }

        return resultText.toString();
    }

    private static PassedDaysAndWeeks getPassedDaysAndWeeks(ZonedDateTime dateFromZoned, ZonedDateTime dateToZoned) {
        long days = ChronoUnit.DAYS.between(dateFromZoned.toLocalDate(), dateToZoned.toLocalDate());
        double weeksNum = (double) days / 7;
        String weeks = new DecimalFormat("#.##").format(weeksNum)
                .toString().replace(',', '.');
        PassedDaysAndWeeks result = new PassedDaysAndWeeks(days, weeks);
        return result;
    }

    private static PassedHoursAndMinutes getPassedHoursAndMinutes(ZonedDateTime dateFromZoned, ZonedDateTime dateToZoned) {
        long hoursPeriod = ChronoUnit.HOURS.between(dateFromZoned, dateToZoned);
        long minutesPeriod = ChronoUnit.MINUTES.between(dateFromZoned, dateToZoned);
        PassedHoursAndMinutes passedHM = new PassedHoursAndMinutes(hoursPeriod, minutesPeriod);
        return passedHM;
    }

    private static PassedTimeInCalendarNotation getPassedTimeInCalendarNotation(ZonedDateTime dateFromZoned, ZonedDateTime dateToZoned) {
        Period period = Period.between(dateFromZoned.toLocalDate(), dateToZoned.toLocalDate());
        int yearsPeriod = period.getYears();
        int monthsPeriod = period.getMonths();
        int daysPeriod = period.getDays();
        PassedTimeInCalendarNotation passedTimeCalendar = new PassedTimeInCalendarNotation(yearsPeriod, monthsPeriod, daysPeriod);
        return passedTimeCalendar;
    }

    public static String adjustPluralForm(long val, String singular, String pluralV1, String pluralV2) {
        if (val == 1) return val + " " + singular;

        long lastDigit = val % 10;
        long lastTwoDigits = val % 100;

        if (lastTwoDigits >= 10 && lastTwoDigits <= 20) {
            return val + " " + pluralV2;
        }

        if (lastDigit >= 2 && lastDigit <= 4) {
            return val + " " + pluralV1;
        }

        return val + " " + pluralV2;
    }
}
