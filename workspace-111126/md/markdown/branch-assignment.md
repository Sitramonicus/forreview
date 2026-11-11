# Branch assignment: method and results

## The rule

1. A **leaf team** is a supervisor who has direct reports and no supervisor reporting to them. Each leaf team gets
   one branch.
2. The branch is the **nearest existing office to the team's geographic centre**, where the centre is the mean of
   its members' home municipalities.
3. **Supervisors who lead other supervisors**, and **every C-level executive**, go to **Main Branch**.
4. Members inherit their leaf team's branch.

## The honest caveat about the addresses

The file's addresses are street-level but fictional, and they are spread across the whole country — Batanes to
Sulu. Nobody can geocode them exactly, so the finest truthful grain is the **municipality or city** each address
names. `Locality.java` holds approximate municipality centres for those places; the centroid is the mean of those.

The consequence is worth stating plainly: **most teams' centroids are hundreds of kilometres from all four offices.**
The rule still decides (there is always a nearest), but for those teams it is deciding by a few kilometres out of
several hundred — that is, by little more than chance. The one team where the answer is genuinely local is the
executive team, whose centroid sits 4 km from Makati.

The alternative — pretending a team of seven people living in Iloilo, Tapaz, Palayan, Caloocan, Bongabon,
Mabalacat and Kabankalan belongs to "Taguig" because Taguig happened to win a 280 km coin-toss — would be worse
than saying so. The assignment is therefore **approximate at municipality level**, and this note is the record.

## Results

| Team (supervisor) | Members geocoded | Centroid | Nearest office | Margin over next |
|---|---|---|---|---|
| Garcia, Manuel III | 3 | 14.535, 120.988 | Makati (4 km) | 3 km over Taguig |
| Reyes, Isabella | 3 | 14.424, 122.222 | Pasig (124 km) | 3 km over Taguig |
| Salcedo, Anthony | 3 | 12.948, 122.087 | Taguig (207 km) | 4 km over Pasig |
| De Leon, Selena | 7 | 12.872, 122.255 | Taguig (224 km) | 4 km over Pasig |
| Lim, Antonio | 3 | 12.598, 122.478 | Taguig (263 km) | 4 km over Pasig |
| Mata, Christian | 7 | 12.172, 121.995 | Taguig (280 km) | 5 km over Pasig |
| San Jose, Brad | 2 | 9.517, 124.279 | Taguig (658 km) | 3 km over Pasig |
| Romualdez, Fredrick | 2 | 8.735, 125.053 | Taguig (777 km) | 3 km over Pasig |
| Villanueva, Andrea Mae | 1 | 8.207, 126.318 | Taguig (906 km) | 3 km over Pasig |
| Alvaro, Roderick | 1 | 7.530, 125.630 | Taguig (924 km) | 3 km over Pasig |
| Aquino, Bianca Sofia | 1 | 14.676, 121.044 | Quezon City (0 km) | 12 km over Pasig |

Note on two rows: **San Jose, Brad** is a leaf team (his two reports, Romualdez 10008 and Atienza 10009, supervise
nobody) and **Villanueva, Andrea Mae** leads the HR team directly, with San Jose as its team leader. Both rules
follow the file rather than the titles.

## Final headcount by branch

| Branch | Headcount | Who |
|---|---|---|
| **Main Branch** | **11** | The four executives (10001–10004); Hernandez (10005, IT head); **Villanueva** (10006, HR Manager — she leads San Jose, who leads a team); **Alvaro** (10010, Accounting Head — he leads Salcedo, who leads a team); **Salcedo** (10011, Payroll Manager — he leads Lopez, who leads a team); **Romualdez** (10015, Account Manager — he leads two team leaders) |
| **Taguig** | **23** | Mata's team (10016–10024, 9 people), De Leon's team (10017, 10025–10031, 8 people), the HR team (10007–10009, 3), the payroll team (10012–10014, 3) |
| **Quezon City** | 0 | Aquino's own team centre is Quezon City, but she is an executive and therefore sits at Main; her department (Accounting/Payroll) inherits Salcedo's Taguig centroid |
| **Pasig** | 0 | The CMO's three-person team (10032–10034) is the only team centred on Pasig, and its leader sits at Main |

The exact list is computed at import time; run `tools/branch-report.sh` to print it.

## Why the C-level rule is uniform

The brief allows a per-executive treatment (the CFO nearer payroll, the CMO as a department head). Rather than give
each executive a private branch rule, all four sit at Main Branch, and their *access* differs by role instead:
the CFO reads payroll totals, the COO reads operational attendance, the CMO sees only marketing. Branch is a
location, not a permission.
