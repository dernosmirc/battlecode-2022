package gen4;

import battlecode.common.*;

public strictfp class Sage {
	/**
	 *
	 * NOTE: (TO whoever writes sage code) :: Do check that if normal damage can deal more
	 * damage(or close death) to archon(-45), in comparison to Sage.envision(type = FURY)
	 */


	public static void run() throws GameActionException {
		Soldier.run();
	}

	public static void init() throws GameActionException {
		Soldier.init();
	}
}
