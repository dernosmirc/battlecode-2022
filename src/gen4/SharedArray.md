## Shared Array Configuration

| Index    | Description                                                                                                                                            | Importance |
|----------|--------------------------------------------------------------------------------------------------------------------------------------------------------|------------|
| [0, 4)   | Enemy archon locations                                                                                                                                 |            |
| 4        | 0-7: Symmetry for next soldier, as per archon index<br> 8-10: Bad symmetries<br> 11-12: Central archon index<br> 13-14: Map symmetry<br> 15: Indicator |            |
| [10, 14) | # of droids spawned by each archon                                                                                                                     |            |
| [14, 18) | Archon Hp                                                                                                                                              | Low        |
| [32, 36) | 0-12: Team archon location<br> 14: Defense needed<br> 15: Indicator                                                                                    |            |
| [36, 48) | Top 12 lead sources on 4x4 Grid                                                                                                                        | High       |
| [48, 50) | Top 2 gold sources on 4x4 Grid                                                                                                                         | High       |
