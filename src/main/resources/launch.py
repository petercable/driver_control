"""
Usage:
    launch.py <base_path> <egg_path>

"""
# parser.add_argument("-c", "--command_port_file", dest='command', help="command port file" )
# parser.add_argument("-e", "--event_port_file", dest='event', help="event port file" )
# parser.add_argument("-p", "--ppid", dest='ppid', required=False, type=int, help="parent process id, if this PPID doesn't exist die" )

import os.path
import os
import sys
import shutil
import docopt
import zipfile

eggs = [
    'APScheduler-2.1.0-py2.7.egg',
    'ntplib-0.3.2-py2.7.egg',
    'snakefood-1.4-py2.7.egg',
    'gitpy-0.6.0-py2.7.egg',
    'pytz-2014.4-py2.7.egg',
    'beautifulsoup4-4.2.1-py2.7.egg',
    'udunitspy-0.0.6-py2.7-macosx-10.9-intel.egg',
    'pyproj-1.9.3-py2.7-macosx-10.9-intel.egg',
    'xlwt-0.7.5-py2.7.egg',
    'xlrd-0.8.0-py2.7.egg',
    'pyparsing-1.5.6-py2.7.egg',
    'netCDF4-1.0.9-py2.7-macosx-10.9-intel.egg',
    'Pydap-3.3.RC1-py2.7.egg',
    'matplotlib-1.1.1-py2.7-macosx-10.9-intel.egg',
    'pygsw-0.0.10-py2.7-macosx-10.9-intel.egg',
    'seawater-2.0.1-py2.7.egg',
    'WebTest-1.4.0-py2.7.egg',
    'python_dateutil-1.5-py2.7.egg',
    'Flask-0.9-py2.7.egg',
    'pyzmq-2.2.0-py2.7-macosx-10.7-intel.egg',
    'pytest_cov-1.6-py2.7.egg',
    'pytest-2.3.2-py2.7.egg',
    'numexpr-2.1-py2.7-macosx-10.9-intel.egg',
    'PasteDeploy-1.5.2-py2.7.egg',
    'PasteScript-1.7.5-py2.7.egg',
    'Paste-1.7.5.1-py2.7.egg',
    'Genshi-0.7-py2.7-macosx-10.9-intel.egg',
    'httplib2-0.9-py2.7.egg',
    'WebOb-1.4-py2.7.egg',
    'Jinja2-2.7.3-py2.7.egg',
    'Werkzeug-0.9.6-py2.7.egg',
    'psutil-1.0.1-py2.7-macosx-10.9-intel.egg',
    'requests-2.3.0-py2.7.egg',
    'lxml-2.3.4-py2.7-macosx-10.9-intel.egg',
    'python_gevent_profiler-0.2-py2.7.egg',
    'ndg_xacml-0.5.1-py2.7.egg',
    'mock-0.8.0-py2.7.egg',
    'readline-6.2.4.1-py2.7-macosx-10.7-intel.egg',
    'antlr_python_runtime-3.1.3-py2.7.egg',
    'ipython-0.13-py2.7.egg',
    'nose-1.1.2-py2.7.egg',
    'coverage-3.7.1-py2.7-macosx-10.9-intel.egg',
    'M2Crypto-0.21.1_pl1-py2.7-macosx-10.9-intel.egg',
    'python_daemon-1.6-py2.7.egg',
    'psycopg2-2.5.3-py2.7-macosx-10.9-intel.egg',
    'CouchDB-0.9-py2.7.egg',
    'zope.interface-4.1.1-py2.7-macosx-10.9-intel.egg',
    'gevent_zeromq-0.2.5-py2.7.egg',
    'pika-0.9.5-py2.7.egg',
    'msgpack_python-0.1.13-py2.7-macosx-10.9-intel.egg',
    'simplejson-3.5.3-py2.7-macosx-10.9-intel.egg',
    'gevent-0.13.7-py2.7-macosx-10.9-intel.egg',
    'greenlet-0.4.0-py2.7-macosx-10.9-intel.egg',
    'utilities-2013.06.11-py2.7.egg',
    'scipy-0.11.0-py2.7-macosx-10.9-intel.egg',
    'geomag-0.9-py2.7.egg',
    'networkx-1.7-py2.7.egg',
    'pydot-1.0.28-py2.7.egg',
    'gsw-3.0.1a1-py2.7.egg',
    'pidantic-0.1.3-py2.7.egg',
    'h5py-2.2.0-py2.7-macosx-10.9-intel.egg',
    'cov_core-1.13.0-py2.7.egg',
    'py-1.4.20-py2.7.egg',
    'MarkupSafe-0.23-py2.7-macosx-10.9-intel.egg',
    'lockfile-0.9.1-py2.7.egg',
    'graypy-0.2.10-py2.7.egg',
    'PyYAML-3.10-py2.7-macosx-10.9-intel.egg',
    'SQLAlchemy-0.7.6-py2.7-macosx-10.9-intel.egg',
    'supervisor-3.0-py2.7.egg',
    'meld3-1.0.0-py2.7.egg',
]

externs = [
    'coi-services',
    'pyon',
    'ion-functions',
    'coverage-model',
]


def set_path(base_path):
    for egg in eggs:
        sys.path.insert(0, os.path.join(base_path, 'eggs', egg))
    for extern in externs:
        sys.path.insert(0, os.path.join(base_path, 'extern', extern))


def patch_zmq_driver():
    """
    Patch an unpacked egg to use JSON instead of PYOBJ.

    assumes we are already in the directory containing the egg to be patched
    :return:
    """
    os.system('sed -i bak s/pyobj/json/g mi/core/instrument/zmq_driver_process.py')
    os.system('sed -i bak s/INFO/DEBUG/g res/config/mi-logging.yml')


def betterParseArgs(*args, **kwargs):
    class OptDiviner():
        command = '/tmp/command_port'
        event = '/tmp/event_port'
        ppid = None

    return OptDiviner

def main():
    import tempfile
    temp_dir = tempfile.mkdtemp()
    sys.path.append(temp_dir)
    print sys.path

    args = docopt.docopt(__doc__, version='egg trebuchet 0.1')

    set_path(args['<base_path>'])
    os.chdir(temp_dir)
    zipfile.ZipFile(args['<egg_path>']).extractall()
    patch_zmq_driver()

    import mi.main

    mi.main.parseArgs = betterParseArgs
    mi.main.run('/tmp/event_port', '/tmp/command_port')

    shutil.rmtree(temp_dir)


if __name__ == '__main__':
    main()
