package core.Relationships;

import java.util.ArrayList;
import java.util.Collections;

public class MeritOrder {
    private float marketPrice;

    private ArrayList<Bid> bidders;

    public MeritOrder() {
        this.marketPrice = 0;
        this.bidders = new ArrayList<Bid>();
    }

    public void computeMarketPrice(float demand){
        //Reset MarketPrice
        marketPrice=0;

        //Sort Bidders by price
        Collections.sort(bidders);

        float offered = 0;

        //Traverse bidders
        for(Bid b : bidders ){
            offered += b.capacity;

            //Stop when demand is met, record market price
            if(offered >= demand ) {
                marketPrice = b.priceMWh;
                break;
            }
        }

    }
}