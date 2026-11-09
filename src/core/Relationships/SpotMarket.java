package core.Relationships;

import core.Gr4spSim;
import core.Technical.Generator;

import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;

//This market uses ABM to simulate the price of electricity in the Spot market using a Merit Order approach.
//TODO: Economic curtailment: When there is a surplus of generation some dispatch periods and generators bid on with very low or negative prices to be able to sell. E.g. Brown coal gens beacuse it might e more expensive to revamp.
// this makes wind and solar needing to pay to the market too if these want to sell. Sometimes, these generators withdrawn or curtail production to avoid having to pay (negative spot prices).
// Economic curtailment example: Solar might bid at higher prices in the spot and then sell their production in other markets.
public class SpotMarket implements java.io.Serializable{
    private double marketPrice;
    private double emissionsIntensity;
    private double demand;
    private double unmetDemand;
    private String name;

    private ArrayList<Bid> bidders;
    private ArrayList<Bid> successfulBids;

    //An interconnector import block: it fills a shortfall after all regional offers (see Settings.interconnectorFile)
    public static class ImportOffer implements java.io.Serializable, Comparable<ImportOffer> {
        public final String link;
        public final double priceMWh;
        public final double mw;
        public ImportOffer(String link, double priceMWh, double mw) { this.link = link; this.priceMWh = priceMWh; this.mw = mw; }
        @Override
        public int compareTo(ImportOffer o) { return Double.compare(priceMWh, o.priceMWh); }
    }
    //Import blocks open this month, cheapest first. importRule is set on the market that receives them (the primary);
    //without it a shortfall keeps the old rule
    private ArrayList<ImportOffer> importOffers = new ArrayList<>();
    private boolean importRule = false;
    //MW imported in the last half-hour, and the link of the last block used ("" if none)
    private double importedMW;
    private String importSetter = "";

    //Diagnostic, off by default: -Dgr4sp.priceSetterLog=<csv> writes the bid that sets the price every half-hour,
    //for the years in -Dgr4sp.priceSetterLog.from / .to (inclusive)
    private static PrintWriter priceSetterLog;
    private static final String PRICE_SETTER_LOG = System.getProperty("gr4sp.priceSetterLog");
    private static final int PRICE_SETTER_FROM = Integer.getInteger("gr4sp.priceSetterLog.from", 0);
    private static final int PRICE_SETTER_TO = Integer.getInteger("gr4sp.priceSetterLog.to", 9999);

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

    public ArrayList<Bid> getBidders() { return bidders; }

    public double getEmissionsIntensity() { return emissionsIntensity; }

    public double getMarketPrice() {
        return marketPrice;
    }

    public double getUnmetDemand() {
        return unmetDemand;
    }

    public double getImportedMW() { return importedMW; }

    public void setImportOffers(java.util.List<ImportOffer> offers) {
        importOffers = new ArrayList<>(offers);
        Collections.sort(importOffers);
        importRule = true;
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
        this.importedMW = 0;
        this.importSetter = "";

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

        //Imports: fill the gap from the interconnector blocks, cheapest first; regional bids already dispatched are paid
        //at least their offer, so the price is the dearer of the last regional offer and the last block used
        if (importRule && Math.ceil(offered) < Math.floor(demand)) {
            for (ImportOffer io : importOffers) {
                double gap = demand - offered - importedMW;
                if (gap <= 0) break;
                double take = Math.min(gap, io.mw);
                if (take <= 0) continue;
                importedMW += take;
                importSetter = io.link;
                marketPrice = Math.max(marketPrice, io.priceMWh);
            }
            double shortfallPrice = data.getShortfallPriceMWh(currentTime);
            if (Math.ceil(offered + importedMW) < Math.floor(demand)) {
                unmetDemand = demand - offered - importedMW;
                if (shortfallPrice >= 0) { marketPrice = shortfallPrice; importSetter = "unserved"; }
                else if (currentYear < data.settings.getBaseYearConsumptionForecast())
                    marketPrice *= 1.0 + data.settings.getImportPriceFactor();
                else
                    marketPrice *= 1.0 + data.settingsAfterBaseYear.getImportPriceFactor();
            }
        }
        else if(Math.ceil(offered) < Math.floor(demand) ) {
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

        if (PRICE_SETTER_LOG != null && currentYear >= PRICE_SETTER_FROM && currentYear <= PRICE_SETTER_TO)
            logPriceSetter(currentTime, data.hasInterconnectors() || data.settings.getShortfallPriceFile() != null);

        /**
         * Communicate successful Bid to each Generator to update future price
         */
        for ( Bid b : successfulBids ) {
            Generator g = (Generator) b.asset;
            double mwhGenerated = b.capacity / 2.0; //30min

            g.setMonthlyGeneratedMWh( g.getMonthlyGeneratedMWh() + mwhGenerated );
            g.addEnergyToday(mwhGenerated);
            g.setHistoricGeneratedMWh( g.getHistoricGeneratedMWh() + mwhGenerated );
            g.setHistoricRevenue( g.getHistoricRevenue() + (mwhGenerated * b.dollarMWh) );
        }
    }

    private synchronized void logPriceSetter(Date currentTime, boolean importColumns) {
        try {
            if (priceSetterLog == null) {
                priceSetterLog = new PrintWriter(new FileWriter(PRICE_SETTER_LOG));
                priceSetterLog.println("time,market,demand,unmet,price,fuel,tech,dispatch,name,offer,historicCF,basePrice,minCF,maxCF"
                        + (importColumns ? ",importMW,importSetter" : ""));
                Runtime.getRuntime().addShutdownHook(new Thread(() -> priceSetterLog.close()));
            }
            String fuel = "", tech = "", dispatch = "", gname = "";
            double offer = 0, hcf = 0, base = 0, minCF = 0, maxCF = 0;
            if (successfulBids.size() > 0) {
                Bid last = successfulBids.get(successfulBids.size() - 1);
                Generator g = (Generator) last.asset;
                fuel = g.getFuelSourceDescriptor(); tech = g.getTechTypeDescriptor();
                dispatch = g.getDispatchTypeDescriptor(); gname = g.getName().replace(",", " ");
                offer = last.dollarMWh; hcf = g.getHistoricCapacityFactor(); base = g.getBasePriceMWh();
                minCF = g.minCapacityFactor; maxCF = g.maxCapacityFactor;
            }
            priceSetterLog.println(new SimpleDateFormat("yyyy-MM-dd HH:mm").format(currentTime) + "," + name + ","
                    + demand + "," + unmetDemand + "," + marketPrice + "," + fuel + "," + tech + "," + dispatch + ","
                    + gname + "," + offer + "," + hcf + "," + base + "," + minCF + "," + maxCF
                    + (importColumns ? "," + importedMW + "," + importSetter : ""));
        } catch (IOException e) {
            System.err.println("Cannot write price setter log: " + e);
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
