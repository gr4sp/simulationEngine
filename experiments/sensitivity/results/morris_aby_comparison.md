# Morris pilot (aby) on the repaired model against the thesis screening

Normalised median mu* from 2019; thesis: simulationData/eet_aby.csv (thesis ranks by mu*). Agreement rule: Spearman rho >= 0.8 and >= 4 of the top 5 shared.

| Output | rho | params compared | top 5 shared | agrees | top 5, thesis | top 5, repaired |
|---|---|---|---|---|---|---|
| tariffsYear | 0.05 | 13 | 1 | no | wholesaleTariffContribution, nameplateCapacityChangeBrownCoal, annualInflation, importPriceFactor, generationRolloutPeriod | nameplateCapacityChangeBrownCoal, nameplateCapacityChangeWind, technologicalImprovement, nameplateCapacityChangeOcgt, semiScheduleGenSpotMarket |
| primaryWholesalePriceYear | 0.07 | 16 | 2 | no | nameplateCapacityChangeBrownCoal, annualInflation, generationRolloutPeriod, nameplateCapacityChangeWind, importPriceFactor | nameplateCapacityChangeBrownCoal, nameplateCapacityChangeWind, nameplateCapacityChangeOcgt, technologicalImprovement, generatorRetirement |
| GHGYear | 0.64 | 8 | 4 | no | domesticConsumptionPercentage, nameplateCapacityChangeBrownCoal, generationRolloutPeriod, semiScheduleGenSpotMarket, nameplateCapacityChangeWind | domesticConsumptionPercentage, nameplateCapacityChangeWind, nameplateCapacityChangeBrownCoal, semiScheduleGenSpotMarket, consumption |
| renewableContributionYear | 0.52 | 11 | 3 | no | nameplateCapacityChangeBrownCoal, generationRolloutPeriod, semiScheduleGenSpotMarket, nameplateCapacityChangeWind, includePublicallyAnnouncedGen | nameplateCapacityChangeWind, semiScheduleGenSpotMarket, nameplateCapacityChangeBrownCoal, technologicalImprovement, nameplateCapacityChangeSolar |

Parameters with no effect in the repaired model (normalised mu* < 0.01 in every output):

annualInflation, learningCurve, nameplateCapacityChangeBattery, priceChangePercentageBattery, priceChangePercentageBrownCoal, priceChangePercentageCcgt, priceChangePercentageOcgt, priceChangePercentageSolar, priceChangePercentageWater, priceChangePercentageWind, wholesaleTariffContribution
