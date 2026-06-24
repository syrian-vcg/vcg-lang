# ================================================================
#  Syrian Private Programming VCG v2.0  —  Makefile
#  Date: 2026-06-06  |  License: MIT
# ================================================================

CC      = gcc
CFLAGS  = -Wall -Wextra -O2 -std=c11 -D_POSIX_C_SOURCE=200809L \
          -Icompiler/include
LDFLAGS = -lm
BIN     = vcgc

SRCS    = compiler/src/lexer.c      \
          compiler/src/ast.c         \
          compiler/src/parser.c      \
          compiler/src/value.c       \
          compiler/src/interpreter.c \
          compiler/src/stdlib.c      \
          compiler/src/codegen.c     \
          compiler/src/main.c

HEADERS = compiler/include/vcg.h

.PHONY: all clean test examples install debug help

# ── Default: build compiler ─────────────────────────────────────
all: $(BIN)

$(BIN): $(SRCS) $(HEADERS)
	$(CC) $(CFLAGS) -o $(BIN) $(SRCS) $(LDFLAGS)
	chmod +x $(BIN)
	@echo "✓ Built: $(BIN) v2.0.0"
	@./$(BIN) --version

# ── Debug build ─────────────────────────────────────────────────
debug: $(SRCS) $(HEADERS)
	$(CC) $(CFLAGS) -g -O0 -DVCG_DEBUG -o $(BIN)_dbg $(SRCS) $(LDFLAGS)
	@echo "✓ Debug build: $(BIN)_dbg"

# ── Compile all examples ────────────────────────────────────────
examples: $(BIN)
	@mkdir -p output
	@echo "Compiling examples..."
	@./$(BIN) examples/basic/hello.vcg           -o output/hello.html           && echo "  ✓ hello"
	@./$(BIN) examples/basic/variables.vcg       -o output/variables.html       && echo "  ✓ variables"
	@./$(BIN) examples/basic/loops.vcg           -o output/loops.html           && echo "  ✓ loops"
	@./$(BIN) examples/advanced/fibonacci.vcg    -o output/fibonacci.html       && echo "  ✓ fibonacci"
	@./$(BIN) examples/advanced/calculator.vcg   -o output/calculator.html      && echo "  ✓ calculator"
	@./$(BIN) examples/advanced/sorting.vcg      -o output/sorting.html         && echo "  ✓ sorting"
	@./$(BIN) examples/advanced/structs.vcg      -o output/structs.html         && echo "  ✓ structs"
	@./$(BIN) examples/advanced/new_concepts.vcg -o output/new_concepts.html    && echo "  ✓ new_concepts"
	@./$(BIN) examples/advanced/ui_media.vcg     -o output/ui_media.html        && echo "  ✓ ui_media"
	@./$(BIN) examples/advanced/v2_concepts.vcg  -o output/v2_concepts.html     && echo "  ✓ v2_concepts"
	@echo "✓ All examples compiled → output/"

# ── Run test suite ──────────────────────────────────────────────
test: $(BIN)
	@echo "Running tests..."
	@cd tests && bash run_tests.sh

# ── Install system-wide ─────────────────────────────────────────
install: $(BIN)
	@cp $(BIN) /usr/local/bin/vcgc
	@echo "✓ Installed vcgc to /usr/local/bin"

# ── Clean ───────────────────────────────────────────────────────
clean:
	@rm -f $(BIN) $(BIN)_dbg
	@rm -f output/*.html
	@echo "✓ Cleaned"

# ── Help ────────────────────────────────────────────────────────
help:
	@echo ""
	@echo "  Syrian Private Programming VCG v2.0"
	@echo ""
	@echo "  make          → Build vcgc compiler"
	@echo "  make examples → Compile all .vcg examples"
	@echo "  make test     → Run test suite (10 tests)"
	@echo "  make debug    → Debug build"
	@echo "  make install  → Install to /usr/local/bin"
	@echo "  make clean    → Remove build artifacts"
	@echo ""
	@echo "  Usage:"
	@echo "    ./vcgc file.vcg              → compile to HTML"
	@echo "    ./vcgc -r file.vcg           → run (interpreter)"
	@echo "    ./vcgc file.vcg -o out.html  → custom output name"
	@echo "    ./vcgc --tokens file.vcg     → dump tokens"
	@echo "    ./vcgc --ast file.vcg        → dump AST"
	@echo "    ./vcgc --version             → show version"
	@echo ""

# ── Build VCG SDK JAR ────────────────────────────────────────────
SDK_VERSION = 2.0.1
SDK_SRC     = sdk/src/main/java
SDK_BIN     = sdk/build/classes
SDK_JAR     = sdk/build/vcg-sdk-$(SDK_VERSION).jar

sdk-build:
	@echo "Building VCG SDK v$(SDK_VERSION)..."
	@mkdir -p $(SDK_BIN)
	@java com.sun.tools.javac.Main -source 11 -target 11 \
	    -d $(SDK_BIN) \
	    $$(find $(SDK_SRC) -name "*.java")
	@cd $(SDK_BIN) && \
	    cp -r ../../src/main/resources/META-INF . && \
	    zip -qr ../vcg-sdk-$(SDK_VERSION).jar .
	@echo "✓ SDK JAR: $(SDK_JAR)"
	@cp $(SDK_JAR) editor/android/app/libs/

sdk-clean:
	@rm -rf sdk/build
	@echo "✓ SDK build cleaned"

.PHONY: sdk-build sdk-clean
