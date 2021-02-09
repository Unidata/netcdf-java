#!/usr/bin/env python
import csv
import datetime as dt
from collections import defaultdict, namedtuple
from itertools import chain
from os import listdir, remove
from os.path import exists, join

debug = False

def parse_table_version(fname):
    # filenames need to look like UserTable_MRMS_v<version>.csv
    return fname.split('S_v', 1)[1].replace('.csv', '')

def parse_file(fname):
    table_version = parse_table_version(fname)
    with open(fname, 'r') as infile:
        reader = csv.reader(infile)
        ret = []
        cols = next(reader)
        cols = [c.replace(' ', '_') for c in cols]
        cols.append('TableVersion')
        RowEntry = namedtuple('Row', cols)
        for row in reader:
            row.append(table_version)
            ret.append(RowEntry(*row))
        return ret

def fix_item(item):
    # To be recognized as a UDUNITS compatible unit, we should fix units that:
    #   - have AGL/MSL in them (add to description instead)
    #   - are index, non-dim, or flag (should be dimensionless)
    #   - are C (use degree_Celsius instead, as C is coulomb, and these fields really are temperature related)
    #   - are years (udunits 1.x says plural ok, but not udunits 2.x, so go with singular year)
    #   - use flashes (use count instead of flashes)
    if item.Unit.endswith(' MSL') or item.Unit.endswith(' AGL'):
        item = item._replace(Unit=item.Unit[:-4],
                Description=item.Description + item.Unit[-4:])
    elif item.Unit == 'non-dim' or item.Unit == 'flag' or item.Unit == 'index':
        item = item._replace(Unit='dimensionless')
    elif item.Unit == 'C':
        item = item._replace(Unit='degree_Celsius')
    elif item.Unit == 'years':
        item = item._replace(Unit='year')
    elif 'flashes' in item.Unit:
        item = item._replace(Description=item.Description.replace('flashes', 'count'))

    # Fix spelling errors for reflectivity
    if 'Reflectivty' in item.Description:
        item = item._replace(Description=item.Description.replace('Reflectivty', 'Reflectivity'))
    if 'Reflectivty' in item.Name:
        item = item._replace(Name=item.Name.replace('Reflectivty', 'Reflectivity'))

    if 'Resouion' in item.Description:
        item = item._replace(Description=item.Description.replace('Resouion', 'Resolution'))

    return item

# extract the important parameters needed by netCDF-Java
def important_table_info(param):
    important_info = '{0.Discipline}, {0.Category}, {0.Parameter}, "{0.Name}", ' + \
                     '"{0.Description}", "{0.Unit}", {0.No_Coverage}, ' + \
                     '{0.Missing}'

    return important_info.format(param)

# extract the critical parameters needed by netCDF-Java
def critical_table_info(param):
    critical_info = '{0.Discipline}, {0.Category}, {0.Parameter}, "{0.Unit}", {0.No_Coverage}, ' + \
                     '{0.Missing}'

    return critical_info.format(param)

def sorter(item):
    # extract the discipline, category, and parameter as int values
    parts = important_table_info(item).split(',')
    return tuple(map(lambda x: int(x), parts[0:3]))

def make_java(info, tables):
    filename = 'MergedTableCode.txt'.format(dt.datetime.now())
    # sort by discipline, category, and parameter, in that order.
    info = sorted(info, key = lambda item: sorter(item))
    with open(filename, 'w') as f:
        f.write('# created {0:%Y}-{0:%m}-{0:%d}T{0:%I%M}\n'.format(dt.datetime.now()))
        f.write('# using tables {}\n'.format(', '.join(tables)))
        for i in info:
            i = fix_item(i)
            # make sure the discipline value is numeric (skip the "Not GRIB2" items)
            if i.Discipline.isnumeric():
                f.write('add({}); // v{}\n'.format(important_table_info(i), i.TableVersion))

