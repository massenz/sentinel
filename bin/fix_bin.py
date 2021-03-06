#!/usr/bin/env python
#
# Copyright 2015 (c) AlertAvert.com. All rights reserved.
# Commercial use or modification of this software without a valid license is expressly forbidden

"""
Replaces the classpath generated by the sbt docker plugin to add the `/etc/sentinel` folder
(and the WORKDIR `/opt/sentinel` too) so that we can edit the configuration files
(`application.conf` and `override.conf.sample`) directly in the container and restart the server
during development.

Even more importantly, we can inject those files at deployment, and the configuration will be
picked up when the server starts (otherwise we would have to re-build the package and the
container image every time we want to change the configuration).

See the `build-sentinel-server.py` script for more details.
"""

import argparse
import logging

# The `sh` module is super-useful, but not part of the standard python library.
try:
    from sh import chmod, cp, mv, ErrorReturnCode
except ImportError:
    print("Missing `sh` module; please install with `pip install sh` (use of virtualenv "
          "is strongly recommended)")
    exit(1)

from tempfile import mkstemp


__author__ = 'Marco Massenzio'
__email__ = 'marco@alertavert.com'


LOG_FORMAT = '%(asctime)s [%(levelname)-5s] %(message)s'
LINE_REPLACEMENT = """declare -r app_classpath="/etc/sentinel:/opt/sentinel/conf:$lib_dir/*" """


def parse_args():
    """ Parse command line arguments and returns a configuration object.

    @return: the configuration object, arguments accessed via dotted notation
    @rtype: dict
    """
    parser = argparse.ArgumentParser()
    parser.add_argument('--binfile', '-f', required=True, help="The file to parse and fix.")
    parser.add_argument('--outfile', '-o', help="The file to emit; optional, if missing "
                                                "--binfile will be replaced instead.")
    parser.add_argument('-v', dest='verbose', help="Info-level logging for interactive usage."
                                                   "If not specified only errors are logged",
                        action='store_true')
    return parser.parse_args()


def fix_binfile(src, dest=None):
    _, outfile = mkstemp()
    logging.info("Updating {} (writing temporary file to {}).".format(src, outfile))

    with open(outfile, 'w') as outf:
        with open(src) as inf:

            for line in inf:
                if line.startswith('declare -r app_classpath='):
                    outf.write(LINE_REPLACEMENT)
                else:
                    outf.write(line)

    if not dest:
        infile_bak = '.'.join([src, 'orig'])
        logging.warning("Overwriting original file {} (backup copy kept in {})".format(
            src, infile_bak))
        try:
            cp(src, infile_bak)
            dest = src
        except ErrorReturnCode as error:
            logging.error("Failed to make backup copy of {}; did you have the necessary "
                          "permissions? (Error: {})".format(src, error.stderr))
            exit(1)

    mv(outfile, dest)
    chmod('ug+x', dest)


def main(cfg):
    fix_binfile(cfg.binfile, cfg.outfile)


if __name__ == '__main__':
    config = parse_args()
    logging.basicConfig(level=logging.WARN if not config.verbose else logging.DEBUG,
                        format=LOG_FORMAT, datefmt="%Y-%m-%d %H:%M:%S")
    main(config)
