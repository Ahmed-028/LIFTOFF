# LIFTOFF — Data Sources

Every number in the catalogs is one of three kinds:

- **PUBLISHED** — copied from the named source.
- **DERIVED** — computed from published numbers; the arithmetic is shown here.
- **GAME_ESTIMATE** — ours, not NASA's; shown with "≈" in the game.

All sources below were opened and read on 2026-11-13.

## Sources

| Key | Source |
|---|---|
| HB | NASA, *Earth Science Reference Handbook* (2006), NP-2006-5-768-GSFC. https://assets.science.nasa.gov/content/dam/science/missions/a-train/publications/2006ReferenceHandbook.pdf — page numbers are PDF pages |
| NSN | NASA, *Near Space Network Complexes*. https://www.nasa.gov/technology/space-comms/near-space-network-complexes/ |
| LSP | NASA, *Launch Services Program Rockets*. https://www.nasa.gov/launch-services-program-rockets |
| ULA | United Launch Alliance, *Atlas V* performance table. https://www.ulalaunch.com/rockets/atlas-v |
| RL | Rocket Lab, *Electron*. https://rocketlabcorp.com/launch/electron/ |
| L9 | NASA release, *NASA Awards Launch Services Contract for Landsat 9 Mission* (2017). https://www.nasa.gov/press-release/nasa-awards-launch-services-contract-for-landsat-9-mission |
| ICON | NASA release, launch services contract for the Ionospheric Connection Explorer. https://www.nasa.gov/news-release/nasa-awards-launch-services-contract-for-ionospheric-connection-explorer/ |
| PACE | NASA release, launch services contract for PACE. https://www.nasa.gov/news-release/nasa-awards-launch-services-contract-for-earth-science-mission/ |

## Instruments (mass, power, data rate: PUBLISHED from HB unless noted)

| Id | Flew on | Mass kg | Power W | Avg data kbps | HB page | Notes |
|---|---|---|---|---|---|---|
| ali | EO-1 | 90 | 100 | 67.8 DERIVED | 143–144 | Raw rate is 55 Mbps. EO-1 took about 120 scenes a week; one 42 km scene at 6.76 km/s ground speed takes 6.2 s. 120 × 6.2 s × 55 Mbps ÷ 7 days = 5.86 Gbit/day = 67.8 kbps average |
| etm-plus | Landsat 7 | 425 | 244.3 DERIVED | 25 050 DERIVED | 189 | 590 W imaging, 175 W standby, 16.7 % imaging duty: 0.167 × 590 + 0.833 × 175 = 244.3 W; 150 Mbps × 0.167 = 25.05 Mbps |
| aster | Terra | 421 | 463 | 8 300 | 241 | Handbook averages (8 % duty already included) |
| modis | Terra, Aqua | 229 | 162.5 | 6 200 | 98 | |
| tmi | TRMM | 54.1 | 66.5 | 8.5 | 260 | |
| virs | TRMM | 48.6 | 51.2 | 49.8 | 261 | Daytime rate used (night is 28.8 kbps), so this is the upper value |
| lis | TRMM | 20 | 33 | 6 | 258 | |
| amsr-e | Aqua | 314 | 350 | 87.4 | 94 | |
| glas | ICESat | 300 | 330 | 450 | 177 | |
| ceres | TRMM, Terra, Aqua | 50 | 47 | 10 | 96 | One scanner |

Consistency check: Aqua's instruments in HB (MODIS 6.2 Mbps, AIRS 1.27 Mbps,
AMSR-E, AMSU-A, HSB, two CERES scanners) sum to about 7.6 Mbps average, which
is 82 GB/day; HB p. 86 states Aqua's average as 89 GB/day. The per-instrument
rates therefore add up to the right daily volume.

## Spacecraft buses

HB publishes whole-spacecraft mass. The bus alone is DERIVED by subtracting the
instruments it flew; the payload the bus "can carry" is the payload it is known
to have carried.

| Id | Based on | Bus mass kg | Payload mass kg | Payload power W | Downlink Mbps | Design life | HB page |
|---|---|---|---|---|---|---|---|
| sorce-class | SORCE | 248.4 = 315 − 66.6 | 66.6 = SIM 30.3 + SOLSTICE 21.1 + TIM 10.7 + XPS 4.5 | 61.3 = 25.3 + 13 + 14 + 9 | 1.5 | 5 years | 232–235 |
| eo1-class | EO-1 | 428.5 = 529 − 100.5 | 100.5 = ALI 90 + LAC 10.5 | 115 = 100 + 15 | 105 (X-band) | 18 months | 142–144 |
| landsat7-class | Landsat 7 | 1557 = 1982 − 425 | 425 (ETM+) | 590 (ETM+ imaging) | 150 (one of three X-band links) | 5 years | 188–190 |

Known limits of this method: Hyperion's mass is not in HB, so it is counted
inside the EO-1 bus figure; SORCE's SOLSTICE is counted once. Both make the
payload capacities conservative.

## Orbits (PUBLISHED, HB)

| Id | Based on | Altitude km | Inclination | HB page |
|---|---|---|---|---|
| tropical-402 | TRMM | 402 | 35° | 256 |
| sso-705 | Aqua, Landsat 7 | 705 | 98.2° | 86, 188 |
| polar-600 | ICESat | 600 | 94° | 176 |

## Ground stations

Names and bands are PUBLISHED (NSN). Latitude and longitude are the geographic
location of the named site, to about 0.1°.

Svalbard (78.2° N), Fairbanks – Alaska Satellite Facility (64.9° N), Wallops
(37.9° N), McMurdo (77.8° S), Singapore (1.3° N), South Point Hawaii (19.0° N).

## Rockets

