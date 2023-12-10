import hashlib
import time
from pathlib import Path

import requests
from flask import Flask, request

app = Flask(__name__)

caches = {}

jcdecaux_cache_dir = Path(__file__).parent / 'cache' / 'jcdecaux'
if not jcdecaux_cache_dir.exists():
    jcdecaux_cache_dir.mkdir(parents=True)


# Cache jcdecaux
@app.get('/jcdecaux/<path:path>')
def jcdecaux_cache(path):
    # Get params
    request_args = request.args

    # Get the cache file path (md5 hash of the path)
    cache_file = jcdecaux_cache_dir / hashlib.md5(path.encode()).hexdigest()

    # If the cache file exists, return it
    if cache_file.exists() and time.time() - caches.get(cache_file, 0) < 60:
        return cache_file.read_text()

    # Otherwise, get the data from the API
    res = requests.get(f'https://api.jcdecaux.com/vls/v3/{path}', params=request_args)
    print(res.url)
    data = res.text

    print(f'https://api.jcdecaux.com/vls/v3/{path}')

    # Write the data to the cache file
    cache_file.write_text(data)
    caches[cache_file] = time.time()

    # Return the data
    return data


if __name__ == '__main__':
    app.run()
