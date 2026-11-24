# Validation practice and its sources (R6, 19 Nov 2026)

What the GR4SP validation does, the practice each part follows, and where that practice comes from. Checked against
the sources on 19 Nov 2026 (bibliographic details by web search; Hansen et al. 2019 read in full). Items marked
"to check" were not read in full.

## What is reported (Angela, 19 Nov 2026)

| Element | Choice | Why | Source |
|---|---|---|---|
| Fit metric | Nash-Sutcliffe efficiency (NSE) | One number per indicator, comparable across units: 1 = perfect, 0 = no better than the record's own mean, below 0 = worse than that mean. Not centred on 0; no lower limit | Nash and Sutcliffe 1970 |
| Direction and size | Bias (mean of simulated minus observed) and RMSE, in the indicator's unit | NSE alone hides whether the model runs high or low, and how far | Bennett et al. 2013; Moriasi et al. 2007 (they pair NSE with percent bias) |
| Supplement only | MAE, NRMSE | Same information as RMSE, in other forms | - |
| Graphical comparison | Record, seed median and 5-95 % seed band per year, month or half-hour | Statistics alone do not show where and when a model departs from the record | Bennett et al. 2013 (visual analysis is one of their five steps) |
| Several patterns, several time scales | Annual indicators (price, renewable share, emissions, tariff), monthly price, monthly generation by fuel, the half-hourly price distribution | A model that reproduces several independent patterns at different scales is more credible than one tuned to one pattern | Grimm et al. 2005 (pattern-oriented modelling); Windrum et al. 2007 |
| Price distribution | Price duration curve (every half-hour ranked from the highest price to the lowest), by period | Tests how often prices are low, middling or spiking without needing a half-hour-by-half-hour match; the standard test in electricity-market agent-based models | Kell et al. 2019 (ElecSim); Nitsch et al. 2021 (AMIRIS back-test) |
| Stochastic runs | 100 seeds, median and 5-95 % range of every statistic | One seed is one possible history of breakdowns; seed 42 ranked 87th of 100 on annual price NSE | Kell et al. 2019 (Monte-Carlo runs) |
| Thresholds | Moriasi et al.'s NSE above 0.50 "satisfactory" is quoted only as a reference point | It was set for monthly streamflow in watershed models, not for electricity systems | Moriasi et al. 2007 |

Inputs that come from the record (half-hourly demand, the generator fleet and its dates, rooftop installations, monthly
hydro energy, gas and carbon prices) are not validation results. The figures and tables label hydro and rooftop PV as
inputs. The validation is a hindcast conditional on these inputs, not an out-of-sample forecast.

## Hansen, Liu and Morrison (2019), what it says about validation

*Agent-based modelling and socio-technical energy transitions: A systematic literature review*, Energy Research &
Social Science 49, 41-52, doi:10.1016/j.erss.2018.10.021. 62 articles reviewed. Copy:
`Sandbox/papers/GR4SPArticle_ERSS/2019 Agent-based modelling and socio-technical energy transitions A systematic literature review.pdf`.

- Table 4 (p. 49), disadvantages and challenges of agent-based modelling, headed "Challenges regarding validation,
  verification, calibration and model description": "Lack of relevant empirical data", "Difficulty of validation",
  "Lack of data for validation and calibration", "Behavioural rules are often ad hoc and not based on systematic
  theories of behaviour", "ABMs are often one-offs exploring specific cases, making insights difficult to generalise".
- p. 48: the usefulness of an agent-based model "rests on rigorous integration of theoretical and empirical
  foundations" (after Robinson and Rai), and "the appropriate integration of empirical data in model initialisation and
  validation would arguably become increasingly difficult with increasing model scale"; a trade-off between model
  robustness and scale.
- p. 48: inconsistent model descriptions; some studies use the ODD protocol, many do not, and its use varies.
- It does **not** count how many reviewed models were validated against historical data. So it supports "validation
  is a recurring difficulty for these models" (methods article draft l.48), but not "testing against the historical record
  ... remains rare" (ERSS revision queue, 29 Oct, Q15 and the options at l.622-623): that wording needs another source
  or a softer claim.

What GR4SP can claim against it: a large-scale model whose system-level outputs are tested against independent records
over two institutional regimes, several indicators and time scales, with the stochastic spread reported and the code
and data published, described with ODD. What it cannot claim: agent-level (behavioural) validation of the offer rules,
parameter uncertainty in the validation (the Morris/Sobol decision is pending), or an out-of-sample test.

## References

- Bennett, N.D., Croke, B.F.W., Guariso, G., et al. (2013). Characterising performance of environmental models.
  Environmental Modelling & Software 40, 1-20. doi:10.1016/j.envsoft.2012.09.011
- Grimm, V., Revilla, E., Berger, U., et al. (2005). Pattern-oriented modeling of agent-based complex systems: lessons
  from ecology. Science 310(5750), 987-991. doi:10.1126/science.1116681 (full text to check)
- Hansen, P., Liu, X., Morrison, G.M. (2019). Agent-based modelling and socio-technical energy transitions: a
  systematic literature review. Energy Research & Social Science 49, 41-52. doi:10.1016/j.erss.2018.10.021 (read)
- Kell, A.J.M., Forshaw, M., McGough, A.S. (2019). ElecSim: Monte-Carlo open-source agent-based model to inform policy
  for long-term electricity planning. Proceedings of the Tenth ACM International Conference on Future Energy Systems
  (e-Energy '19). doi:10.1145/3307772.3335321 (arXiv 1911.01203; compares price duration curves, UK 2018)
- Kell, A.J.M., Forshaw, M., McGough, A.S. (2020). Long-term electricity market agent based model validation using
  genetic algorithm based optimization. e-Energy '20. doi:10.1145/3396851.3397682 (arXiv 2005.10346)
- Moriasi, D.N., Arnold, J.G., Van Liew, M.W., Bingner, R.L., Harmel, R.D., Veith, T.L. (2007). Model evaluation
  guidelines for systematic quantification of accuracy in watershed simulations. Transactions of the ASABE 50(3),
  885-900. (NSE > 0.50 satisfactory for monthly streamflow; the finer grades to check in the full text)
- Nash, J.E., Sutcliffe, J.V. (1970). River flow forecasting through conceptual models part I: a discussion of
  principles. Journal of Hydrology 10(3), 282-290. (to check)
- Nitsch, F., Schimeczek, C., Wehrle, S. (2021). Back-testing the agent-based model AMIRIS for the Austrian day-ahead
  electricity market. Zenodo, doi:10.5281/zenodo.5726738 (DLR lists 10.5281/zenodo.5726737, the concept DOI; check).
  Finds AMIRIS overestimates low prices and underestimates high ones, and misses negative prices.
- Windrum, P., Fagiolo, G., Moneta, A. (2007). Empirical validation of agent-based models: alternatives and prospects.
  Journal of Artificial Societies and Social Simulation 10(2), 8. https://www.jasss.org/10/2/8.html
