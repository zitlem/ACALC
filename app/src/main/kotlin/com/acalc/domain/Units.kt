package com.acalc.domain

enum class LengthUnit(val displayName: String) {
    MM("Millimeter"), CM("Centimeter"), M("Meter"), KM("Kilometer"),
    INCH("Inch"), FOOT("Foot"), YARD("Yard"), MILE("Mile")
}

enum class WeightUnit(val displayName: String) {
    MG("Milligram"), G("Gram"), KG("Kilogram"),
    OZ("Ounce"), LB("Pound"), TON("Metric ton")
}

enum class VolumeUnit(val displayName: String) {
    ML("Milliliter"), L("Liter"),
    TSP("Teaspoon"), TBSP("Tablespoon"), CUP("Cup"), FL_OZ("Fluid oz (US)"), GALLON("Gallon (US)")
}

enum class TempUnit(val displayName: String) {
    CELSIUS("Celsius"), FAHRENHEIT("Fahrenheit"), KELVIN("Kelvin")
}

enum class AreaUnit(val displayName: String) {
    SQ_MM("mm²"), SQ_CM("cm²"), SQ_M("m²"), SQ_KM("km²"),
    SQ_IN("in²"), SQ_FT("ft²"), ACRE("Acre"), HECTARE("Hectare")
}

enum class SpeedUnit(val displayName: String) {
    M_PER_S("m/s"), KM_PER_H("km/h"), MPH("mph"), KNOTS("Knot"), MACH("Mach")
}

enum class TimeUnit(val displayName: String) {
    MILLISECOND("Millisecond"), SECOND("Second"), MINUTE("Minute"), HOUR("Hour"),
    DAY("Day"), WEEK("Week"), MONTH("Month"), YEAR("Year")
}

enum class ForceUnit(val displayName: String) {
    NEWTON("Newton"), KILONEWTON("Kilonewton"), DYNE("Dyne"),
    GRAM_FORCE("Gram force"), KG_FORCE("Kilogram force"),
    LB_FORCE("Pound force"), POUNDAL("Poundal")
}

enum class PressureUnit(val displayName: String) {
    PASCAL("Pascal"), HECTOPASCAL("Hectopascal"), KILOPASCAL("Kilopascal"),
    MEGAPASCAL("Megapascal"), BAR("Bar"), MILLIBAR("Millibar"),
    ATMOSPHERE("Atmosphere"), PSI("Psi"), MMHG("mmHg"), INHG("inHg"), TORR("Torr")
}

enum class EnergyUnit(val displayName: String) {
    JOULE("Joule"), KILOJOULE("Kilojoule"), MEGAJOULE("Megajoule"),
    CALORIE("Calorie"), KILOCALORIE("Kilocalorie"),
    WATT_HOUR("Watt hour"), KILOWATT_HOUR("Kilowatt hour"), BTU("BTU")
}

enum class PowerUnit(val displayName: String) {
    MILLIWATT("Milliwatt"), WATT("Watt"), KILOWATT("Kilowatt"),
    MEGAWATT("Megawatt"), HP_MECH("Horsepower (HP)"), HP_METRIC("Horsepower (PS)")
}

enum class AngleUnit(val displayName: String) {
    DEGREE("Degree"), RADIAN("Radian"), GRADIAN("Gradian")
}

enum class DataUnit(val displayName: String) {
    BIT("Bit"), BYTE("Byte"), KILOBYTE("Kilobyte"), MEGABYTE("Megabyte"),
    GIGABYTE("Gigabyte"), TERABYTE("Terabyte"), PETABYTE("Petabyte")
}
