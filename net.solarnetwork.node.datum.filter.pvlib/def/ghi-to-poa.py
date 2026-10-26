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
from pvlib.bifacial import infinite_sheds
from pvlib.location import Location

# transposition models supported by the infinite sheds bifacial model (see the
# `model` parameter at
# https://pvlib-python.readthedocs.io/en/v0.16.1/reference/generated/pvlib.bifacial.infinite_sheds.get_irradiance.html);
# any other model falls back to the first one in bifacial mode
BIFACIAL_MODELS = ('haydavies', 'isotropic')

# suppress numpy/pvlib runtime warnings.
warnings.simplefilter('ignore')

def usage():
    print("""Usage:

-a --altitude        elevation above sea level, in meters
-A --max-angle       optional maximum tracker rotation angle from horizontal, in degrees,
                     0 exclusive to 180 (default 90)
-b --backtrack       optional 'true'/'false' to enable tracker backtracking (default false)
-B --bifacial        optional 'true'/'false' to enable bifacial (infinite sheds) mode;
                     transposition models other than 'haydavies' or 'isotropic' fall
                     back to 'haydavies' (default false)
-c --bifaciality     optional ratio of rear to front module efficiency, 0 to 1
                     (default 0.8); bifacial mode only
-d --date            date, like YYYY-MM-DDTHH:mm:ss
-e --albedo          optional ground albedo, 0 to 1 (default 0.25)
-g --gcr             optional ground coverage ratio, used for backtracking and
                     bifacial mode, 0 exclusive to 1 (default 0.2857)
-H --height          optional height of the row centre above ground, same units as
                     --pitch, 0 exclusive to 100 (default 1.5); bifacial mode only
-i --irradiance      GHI irradiance, in W/m^2
-k --tracking        'true'/'false' to enable single-axis tracker mode; when true
                     --array-tilt and --array-azimuth are ignored
-l --latitude        decimal latitude
-L --longitude       decimal longitude
-m --min-cos-zenith  optional minimum cos(zenith) value when calculating global clearness index
-M --max-zenith      optional maximum zenith value in DNI calculation
-p --pitch           optional distance between rows, same units as --height,
                     0 exclusive to 1000 (default 5); bifacial mode only
-r --transmission-factor
                     optional fraction of rear irradiance lost by transmission through
                     the module, -1 to 0 (default 0); bifacial mode only
-s --shade-factor    optional fraction of rear irradiance lost to shading by racking,
                     -1 to 0 (default -0.02); bifacial mode only
-t --array-tilt      solar array tilt angle from horizontal, in degrees
-T --transpose       the transposition model to use, e.g. 'haydavies', 'perez-driesse'
-u --array-azimuth   solar array angle clockwise from north
-x --axis-tilt       tracker axis tilt angle from horizontal, in degrees, 0 to 90 (default 0);
                     must be 0 in bifacial mode
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
                       gcr=2.0/7.0,
                       albedo=None,
                       bifacial=False,
                       bifaciality=0.8,
                       height=1.5,
                       pitch=5.0,
                       shade_factor=-0.02,
                       transmission_factor=0.0) -> dict:

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
    elif bifacial and array_tilt < 0:
        # infinite sheds does not handle a negative tilt consistently across
        # pvlib versions: use the equivalent positive tilt facing the other way
        surface_tilt = -array_tilt
        surface_azimuth = (array_azimuth + 180) % 360
    else:
        surface_tilt = array_tilt
        surface_azimuth = array_azimuth

    if bifacial:
        # infinite sheds: front and rear irradiance of interior rows, including
        # row-to-row shading and restricted sky/ground views; it supports only
        # some transposition models, so fall back to haydavies for the others
        if transposition_model not in BIFACIAL_MODELS:
            transposition_model = BIFACIAL_MODELS[0]
        sheds = infinite_sheds.get_irradiance(
            surface_tilt = surface_tilt,
            surface_azimuth = surface_azimuth,
            solar_zenith = solar_position['apparent_zenith'],
            solar_azimuth = solar_position['azimuth'],
            gcr = gcr,
            height = height,
            pitch = pitch,
            ghi = ghi_data,
            dhi = erbs['dhi'],
            dni = erbs['dni'],
            albedo = 0.25 if albedo is None else albedo,
            model = transposition_model,
            dni_extra = dni_extra,
            bifaciality = bifaciality,
            shade_factor = shade_factor,
            transmission_factor = transmission_factor)
        # keep the standard keys: poa_global is the effective total (front plus
        # rear weighted by bifaciality, shade and transmission factors), the
        # component keys describe the front surface
        poa = pd.DataFrame({
            'poa_global': sheds['poa_global'],
            'poa_direct': sheds['poa_front_direct'],
            'poa_diffuse': sheds['poa_front_diffuse'],
            'poa_sky_diffuse': sheds['poa_front_sky_diffuse'],
            'poa_ground_diffuse': sheds['poa_front_ground_diffuse'],
            'poa_front': sheds['poa_front'],
            'poa_back': sheds['poa_back'],
            'shaded_fraction_front': sheds['shaded_fraction_front'],
            })
    else:
        extra = {} if albedo is None else {'albedo': albedo}
        poa = irradiance.get_total_irradiance(
            model = transposition_model,
            surface_tilt = surface_tilt,
            surface_azimuth = surface_azimuth,
            dni = erbs['dni'],
            dhi = erbs['dhi'],
            dni_extra = dni_extra,
            ghi = ghi_data,
            solar_azimuth = solar_position['azimuth'],
            solar_zenith = solar_position['apparent_zenith'],
            **extra
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

    if albedo is not None:
        result['albedo'] = albedo
    if bifacial:
        result.update({'bifaciality': bifaciality,
                       'gcr': gcr,
                       'height': height,
                       'pitch': pitch,
                       'shade_factor': shade_factor,
                       'transmission_factor': transmission_factor,
                       'transposition_model': transposition_model,
                       })

    # JSON has no NaN/Infinity: replace any non-finite number with 0
    for k, v in result.items():
        if isinstance(v, float) and not math.isfinite(v):
            result[k] = 0.0

    return result

try:
    opts, args = getopt.getopt(
        sys.argv[1:],
        'a:A:b:B:c:d:e:g:H:i:k:l:L:m:M:p:r:s:t:T:u:x:X:z:',
        ['altitude=', 'date=', 'irradiance=',
        'latitude=', 'longitude=',
        'min-cos-zenith=', 'max-zenith=',
        'array-tilt=', 'transpose=',
        'array-azimuth=', 'zone=',
        'tracking=', 'axis-tilt=', 'axis-azimuth=',
        'max-angle=', 'backtrack=', 'gcr=',
        'bifacial=', 'bifaciality=', 'albedo=', 'height=', 'pitch=',
        'shade-factor=', 'transmission-factor='],
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

albedo = None
bifacial = False
bifaciality = 0.8
height = 1.5
pitch = 5.0
shade_factor = -0.02
transmission_factor = 0.0

ghi = 0
date = datetime.now(timezone.utc).strftime('%Y-%m-%dT%H:%M:%S')

for opt, arg in opts:
    if opt in ('-a', '--altitude'): # m
        alt = float(arg)
    elif opt in ('-A', '--max-angle'): # angle in degrees
        max_angle = parse_ranged_float(opt, arg, 0, 180, lo_exclusive=True)
    elif opt in ('-b', '--backtrack'):
        backtrack = parse_bool(opt, arg)
    elif opt in ('-B', '--bifacial'):
        bifacial = parse_bool(opt, arg)
    elif opt in ('-c', '--bifaciality'):
        bifaciality = parse_ranged_float(opt, arg, 0, 1)
    elif opt in ('-d', '--date'):
        date = arg
    elif opt in ('-e', '--albedo'):
        albedo = parse_ranged_float(opt, arg, 0, 1)
    elif opt in ('-g', '--gcr'):
        gcr = parse_ranged_float(opt, arg, 0, 1, lo_exclusive=True)
    elif opt in ('-H', '--height'):
        height = parse_ranged_float(opt, arg, 0, 100, lo_exclusive=True)
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
    elif opt in ('-p', '--pitch'):
        pitch = parse_ranged_float(opt, arg, 0, 1000, lo_exclusive=True)
    elif opt in ('-r', '--transmission-factor'):
        transmission_factor = parse_ranged_float(opt, arg, -1, 0)
    elif opt in ('-s', '--shade-factor'):
        shade_factor = parse_ranged_float(opt, arg, -1, 0)
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

if bifacial:
    # infinite sheds memory/time grows with height/pitch
    if height / pitch > 10:
        invalid_value("--height: value %s more than 10 times --pitch %s" % (height, pitch))
    # the lower module edge must be above ground at the steepest tilt; a tracker
    # can rotate up to max_angle, which is steepest at 90
    tilt = min(max_angle, 90) if tracking else array_tilt
    drop = gcr * pitch / 2 * abs(math.sin(math.radians(tilt)))
    if height <= drop or math.isclose(height, drop):
        invalid_value("--height: value %s not above lower module edge drop %s (--gcr %s, --pitch %s, tilt %s)"
                      % (height, round(drop, 4), gcr, pitch, tilt))
    # infinite sheds assumes a horizontal tracker axis on level ground
    if tracking and axis_tilt != 0:
        invalid_value("--axis-tilt: value %s not supported in bifacial mode, must be 0" % axis_tilt)

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
    gcr = gcr,
    albedo = albedo,
    bifacial = bifacial,
    bifaciality = bifaciality,
    height = height,
    pitch = pitch,
    shade_factor = shade_factor,
    transmission_factor = transmission_factor
)

print(json.dumps(poa))
