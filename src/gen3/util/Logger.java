package gen3.util;

import battlecode.common.Clock;

import static gen3.RobotPlayer.*;

public class Logger {
    private final int start, startRN;
    private int prev, prevRN;
    private final StringBuilder pre = new StringBuilder(), output = new StringBuilder();
    private int totalLogs = 0;
    private final boolean flushOnlyIfExceeds;

    // LogCounts is total number of logs for a class including total.
    public Logger (String title, boolean flushOnlyIfExceeds) {
        start = prev = Clock.getBytecodeNum();
        startRN = prevRN = rc.getRoundNum();
        this.flushOnlyIfExceeds = flushOnlyIfExceeds;
        if (DEBUG) {
            pre.append("Log_Start@").append(prevRN).append(" ");
            output.append(" ").append(title).append('\n');
        }
    }

    public void log (String event) {
        if (DEBUG) {
            totalLogs++;
            output.append(event).append("\t:")
                    .append(Clock.getBytecodeNum() - prev + (rc.getRoundNum() - prevRN) * myType.bytecodeLimit).append('\n');
            prev = Clock.getBytecodeNum();
            prevRN = rc.getRoundNum();
        }
    }

    public void flush () {
        if (DEBUG) {
            if (!flushOnlyIfExceeds || startRN < rc.getRoundNum()) {
                log("end\t\t\t");
                output.append("total ")
                        .append(Clock.getBytecodeNum() - start + (rc.getRoundNum() - startRN) * myType.bytecodeLimit);
                pre.append(totalLogs).append(output.toString());
                System.out.println(pre.toString());
            }
        }
    }
}
