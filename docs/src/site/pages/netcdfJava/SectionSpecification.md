---
title: Section Specification
last_updated: 2026-06-23
sidebar: netcdfJavaTutorial_sidebar
toc: false
permalink: section_specification_ref.html
---
## CDM Section Specification

### Syntax

Array sections can be specified with Fortran 90 array section syntax, using zero-based indexing. For example:

`/group1/group2/varName(12:22,0:100:2,:,17)`

specifies an array section for a four-dimensional variable:

1. `12:22` includes all the elements from 12 to 22 inclusive
1. `0:100:2` includes the elements from 0 to 100 inclusive, with a stride of 2
1. `:` includes all the elements
1. `17` includes just the 18th element.

For structures, you can specify nested selectors, e.g. `record(12).wind(1:20,:,3)` does a selection on the `wind` member variable of the `record` structure at index 12.
If you don’t specify a section, it means read the entire variable, e.g. `record.wind` means all the data in all the wind variables in all the record structures.

Formally:

```
sectionSpec := selector | selector '.' selector
selector := varName ['(' sectionSpec ')']
varName := STRING
sectionSpec := dim | dim ',' sectionSpec
dim := ':' | slice | start ':' end | start ':' end ':' stride


slice := INTEGER
start := INTEGER
stride := INTEGER
end := INTEGER
STRING := String with escaped chars = '.', '/', '(', and ')'
```

where:

* Nonterminals are in lower case, terminals are in upper case, and literals are in single quotes.
* Optional components are enclosed between square braces `[` and `]`.

TBD:

* escape mechanism is currently %xx , mostly following opendap
* not sure if any other chars need to be escaped.

### Restrictions

A Sequence is a one-dimensional Structure with a variable length, it cannot be subsetted.

A variable long name must be used, that is with its group names: `/group1/group2/varName`

### Use

`public Array NetcdfFile.readSection(String variableSection);`
