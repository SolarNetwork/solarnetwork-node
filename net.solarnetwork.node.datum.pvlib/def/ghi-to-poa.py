import getopt
import json
import pandas as pd
import sys

from pvlib import irradiance
from pvlib.location import Location

def ghi_get_irradiance(location: Location,
                       array_tilt: float,
                       array_azimuth: float,
                       ghi: float,
                       date: str,
                       min_cos_zenith=None,
                       max_zenith=None) -> dict:
    
    times = pd.DatetimeIndex([date])
    
    solar_position = location.get_solarposition(times=times)
    
    ghi_data = pd.Series([ghi], index=times)
    
    min_cos_zenith = 0.065 if min_cos_zenith is None else min_cos_zenith
    max_zenith = 87 if max_zenith is None else max_zenith
    
    erbs = irradiance.erbs(
        ghi = ghi,
        zenith = solar_position['apparent_zenith'],
        min_cos_zenith = min_cos_zenith,
        max_zenith = max_zenith,
        datetime_or_doy = times
    )
    
    dni_extra = irradiance.get_extra_radiation(times)
    
    poa = irradiance.get_total_irradiance(
        model = "haydavies",
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

opts, args = getopt.getopt(
    sys.argv[1:],
    'a:d:i:l:L:m:M:t:u:z:',
    ['altitude=', 'date=', 'irradiance=',
     'latitude=', 'longitude=', 
     'min-cos-zenith=', 'max-zenith=', 
     'array-tilt=', 'array-azimuth=', 'zone='],
)

lat = 0
lon = 0
alt = 0
zone = 'UTC'
array_azimuth = 0
array_tilt = 0

min_cos_zenith = None
max_zenith = None

ghi = 0
date = '2024-11-16T12:00:00'

for opt, arg in opts:
    if opt in ('-a', '--altitude'):
        alt = float(arg)
    elif opt in ('-d', '--date'):
        date = arg
    elif opt in ('-i', '--irradiance'):
        ghi = float(arg)
    elif opt in ('-l', '--latitude'):
        lat = float(arg)
    elif opt in ('-L', '--longitude'):
        lon = float(arg)
    elif opt in ('-m', '--min-cos-zenith'):
        min_cos_zenith = float(arg)
    elif opt in ('-M', '--max-zenith'):
        max_zenith = float(arg)
    elif opt in ('-t', '--array-tilt'):
        array_tilt = float(arg)
    elif opt in ('-u', '--array-azimuth'):
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
    date = date
)

print(json.dumps(poa))
