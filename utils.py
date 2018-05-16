import numpy as np
import math
from datetime import datetime, timedelta

def read_filedata(filename):
    """
    Returns list of tuple consisting - `timestamp`, `x`, `y`, `z`
    [
    (`timestamp`,`x`,`y`,`z`), ...
    ]
    """
    with open(filename) as f:
        lines = f.readlines()
        # line.strip().split()
        # will give `timestamp` `x` `y` `z`
        lines = [line.strip().split() for line in lines]

        # Some data has less datapoints because the output was not flushed
        # So we only filter out normal ones
        lines = [line for line in lines if len(line) == 4]
        lines = [(datetime.fromtimestamp(float(ts)/1000), float(x), float(y), float(z)) for (ts, x, y, z) in lines]
    return lines

def preprocess(data, seconds=10):
    #print("original data points: ", len(data))
    # Remove duplicates
    no_dup = []
    for idx, line in enumerate(data):
        ts, x, y, z = line
        if 0 <= idx-1 and ts == data[idx-1][0]:
            continue
        else:
            no_dup.append(line)
    #print("Data points after removing duplicates", len(no_dup))

    # Remove n seconds from start / end
    delta = timedelta(days=0,seconds=seconds)
    start_time = no_dup[0][0] + delta
    end_time = no_dup[-1][0] - delta
    no_dup = [(ts, x, y, z) for (ts, x, y, z) in no_dup if start_time <= ts <= end_time]
    return no_dup


def interpolation(time, x, y, z, unit):
    timeStart = math.ceil(time[0] / unit) * unit
    timeEnd = math.ceil(time[len(time) - 1] / unit) * unit

    interpTime = np.arange(timeStart, timeEnd, unit)
    interpX = np.interp(interpTime, time, x)
    interpY = np.interp(interpTime, time, y)
    interpZ = np.interp(interpTime, time, z)

    return (interpTime, interpX, interpY, interpZ)

