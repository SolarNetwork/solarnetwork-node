import getopt
import json
import math
import pandas as pd
import sys
import warnings

from datetime import datetime
from datetime import timezone
from pvlib import irradiance
from pvlib import tracking as pvtracking
from pvlib.location import Location

# suppress numpy/pvlib runtime warnings.
warnings.simplefilter('ignore')

def usage():
    print("""Usage:

-a --altitude        elevation above sea level, in meters
-A --max-angle       optional maximum tracker rotation angle from horizontal, in degrees,
                     0 exclusive to 180 (default 90)
-b --backtrack       optional 'true'/'false' to enable tracker backtracking (default false)
-d --date            date, like YYYY-MM-DDTHH:mm:ss
-g --gcr             optional ground coverage ratio, used for backtracking,
                     0 exclusive to 1 (default 0.2857)
-i --irradiance      GHI irradiance, in W/m^2
-k --tracking        'true'/'false' to enable single-axis tracker mode; when true
                     --array-tilt and --array-azimuth are ignored
-l --latitude        decimal latitude
-L --longitude       decimal longitude
-m --min-cos-zenith  optional minimum cos(zenith) value when calculating global clearness index
-M --max-zenith      optional maximum zenith value in DNI calculation
-t --array-tilt      solar array tilt angle from horizontal, in degrees
-T --transpose       the transposition model to use, e.g. 'haydavies', 'perez-driesse'
-u --array-azimuth   solar array angle clockwise from north
-x --axis-tilt       tracker axis tilt angle from horizontal, in degrees, 0 to 90 (default 0)
-X --axis-azimuth    tracker axis angle clockwise from north, in degrees, 0 to 360 (default 0)
-z --zone            time zone, like Pacific/Auckland
""")

def invalid_value(message: str):
    print(message, file=sys.stderr)
    sys.exit(2)

def parse_bool(opt: str, s: str) -> bool:
    v = s.strip().lower()
    if v in ('true', '1', 'yes', 'y'):
        return True
    if v in ('false', '0', 'no', 'n'):
        return False
    invalid_value("%s: invalid boolean value '%s'" % (opt, s))

def parse_ranged_float(opt: str, s: str, lo: float, hi: float, lo_exclusive=False) -> float:
    try:
        v = float(s)
    except ValueError:
        v = math.nan
    if not math.isfinite(v) or v > hi or (v <= lo if lo_exclusive else v < lo):
        invalid_value("%s: value '%s' not a number between %s%s and %s"
                      % (opt, s, lo, ' (exclusive)' if lo_exclusive else '', hi))
    return v

def ghi_get_irradiance(location: Location,
                       array_tilt: float,
                       array_azimuth: float,
                       ghi: float,
                       date: str,
                       min_cos_zenith=None,
                       max_zenith=None,
                       transposition_model='haydavies',
                       tracking=False,
                       axis_tilt=0,
                       axis_azimuth=0,
                       max_angle=90,
                       backtrack=False,
                       gcr=2.0/7.0) -> dict:

    times = pd.DatetimeIndex(data = [date], tz = location.tz)

    solar_position = location.get_solarposition(times=times)

    ghi_data = pd.Series([ghi], index=times)

    min_cos_zenith = 0.065 if min_cos_zenith is None else min_cos_zenith
    max_zenith = 87 if max_zenith is None else max_zenith

    erbs = irradiance.erbs(
        ghi = ghi_data,
        zenith = solar_position['apparent_zenith'],
        min_cos_zenith = min_cos_zenith,
        max_zenith = max_zenith,
        datetime_or_doy = times
    )

    dni_extra = irradiance.get_extra_radiation(times)

    tracker = None
    if tracking:
        # single-axis tracker: derive the panel orientation from the sun
        # position; sun below the horizon produces NaN, fall back to flat
        tracker = pvtracking.singleaxis(
            apparent_zenith = solar_position['apparent_zenith'],
            apparent_azimuth = solar_position['azimuth'],
            axis_tilt = axis_tilt,
            axis_azimuth = axis_azimuth,
            max_angle = max_angle,
            backtrack = backtrack,
            gcr = gcr).fillna(0)
        surface_tilt = tracker['surface_tilt']
        surface_azimuth = tracker['surface_azimuth']
    else:
        surface_tilt = array_tilt
        surface_azimuth = array_azimuth

    poa = irradiance.get_total_irradiance(
        model = transposition_model,
        surface_tilt = surface_tilt,
        surface_azimuth = surface_azimuth,
        dni = erbs['dni'],
        dhi = erbs['dhi'],
        dni_extra = dni_extra,
        ghi = ghi_data,
        solar_azimuth = solar_position['azimuth'],
        solar_zenith = solar_position['apparent_zenith']
        )

    # transpose single row (timestamp) into into simple dictionary
    result = {'date': date,
              'zone': location.tz,
              'ghi': ghi,
              'dni': erbs['dni'].iloc[0],
              'dhi': erbs['dhi'].iloc[0],
              'zenith': solar_position['apparent_zenith'].iloc[0],
              'azimuth': solar_position['azimuth'].iloc[0],
              'min_cos_zenith': min_cos_zenith,
              'max_zenith': max_zenith,
              }
    for d in poa:
        for r in poa[d]:
            result.update({d: r})

    if tracker is not None:
        result.update({'tracker_theta': tracker['tracker_theta'].iloc[0],
                       'aoi': tracker['aoi'].iloc[0],
                       'surface_tilt': tracker['surface_tilt'].iloc[0],
                       'surface_azimuth': tracker['surface_azimuth'].iloc[0],
                       })

    return result

