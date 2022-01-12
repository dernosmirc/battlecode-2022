## Shared Array Configuration

value = setBits(0, 15, 15, 1); // to check if set case (0, 0)\
value = setBits(value, 6, 11, rc.getLocation().x);\
value = setBits(value, 0, 5, rc.getLocation().y);


| Index          | Description                        |
|----------------|------------------------------------|
| [0, 4)         | enemyArchonLocation                |
| [10, 14)       | # of droids spawned by each archon |
| [32, 36)       | Team Archon Locations              |
| [36, 44)       | Top 8 lead sources on 4x4 Grid     |
| [44, 46)       | Top 2 gold sources on 4x4 Grid     |