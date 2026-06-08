# ================================================================
#  Syrian Private Programming VCG v1.0  —  Makefile
#  Date: 2026-06-06
# ================================================================

CC      = gcc
CFLAGS  = -Wall -Wextra -O2 -std=c11 -D_POSIX_C_SOURCE=200809L \
          -Icompiler/include
LDFLAGS = -lm
BIN     = vcgc

SRCS = compiler/src/lexer.c      \
       compiler/src/ast.c         \
       compiler/src/parser.c      \
       compiler/src/value.c       \
       compiler/src/interpreter.c \
       compiler/src/stdlib.c      \
       compiler/src/codegen.c     \
       compiler/src/main.c

.PHONY: all clean test examples install

all: $(BIN)

$(BIN): $(SRCS) compiler/include/vcg.h
	$(CC) $(CFLAGS) -o $(BIN) $(SRCS) $(LDFLAGS)
	chmod +x $(BIN)
	@echo "✓ Built: $(BIN) v1.0.0"

examples: $(BIN)
	@mkdir -p output
	./$(BIN) examples/basic/hello.vcg          -o output/hello.html
	./$(BIN) examples/basic/variables.vcg      -o output/variables.html
	./$(BIN) examples/basic/loops.vcg          -o output/loops.html
	./$(BIN) examples/advanced/fibonacci.vcg   -o output/fibonacci.html
	./$(BIN) examples/advanced/calculator.vcg  -o output/calculator.html
	./$(BIN) examples/advanced/sorting.vcg     -o output/sorting.html
	./$(BIN) examples/advanced/structs.vcg     -o output/structs.html
	./$(BIN) examples/advanced/new_concepts.vcg -o output/new_concepts.html
	./$(BIN) examples/advanced/ui_media.vcg    -o output/ui_media.html
	@echo "✓ All examples compiled → output/"

test: $(BIN)
	@cd tests && bash run_tests.sh

install: $(BIN)
	cp $(BIN) /usr/local/bin/vcgc
	@echo "✓ Installed vcgc to /usr/local/bin"

clean:
	rm -f $(BIN)
	rm -f output/*.html
