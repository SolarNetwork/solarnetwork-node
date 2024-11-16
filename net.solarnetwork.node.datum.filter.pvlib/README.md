# pvlib Datum Filter

## Developer Setup

Create a Python virtual environment one time, for example:

```sh
# create virtual environment
python3 -m venv ~/.python/venv/solarnode

# activate virtual environment
source ~/.python/venv/solarnode/bin/activate

# install deps
pip install -r def/requirements.txt
```

Each time you want to work with the environment in a new shell, execute

```sh
source ~/.python/venv/solarnode/bin/activate
```

You can execute the [ghi-to-poa.py](./def/ghi-to-poa.py) script like:

```sh
python def/ghi-to-poa.py --latitude -36.8509 --longitude 174.7645 \
  --zone Pacific/Auckland --array-tilt 2.5 --array-azimuth 170 \
  --date 2024-11-16T10:00 --irradiance 1000
```
