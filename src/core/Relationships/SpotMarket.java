package core.Relationships;

import core.Gr4spSim;
import core.Technical.Generator;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;

//This market uses ABM to simulate the price of electricity in the Spot market using a Merit Order approach.

public class SpotMarket implements java.io.Serializable{
    private double marketPrice;
    private double emissionsIntensity;
    private double demand;
    private double unmetDemand;
    private String name;

    private ArrayList<Bid> bidders;
    private ArrayList<Bid> successfulBids;

    public SpotMarket(String name) {
        this.marketPrice = 0;
        this.unmetDemand = 0;
        this.demand = 0;
        this.bidders = new ArrayList<Bid>();
        this.successfulBids= new ArrayList<Bid>();
        this.name = name;
    }

    public String getName() { return name; }

    public double getDemand() {
        return demand;
    }

    public void setDemand(double demand) {
        this.demand = demand;
    }

    public void setMarketPrice(double marketPrice) {
        this.marketPrice = marketPrice;
    }

    public void setEmissionsIntensity(double emissionsIntensity) {
        this.emissionsIntensity = emissionsIntensity;
    }

    public ArrayList<Bid> getSuccessfulBids() { return successfulBids; }

    public double getEmissionsIntensity() { return emissionsIntensity; }

    public double getMarketPrice() {
        return marketPrice;
    }

    public double getUnmetDemand() {
        return unmetDemand;
    }

    public void clearBidders(){
        bidders.clear();
        successfulBids.clear();
    }

    public void addBidder(Bid b){
        bidders.add(b);
    }

    private double getCapacitySamePrice( int init_idx ){
        double total_capacity = 0;
        double initial_price = bidders.get(init_idx).dollarMWh;

        //Sum total capacity of bids with the same price
        for (int idx = init_idx; idx < bidders.size(); idx++) {
            Bid b = bidders.get(idx);

            if( b.dollarMWh != initial_price && b.capacity!= 0) break;

            total_capacity += b.capacity;

        }
        return total_capacity;
    }

    private ArrayList<Bid> getBidsSamePrice( int init_idx ){
        ArrayList<Bid> bids = new ArrayList<Bid>();
        double initial_price = bidders.get(init_idx).dollarMWh;

        //Sum total capacity of bids with the same price
        for (int idx = init_idx; idx < bidders.size(); idx++) {
            Bid b = bidders.get(idx);
            if( b.dollarMWh != initial_price ) break;
            bids.add(b);

        }
        return bids;
    }

    private boolean isResidual(int init_idx, double offered, double demand) {

        double totalCapacitySamePrice = getCapacitySamePrice( init_idx );

        //if total capacity offered at price X is greater than (demand-offered_cheaper), then this it is residual
        return offered + totalCapacitySamePrice > demand;
    }


    public void computeMarketPrice(double demand, Gr4spSim state, Date currentTime, int currentYear) {
        Gr4spSim data = state;

        this.demand = demand;
        this.unmetDemand = 0;

        //Reset MarketPrice
        marketPrice=0;

        //Sort Bidders by price
        Collections.sort(bidders);

        float offered = 0;

        //Traverse bidders
        for(int idx = 0; idx < bidders.size(); idx++ ){
            Bid b = bidders.get(idx);

            if( isResidual(idx, offered, demand) ){

                double totalCapacitySamePrice = getCapacitySamePrice( idx );
                ArrayList<Bid> bidsSamePrice = getBidsSamePrice(idx);

                //Residual / total capacity of bids with the same price
                double percentageResidualBid = (demand - offered) / totalCapacitySamePrice;

                //Include all bids with the price, but share the residual capacity among them
                for ( Bid bsp : bidsSamePrice ) {
                    //get only the portion needed to meet demand at that price
                    bsp.capacity = bsp.capacity * percentageResidualBid;
                    offered += bsp.capacity;
                    marketPrice = bsp.dollarMWh;
                    successfulBids.add(bsp);
                }

                //Stop when demand is met, record market price
                break;

            }else {
                //Include bid fully
                offered += b.capacity;
                marketPrice = b.dollarMWh;
                successfulBids.add(b);
            }

        }

        if(Math.ceil(offered) < Math.floor(demand) ) {
            /* The median contribution from imports in the historic wholesale has been 29%
            * The import price varies significantly over the historic period analysed in OpenNem.
            * In the first years of analysis a pattern of more expensive import price in summer is evident.
            * However this pattern blurs with the years, therefore it is decided to assign the median of 29% for the BAU */
            if( currentYear < data.settings.getBaseYearConsumptionForecast() )
                marketPrice *= 1.0 + data.settings.getImportPriceFactor();
            else
                marketPrice *= 1.0 + data.settingsAfterBaseYear.getImportPriceFactor();
            unmetDemand = demand - offered;
            if( successfulBids.size() > 0 ) {
                Generator lastgen = (Generator) successfulBids.get(successfulBids.size() - 1).asset;
                Bid lastBid = successfulBids.get(successfulBids.size() - 1);
                data.LOGGER.warning(currentTime + " - " + this.name + " The highest bidder: " + lastgen.getFuelSourceDescriptor() + "-"
                        + lastgen.getTechTypeDescriptor() + " - " + lastgen.getName() +
                        " - " + lastgen.getOwnerName() + " at " + lastBid.dollarMWh + " $/MWh" +
                        " at CF " + lastgen.getHistoricCapacityFactor() + " Base Price: " + lastgen.basePriceMWh +
                        "$/MWh (divided by HistCF is "+lastgen.basePriceMWh / lastgen.getHistoricCapacityFactor() + ")"
                );
            }
        }

        /**
         * Communicate successful Bid to each Generator to update future price
         */
        for ( Bid b : successfulBids ) {
            Generator g = (Generator) b.asset;
            double mwhGenerated = b.capacity / 2.0; //30min

            g.setMonthlyGeneratedMWh( g.getMonthlyGeneratedMWh() + mwhGenerated );
            g.setHistoricGeneratedMWh( g.getHistoricGeneratedMWh() + mwhGenerated );
            g.setHistoricRevenue( g.getHistoricRevenue() + (mwhGenerated * b.dollarMWh) );
        }
    }

    void computeGenEmissionIntensity(int currentYear){

        double sumCapacity = 0.0;
        emissionsIntensity = 0.0;

        for ( Bid b : successfulBids ) {

            Generator g = (Generator) b.asset;

            emissionsIntensity += b.capacity * g.getEmissionsFactor(currentYear);

            sumCapacity += b.capacity;
        }

        emissionsIntensity /= sumCapacity;

    }
}
