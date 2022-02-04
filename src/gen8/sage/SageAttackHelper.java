package gen8.sage;

import battlecode.common.*;
import gen8.Sage;
import gen8.common.MovementHelper;

import static gen8.RobotPlayer.*;

public class SageAttackHelper {


    // ARCHON
    // LABORATORY
    // WATCHTOWER
    // MINER
    // BUILDER
    // SOLDIER
    // SAGE
    private static final int[] priority = {3, 1, 4, 0, 2, 5, 6};

    private static final double EXP_DAMAGE_FACTOR = 1;
    private static final double EXP_RUBBLE_FACTOR = 1;

    enum AttackType {
        Fury, Charge, Attack
    }

    static class AttackInfo {
        final int totalMaxDamage;
        final AttackType type;
        final MapLocation attackLocation;

        AttackInfo(AttackType type, int damage) {
            this.attackLocation = null;
            this.type = type;
            this.totalMaxDamage = damage;
        }

        AttackInfo(AttackType type, int damage, MapLocation attackLocation) {
            this.attackLocation = attackLocation;
            this.type = type;
            this.totalMaxDamage = damage;
        }
    }

    public static AttackInfo bestDamageFrom(MapLocation ml, RobotInfo[] ris) throws GameActionException {
        if (!ml.equals(rc.getLocation()) && (!rc.onTheMap(ml) || rc.canSenseRobotAtLocation(ml))) return null;

        int maxPriority = -1;
        int furyDamage = 0, chargeDamage = 0, attackDamage = 0;
        int minHp = Integer.MAX_VALUE;
        int maxHp45 = 0;
        RobotInfo robotToAttack = null;
        RobotInfo robotToAttack45 = null;
        for (int i = ris.length; --i >= 0; ) {
            RobotInfo robot = ris[i];
            if (!robot.location.isWithinDistanceSquared(ml, myType.actionRadiusSquared)) {
                continue;
            }

            if (robot.team == myTeam) {
                if (robot.mode == RobotMode.TURRET) {
                    furyDamage = Integer.MIN_VALUE;
                }
                continue;
            }

            if (Sage.isLabHunter && robot.type == RobotType.BUILDER) {
                return new AttackInfo(AttackType.Attack, 120, robot.location);
            }

            if (robot.mode == RobotMode.TURRET) {
                furyDamage += Math.min(robot.health, robot.type.getMaxHealth(robot.level)/10);
            } else if (robot.mode == RobotMode.DROID && robot.type.canAttack()) {
                chargeDamage += Math.min(robot.health, (robot.type.getMaxHealth(robot.level) * 22)/100);
            }
            int typeIndex = robot.type.ordinal();
            int p = priority[typeIndex], damage = Math.min(robot.health, RobotType.SAGE.damage);
            int health = robot.health;
            if (p > maxPriority) {
                maxPriority = p;
                if (health > 45) {
                    minHp = health;
                    attackDamage = damage;
                    robotToAttack = robot;
                } else {
                    maxHp45 = health;
                    attackDamage = damage;
                    robotToAttack45 = robot;
                }
            } else if (p == maxPriority) {
                if (health > 45 && health < minHp) {
                    minHp = health;
                    attackDamage = damage;
                    robotToAttack = robot;
                } else if (health <= 45 && health > maxHp45) {
                    maxHp45 = health;
                    attackDamage = damage;
                    robotToAttack45 = robot;
                }
            }
        }

        if (furyDamage > 0 && furyDamage >= attackDamage && furyDamage >= chargeDamage) {
            return new AttackInfo(AttackType.Fury, furyDamage);
        }

        if (chargeDamage > attackDamage) {
            return new AttackInfo(AttackType.Charge, chargeDamage);
        }

        if (maxPriority != -1 && attackDamage > 0) {
            if (robotToAttack45 != null && robotToAttack != null) {
                if (priority[robotToAttack.type.ordinal()] > priority[robotToAttack45.type.ordinal()]) {
                    robotToAttack45 = null;
                }
            }
            if (robotToAttack45 != null) {
                return new AttackInfo(AttackType.Attack, attackDamage, robotToAttack45.location);
            } else {
                return new AttackInfo(AttackType.Attack, attackDamage, robotToAttack.location);
            }
        }

        return null;
    }

    public static void attack() throws GameActionException {
        RobotInfo[] robots = rc.senseNearbyRobots(myType.visionRadiusSquared);
        MapLocation rn = rc.getLocation();

        Direction bestDirection = Direction.CENTER;
        AttackInfo current = bestDamageFrom(rn, robots);
        double bestFactor = 0;
        if (current != null) {
            bestFactor = Math.pow(current.totalMaxDamage, EXP_DAMAGE_FACTOR) /
                    Math.pow(1 + rc.senseRubble(rn), EXP_RUBBLE_FACTOR);
        }

        if (rc.isMovementReady()) {
            for (int i = 8; --i >= 0;) {
                Direction d = directions[i];
                MapLocation ml = rn.add(d);
                AttackInfo info = bestDamageFrom(ml, robots);
                if (info == null) continue;
                double factor = Math.pow(info.totalMaxDamage, EXP_DAMAGE_FACTOR) /
                        Math.pow(1 + rc.senseRubble(ml), EXP_RUBBLE_FACTOR);
                if (bestFactor < factor) {
                    bestFactor = factor;
                    bestDirection = d;
                }
            }
        }

        if (bestFactor > 0) {
            AttackInfo best;
            if (MovementHelper.tryMove(bestDirection, true)) {
                best = bestDamageFrom(rc.getLocation(), rc.senseNearbyRobots(myType.actionRadiusSquared));
            } else {
                best = current;
            }
            if (best == null) {
                return;
            }
            switch (best.type) {
                case Fury:
                    if (rc.canEnvision(AnomalyType.FURY)) {
                        rc.envision(AnomalyType.FURY);
                        Sage.attackedThisRound = true;
                    }
                    break;
                case Charge:
                    if (rc.canEnvision(AnomalyType.CHARGE)) {
                        rc.envision(AnomalyType.CHARGE);
                        Sage.attackedThisRound = true;
                    }
                    break;
                case Attack:
                    if (rc.canAttack(best.attackLocation)) {
                        rc.attack(best.attackLocation);
                        Sage.attackedThisRound = true;
                    }
                    break;
            }
        }
    }
}
