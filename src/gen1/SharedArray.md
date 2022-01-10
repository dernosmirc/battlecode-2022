## Shared Array Configuration

value = setBits(0, 15, 15, 1);
value = setBits(value, 6, 11, rc.getLocation().x);
value = setBits(value, 0, 5, rc.getLocation().y);

0 = enemyArchonLocation