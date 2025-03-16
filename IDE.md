# IDE Setup for Sylect

## IntelliJ IDEA

### Syntax Highlighting

Currently only basic syntax highlighting is supported in IntelliJ IDEA.

1. Go to `File -> Settings -> Editor -> File Types`.
2. Press `+` button to add a new one.
3. Input "Sylect" as `Name`, in `Line comment` write `//`, in `Number postfixes` - `FL`.
4. Check `Support paired braces/brackets/parens` checkboxes, but leave out `Support string escapes`.
5. Add keywords from lists below to corresponding tabs.
6. Press `+` button to add a new `File name pattern` and input `*.sy`.

First tab:

```
:
<:
break
class
continue
each
else
if
import
interface
native
return
static
super
this
var
while
```

Second tab:

```
!=
<
<=
==
>
>=
```

Third tab:

```
double
float
int
long
bool
byte
char
short
void
[
]
```

Fourth tab:

```
%
&
*
+
-
/
<<
>>
>>>
^
|
```
