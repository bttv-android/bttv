"""Check the actual Twitch 31 ingress filter and execute bridge control flow.

The small interpreter below exercises the production smali predicate, not a
parallel hand-written filtering algorithm. It is not an Android ART verifier.
Run alongside the JVM test of the real Blacklist matcher and smali assembly.
"""
from pathlib import Path
import re

ROOT = Path(__file__).resolve().parents[1]
def read(path):
    return (ROOT / path).read_text(encoding="utf-8")

patch = read("patches/live/bttv.highlight.LiveBlacklist.smali.patch")
bridge = "\n".join(line[1:] for line in patch.splitlines() if line.startswith("+") and not line.startswith("+++"))
assert int(re.search(r"@@ -0,0 \+1,(\d+) @@", patch)[1]) == len(bridge.splitlines())
hook = read("patches/live/xdw.smali.patch")
stock = read(".bttv-cache/disass/smali_classes2/xdw.smali")
assert hook.count("LiveBlacklist;->wrap") == 1
assert "invoke-static {v2}" in hook and "move-result-object v2" in hook
assert "invoke-direct {v2, v1, v4}, Llev;-><init>(Lchi;I)V" in hook
assert "const/16 v4, 0x19" in stock
assert "invoke-direct {v1, v0, v2, v3}, Lio/reactivex/internal/operators/flowable/y0;-><init>(Lio/reactivex/f;Lio/reactivex/functions/l;I)V" in stock
assert stock.index("const/16 v4, 0x19") < stock.index("Lcvn;->a(") < stock.index("new-instance v1, Ls3n;")
# Native filter is on La26 events (not batches or rows). It retains Rx's
# fused/conditional subscribers and replacement-demand behavior.
ezt = read(".bttv-cache/disass/smali_classes2/ezt.smali")
assert "check-cast v1, La26;" in ezt and "La26;->b:Lf56;" in ezt
gvu = read(".bttv-cache/disass/smali_classes2/gvu.smali")
assert "Llmp;->b:Ljava/util/List;" in gvu
rx = read(".bttv-cache/disass/smali/io/reactivex/internal/operators/flowable/x0.smali")
assert "Lio/reactivex/functions/l;->test(Ljava/lang/Object;)Z" in rx
assert "->request(J)V" in rx and ".method public final poll()Ljava/lang/Object;" in rx
mapping = read(".bttv-cache/disass/smali/s3n.smali")
assert "La26;->b:Lf56;" in mapping and "Lz86;->a(" in mapping
assert "highlighted-message" in mapping and "custom-reward-id" in mapping
z86 = read(".bttv-cache/disass/smali_classes2/z86.smali")
assert "Lf56;->e:Ljava/util/List;" in z86 and "Lamp;->a:Ljava/lang/String;" in z86
assert "new-instance v0, Limp;" in z86 or "Limp;-><init>(Ljava/lang/String;)V" in z86
assert "Lamp;" in bridge and "Limp;" not in bridge
assert "Lxlp;" not in bridge and "Lfmp;" not in bridge
assert "move-object/from16 v2, p5" in read("patches/live/f86.smali.patch")
settings = read("mod/app/src/main/java/bttv/settings/Settings.java")
assert '"bttv_blacklist_set"' in settings
assert re.search(r'"bttv_blacklist_set",\s*new HashSet<>\(\)', settings)
assert 'text.split(" ")' in read("mod/app/src/main/java/bttv/highlight/Blacklist.java")
assert bridge.index("->test(Ljava/lang/Object;)Z") < bridge.index(":try_start_text")
assert all(s not in bridge for s in ("Log;", "remove(", "set(", "messageType", "Network;"))

# Execute only the supported instruction subset, rejecting unknown opcodes.
method = bridge.split(".method public test(Ljava/lang/Object;)Z", 1)[1].split(".end method", 1)[0]
lines = [line.strip() for line in method.splitlines() if line.strip() and not line.strip().startswith(("#", "."))]
labels = {line: i for i, line in enumerate(lines) if line.startswith(":")}
class Obj:
    def __init__(self, kind, **fields): self.kind, self.fields = kind, fields
