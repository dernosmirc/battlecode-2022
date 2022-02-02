package gen7.sage;

import battlecode.common.*;
import gen7.common.MovementHelper;

import static gen7.RobotPlayer.*;

public class SageAttackHelper {


    // ARCHON
    // LABORATORY
    // WATCHTOWER
    // MINER
    // BUILDER
    // SOLDIER
    // SAGE
    private static final int[] priority = {3, 1, 4, 0, 2, 5, 6};

    private static final int SAGE_ATTACK_THRESHOLD = 1;
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
        if (!rc.onTheMap(ml) || rc.canSenseRobotAtLocation(ml)) return null;

        int maxPriority = -1;
        int furyDamage = 0, chargeDamage = 0, attackDamage = 0;
        RobotInfo robotToAttack = null;
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

            if (robot.mode == RobotMode.TURRET) {
                furyDamage += Math.min(robot.health, robot.type.getMaxHealth(robot.level)/10);
            } else if (robot.mode == RobotMode.DROID && robot.type.canAttack()) {
                chargeDamage += Math.min(robot.health, (robot.type.getMaxHealth(robot.level) * 22)/100);
            }
            int typeIndex = robot.type.ordinal();
            int p = priority[typeIndex], damage = Math.min(robot.health, RobotType.SAGE.damage);
            if (p > maxPriority) {
                maxPriority = p;
                attackDamage = damage;
                robotToAttack = robot;
            } else if (p == maxPriority && damage > attackDamage) {
                attackDamage = damage;
                robotToAttack = robot;
            }
        }

        if (furyDamage > 0 && furyDamage >= attackDamage && furyDamage >= chargeDamage) {
            return new AttackInfo(AttackType.Fury, furyDamage);
        }

        if (chargeDamage >= SAGE_ATTACK_THRESHOLD && chargeDamage >= attackDamage && chargeDamage >= furyDamage) {
            return new AttackInfo(AttackType.Charge, chargeDamage);
        }

        if (maxPriority != -1 && attackDamage >= SAGE_ATTACK_THRESHOLD) {
            return new AttackInfo(AttackType.Attack, attackDamage, robotToAttack.location);
        }

        return null;
    }

    public static void attack() throws GameActionException {
        RobotInfo[] robots = rc.senseNearbyRobots(myType.visionRadiusSquared);
        MapLocation rn = rc.getLocation();

        Direction bestDirection = Direction.CENTER;
        AttackInfo current = bestDamageFrom(rn, robots);
        double bestFactor = 0;
        AttackInfo best = null;
        if (current != null) {
            bestFactor = Math.pow(current.totalMaxDamage, EXP_DAMAGE_FACTOR) /
                    Math.pow(1 + rc.senseRubble(rn), EXP_RUBBLE_FACTOR);
            best = current;
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
                    best = info;
                }
            }
        }

        if (bestFactor > 0) {
            if (!MovementHelper.tryMove(bestDirection, true)) {
                best = current;
            }
            if (best == null) {
                return;
            }
            switch (best.type) {
                case Fury:
                    if (rc.canEnvision(AnomalyType.FURY)) {
                        rc.envision(AnomalyType.FURY);
                    }
                    break;
                case Charge:
                    if (rc.canEnvision(AnomalyType.CHARGE)) {
                        rc.envision(AnomalyType.CHARGE);
                    }
                    break;
                case Attack:
                    if (rc.canAttack(best.attackLocation)) {
                        rc.attack(best.attackLocation);
                    }
                    break;
            }
        }
    }
}
