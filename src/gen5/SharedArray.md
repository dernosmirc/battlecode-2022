## Shared Array Configuration

| Index    | Description                                                                                                                                            | Min Required |
|----------|--------------------------------------------------------------------------------------------------------------------------------------------------------|--------------|
| [0, 4)   | Enemy archon locations                                                                                                                                 |              |
| 4        | 0-7: Symmetry for next soldier, as per archon index<br> 8-10: Bad symmetries<br> 11-12: Central archon index<br> 13-14: Map symmetry<br> 15: Indicator |              |
| [10, 14) | 0-11: # of droids spawned by each archon <br> 12-15: # of watchtowers nearby                                                                           |              |
| [14, 18) | 0-10: Archon Hp <br> 11-12: Type of builder spawned <br> 13-14: Archon Level <br> 15: Lab spawned                                                      | 2            |
| [32, 36) | 0-12: Team archon location<br> 14: Defense needed<br> 15: Indicator                                                                                    |              |
| [36, 48) | Top 12 lead sources on 4x4 Grid                                                                                                                        | 8            |
| [48, 50) | Top 2 gold sources on 4x4 Grid                                                                                                                         | 2            |