def text(value): return Obj("Lamp;", a=value)
def event(tokens): return Obj("La26;", b=Obj("Lf56;", e=tokens))
def execute(value, predicate=lambda value: True, matcher=lambda value: value == "blocked"):
    regs = {"p0": Obj("bridge", original=predicate), "p1": value}
    pc, result, pending = 0, None, None
    while pc < len(lines):
        line = lines[pc]
        try:
            if line.startswith(":"): pass
            elif line.startswith("iget-object"):
                dest, source, field = line[12:].split(", ")
                regs[dest] = regs[source].fields[field.split("->")[1].split(":")[0]]
            elif line.startswith("instance-of"):
                dest, source, kind = line[12:].split(", ")
                regs[dest] = isinstance(regs[source], Obj) and regs[source].kind == kind
            elif line.startswith("check-cast"): pass
            elif line.startswith("invoke-"):
                args = [regs[r] for r in re.search(r"\{(.*?)\}", line)[1].split(", ")]
                if "->test(" in line: result = args[0](args[1])
                elif "->iterator()" in line: result = [list(args[0]), 0]
                elif "->hasNext()" in line: result = args[0][1] < len(args[0][0])
                elif "->next()" in line:
                    result = args[0][0][args[0][1]]; args[0][1] += 1
                elif "->shouldBlockText(" in line: result = matcher(args[0])
                else: raise AssertionError(line)
            elif line.startswith("move-result"): regs[line.split()[1]] = result
            elif line.startswith("move-exception"): regs[line.split()[1]] = pending
            elif line.startswith(("if-eqz", "if-nez")):
                op, reg, label = line.replace(",", "").split()
                if bool(regs[reg]) == (op == "if-nez"):
                    pc = labels[label]; continue
            elif line.startswith("goto "): pc = labels[line.split()[1]]; continue
            elif line.startswith("const/4"):
                _, reg, number = line.replace(",", "").split(); regs[reg] = int(number, 16)
            elif line.startswith("return "): return bool(regs[line.split()[1]])
            else: raise AssertionError(line)
        except AssertionError: raise
        except Exception as error:
            if not labels[":try_start_text"] <= pc < labels[":try_end_text"]: raise
            pending = error; pc = labels[":text_failed"]; continue
        pc += 1
    raise AssertionError("no return")

assert not execute(event([text("blocked")]))
assert execute(event([text("allowed")]))
assert execute(event([]))
assert execute(event([Obj("Lxlp;", a="blocked", b="blocked")]))
assert execute(event([Obj("Lylp;", a="blocked", b="blocked")]))
assert execute(event([Obj("Lzlp;", a="blocked")]))
assert not execute(event([text("allowed"), text("blocked")]))
assert not execute(event([text("monkaS")]), matcher=lambda value: value == "monkaS")
assert execute(event([Obj("Lxlp;", a="native-id", b="monkaS")]), matcher=lambda _: True)
assert execute(event([text("blocked")]), matcher=lambda _: False)  # empty blacklist
assert execute(object()) and execute(None) and execute(event(None))
def explode(*args): raise RuntimeError("settings unavailable")
assert execute(event([text("blocked")]), matcher=explode)
assert not execute(event([text("allowed")]), predicate=lambda _: False, matcher=explode)
try: execute(event([]), predicate=explode)
except RuntimeError: pass
else: raise AssertionError("native errors must propagate")
original = event([text("blocked")]); old_list = original.fields["b"].fields["e"]
assert not execute(original)
assert original.fields["b"].fields["e"] is old_list and old_list[0].fields["a"] == "blocked"
# Simulate only the Rx filter's verified keep/drop contract: no placeholder,
# no unrelated event loss, no re-evaluation of previously delivered entries.
kept, other, blocked = event([text("allowed")]), object(), event([text("blocked")])
rows = [item for item in [kept, blocked, other] if execute(item)]
assert rows == [kept, other]
execute(blocked, matcher=lambda _: False)
assert rows == [kept, other]
print("PASS Stage 5L native ingress, token types, filter control flow and fail-open contract")
