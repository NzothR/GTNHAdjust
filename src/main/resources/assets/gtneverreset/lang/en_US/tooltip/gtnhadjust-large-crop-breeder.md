Insert an analyzed seed to archive its species with 31/31/31 stats, consuming the seed
Select a target to breed it from archived parents. Successful breeding outputs 31/31/31 seeds
Existing archive entries retain their stats; newly bred species are archived at 31/31/31
Place required substrates in an {white:input bus}; they are {green:not consumed}. Vanilla substrates need no item

Breeding grades 1–5 require ULV / LV / MV / HV / EV; check the GUI for each crop
Grade 1 takes {white:{var:firstSeconds} seconds}; each grade doubles the time. Higher voltage applies lossy overclocks
Starts with 1 parallel at ULV; every {gold:two voltage tiers} doubles the parallel limit
Each parallel starts at {white:8 EU/t} and consumes {white:{var:fertilizer} L enriched fertilizer} and catalysts per attempt
Default chances for grades 1–5 are 100% / 80% / 60% / 40% / 20%; each parallel rolls independently
Failed attempts still consume fertilizer and catalysts; substrates are retained

Seed improvement uses {white:{var:baseEu} EU/t}
Every {white:{var:washSeconds} seconds}, it raises one stat below 31 by one point
