package core.Policies;

public class AccelerateSolarPV implements java.io.Serializable{

    private double monthlyHousholdsPercentageConversion;

    public AccelerateSolarPV(double monthlyHousholdsPercentageConversion) {
        this.monthlyHousholdsPercentageConversion = monthlyHousholdsPercentageConversion;
    }

    public double getMonthlyHousholdsPercentageConversion() {
        return monthlyHousholdsPercentageConversion;
    }

    public void setMonthlyHousholdsPercentageConversion(double monthlyHousholdsPercentageConversion) {
        this.monthlyHousholdsPercentageConversion = monthlyHousholdsPercentageConversion;
    }

}
