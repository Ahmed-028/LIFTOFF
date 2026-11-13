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
figure; near-polar and sun-synchronous orbits use the 90° figure. Where no
figure for a class of orbit was found, the rocket is not offered for it.

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

## Still to source

1. Polar-orbit capacity for Falcon 9 and Pegasus XL (both fly polar missions;
   they are simply not offered for polar orbits until a figure is cited).
2. Real cost anchors for buses and instruments, to replace or calibrate the
   per-kilogram rule.
3. Onboard data storage per bus (needed by the M3 simulation).
4. Mass and power of separate radios and solar arrays (M2+).