| Id | Capacity kg | Capacity source | Cost $M | Cost source |
|---|---|---|---|---|
| electron | 300 low-inclination LEO; 200 sun-synchronous | RL; SSO figure from Rocket Lab as quoted by Wikipedia | 7.5 | Rocket Lab's publicly stated price, as reported in the press; not a NASA contract |
| pegasus-xl | 453.59 LEO | LSP ("up to 1,000 pounds") | 56.3 | ICON (total cost to NASA to launch ICON) |
| falcon-9 | 22 800 LEO at 28.5°, expendable | SpaceX figure as quoted by Wikipedia | 80.4 | PACE (total cost to NASA to launch PACE) |
| atlas-v-401 | 9 800 LEO at 28.7°; 8 080 at 90° | ULA | 153.8 | L9 (total cost to NASA to launch Landsat 9) |

Simplifications: orbits inclined up to 40° use the rocket's reference-LEO
figure; near-polar and sun-synchronous orbits use the 90° figure. The game uses a single reference capacity for each class. The additional
polar and transfer reference points below describe specific trajectories.

## Game estimates (not NASA data)

| Value | Rule |
|---|---|
| Instrument cost | $0.5M per kg |
| Bus cost | $0.15M per kg |
| Ground station cost | $0.15M per station per month |
| Build time | Bus 24 / 30 / 42 months by size; instrument 18 months up to 60 kg, 30 up to 250 kg, 36 above; plus 6 months of integration and 2 per instrument |
| Objective budget caps, deadlines, minimum science duration | Scenario settings |
| "TIGHT" warning at 85 % | Teaching device for margins |
| 5° minimum elevation for ground contact | Common planning assumption |

## Mission settings

The original Earth briefings retain their published measurement references and
scenario caps. The ladder runs Farm Watch, Moon Landing and Mars Sample Drones.
Storm Chasers and Ice Patrol are optional Earth missions.

