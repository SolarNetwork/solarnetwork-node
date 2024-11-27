import getopt
import json
import pandas as pd
import sys

from datetime import datetime
from datetime import timezone
from pvlib import irradiance
from pvlib.location import Location

def usage():
    print("""Usage:

-a --altitude        elevation above sea level, in meters
-d --date            date, like YYYY-MM-DDTHH:mm:ss
-i --irradiance      GHI irradiance, in W/m^2
-l --latitude        decimal latitude
-L --longitude       decimal longitude
-m --min-cos-zenith  optional minimum cos(zenith) value when calculating global clearness index
-M --max-zenith      optional maximum zenith value in DNI calculation
-t --array-tilt      solar array tilt angle from horizontal, in degrees
-T --transpose       the transposition model to use, e.g. 'haydavies', 'perez-driesse'
-u --array-azimuth   solar array angle clockwise from north
-z --zone            time zone, like Pacific/Auckland
""")

def ghi_get_irradiance(location: Location,
                       array_tilt: float,
                       array_azimuth: float,
                       ghi: float,
                       date: str,
                       min_cos_zenith=None,
                       max_zenith=None,
                       transposition_model='haydavies') -> dict:
    
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
    
    poa = irradiance.get_total_irradiance(
        model = transposition_model,
        surface_tilt = array_tilt,
        surface_azimuth = array_azimuth,
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

    return result

try:
    opts, args = getopt.getopt(
        sys.argv[1:],
        'a:d:i:l:L:m:M:t:T:u:z:',
        ['altitude=', 'date=', 'irradiance=',
        'latitude=', 'longitude=', 
        'min-cos-zenith=', 'max-zenith=', 
        'array-tilt=', 'transpose=',
        'array-azimuth=', 'zone='],
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

ghi = 0
date = datetime.now(timezone.utc).strftime('%Y-%m-%dT%H:%M:%S')

for opt, arg in opts:
    if opt in ('-a', '--altitude'): # m
        alt = float(arg)
    elif opt in ('-d', '--date'):
        date = arg
    elif opt in ('-i', '--irradiance'): # W/m2
        ghi = float(arg)
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
    transposition_model = model
)

print(json.dumps(poa))
