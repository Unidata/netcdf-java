#Note: The bison parser has been replaced by a DOM parser+walker.
#The bison code is left in the build tree in case it is useful
#elsewhere.

DAP4TARGET=../src/main/java/dap4/core/dmr/parser/bison/Dap4BisonParser.java

CETARGET=../src/main/java/dap4/core/ce/parser/CEBisonParser.java

.PHONEY: bison

all: dap4 ce

# Requires bison 3.0 or later
dap4::
	bison  -v dap4.y -o Dap4BisonParser.java
#	if test -f ${DAP4TARGET} ; then rm ${DAP4TARGET}; fi
#	cp Dap4BisonParser.java ${DAP4TARGET}

ce::
	bison  -v ce.y -o CEBisonParser.java
	if test -f ${CETARGET} ; then rm ${CETARGET}; fi
	cp CEBisonParser.java ${CETARGET}

clean::
	rm -fr Dap4BisonParser.java Dap4BisonParser.output




