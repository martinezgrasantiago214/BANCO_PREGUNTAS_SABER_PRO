"""Generador minimo de diagramas de secuencia UML en SVG (sin PlantUML)."""
import sys, json, html
def render(spec, out):
    parts = spec["participants"]; msgs = spec["messages"]
    W = 190; X0 = 110; top = 70; step = 34
    xs = {p: X0 + i*W for i, p in enumerate(parts)}
    h = top + 50 + step*len(msgs) + 40
    w = X0 + W*(len(parts)-1) + 300
    s = [f'<svg xmlns="http://www.w3.org/2000/svg" width="{w}" height="{h}" font-family="DejaVu Sans" font-size="11">',
         f'<rect width="{w}" height="{h}" fill="white"/>',
         f'<text x="{w/2}" y="24" text-anchor="middle" font-size="14" font-weight="bold">{html.escape(spec["title"])}</text>',
         '<defs><marker id="a" markerWidth="10" markerHeight="8" refX="9" refY="4" orient="auto"><path d="M0,0 L10,4 L0,8 z" fill="#333"/></marker>'
         '<marker id="o" markerWidth="10" markerHeight="8" refX="9" refY="4" orient="auto"><path d="M0,0 L10,4 L0,8" fill="none" stroke="#333"/></marker></defs>']
    for p in parts:
        x = xs[p]; lines = p.split("\\n")
        s.append(f'<rect x="{x-80}" y="{top-28}" width="160" height="{16+14*len(lines)}" rx="6" fill="#1168BD" stroke="#0B4884"/>')
        for i, l in enumerate(lines):
            s.append(f'<text x="{x}" y="{top-12+14*i}" text-anchor="middle" fill="white">{html.escape(l)}</text>')
        s.append(f'<line x1="{x}" y1="{top+14*len(lines)-8}" x2="{x}" y2="{h-20}" stroke="#999" stroke-dasharray="4,4"/>')
    y = top + 50
    for m in msgs:
        if "note" in m:
            s.append(f'<rect x="{X0-60}" y="{y-14}" width="{w-X0-10}" height="20" fill="#FFF2CC" stroke="#D6B656"/>')
            s.append(f'<text x="{X0-52}" y="{y}" font-style="italic">{html.escape(m["note"])}</text>')
        else:
            a, b, t = xs[m["from"]], xs[m["to"]], m["text"]
            dash = ' stroke-dasharray="6,4"' if m.get("async") or m.get("reply") else ''
            mk = "o" if m.get("async") else "a"
            color = "#C0392B" if m.get("error") else "#333"
            if a == b:
                s.append(f'<path d="M{a},{y-6} h40 v14 h-38" fill="none" stroke="{color}" marker-end="url(#a)"/>')
                s.append(f'<text x="{a+46}" y="{y+2}" fill="{color}">{html.escape(t)}</text>')
            else:
                s.append(f'<line x1="{a}" y1="{y}" x2="{b}" y2="{y}" stroke="{color}"{dash} marker-end="url(#{mk})"/>')
                s.append(f'<text x="{(a+b)/2}" y="{y-5}" text-anchor="middle" fill="{color}">{html.escape(t)}</text>')
        y += step
    s.append('</svg>')
    open(out, "w").write("\n".join(s))
render(json.load(open(sys.argv[1])), sys.argv[2])