try:
    opts, args = getopt.getopt(
        sys.argv[1:],
        'a:A:b:d:g:i:k:l:L:m:M:t:T:u:x:X:z:',
        ['altitude=', 'date=', 'irradiance=',
        'latitude=', 'longitude=',
        'min-cos-zenith=', 'max-zenith=',
        'array-tilt=', 'transpose=',
        'array-azimuth=', 'zone=',
        'tracking=', 'axis-tilt=', 'axis-azimuth=',
        'max-angle=', 'backtrack=', 'gcr='],
    )
except getopt.GetoptError as e:
    print(e)
    usage()
    sys.exit(2)

lat = 0
lon = 0
alt = 0
zone = 'UTC'
array_azimuth = 0
array_tilt = 0

min_cos_zenith = None
max_zenith = None
model = 'haydavies'

tracking = False
axis_tilt = 0
axis_azimuth = 0
max_angle = 90
backtrack = False
gcr = 2.0/7.0

ghi = 0
date = datetime.now(timezone.utc).strftime('%Y-%m-%dT%H:%M:%S')

for opt, arg in opts:
    if opt in ('-a', '--altitude'): # m
        alt = float(arg)
    elif opt in ('-A', '--max-angle'): # angle in degrees
        max_angle = parse_ranged_float(opt, arg, 0, 180, lo_exclusive=True)
    elif opt in ('-b', '--backtrack'):
        backtrack = parse_bool(opt, arg)
    elif opt in ('-d', '--date'):
        date = arg
    elif opt in ('-g', '--gcr'):
        gcr = parse_ranged_float(opt, arg, 0, 1, lo_exclusive=True)
    elif opt in ('-i', '--irradiance'): # W/m2
        ghi = float(arg)
    elif opt in ('-k', '--tracking'):
        tracking = parse_bool(opt, arg)
    elif opt in ('-l', '--latitude'):
        lat = float(arg)
    elif opt in ('-L', '--longitude'):
        lon = float(arg)
    elif opt in ('-m', '--min-cos-zenith'):
        min_cos_zenith = float(arg)
    elif opt in ('-M', '--max-zenith'):
        max_zenith = float(arg)
    elif opt in ('-t', '--array-tilt'): # angle in degrees
        array_tilt = float(arg)
    elif opt in ('-T', '--transpose'):
        model = arg
    elif opt in ('-u', '--array-azimuth'): # angle in degrees
        array_azimuth = float(arg)
    elif opt in ('-x', '--axis-tilt'): # angle in degrees
        axis_tilt = parse_ranged_float(opt, arg, 0, 90)
    elif opt in ('-X', '--axis-azimuth'): # angle in degrees
        axis_azimuth = parse_ranged_float(opt, arg, 0, 360)
    elif opt in ('-z', '--zone'):
        zone = arg

loc = Location(lat, lon, tz=zone, altitude=alt)

poa = ghi_get_irradiance(
    location = loc,
    array_tilt = array_tilt,
    array_azimuth = array_azimuth,
    min_cos_zenith = min_cos_zenith,
    max_zenith = max_zenith,
    ghi = ghi,
    date = date,
    transposition_model = model,
    tracking = tracking,
    axis_tilt = axis_tilt,
    axis_azimuth = axis_azimuth,
    max_angle = max_angle,
    backtrack = backtrack,
    gcr = gcr
)

print(json.dumps(poa))
