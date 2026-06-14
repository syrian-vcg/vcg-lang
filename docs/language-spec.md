# VCG Language Specification v2.0

## 1. Comments
```vcg
# Single line comment
// Also single line
/* Multi-line
   comment */
```

## 2. Variables
```vcg
let x = 10           # mutable
const PI = 3.14159   # immutable
w log = "event"      # write-only (audit log)
```

## 3. Data Types
```vcg
let i = 42           # int
let f = 3.14         # float
let s = "hello"      # string
let b = true         # bool (true/false)
let n = nil          # nil
let a = [1, 2, 3]   # array
let o = { x: 1 }    # struct/object
```

## 4. Operators
```vcg
# Arithmetic
+  -  *  /  %  **     # power

# Comparison
==  !=  <  >  <=  >=

# Logical
and  or  not

# Assignment
=  +=  -=  *=  /=

# Range
1..10                  # [1,2,...,9]

# Pipeline
val |> func1 |> func2  # func2(func1(val))

# Bitwise
|  &  <<  >>
```

## 5. Control Flow
```vcg
# if/else
if x > 0 {
    show("positive")
} else if x < 0 {
    show("negative")
} else {
    show("zero")
}

# Ternary
let label = x > 0 ? "pos" : "non-pos"

# while
while x > 0 { x -= 1 }

# for-in
for item in [1, 2, 3] { show(item) }
for i in 0..10 { show(i) }

# repeat
repeat 5 { show("hello") }

# match/when
match status {
    when 0 -> show("off")
    when 1 -> show("on")
}
```

## 6. Functions
```vcg
func add(a, b) {
    return a + b
}

# Lambda
let double = \x -> x * 2

# Async
async func load(url) {
    let data = await fetch(url)
    return data
}

# Variadic
func sum_all(..nums) {
    return reduce(add, 0, nums)
}
```

## 7. Classes (OOP)
```vcg
class Animal {
    func init(name, sound) {
        self.name = name
        self.sound = sound
    }
    func speak() {
        return self.name + ": " + self.sound
    }
}

class Dog extends Animal {
    func init(name) {
        self.name  = name
        self.sound = "Woof"
    }
    func fetch(item) {
        return self.name + " fetches " + item
    }
}

let dog = new Dog("Rex")
show(dog.speak())      # Rex: Woof
show(dog.fetch("ball"))
```

## 8. Modules
```vcg
module Math2 {
    func square(x) { return x * x }
    let PI2 = 3.14159
}

show(Math2.PI2)
show(Math2.square(5))

from Math2 import square
show(square(9))
```

## 9. Enums
```vcg
enum Color { Red, Green, Blue }
enum Status { Pending, Active, Done }

show(Color.Red)    # 0
show(Status.Done)  # 2
```

## 10. Error Handling
```vcg
try {
    throw "something went wrong"
} catch err {
    show("caught:", err)
}

# Safe block
safe {
    # If error occurs, continues silently
    risky_operation()
}

# Guard
guard x > 0 else {
    show("x must be positive")
    return
}
```

## 11. Reactive Store
```vcg
watch("score", func(v) {
    show("score changed to", v)
})

$set("score", 100)
$set("score", 200)    # triggers watcher
show($get("score"))   # 200
```

## 12. Channels
```vcg
c tasks

send(tasks, "task 1")
send(tasks, "task 2")

let t = recv(tasks)
while t != nil {
    show("processing:", t)
    t = recv(tasks)
}
```

## 13. Testing
```vcg
test "math works" {
    assert_eq(2 + 2, 4)
    assert_true(10 > 5)
    assert_false(1 == 2)
}

test "strings" {
    assert_eq(len("hello"), 5)
    assert_ne("a", "b")
}
```