| Mission | Scenario settings (GAME_ESTIMATE) | Onboard delta-v | Briefing source |
| --- | --- | --- | --- |
| farm-watch | 450 $M, 78 months to launch, 24 minimum legacy science months. One step per selected science month. Robotic. | 0 m/s (GAME_ESTIMATE). Game assumes direct launcher insertion into the selected Earth science orbit. It budgets zero post-separation maneuver delta-v and omits orbit maintenance. [NASA Basics of Space Flight, Chapter 14](https://science.nasa.gov/learn/basics-of-space-flight/chapter14-1/) | [NASA Landsat Science, Operational Land Imager](https://landsat.gsfc.nasa.gov/satellites/landsat-8/spacecraft-instruments/operational-land-imager/) |
| moon-landing | 6000 $M, 108 months to launch, 1 minimum legacy science months. 8 day steps, 2 crew | 6035 m/s (DERIVED). Table 3, p. 24. Derived spacecraft burns: 60 outbound correction + 915 lunar insertion + 2165 descent + 1920 ascent + 915 Earth return injection + 60 return correction. Launcher provides translunar injection. Atmospheric return to Earth omits circularization and station rendezvous. [Lunar Operations Delta V Budget](https://ntrs.nasa.gov/api/citations/19850008679/downloads/19850008679.pdf) | [NASA Orion Reference Guide](https://www.nasa.gov/reference/orion-spacecraft/) |
| mars-drones | 950 $M, 96 months to launch, 6 minimum legacy science months. 6 month steps, 0 crew | 100 m/s (GAME_ESTIMATE). Powered descent minimum: 80 - 0.75 = 79.25 m/s, rounded up to 80. Add a 20 m/s game allowance for cruise corrections based on Spirit's 14.3 m/s first correction (JPL, June 20, 2003). Omits gravity losses, divert and flyaway. Launcher provides transfer injection. [NASA Mars Helicopter Attached to Perseverance](https://www.nasa.gov/missions/mars-2020-perseverance/mars-helicopter-attached-to-nasas-perseverance-rover/) | [JPL Mars 2020 Launch Press Kit](https://www.jpl.nasa.gov/news/press_kits/mars_2020/launch/quick_facts/) |
| storm-chasers | 200 $M, 60 months to launch, 12 minimum legacy science months. One step per selected science month. Robotic. | 0 m/s (GAME_ESTIMATE). Game assumes direct launcher insertion into the selected Earth science orbit. It budgets zero post-separation maneuver delta-v and omits orbit maintenance. [NASA Basics of Space Flight, Chapter 14](https://science.nasa.gov/learn/basics-of-space-flight/chapter14-1/) | [NASA Earth Science Reference Handbook (2006)](https://assets.science.nasa.gov/content/dam/science/missions/a-train/publications/2006ReferenceHandbook.pdf) |
| ice-patrol | 600 $M, 108 months to launch, 36 minimum legacy science months. One step per selected science month. Robotic. | 0 m/s (GAME_ESTIMATE). Game assumes direct launcher insertion into the selected Earth science orbit. It budgets zero post-separation maneuver delta-v and omits orbit maintenance. [NASA Basics of Space Flight, Chapter 14](https://science.nasa.gov/learn/basics-of-space-flight/chapter14-1/) | [NASA Earth Science Reference Handbook (2006)](https://assets.science.nasa.gov/content/dam/science/missions/a-train/publications/2006ReferenceHandbook.pdf) |

Moon Landing carries **4.99 kg per person per day (DERIVED)**. Figure 1 adds
0.84 kg oxygen, 0.62 kg dry food, 1.15 kg water in food, 0.76 kg preparation
water and 1.62 kg drinking water. The game omits hygiene water and recycling.
[NASA/TM-2004-212940](https://ntrs.nasa.gov/api/citations/20040035652/downloads/20040035652.pdf)

The Mars cruise allowance references Spirit's first correction of 14.3 m/s.
[JPL Spirit Mission Status, June 20, 2003](https://www.jpl.nasa.gov/news/mars-rover-spirit-mission-status-2/)

## Ship parts

All part costs use the existing bus estimate of $0.15M/kg. The slot contract
uses zero for inapplicable figures. A zero tank capacity on the Earth buses
represents the direct-insertion scenario. The fit model uses one length and
one width per part. CubeSat width is the diagonal of its cross section.
Several related parts share one mesh from the Member 4 hangar branch.

| Id | Catalog figures | Basis | Source and derivation |
| --- | --- | --- | --- |
| sorce-class | 248.4 kg mass, 66.6 kg payload | PUBLISHED / DERIVED | Key SORCE Facts and instrument facts, PDF p. 232-235. Bus mass = 315 kg spacecraft minus 66.6 kg of instruments flown. [NASA Earth Science Reference Handbook (2006)](https://assets.science.nasa.gov/content/dam/science/missions/a-train/publications/2006ReferenceHandbook.pdf) |
| eo1-class | 428.5 kg mass, 100.5 kg payload | PUBLISHED / DERIVED | Key EO-1 Facts and instrument facts, PDF p. 142-144. Bus mass = 529 kg spacecraft minus 100.5 kg of instruments (ALI and LAC). [NASA Earth Science Reference Handbook (2006)](https://assets.science.nasa.gov/content/dam/science/missions/a-train/publications/2006ReferenceHandbook.pdf) |
| landsat7-class | 1557 kg mass, 425 kg payload | PUBLISHED / DERIVED | Key Landsat 7 Facts and Key ETM+ Facts, PDF p. 188-190. Bus mass = 1982 kg spacecraft minus the 425 kg ETM+. Downlink is one of its three 150 Mbps links. [NASA Earth Science Reference Handbook (2006)](https://assets.science.nasa.gov/content/dam/science/missions/a-train/publications/2006ReferenceHandbook.pdf) |
| shuttle-orbiter | 74844 kg mass, 29484 kg payload, 10830 kg propellant | PUBLISHED | Manned Spacecraft Dimensions table: empty mass and maximum cargo. Usable OMS propellant 10,830 kg from NASA NTRS 19750024086, p. 36. [NASA Spacecraft Dimensions and Flight Summary Information](https://ntrs.nasa.gov/api/citations/19940023701/downloads/19940023701.pdf) |
| shuttle-bay | 0 kg mass, 29484 kg payload, 18.29 m bay length, 4.57 m bay width | PUBLISHED / GAME_ESTIMATE for included bay mass | Manned Spacecraft Dimensions table: payload bay and maximum cargo. Bay mass is included in the airframe model, so the game adds no separate mass. [NASA Spacecraft Dimensions and Flight Summary Information](https://ntrs.nasa.gov/api/citations/19940023701/downloads/19940023701.pdf) |
| cubesat-1u | 2 kg mass, 0.1135 m length, 0.141421 m width | PUBLISHED / DERIVED | Table 1 maximum mass; Appendix B nominal rail envelope. Circular fit width is the diagonal of the 0.1 m by 0.1 m cross section. [Cal Poly CubeSat Design Specification Rev 14.1](https://static1.squarespace.com/static/5418c831e4b0fa4ecac1bacd/t/62193b7fc9e72e0053f00910/1645820809779/CDS%2BREV14_1%2B2022-02-09.pdf) |
| cubesat-3u | 6 kg mass, 0.3405 m length, 0.141421 m width | PUBLISHED / DERIVED | Table 1 maximum mass; Appendix B nominal rail envelope. Circular fit width is the diagonal of the 0.1 m by 0.1 m cross section. [Cal Poly CubeSat Design Specification Rev 14.1](https://static1.squarespace.com/static/5418c831e4b0fa4ecac1bacd/t/62193b7fc9e72e0053f00910/1645820809779/CDS%2BREV14_1%2B2022-02-09.pdf) |
| bay-3u | 2 kg mass, 6 kg payload, 0.3405 m bay length, 0.141421 m bay width | PUBLISHED / DERIVED fit | Properties table, 3U ISIPOD: 2 kg published deployer mass, 6 kg maximum payload. The game uses the nominal 3U CubeSat rail envelope as the fit limit (Cal Poly CDS Rev 14.1, Appendix B). [ISISPACE CubeSat Deployers brochure](https://www.isispace.nl/wp-content/uploads/2016/02/CubeSat-deployers-Brochure-web-compressed.pdf) |
| cubesat-6u | 12 kg mass, 0.366 m length, 0.24741 m width | PUBLISHED / DERIVED | Table 1 maximum mass; Appendix B nominal rail envelope. Circular fit width is the diagonal of the 0.2263 m by 0.1 m cross section. [Cal Poly CubeSat Design Specification Rev 14.1](https://static1.squarespace.com/static/5418c831e4b0fa4ecac1bacd/t/62193b7fc9e72e0053f00910/1645820809779/CDS%2BREV14_1%2B2022-02-09.pdf) |
| bay-6u | 4.5 kg mass, 12 kg payload, 0.366 m bay length, 0.24741 m bay width | PUBLISHED / DERIVED fit | Properties table, 6U DuoPack: upper bound of the published 4–4.5 kg deployer range, 12 kg maximum payload. The game uses the nominal 6U CubeSat rail envelope as the fit limit (Cal Poly CDS Rev 14.1, Appendix B). [ISISPACE CubeSat Deployers brochure](https://www.isispace.nl/wp-content/uploads/2016/02/CubeSat-deployers-Brochure-web-compressed.pdf) |
| cubesat-12u | 24 kg mass, 0.366 m length, 0.320037 m width | PUBLISHED / DERIVED | Table 1 maximum mass; Appendix B nominal rail envelope. Circular fit width is the diagonal of the 0.2263 m by 0.2263 m cross section. [Cal Poly CubeSat Design Specification Rev 14.1](https://static1.squarespace.com/static/5418c831e4b0fa4ecac1bacd/t/62193b7fc9e72e0053f00910/1645820809779/CDS%2BREV14_1%2B2022-02-09.pdf) |
| bay-12u | 7.5 kg mass, 24 kg payload, 0.366 m bay length, 0.320037 m bay width | PUBLISHED / DERIVED fit | Properties table, 12U QuadPack: upper bound of the published 6–7.5 kg deployer range, 24 kg maximum payload. The game uses the nominal 12U CubeSat rail envelope as the fit limit (Cal Poly CDS Rev 14.1, Appendix B). [ISISPACE CubeSat Deployers brochure](https://www.isispace.nl/wp-content/uploads/2016/02/CubeSat-deployers-Brochure-web-compressed.pdf) |
| orion-esm | 3500 kg mass, 10387 kg payload, 8600 kg propellant | PUBLISHED | Dry design mass 3500 kg; carries 8600 kg propellant. NASA Orion by the Numbers: crew module mass at launch is 10387 kg, used here as demonstrated carried mass. [ESA European Service Module gets real](https://blogs.esa.int/orion/2014/11/26/european-service-module-gets-real/) |
| apollo-service-module | 5230 kg mass, 5556.51 kg payload, 18630 kg propellant | PUBLISHED / DERIVED | P. 161 Apollo SM dry mass; main propellant = 7170 + 11460 kg. Apollo 11 Press Kit, CSM Structure: demonstrated cabin mass 12250 lb, converted to kg. [Boeing Service Module for the ACRV](https://ntrs.nasa.gov/api/citations/19930013818/downloads/19930013818.pdf) |
| apollo-11-descent-stage | 2034 kg mass, 4821 kg payload, 8248 kg propellant | PUBLISHED / DERIVED | Table 1 Apollo 11 descent dry mass and propellant. Demonstrated load uses ascent dry mass plus its propellant. [NASA Apollo Lunar Modules](https://ntrs.nasa.gov/api/citations/20190031985/downloads/20190031985_rev1.pdf) |
| apollo-11-lander | 4479 kg mass, 6.985 m length, 4.2926 m width, 10624 kg propellant | PUBLISHED / DERIVED | Table 1: dry descent 2034 kg plus dry ascent 2445 kg. Apollo 11 Press Kit, LM Structure: 22 ft 11 in. height and 14 ft 1 in. ascent width; game uses cabin width as the stowed fit width. Table 1 stage propellant: 8248 + 2376 kg. [NASA Apollo Lunar Modules](https://ntrs.nasa.gov/api/citations/20190031985/downloads/20190031985_rev1.pdf) |
| apollo-11-adapter | 1792.14 kg mass, 15094.6 kg payload, 8.5344 m bay length, 6.604 m bay width | PUBLISHED / DERIVED / GAME_ESTIMATE for fit or heritage proxy | Apollo 11: adapter 3951 lb and demonstrated LM payload 33278 lb. Apollo 11 Press Kit, SLA Structure: 28 ft long and 260 in. base diameter. The game uses the base diameter as the fit width. [NASA Apollo by the Numbers, Ground Ignition Weights](https://www.nasa.gov/wp-content/uploads/static/history/SP-4029/Apollo_18-19_Ground_Ignition_Weights.htm) |
| apollo-12-lander | 4214 kg mass, 6.985 m length, 4.2926 m width, 10851 kg propellant | PUBLISHED / DERIVED | Table 1: dry descent 2034 kg plus dry ascent 2180 kg. Apollo 11 Press Kit, LM Structure: 22 ft 11 in. height and 14 ft 1 in. ascent width; game uses cabin width as the stowed fit width. Table 1 stage propellant: 8212 + 2639 kg. [NASA Apollo Lunar Modules](https://ntrs.nasa.gov/api/citations/20190031985/downloads/20190031985_rev1.pdf) |
| apollo-12-adapter | 1796.23 kg mass, 15223.5 kg payload, 8.5344 m bay length, 6.604 m bay width | PUBLISHED / DERIVED / GAME_ESTIMATE for fit or heritage proxy | Apollo 12: adapter 3960 lb and demonstrated LM payload 33562 lb. Apollo 11 Press Kit, SLA Structure: 28 ft long and 260 in. base diameter. The game uses the base diameter as the fit width. [NASA Apollo by the Numbers, Ground Ignition Weights](https://www.nasa.gov/wp-content/uploads/static/history/SP-4029/Apollo_18-19_Ground_Ignition_Weights.htm) |
| apollo-13-lander | 4226 kg mass, 6.985 m length, 4.2926 m width, 10690 kg propellant | PUBLISHED / DERIVED | Table 1: dry descent 2109 kg plus dry ascent 2117 kg. Apollo 11 Press Kit, LM Structure: 22 ft 11 in. height and 14 ft 1 in. ascent width; game uses cabin width as the stowed fit width. Table 1 stage propellant: 8318 + 2372 kg. [NASA Apollo Lunar Modules](https://ntrs.nasa.gov/api/citations/20190031985/downloads/20190031985_rev1.pdf) |
| apollo-13-adapter | 1790.33 kg mass, 15192.2 kg payload, 8.5344 m bay length, 6.604 m bay width | PUBLISHED / DERIVED / GAME_ESTIMATE for fit or heritage proxy | Apollo 13: adapter 3947 lb and demonstrated LM payload 33493 lb. Apollo 11 Press Kit, SLA Structure: 28 ft long and 260 in. base diameter. The game uses the base diameter as the fit width. [NASA Apollo by the Numbers, Ground Ignition Weights](https://www.nasa.gov/wp-content/uploads/static/history/SP-4029/Apollo_18-19_Ground_Ignition_Weights.htm) |
| apollo-14-descent-stage | 2134 kg mass, 4700 kg payload, 8200 kg propellant | PUBLISHED / DERIVED | Table 1 Apollo 14 descent dry mass and propellant. Demonstrated load uses ascent dry mass plus its propellant. [NASA Apollo Lunar Modules](https://ntrs.nasa.gov/api/citations/20190031985/downloads/20190031985_rev1.pdf) |
| apollo-14-lander | 4284 kg mass, 6.985 m length, 4.2926 m width, 10750 kg propellant | PUBLISHED / DERIVED | Table 1: dry descent 2134 kg plus dry ascent 2150 kg. Apollo 11 Press Kit, LM Structure: 22 ft 11 in. height and 14 ft 1 in. ascent width; game uses cabin width as the stowed fit width. Table 1 stage propellant: 8200 + 2550 kg. [NASA Apollo Lunar Modules](https://ntrs.nasa.gov/api/citations/20190031985/downloads/20190031985_rev1.pdf) |
| apollo-14-adapter | 1797.13 kg mass, 15279.3 kg payload, 8.5344 m bay length, 6.604 m bay width | PUBLISHED / DERIVED / GAME_ESTIMATE for fit or heritage proxy | Apollo 14: adapter 3962 lb and demonstrated LM payload 33685 lb. Apollo 11 Press Kit, SLA Structure: 28 ft long and 260 in. base diameter. The game uses the base diameter as the fit width. [NASA Apollo by the Numbers, Ground Ignition Weights](https://www.nasa.gov/wp-content/uploads/static/history/SP-4029/Apollo_18-19_Ground_Ignition_Weights.htm) |
| orion-cabin | 10387 kg mass, 3.35 m length, 5 m width | PUBLISHED | Crew Module panel: mass at liftoff, height and diameter. Orion overview lists accommodation for four crew. [NASA Orion by the Numbers](https://www.nasa.gov/wp-content/uploads/2026/01/orion-by-the-numbers-2026b.pdf) |
| insight-cruise | 79 kg mass, 547 kg payload, 67 kg propellant | PUBLISHED / DERIVED | Quick Facts: cruise stage mass, 67 kg loaded propellant and pressurant, and carried mass = 358 kg lander + 189 kg aeroshell. [JPL InSight Launch Press Kit](https://www.jpl.nasa.gov/news/press_kits/insight/launch/facts/) |
| spirit-cruise | 183 kg mass, 827 kg payload, 52 kg propellant | PUBLISHED / DERIVED | Quick Facts: stage and launch propellant masses. Demonstrated carried mass = 174 + 365 + 198 + 90 kg. [JPL Mars Exploration Rover Launch Press Kit](https://www.jpl.nasa.gov/news/press_kits/merlaunch.pdf) |
| pathfinder-cruise | 216 kg mass, 584 kg payload, 94 kg propellant | PUBLISHED / DERIVED | Spacecraft Summary: 894 kg launch minus 584 kg entry mass minus 94 kg propellant (JPL Mars Pathfinder Landing Press Kit, Spacecraft) = 216 kg. Carried mass uses the entry mass. [NASA PDS Mars Pathfinder Lander Description](https://planetarydata.jpl.nasa.gov/img/data/mpfl-m-imp-2-edr-v1.0/mpim_0001/document/insthost.htm) |
| curiosity-cruise | 451 kg mass, 3300 kg payload, 88 kg propellant | PUBLISHED / DERIVED | Quick Facts: 539 kg fueled stage; carried 899 kg rover + 2401 kg entry system. MSL full load of 475 kg (NASA Mars 2020 final EIS §2.6.2.2) minus 387 kg usable descent load (MSL press kit, Descent Stage) gives an approximate cruise allocation of 88 kg. Game derives dry mass as 539 - 88 kg. [JPL Mars Science Laboratory Landing Press Kit](https://mars.nasa.gov/internal_resources/823/) |
| insight-aeroshell | 189 kg mass, 358 kg payload, 1.76 m bay length, 2.64 m bay width | PUBLISHED / GAME_ESTIMATE for fit or heritage proxy | Quick Facts: mass, lander mass and cruise assembly height and diameter as game fit bounds. [JPL InSight Launch Press Kit](https://www.jpl.nasa.gov/news/press_kits/insight/launch/facts/) |
| spirit-aeroshell | 288 kg mass, 539 kg payload, 1.6 m bay length, 2.65 m bay width | PUBLISHED / DERIVED / GAME_ESTIMATE for fit or heritage proxy | Quick Facts: 198 kg backshell and parachute + 90 kg shield; 174 kg rover + 365 kg lander. Cruise assembly height and diameter are game fit bounds. [JPL Mars Exploration Rover Launch Press Kit](https://www.jpl.nasa.gov/news/press_kits/merlaunch.pdf) |
| pathfinder-aeroshell | 214 kg mass, 370 kg payload, 1.5 m bay length, 2.65 m bay width | PUBLISHED / GAME_ESTIMATE for fit or heritage proxy | Entry system mass 584 - 370 kg. JPL Landing Press Kit, Spacecraft: 1.5 m cruise vehicle height and 2.65 m diameter are game fit bounds. [NASA PDS Mars Pathfinder Lander Description](https://planetarydata.jpl.nasa.gov/img/data/mpfl-m-imp-2-edr-v1.0/mpim_0001/document/insthost.htm) |
| mars-2020-entry | 1904.5 kg mass, 1025 kg payload, 3 m bay length, 4.5 m bay width, 390 kg propellant | PUBLISHED / GAME_ESTIMATE for fit or heritage proxy | Mars 2020, p. 34: 3317.3 kg entry mass minus 1022.8 kg payload, minus 390 kg design descent propellant (NASA Mars 2020 final EIS §2.1.2). Game uses the rounded JPL 1025 kg rover mass as the demonstrated fit limit. The 4.5 m outer diameter and Curiosity's 3 m cruise height are game fit bounds. Uses the same 390 kg design load as the descent tank capacity. [NASA Planetary Mission Entry Vehicles Quick Reference Guide](https://www.nasa.gov/wp-content/uploads/2023/08/final-planetary-mission-entry-vehicles-quick-reference-guide-v4.1-.pdf) |
| perseverance | 1025 kg mass, 3 m length, 2.7 m width | PUBLISHED | Quick Facts: rounded rover mass, length and width. [JPL Mars 2020 Launch Press Kit](https://www.jpl.nasa.gov/news/press_kits/mars_2020/launch/quick_facts/) |
| ingenuity | 1.8 kg mass, 0.49 m length, 1.2 m width | PUBLISHED / GAME_ESTIMATE for fit width | Quick Facts: mass, height and operating rotor diameter used as a conservative fit width. The flight battery and solar panel are onboard. [JPL Mars 2020 Launch Press Kit](https://www.jpl.nasa.gov/news/press_kits/mars_2020/launch/quick_facts/) |
| sojourner | 10.5 kg mass, 0.65 m length, 0.48 m width | PUBLISHED | Flight project overview: rover mass, length and width. [JPL Robotics Mars Pathfinder Rover](https://www-robotics.jpl.nasa.gov/what-we-do/flight-projects/pathfinder/) |
| curiosity | 899 kg mass, 3 m length, 2.7 m width | PUBLISHED | Quick Facts and Comparing Two Mars Rover Projects: mass, length and width. [JPL Mars Science Laboratory Landing Press Kit](https://mars.nasa.gov/internal_resources/823/) |
| spirit | 174 kg mass, 1.6 m length, 2.3 m width | PUBLISHED | Quick Facts: launch roster mass, length and width. [JPL Mars Exploration Rover Launch Press Kit](https://www.jpl.nasa.gov/news/press_kits/merlaunch.pdf) |
| mr-103 | 0.37 kg mass, 1 N thrust, 202 s impulse | PUBLISHED | Liquid propulsion summary table, Aerojet MR-103. Lower bound for specific impulse, upper bound for mass. [NASA Small Spacecraft Technology, In-Space Propulsion](https://www.nasa.gov/smallsat-institute/sst-soa/in-space_propulsion/) |
| mr-106 | 0.59 kg mass, 22 N thrust, 228 s impulse | PUBLISHED | Liquid propulsion summary table, Aerojet MR-106. Lower bound for specific impulse. [NASA Small Spacecraft Technology, In-Space Propulsion](https://www.nasa.gov/smallsat-institute/sst-soa/in-space_propulsion/) |
| monarc-1 | 0.38 kg mass, 1 N thrust, 227 s impulse | PUBLISHED | Liquid propulsion summary table, Moog MONARC-1. Thruster row. [NASA Small Spacecraft Technology, In-Space Propulsion](https://www.nasa.gov/smallsat-institute/sst-soa/in-space_propulsion/) |
| monarc-5 | 0.49 kg mass, 4.5 N thrust, 226 s impulse | PUBLISHED | Liquid propulsion summary table, Moog MONARC-5. Thruster row. [NASA Small Spacecraft Technology, In-Space Propulsion](https://www.nasa.gov/smallsat-institute/sst-soa/in-space_propulsion/) |
| bit-3 | 2.9 kg mass, 70 W drawn, 0.001 N thrust, 1960 s impulse | PUBLISHED | Gridded-Ion Electric Propulsion table: 1.0 mN, 1960 s, 2.9 kg, 70 W. The mesh represents an ion thruster. [NASA Small Spacecraft Technology, In-Space Propulsion](https://www.nasa.gov/smallsat-institute/sst-soa/in-space_propulsion/) |
| shuttle-oms | 118 kg mass, 26700 N thrust, 316 s impulse | PUBLISHED | Shuttle OMS engine specifications: 118 kg dry, 26.7 kN in vacuum, 316 s vacuum. [NASA Imagine the Universe](https://imagine.gsfc.nasa.gov/ask_astro/satellites.html) |
| orion-oms-e | 118 kg mass, 25700 N thrust, 310 s impulse | PUBLISHED / GAME_ESTIMATE for heritage mass | Technical details: 25.7 kN. NASA NTRS 20180004487 slide 5: 310 s minimum and direct reuse of Shuttle engines. The game uses the 118 kg Shuttle dry mass as a heritage estimate before Orion-specific changes. [ESA European Service Module Propulsion](https://www.esa.int/Science_Exploration/Human_and_Robotic_Exploration/Orion/European_Service_Module_Propulsion) |
| rl10c-1 | 190.51 kg mass, 101820 N thrust, 449.7 s impulse | PUBLISHED / DERIVED | RL10C-1 column: 22890 lbf converted to newtons and 420 lb converted to kg. 449.7 s specific impulse. [L3Harris RL10 Propulsion System](https://www.l3harris.com/sites/default/files/2024-07/l3harris-ar-rl10-spec-sheet.pdf) |
| sp1x-solar | 0.035294 kg mass, 2.4 W supplied | PUBLISHED / DERIVED | Table 3-2. Nominal 2.4 W and 68 W/kg. Derived mass is power divided by specific power. [NASA Small Spacecraft Technology, Power](https://www.nasa.gov/smallsat-institute/sst-soa/power-subsystems/) |
| sp6x-solar | 0.259459 kg mass, 19.2 W supplied | PUBLISHED / DERIVED | Table 3-2. Nominal 19.2 W and 74 W/kg. Derived mass is power divided by specific power. [NASA Small Spacecraft Technology, Power](https://www.nasa.gov/smallsat-institute/sst-soa/power-subsystems/) |
| sp6x-wings | 0.778378 kg mass, 57.6 W supplied | PUBLISHED / DERIVED | Table 3-2. Nominal 57.6 W and 74 W/kg. Derived mass is power divided by specific power. [NASA Small Spacecraft Technology, Power](https://www.nasa.gov/smallsat-institute/sst-soa/power-subsystems/) |
| pumpkin-clamshell | 5 kg mass, 220 W supplied | PUBLISHED / DERIVED | Table 3-2. Nominal 220 W and 44 W/kg. Derived mass is power divided by specific power. [NASA Small Spacecraft Technology, Power](https://www.nasa.gov/smallsat-institute/sst-soa/power-subsystems/) |
| mmrtg | 45 kg mass, 110 W supplied | PUBLISHED | Power Source tech specs: 45 kg, about 110 W at launch. [NASA Perseverance Rover Components](https://science.nasa.gov/mission/mars-2020-perseverance/rover-components/) |
| shuttle-fuel-cell | 115.67 kg mass, 7000 W supplied | PUBLISHED / DERIVED | Each plant supplies 7 kW maximum continuous and weighs 255 lb, converted to kg. Reactant mass is outside this entry. [NASA Space Shuttle Fuel Cell Power Plants](https://chem.beloit.edu/edetc/nanolab/fuelcell2/NASA%20Space%20Shuttle%20Fuel%20Cell%20Power%20Plants%20%282002%29.pdf) |

Additional sources used in the calculations above include the
[Shuttle OMS usable propellant budget](https://ntrs.nasa.gov/api/citations/19750024086/downloads/19750024086.pdf),
[Apollo 11 Press Kit](https://www.nasa.gov/wp-content/uploads/static/history/alsj/a11/A11_PressKit.pdf),
[NASA Mars Pathfinder Landing Press Kit](https://mars.nasa.gov/internal_resources/816/),
[Mars 2020 Final EIS](https://www.nasa.gov/wp-content/uploads/2015/04/mars-2020-final-eis.pdf).

## Added instruments

All new instruments use the existing $0.5M/kg cost and the 18-month build
time for small instruments. Both are GAME_ESTIMATE. Published operating
power is used as a conservative input when the source gives an upper bound.

| Id | Catalog figures | Basis | Source and derivation |
| --- | --- | --- | --- |
| gecko-pan-ms | 0.5 kg, 3.5 W, 1.556481 kbps, 10 m ground sampling at 500 km | PUBLISHED power / GAME_ESTIMATE data | Technical Specifications, PAN/MS column: 0.5 kg, under 3.5 W imaging, 10 m PAN at 500 km, 41 km swath, 8-bit mode. Game scenario assumes one uncompressed square PAN image per Earth day. [SCS & Space Advisory Company Gecko Imager datasheet](https://satcatalog.s3.amazonaws.com/components/761/SatCatalog_-_SCS_Space_-_Gecko_Imager_PAN__4_MS_-_Datasheet.pdf) |
| lola | 9.6 kg, 26.2 W, 10.000000 kbps | PUBLISHED power / PUBLISHED data | Technical specifications: mass, power and nominal data rate. [NASA LOLA Fact Sheet](https://science.nasa.gov/wp-content/uploads/2024/01/lola-fact-sheet.pdf) |
| diviner | 11.9 kg, 21.6 W, 24.000000 kbps | PUBLISHED power / DERIVED data | Mass and orbit-average power allocations. NASA PDS LRO-L-DLRE-2-EDR-V1.0: 16 sets of 192 measurements at 16 bits every 2.048 s. Excludes engineering overhead. [LRO Instrument Suite and Mission, Table 7](https://science.nasa.gov/wp-content/uploads/2024/01/lro-space-science-paper.pdf) |
| apollo-psep | 48 kg, 46 W, 1.060000 kbps | GAME_ESTIMATE power / GAME_ESTIMATE data | Passive Seismic Experiment: package mass. Apollo 11 Mission Report section A.4.3: 46 W upper solar supply used as a conservative power proxy. NASA ALSEP telemetry normal downlink of 1060 bit/s used as a game proxy. [NASA Apollo 11 Preliminary Science Report](https://www.nasa.gov/wp-content/uploads/static/history/alsj/a11/as11psr.pdf) |
| mastcam-z | 4 kg, 17.4 W, 1.667136 kbps | PUBLISHED power / DERIVED data | Mastcam-Z Tech Specs. 148 Mbit per sol divided by 88775 seconds. [NASA Perseverance Science Instruments](https://science.nasa.gov/mission/mars-2020-perseverance/science-instruments/) |
| supercam | 10.6 kg, 17.9 W, 0.048611 kbps | PUBLISHED power / DERIVED data | SuperCam Tech Specs. Mass = 5.6 + 4.8 + 0.2 kg. Data = 4.2 Mbit/day divided by 86400 seconds. [NASA Perseverance Science Instruments](https://science.nasa.gov/mission/mars-2020-perseverance/science-instruments/) |
| pixl | 6.915 kg, 25 W, 0.185185 kbps | PUBLISHED power / DERIVED data | PIXL Tech Specs. Mass = 4.3 + 2.6 + 0.015 kg. Data = 2 MB/day times 8 divided by 86400 seconds. [NASA Perseverance Science Instruments](https://science.nasa.gov/mission/mars-2020-perseverance/science-instruments/) |
| rimfax | 3 kg, 10 W, 0.090115 kbps | PUBLISHED power / GAME_ESTIMATE data | RIMFAX Tech Specs. Uses upper published bounds for mass, power and data per sounding. Game assumes 100 soundings per sol at 10 kB each. [NASA Perseverance Science Instruments](https://science.nasa.gov/mission/mars-2020-perseverance/science-instruments/) |
| meda | 5.5 kg, 17 W, 0.991270 kbps | PUBLISHED power / GAME_ESTIMATE data | MEDA Tech Specs. Uses published peak power. Game treats the 11 MB data-return figure as one batch per sol. [NASA Perseverance Science Instruments](https://science.nasa.gov/mission/mars-2020-perseverance/science-instruments/) |

The Diviner rate uses the science frame structure in the
[NASA PDS Diviner dataset](https://pds.nasa.gov/ds-view/pds/viewProfile.jsp?dsid=LRO-L-DLRE-2-EDR-V1.0).
The Apollo seismic package uses the ALSEP telemetry proxy from the
[NASA ALSEP Archive Tape Description](https://nssdc.gsfc.nasa.gov/misc/documents/b56514.pdf).
Its power proxy comes from the [Apollo 11 Mission Report](https://sma.nasa.gov/SignificantIncidents/assets/apollo-11-mission-report.pdf).
SuperCam and PIXL masses are DERIVED by summing their listed components.
The Mars sol conversion uses the 24 h 39 min 35 s value in the
[JPL Mars 2020 Quick Facts](https://www.jpl.nasa.gov/news/press_kits/mars_2020/launch/quick_facts/).

## Added launch figures and recorder capacities

| Entry | Figures and basis | Source |
| --- | --- | --- |
| pegasus-xl | 221 kg POLAR (PUBLISHED), 741 km at 90° without HAPS | [Pegasus User's Guide, Fig. 3-1](https://cdn.northropgrumman.com/-/media/wp-content/uploads/Pegasus-User-Guide-1.pdf) |
| falcon-9 | 1700 kg POLAR (DERIVED demonstrated reference load), 3748 lb × 0.45359237 kg/lb, rounded, to 676 km sun-synchronous. 4020 kg TRANS_MARS (PUBLISHED) | [NASA PACE spacecraft facts](https://science.nasa.gov/blogs/pace/2024/02/08/meet-nasas-pace-spacecraft-science-instruments/), [NASA PACE orbit](https://pace.gsfc.nasa.gov/), [SpaceX Falcon 9](https://www.spacex.com/vehicles/falcon-9/) |
| sls-block-1 | 27000 kg TRANS_LUNAR (PUBLISHED lower bound). $2200M (PUBLISHED SLS production cost estimate) | [NASA SLS reference](https://www.nasa.gov/reference/space-launch-system/), [NASA OIG IG-22-003, p. 23](https://oig.nasa.gov/wp-content/uploads/2024/02/IG-22-003.pdf) |
| atlas-v-541 | 3893 kg TRANS_MARS (PUBLISHED demonstrated payload to Curiosity's transfer). $243M (PUBLISHED Mars 2020 launch services cost) | [JPL MSL Landing Press Kit, p. 6](https://mars.nasa.gov/internal_resources/823/), [NASA Mars 2020 launch services award](https://www.nasa.gov/news-release/nasa-awards-launch-services-contract-for-mars-2020-rover-mission/) |
| eo1-class | 48 Gbit (PUBLISHED) | [NASA Earth Observing-1, WARP description](https://science.nasa.gov/earth/earth-observatory/earth-observing-1/) |
| landsat7-class | 380 Gbit (PUBLISHED) | [NASA Earth Science Reference Handbook, PDF p. 190](https://assets.science.nasa.gov/content/dam/science/missions/a-train/publications/2006ReferenceHandbook.pdf) |
| saturn-v | 43500 kg TRANS_LUNAR (PUBLISHED approximate). $185M (PUBLISHED Apollo 15 launch-vehicle estimate in 1971 dollars) | [NASA Saturn V reference](https://www.nasa.gov/learning-resources/for-kids-and-students/what-was-the-saturn-v-grades-5-8/), [Apollo 15 Press Kit](https://www.nasa.gov/wp-content/uploads/static/apollo50th/pdf/A15_PressKit.pdf) |

## Deep-space flight assumptions

| Mission | Surface burns | Radio reference | Game contact window |
| --- | --- | --- | --- |
| Moon Landing | 4085 m/s DERIVED from 2165 m/s descent plus 1920 m/s ascent. The service module handles the remaining 1950 m/s. [Lunar Operations Delta V Budget, Table 3](https://ntrs.nasa.gov/api/citations/19850008679/downloads/19850008679.pdf) | 80 Mbps nominal optical return. [NASA Artemis II optical communications, Mission Objectives](https://ntrs.nasa.gov/api/citations/20250009875/downloads/Optical%20Comm%20for%20Artemis%20II%20Crewed%20Mission.pdf) | 30 minutes per day (GAME_ESTIMATE) |
| Mars Sample Drones | 80 m/s GAME_ESTIMATE for powered descent. The cruise stage handles the other 20 m/s. | 2 Mbps peak rover-to-orbiter rate. [NASA Mars Relay Network](https://science.nasa.gov/mars/mars-relay-network/) | 30 minutes per day (GAME_ESTIMATE) |

The builder applies the chosen engine's specific impulse to both stages and
uses the combined lunar lander tanks for the surface leg. This simplified
model omits individual stage separation, propellant compatibility and lunar
landing thrust checks. Selected ground stations share the contact window.
The mission scenarios use the listed historical launch costs in nominal dollars.

## Flight events

The event descriptions use documented examples. The simulator supplies the
gameplay frequency and effect sizes.

| Id | Real example | Source |
| --- | --- | --- |
| halloween-solar-storm | During the October 2003 solar storms, Genesis switched to safe mode and radiation interrupted Chandra observations. | 2003 Halloween Storm Impacts on Spacecraft (1). [NASA, Preliminary ES31 Radiation Environment](https://ntrs.nasa.gov/api/citations/20200008611/downloads/20200008611.pdf) |
| dss43-offline | The Canberra DSS-43 antenna went offline for equipment upgrades in 2020, interrupting command capability for Voyager 2. | DSS-43 upgrade and Voyager 2 command coverage. [NASA Deep Space Station in Australia Is Getting an Upgrade](https://www.nasa.gov/centers-and-facilities/jpl/nasas-deep-space-station-in-australia-is-getting-an-upgrade/) |
| hubble-camera-fault | An electrical fault shut down all three channels of Hubble's Advanced Camera for Surveys in January 2007. | Background on the 2007 ACS power failure. [NASA SVS HST SM4 ACS Repair EVA](https://svs.gsfc.nasa.gov/10229) |
| artemis-launch-slip | A hydrogen leak forced NASA to call off an Artemis I launch attempt in September 2022. | Artemis I launch attempt 2 slide. [NASA Advisory Council, Artemis I Status](https://www.nasa.gov/wp-content/uploads/2023/10/nac-october-2022-artemis-final-rev-b.pdf) |
| insight-dust-storm | A January 2022 dust storm reduced sunlight enough for InSight to pause its science work and conserve power. | February 2022 Mars Report transcript. [NASA Mars Report, Dust Storms on Mars](https://science.nasa.gov/resource/mars-report-dust-storms-on-mars/) |

## Still to source

- Falcon 9 maximum capacity for the game's 600 km and 705 km near-polar
  orbits. The catalog uses a documented flight reference at 676 km.
- Atlas V 541 maximum capability at the Mars 2020 departure energy. The
  catalog uses the flown Curiosity payload.
- SORCE onboard recorder capacity. The catalog leaves it unset.
- Dry mass, payload capacity and usable internal dimensions for X-37B and
  Dream Chaser. The Earth airframe choices use Shuttle and three sourced buses.
- Stowed Apollo lander envelopes. Current fit bounds are documented above.
- As-flown Mars 2020 cruise stage dry mass, standalone sky crane dry mass,
  stowed packaging dimensions, and the exact post-injection delta-v budget.
  The catalog uses heritage hardware, bundled entry data, and a scenario
  delta-v estimate.
- Artemis lander, Artemis surface instrument specifications, and lunar
  sample-box mass and dimensions. The Moon choices use Apollo and LRO heritage.
- Published daily Gecko, RIMFAX and MEDA duty cycles for the game scenarios.
- Real cost anchors for buses and instruments, and component-level radio
  and solar figures beyond the listed parts.
