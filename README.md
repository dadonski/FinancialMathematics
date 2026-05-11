This repo stores the project I've accomplished following the university course of "**Mathematical Finance**" held by Prof. Alessandro Gnoatto at the University of Verona, within the Master's Degree in Quantitative Finance.

You can find the original instructions in fiMaProjectWork20252026.pdf.

My group's work is then presented and commented in FiMa_Project_Work.pdf.

SUMMARY:
This project is an extension of finmath-lib (Christian Fries, [github.com/finmath/finmath-lib](url)), an open-source Java library for mathematical finance. finmath-lib provides the foundational abstractions used throughout this project.
The starting library already provided the three tree models (CRR, Boyle, Jarrow-Rudd) and European/American option pricing. 
This project extends it with the followings:
**Dividend class hierarchy**: Design and implementation of a DividendModel interface and a MultiplicativeDividendModel sub-interface, together with three concrete dividend specifications: ProportionalDividend, DiscreteProportionalDividends, and ContinuousDividendYield.
**Dividend-aware tree models**: Modification of all three tree models to accept a MultiplicativeDividendModel. Discrete proportional dividends are applied as a cumulative factor on node values (tree stays recombining); continuous dividend yield adjusts the risk-neutral drift and hence the transition probabilities.
**Barrier options**: Implementation of BarrierOption, covering all eight barrier types (down/up × in/out × call/put) plus an optional rebate. In-type barriers are priced via in-out parity (Down-In = European − Down-Out, Up-In = European − Up-Out), so only the out-option backward induction needs to be implemented directly.
