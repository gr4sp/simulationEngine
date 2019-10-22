package core.Relationships;

import core.Technical.Generator;

import java.util.ArrayList;
import java.util.Collections;

public class MeritOrder {
    private double marketPrice;
    private double emissionsIntensity;

    private ArrayList<Bid> bidders;
    private ArrayList<Bid> successfulBids;

    public MeritOrder() {
        this.marketPrice = 0;
        this.bidders = new ArrayList<Bid>();
        this.successfulBids= new ArrayList<Bid>();
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

            if( b.dollarMWh != initial_price ) break;

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
        if(offered + totalCapacitySamePrice > demand) return true;

        return false;
    }


    public void computeMarketPrice(double demand){
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
            marketPrice *= 1.2;
            System.out.println("Unmet Demand (imported) " +(demand - offered) );
        }

        /**
         * Communicate successful Bid to each Generator to update future price
         */
        for ( Bid b : successfulBids ) {
            Generator g = (Generator) b.asset;
            double mwhGenerated = b.capacity / 2.0; //30min
            g.setHistoricGeneratedMW( g.getHistoricGeneratedMW() + mwhGenerated );
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