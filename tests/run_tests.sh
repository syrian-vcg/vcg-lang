#!/usr/bin/env bash
# ================================================================
#  VCG Test Runner
# ================================================================
set -e
PASS=0; FAIL=0; VCG="../vcgc"

run_test() {
    local name="$1" file="$2" expected="$3"
    local got; got=$(${VCG} -r "$file" 2>&1 | head -1)
    if [ "$got" = "$expected" ]; then
        echo "  PASS: $name"
        PASS=$((PASS+1))
    else
        echo "  FAIL: $name"
        echo "    expected: $expected"
        echo "    got:      $got"
        FAIL=$((FAIL+1))
    fi
}

echo "Running VCG tests..."

# Inline tests via temp files
tmpdir=$(mktemp -d)

cat > "$tmpdir/arith.vcg" << 'VCGEOF'
show(2 + 3)
VCGEOF
run_test "addition" "$tmpdir/arith.vcg" "5"

cat > "$tmpdir/string.vcg" << 'VCGEOF'
show("hello" + " " + "world")
VCGEOF
run_test "string concat" "$tmpdir/string.vcg" "hello world"

cat > "$tmpdir/fib.vcg" << 'VCGEOF'
func fib(n) {
    if n <= 1 { return n }
    return fib(n-1) + fib(n-2)
}
show(fib(10))
VCGEOF
run_test "fibonacci(10)" "$tmpdir/fib.vcg" "55"

cat > "$tmpdir/arr.vcg" << 'VCGEOF'
let a = [3,1,4,1,5]
a.push(9)
show(len(a))
VCGEOF
run_test "array push+len" "$tmpdir/arr.vcg" "6"

cat > "$tmpdir/closure.vcg" << 'VCGEOF'
func make_adder(x) {
    func add(y) { return x + y }
    return add
}
let add5 = make_adder(5)
show(add5(3))
VCGEOF
run_test "closure" "$tmpdir/closure.vcg" "8"

cat > "$tmpdir/loop.vcg" << 'VCGEOF'
let s = 0
for i in 1..100 { s += i }
show(s)
VCGEOF
run_test "for-in range sum 1..100" "$tmpdir/loop.vcg" "5050"

cat > "$tmpdir/trycatch.vcg" << 'VCGEOF'
try {
    throw "oops"
} catch e {
    show(e)
}
VCGEOF
run_test "try/catch" "$tmpdir/trycatch.vcg" "oops"

cat > "$tmpdir/method.vcg" << 'VCGEOF'
let s = "Hello World"
show(s.lower())
VCGEOF
run_test "string method lower" "$tmpdir/method.vcg" "hello world"

cat > "$tmpdir/struct.vcg" << 'VCGEOF'
let p = { x: 3, y: 4 }
let d = sqrt(p.x * p.x + p.y * p.y)
show(int(d))
VCGEOF
run_test "struct field + sqrt" "$tmpdir/struct.vcg" "5"

cat > "$tmpdir/match.vcg" << 'VCGEOF'
func grade(s) {
    match s {
        when 5 -> return "ممتاز"
        when 4 -> return "جيد"
        when 3 -> return "مقبول"
    }
    return "راسب"
}
show(grade(5))
VCGEOF
run_test "match/when" "$tmpdir/match.vcg" "ممتاز"

rm -rf "$tmpdir"

echo ""
echo "Results: PASS=$PASS  FAIL=$FAIL"
[ $FAIL -eq 0 ] && echo "All tests passed!" && exit 0 || exit 1
