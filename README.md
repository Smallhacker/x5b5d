# x5b5d
**x5b5d** is an esoteric programming language centered around arrays.

## Usage
The main method provided in `X5Runner.kt` takes in the file name for the
source file. Optionally, it can also accept some flags:
* `-n` attempts to retain some macro names in the output, occasionally
making the output somewhat easier for a human to read.
* `-p` only runs a pre-processing step on the source file without
evaluating the result. Effectively, this means expanding all macros to
their corresponding expressions. This flag cannot be used in conjunction
with other flags.

## Language
Source files are UTF-8 encoded text files with `.x5` as its recommended
file extension. Whitespace is ignored and can be freely be added to
increase readability (or removed to reduce readability). The `;` character
is used to begin a comment; all further characters on that line will be
ignored by the parser.

All x5b5d source files must contain exactly one expression and zero or
more macro declarations.

### Arrays
In x5b5d, all values are arrays. There are two distinct flavors of arrays:
*static* and *dynamic*.

#### Static Arrays
A static array is created by surrounding comma-separated elements with with
square brackets. For instance, `[]` is an empty array while
`[ [], [], [] ]` is an array containing three empty arrays.

#### Indexing
Arrays can be indexed to extract individual values from them. This is done
using the indexing operator, `[x]` where `x` is the index of the element
you wish to retrieve. Since x5b5d doesn't have numerical values, arrays are
indexed using arrays. Which element to extract is determined by the index's
*rank*, a measure of how deeply nested the array is. Indexing by an array
of rank 0 gives the first value, a rank of 1 gives the second value, and so
on.

The rank of an array can be described as "how many times does one have to
extract the first element of the array until one reaches an empty array".
As such, `[]` has a rank of 0, `[[]]` has a rank of 1, `[[[]]]` has a rank
of 2, `[[], [[[[[[[]]]]]]]]` has a rank of 1 since only the initial
elements in each level is relevant. One observation is that `[]` is the
only array expressible in x5b5d with a rank of 0.

As an example, `[ [[[]]], [[]], [] ][ [] ]` is the array
`[ [[[]]], [[]], [] ]` indexed by `[]`, which has a rank of 0. As such, the
result is the first element of the array, that is `[[[]]]`.

If one attempts to retrieve an element beyond the end of the array, the
last element of the array is retrieved. In the case of an empty array, an
empty array is retrieved. As a consequence of this, all arrays in x5b5d are
effectively infinitely long.

#### Dynamic Arrays
Dynamic arrays are ones where the individual values are calculated
on-the-fly when indexed. They are created by surrounding an expression in
curly braces. For instance, `{[]}` is a dynamic array where all elements
are empty arrays. Dynamic arrays can also be nested; `{{[]}}` is a dynamic
array where each element is an array whose every element is an empty array.

Within dynamic arrays, an automatic *index array*, `@`, can be used to
access the value that the dynamic array is indexed with. The index array
is an array where the first value (`@[[]]`) is the index to the current
dynamic array, the second value (`@[[[]]]`) is the index of the outer
dynamic array (if applicable), and so on.

As an example, `{ @[[]] }` is a dynamic array where every element is the
index that was used to access it. This is effectively an *identity array*,
the array analogue to an *identity function*.

Note that dynamic arrays could in theory be used to create an array that
is infinitely nested. Such an array would lack a well-defined rank and
attempting to use it as an index would constitute undefined behavior;
x5b5d interpreters are likely to lock up in infinite loops when trying
to evaluate the expression.

### Macros
x5b5d supports a simple macro system. A macro declaration, such as
`<EMPTY_ARRAY, []>`, consists of a name and an expression. That expression
is then effectively pasted in place of any other uses of the macro's name.
No two macros can have the same name and it is not possible to redefine it
elsewhere in the code.

Macro names may only contain `A-Z` (upper-case) as well as `_`. Lower-case
letters and all other characters are reserved.

Macros can make use of other macros, but no macro may, directly or
indirectly, reference itself. The order of macro declarations is
irrelevant; a macro can reference other macro regardless of whether it 
comes before or after them.

Macros may contain uses of the index array (`@`) to access the index of
the dynamic array it gets included into. However, as may be expected, the
macro will then not function as intended when used outside of dynamic
arrays.

## Examples

One can take advantage of the notion of array rank to represent numerical
values. The following set of macros can be used to define a few basic
numerical constants and increment/decrement macros:

```
<ZERO, []>
<ONE, INC[ZERO]>
<TWO, INC[ONE]>
<THREE, INC[TWO]>
; Define increment as wrapping the index in an array
<INC, {[@[ZERO]]}>
; Define decrement as getting the first element of the index
; Of note, DEC[ZERO] becomes [ZERO]
<DEC, {@[ZERO][ZERO]}>

; What comes after 3?
INC[THREE]
```

One could also represent booleans by using the empty array to represent
`FALSE` and any other array (with a well-defined rank) to represent
`TRUE`. In the example below, we index the `IF` array with an array
`[condition, onTrue, onFalse]`. We then create the array
`[onFalse, onTrue]` and index it with `condition`. Indices with a rank
of 0 would yield `onFalse` while indices with a rank of 1 would yield
`onTrue`. As mentioned above, indices with ranks that are too high yield
the last element of the array, so any non-zero ranked index would behave
the same way as `TRUE`.

```
<FALSE, []>
<TRUE, [[]]>
<IF, {[@[ZERO][TWO], @[ZERO][ONE]][@[ZERO][ZERO]]}>

; Returns TWO. FALSE would return THREE.
IF[[TRUE, TWO, THREE]]
```

Lastly, an example of an `ADD` macro that adds to "numbers" together.
This code uses a `Y` macro to enable recursive looping by granting the
dynamic array access to itself, thus allowing it to reference itself.


```
<INDEX, @[ZERO]>
<OUTER_INDEX, @[ONE]>

<ZERO, []>
<ONE, INC[ZERO]>
<TWO, INC[ONE]>
<THREE, INC[TWO]>

<INC, {[INDEX]}>
<DEC, {INDEX[ZERO]}>

<IFEMPTY, {[INDEX[ONE], INDEX[TWO]][INDEX[ZERO]]}>

<Y, {{OUTER_INDEX[[OUTER_INDEX, INDEX]]}}>
<YTHIS, Y[INDEX[ZERO]]>
<YINDEX, INDEX[ONE]>

<ADD, Y[{
    IFEMPTY[[
        YINDEX[ONE],
        YINDEX[ZERO],
        YTHIS[[
            INC[YINDEX[ZERO]],
            DEC[YINDEX[ONE]]
        ]]
    ]]
}]>

; Returns [[[[[[]]]]]], i.e. an array of rank 5
ADD[[TWO, THREE]]
```