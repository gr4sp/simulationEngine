package core.Technical;

import java.util.Calendar;
import java.util.Date;

/**
 * Irradiance on a fixed, tilted rooftop array from the half-hourly global horizontal irradiance (GHI) of the register
 * (N23, 14 Nov 2026). The Clean Energy Council's yield formula, whose de-rating factors the model uses (solarEfficiency),
 * multiplies them by the irradiation on the tilted array, H_tilt (CEC, Grid-connected solar PV systems, design guidelines,
 * Issue 6.0, Dec 2012, section 8.1); the model multiplied them by GHI. Australian rooftop arrays face the equator at a
 * mean tilt of 16.1 degrees (Killinger, Lingfors and Saint-Drenan 2018, Solar Energy 173, 1087-1106), which receives
 * about 8.6 % more energy a year than a horizontal surface in Melbourne (2007-2019).
 *
 * Method: solar position (Spencer 1971 declination and equation of time) at the half-hour's time stamp, which the
 * register gives at the interval midpoint (the mean daily profile is centred on solar noon); diffuse fraction from the
 * clearness index (Erbs, Klein and Duffie 1982); transposition to the array plane (Hay and Davies 1980) with ground
 * reflectance 0.2. Times are the register's wall time, Australian Eastern Standard Time (UTC+10).
 */
public final class RooftopIrradiance implements java.io.Serializable {
    private static final double SOLAR_CONSTANT_KW = 1.367;
    private static final double MIN_COS_ZENITH = 0.065;   // below about 86 degrees, the beam is not resolved

    private final double lat, lonDeg, tilt, azimuth, albedo, tzHours;

    /** Angles in degrees; azimuth of the array measured from north, clockwise (0 = facing north). */
    public RooftopIrradiance(double latDeg, double lonDeg, double tiltDeg, double azimuthDeg, double albedo, double tzHours) {
        this.lat = Math.toRadians(latDeg);
        this.lonDeg = lonDeg;
        this.tilt = Math.toRadians(tiltDeg);
        this.azimuth = Math.toRadians(azimuthDeg);
        this.albedo = albedo;
        this.tzHours = tzHours;
    }

    /** Mean irradiance on the array (kW/m2) in the half-hour stamped t, given its GHI (kW/m2). */
    public double planeOfArray(double ghi, Date t) {
        if (ghi <= 0) return 0.0;
        Calendar c = Calendar.getInstance();
        c.setTime(t);
        int doy = c.get(Calendar.DAY_OF_YEAR);
        double hour = c.get(Calendar.HOUR_OF_DAY) + c.get(Calendar.MINUTE) / 60.0;
        double g = 2 * Math.PI * (doy - 1) / 365.0;
        double dec = 0.006918 - 0.399912 * Math.cos(g) + 0.070257 * Math.sin(g) - 0.006758 * Math.cos(2 * g)
                + 0.000907 * Math.sin(2 * g) - 0.002697 * Math.cos(3 * g) + 0.00148 * Math.sin(3 * g);
        double eot = 229.18 * (0.000075 + 0.001868 * Math.cos(g) - 0.032077 * Math.sin(g)
                - 0.014615 * Math.cos(2 * g) - 0.040849 * Math.sin(2 * g));          // minutes
        double trueSolarMin = hour * 60 + eot + 4 * lonDeg - 60 * tzHours;
        double ha = Math.toRadians(trueSolarMin / 4 - 180);
        double cosz = Math.sin(lat) * Math.sin(dec) + Math.cos(lat) * Math.cos(dec) * Math.cos(ha);
        double cosTilt = Math.cos(tilt);
        if (cosz <= MIN_COS_ZENITH) return ghi * (1 + cosTilt) / 2;               // sun near the horizon: diffuse only
        double i0 = SOLAR_CONSTANT_KW * (1 + 0.033 * Math.cos(2 * Math.PI * doy / 365.0));
        double kt = Math.min(1.0, ghi / (i0 * cosz));
        double fd = kt <= 0.22 ? 1 - 0.09 * kt
                : kt <= 0.8 ? 0.9511 - 0.1604 * kt + 4.388 * kt * kt - 16.638 * Math.pow(kt, 3) + 12.336 * Math.pow(kt, 4)
                : 0.165;
        double dhi = ghi * fd;
        double dni = (ghi - dhi) / cosz;
        double sinz = Math.sqrt(Math.max(0.0, 1 - cosz * cosz));
        double cosAz = (Math.sin(dec) - Math.sin(lat) * cosz) / (Math.cos(lat) * Math.max(sinz, 1e-6));
        double sunAz = Math.acos(Math.max(-1.0, Math.min(1.0, cosAz)));            // from north
        if (ha > 0) sunAz = 2 * Math.PI - sunAz;                                    // afternoon: west of north
        double cosInc = Math.max(0.0, cosz * cosTilt + sinz * Math.sin(tilt) * Math.cos(sunAz - azimuth));
        double ai = dni / i0;
        return dni * cosInc + dhi * ((1 - ai) * (1 + cosTilt) / 2 + ai * cosInc / cosz) + ghi * albedo * (1 - cosTilt) / 2;
    }
}
