package com.example.plant_app.utils;

import java.time.LocalDate;

public class SeasonUtil {
    public static String getCurrentSeason() {
        int month = LocalDate.now().getMonthValue();
        if (month >= 3 && month <= 5) return "весна";
        if (month >= 6 && month <= 8) return "лето";
        if (month >= 9 && month <= 11) return "осень";
        return "зима";
    }
}
