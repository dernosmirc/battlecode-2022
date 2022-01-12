## Shared Array Configuration

value = setBits(0, 15, 15, 1); // to check if set case (0, 0)\
value = setBits(value, 6, 11, rc.getLocation().x);\
value = setBits(value, 0, 5, rc.getLocation().y);

0, 1, 2, 3 = enemy archon location(in symmetrical ordrer, wrt archon location in shared array)

32, 33, 34, 35 = our archon location

10, 11, 12, 13 = # of droids spawned by each archon