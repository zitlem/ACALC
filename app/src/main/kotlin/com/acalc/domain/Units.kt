package com.acalc.domain

enum class LengthUnit(val displayName: String) {
    MM("mm"), CM("cm"), M("m"), KM("km"),
    INCH("in"), FOOT("ft"), YARD("yd"), MILE("mi")
}

enum class WeightUnit(val displayName: String) {
    MG("mg"), G("g"), KG("kg"),
    OZ("oz"), LB("lb"), TON("t")
}

enum class VolumeUnit(val displayName: String) {
    ML("mL"), L("L"),
    TSP("tsp"), TBSP("tbsp"), CUP("cup"), FL_OZ("fl oz"), GALLON("gal")
}

enum class TempUnit(val displayName: String) {
    CELSIUS("°C"), FAHRENHEIT("°F"), KELVIN("K")
}

enum class AreaUnit(val displayName: String) {
    SQ_MM("mm²"), SQ_CM("cm²"), SQ_M("m²"), SQ_KM("km²"),
    SQ_IN("in²"), SQ_FT("ft²"), ACRE("ac")
}

enum class SpeedUnit(val displayName: String) {
    M_PER_S("m/s"), KM_PER_H("km/h"), MPH("mph"), KNOTS("kn")
}