# return true if the parameters needed by netcdf-java are the same.
def parameter_comparison(param_a, param_b):
    critical = False
    if critical:
        marker_a = critical_table_info(param_a)
        marker_b = critical_table_info(param_b)
    else:
        marker_a = important_table_info(param_a)
        marker_b = important_table_info(param_b)

    marker_a = marker_a.replace('_', '')
    marker_b = marker_b.replace('_', '')
    marker_a = marker_a.replace(' ', '')
    marker_b = marker_b.replace(' ', '')
    return marker_a.lower() == marker_b.lower()

def process_tables(tables, interactive=False):

    params = defaultdict(list)
    for table in tables:
        info = parse_file(table)
        for param in info:
            if param.Discipline.isnumeric():
                key = '-'.join([param.Discipline, param.Category, param.Parameter])
                params[key].append(param)

    dupes = 0
    unique_params = []
    for param_key in params:
        # if there is only one entry for a given discipline-category-parameter combination, use it
        if len(params[param_key]) == 1:
            unique_params.append(params[param_key][0])
        else:
            # let's figure out if there are differences between the table entries with the same discipline-category-parameter combination
            has_diffs = False
            param_versions = params[param_key]
            first = param_versions[0]
            for version in range(1, len(param_versions)):
                # compare the parameters from different tables to see if they are different
                has_diffs = not parameter_comparison(first, param_versions[version])
                if has_diffs:
                    # ok, at least one entry is different, so let's stop checking here
                    break
            # If they are all equal (except for the table version part)
            if not has_diffs:
                # all the same, so pick one (we'll go with the latest)
                unique_params.append(param_versions[-1])
            else:
                # So we have multiple table entries for a given discipline-category-parameter combination.
                # Need a human to sort this out. List the options and ask.
                dupes += 1

                if not interactive:
                    selection = -1
                else:
                    print('Which version of {} would you like to use?'.format(param_key))
                    number_of_choices = len(param_versions)
                    # figure out padding for pretty printing
                    max_version_len = 0
                    for version in range(number_of_choices):
                      versioned_param = param_versions[version]
                      table_version_len = len(versioned_param[-1])
                      if (table_version_len > max_version_len):
                          max_version_len = table_version_len

                    for version in range(number_of_choices):
                        versioned_param = param_versions[version]
                        padding = ' '*(max_version_len - len(versioned_param[-1]))
                        print('    {} ({}): {}{}'.format(version + 1, versioned_param[-1], padding, important_table_info(versioned_param)))

                    valid_selection = False
                    while not valid_selection:
                        if interactive:
                            selection = input('  1 - {} (default {}): '.format(number_of_choices, number_of_choices))
                        else:
                            selection = ''

                        if selection == '':
                            # if default, select the last choice in the list
                            selection = -1
                            valid_selection = True
                        elif selection.isnumeric():
                            selection = int(selection)
                            if (selection > 0) and (selection <= number_of_choices):
                                selection -= 1
                                valid_selection = True

                        if not valid_selection:
                            print('  --> {} is invalid. Please choose between 1 and {}.'.format(selection, number_of_choices))

                unique_params.append(param_versions[selection])

    if debug or interactive:
        print('Selected {}'.format(param_versions[selection]))

    if debug:
        print('Dupes: {} of {} params.'.format(dupes, len(params)))

    make_java(unique_params, tables = tables)

if __name__ == '__main__':
    import argparse

    parser = argparse.ArgumentParser(description="Convert MRMS GRIB 2 table to Java code")
    parser.add_argument('--table', type=str, required=False, action='append', nargs='+',
      help="One or more source table (multiple tables separated by spaces). If missing, will try to merge all source tables located in tables/")
    parser.add_argument('--interactive', action='store_true',
      help="When merging tables, prompt user to select between conflicting parameter definitions.")
    args = parser.parse_args()

    tables = args.table
    if tables is None:
        table_directory_name = 'tables/'
        tables = listdir(table_directory_name)
        tables = map(lambda x: join(table_directory_name, x), tables)
    else:
        if isinstance(tables, str):
            tables = [tables]
        else:
            tables = list(chain.from_iterable(tables))

    process_tables(tables, interactive = args.interactive)
