"""Exercise the actual web package file generator without reading private config."""
import pathlib
import subprocess
import sys
import tempfile

ROOT = pathlib.Path(__file__).resolve().parents[1]
sys.path.insert(0, str(ROOT / 'tools'))
import webapp

app = webapp.files(dict(webapp.GENERIC), 'web-i18n regression')
with tempfile.TemporaryDirectory(prefix='freemytv-i18n-') as directory:
    for name in ('i18n.js', 'strings.js', 'home.js'):
        pathlib.Path(directory, name).write_bytes(app[name])
    subprocess.run(['node', str(ROOT / 'tests/web-i18n.cjs'), directory], cwd=ROOT, check=True)
