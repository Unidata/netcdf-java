#!/usr/bin/env python
import csv
from datetime import datetime
from collections import defaultdict, namedtuple

debug = False

def parse_table_version(fname):
    # Filenames need to look like path/blah/UserTable_MRMS_v<version>.csv
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

# Extract the important parameters needed by netCDF-Java
def important_table_info(param):
    return (f'{param.Discipline}, {param.Category}, {param.Parameter}, "{param.Name}", '
            f'"{param.Description}", "{param.Unit}", {param.No_Coverage}, '
            f'{param.Missing}')

# Extract the critical parameters needed by netCDF-Java
def critical_table_info(param):
    return (f'{param.Discipline}, {param.Category}, {param.Parameter}, "{param.Unit}", '
            f'{param.No_Coverage}, {param.Missing}')

def sorter(item):
    # Extract the discipline, category, and parameter as int values
    parts = important_table_info(item).split(',')
    return tuple(map(lambda x: int(x), parts[0:3]))

def make_java(info, tables):
    filename = 'MergedTableCode.txt'
    # Sort by discipline, category, and parameter, in that order.
    info = sorted(info, key = lambda item: sorter(item))
    with open(filename, 'w') as f:
        f.write('# created {0:%Y}-{0:%m}-{0:%d}T{0:%I%M}\n'.format(datetime.now()))
        f.write('# using tables {}\n'.format(', '.join(tables)))
        for i in info:
            i = fix_item(i)
            # Make sure the discipline value is numeric (skip the "Not GRIB2" items)
            if i.Discipline.isnumeric():
                f.write('add({}); // v{}\n'.format(important_table_info(i), i.TableVersion))

# Return true if the parameters needed by netcdf-java are the same.
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
    # Sort tables by name, lexicographically.
    # Should restuld in the tables being sorted by version (older to newer).
    tables.sort()
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
        if len(params[param_key]) > 1:
            # There are multiple entries for a given discipline-category-parameter combination.
            # Let's figure out if there are differences between the table entries with the same discipline-category-parameter combination
            param_versions = params[param_key]
            first = param_versions[0]
            # By default, we will pick the latest version (for non-interactive cases)
            selection = -1
            for version in range(1, len(param_versions)):
                if parameter_comparison(first, param_versions[version]):
                    # Parameter definitions are the same, check next version
                    continue

                # If we make it here, we have multiple table entries for a given discipline-category-parameter
                # combination that are different. We might need a human to sort this out.
                dupes += 1

                if interactive:
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

                    while True:
                        selection = input('  1 - {} (default {}): '.format(number_of_choices, number_of_choices))

                        if selection == '':
                            # if default, select the last choice in the list
                            selection = -1
                            break
                        elif selection.isnumeric():
                            selection = int(selection)
                            if (selection > 0) and (selection <= number_of_choices):
                                selection -= 1
                                break
                        else:
                            print('  --> {} is invalid. Please choose between 1 and {}.'.format(selection, number_of_choices))
                break # break out of version loop since we resolved the multiple versions
            # The issue of multiple, possibly conflicting, versions of a parameter has been resolved - add that result
            # to the unique parameter list
            unique_params.append(param_versions[selection])
        else:
            # Only one entry for a given discipline-category-parameter combination exists across various versions of a
            # table, so add it to the unique parameter list.
            unique_params.append(params[param_key][0])

    if debug or interactive:
        print('Selected {}'.format(param_versions[selection]))

    if debug:
        print('Dupes: {} of {} params.'.format(dupes, len(params)))

    make_java(unique_params, tables = tables)

if __name__ == '__main__':
    import argparse
    from pathlib import Path

    parser = argparse.ArgumentParser(description="Convert MRMS GRIB 2 table to Java code")
    parser.add_argument('--table', type=str, required=False, action='append',
      help="Path to a GRIB table (can be used multiple times to provide multiple tables). If missing, will try to merge all GRIB tables located in tables/")
    parser.add_argument('--interactive', action='store_true',
      help="When merging tables, prompt user to select between conflicting parameter definitions.")
    args = parser.parse_args()

    tables = args.table
    if tables is None:
        tables = list(map(str, Path('tables/').iterdir()))

    process_tables(tables, interactive=args.interactive)
