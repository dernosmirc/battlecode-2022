## Shared Array Configuration

| Index    | Description                                                                                                                                            | Min Required |
|----------|--------------------------------------------------------------------------------------------------------------------------------------------------------|--------------|
| [0, 4)   | Enemy archon locations                                                                                                                                 |              |
| 4        | 0-7: Symmetry for next soldier, as per archon index<br> 8-10: Bad symmetries<br> 11-12: Central archon index<br> 13-14: Map symmetry<br> 15: Indicator |              |
| 5        | 0-11: Location of target (for tailing)<br> 12-14: Priority of target<br> 15: Indicator                                                                 |              |
| 6        | 0-5: Number of enemy attack units near target (capped at 63)                                                                                           |              |
| 7        | Number of total alive soldiers in previous round                                                                                                       |              |
| 8        | Number of total alive miners in previous round                                                                                                         |              |
| 9        | Number of total alive sage in previous round                                                                                                           |              |
| [10, 14) | 0-11: # of droids spawned by each archon <br> 12-15: # of watchtowers nearby                                                                           |              |
| [14, 18) | 0-10: Archon Hp <br> 11-12: Type of builder spawned <br> 13-14: Archon Level <br> 15: Lab spawned                                                      | 2            |
| [32, 36) | 0-12: Team archon location<br> 14: Defense needed<br> 15: Indicator                                                                                    |              |
| [36, 48) | Top 12 lead sources on 3x3 Grid                                                                                                                        | 8            |
| [48, 50) | Top 2 gold sources on 3x3 Grid                                                                                                                         | 2            |
