package gen8.common.util;

import battlecode.common.Clock;

import static gen8.RobotPlayer.*;

public class Logger {
    private final int start, startRN;
    private int prev, prevRN;
    private final StringBuilder pre = new StringBuilder(), output = new StringBuilder();
    private int totalLogs = 0;
    private final LogCondition logCondition;

    // LogCounts is total number of logs for a class including total.
    public Logger (String title, LogCondition condition) {
        start = prev = Clock.getBytecodeNum();
        startRN = prevRN = rc.getRoundNum();
        logCondition = condition;
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

    public int getTotal() {
        return Clock.getBytecodeNum() - start + (rc.getRoundNum() - startRN) * myType.bytecodeLimit;
    }

    public void flush () {
        if (DEBUG || logCondition == LogCondition.OverrideDebug) {
            int total = getTotal();
            switch (logCondition) {
                case ExceedsRound:
                    if (startRN == rc.getRoundNum()) {
                        break;
                    }
                case ExceedsBytecode:
                    if (total < myType.bytecodeLimit) {
                        break;
                    }
                case Always:
                case OverrideDebug:
                    log("end\t\t\t");
                    output.append("total ").append(total);
                    pre.append(totalLogs).append(output);
                    System.out.println(pre);
            }
        }
    }
}
