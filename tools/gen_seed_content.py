#!/usr/bin/env python3
"""Generate app/.../core/data/db/SeedContent.kt from design/leaf-mockup-v2.html.

The mockup's SEED_DOCS array is the source of truth for the demo library, so the
Kotlin seed is generated from it rather than retyped. Timestamps keep the mockup's
own relative expressions (now - 12 * DAY) so the demo data ages the same way.

Run from the repo root:  python3 tools/gen_seed_content.py
"""
import os
import re

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
HTML = os.path.join(ROOT, "design", "leaf-mockup-v2.html")
OUT = os.path.join(
    ROOT, "app", "src", "main", "java", "app", "leaf", "reader",
    "core", "data", "db", "SeedContent.kt",
)

TYPE_MAP = {
    "pdf": "DocType.PDF",
    "doc": "DocType.DOCX",
    "xlsx": "DocType.XLSX",
    "pptx": "DocType.PPTX",
    "txt": "DocType.TXT",
    "epub": "DocType.EPUB",
}


def load_seed_docs():
    html = open(HTML, encoding="utf-8").read()
    start = html.index("const SEED_DOCS = [")
    start = html.index("[", start)
    end = html.index("\n];", start)
    return html[start + 1:end]


def split_top_level_objects(src):
    """Split `src` (the body of the SEED_DOCS array) into its top-level {…} objects."""
    objs, depth, start = [], 0, None
    i = 0
    while i < len(src):
        ch = src[i]
        if ch == "{":
            if depth == 0:
                start = i
            depth += 1
        elif ch == "}":
            depth -= 1
            if depth == 0:
                objs.append(src[start:i + 1])
        elif ch == '"':
            i += 1
            while src[i] != '"' or src[i - 1] == "\\":
                i += 1
        i += 1
    return objs


def unescape(s):
    out, i = [], 0
    while i < len(s):
        if s[i] == "\\" and i + 1 < len(s):
            nxt = s[i + 1]
            if nxt == "u" and i + 6 <= len(s) - 1 + 1:
                try:
                    out.append(chr(int(s[i + 2:i + 6], 16)))
                    i += 6
                    continue
                except ValueError:
                    pass
            out.append({"n": "\n", "t": "\t", "r": "\r"}.get(nxt, nxt))
            i += 2
            continue
        out.append(s[i])
        i += 1
    return "".join(out)


def find_string(body, key):
    m = re.search(key + r":\s*'((?:[^'\\]|\\.)*)'", body)
    if m:
        return unescape(m.group(1))
    m = re.search(key + r':\s*"((?:[^"\\]|\\.)*)"', body)
    return unescape(m.group(1)) if m else None


def find_expr(body, key):
    """Read a numeric-or-expression field such as `now-12*DAY` or `2.4`."""
    m = re.search(key + r":\s*([^\s,}\]]+)", body)
    return m.group(1) if m else None


def find_block(body, key):
    """Return the raw [...] or {...} that follows `key:`."""
    i = body.index(key + ":")
    i = body.index(":", i) + 1
    while body[i] in " \n\t":
        i += 1
    opener, closer = body[i], ("}" if body[i] == "{" else "]")
    depth, j = 0, i
    while j < len(body):
        if body[j] == '"':
            j += 1
            while body[j] != '"' or body[j - 1] == "\\":
                j += 1
        elif body[j] == opener:
            depth += 1
        elif body[j] == closer:
            depth -= 1
            if depth == 0:
                return body[i:j + 1]
        j += 1
    raise ValueError("unterminated block for " + key)


def duration(value):
    """`now-12*DAY` -> `now - 12 * DAY_MS`; `now` -> `now`.

    The mockup writes timestamps as expressions; keep them relative so the demo
    library ages exactly the way the prototype's does.
    """
    if value is None:
        return "now"
    e = value.replace(" ", "")
    if e == "now":
        return "now"
    sign, body = "-", e
    if e.startswith("now-"):
        body = e[len("now-"):]
    elif e.startswith("now+"):
        sign, body = "+", e[len("now+"):]
    body = re.sub(r"\bDAY\b", "DAY_MS", body)
    body = re.sub(r"\bHOUR\b", "HOUR_MS", body)
    body = re.sub(r"\bMIN\b", "MINUTE_MS", body)
    return "now %s %s" % (sign, body)


def parse_bookmarks(block):
    out = []
    for obj in re.findall(r"\{([^{}]*)\}", block):
        out.append(
            "SeedBookmark(page = %s, label = %s, createdAt = %s)"
            % (
                find_expr(obj, "page"),
                kotlin_string(find_string(obj, "label")),
                duration(find_expr(obj, "at")),
            )
        )
    return out


def parse_pages(block):
    pages = []
    for page in split_pages(block):
        paras = [kotlin_string(unescape(m)) for m in re.findall(r'"((?:[^"\\]|\\.)*)"', page)]
        pages.append(paras)
    return pages


def split_pages(block):
    """Split the pages array into one string per page (the inner [ … ] groups)."""
    inner = block[block.index("[") + 1:block.rindex("]")]
    pages, depth, start = [], 0, None
    i = 0
    while i < len(inner):
        ch = inner[i]
        if ch == "[":
            if depth == 0:
                start = i
            depth += 1
        elif ch == "]":
            depth -= 1
            if depth == 0:
                pages.append(inner[start:i + 1])
        elif ch == '"':
            i += 1
            while inner[i] != '"' or inner[i - 1] == "\\":
                i += 1
        i += 1
    return pages


def kotlin_string(s):
    s = s.replace("\\", "\\\\").replace('"', '\\"').replace("$", "\\$")
    return '"%s"' % s


def main():
    docs = []
    for obj in split_top_level_objects(load_seed_docs()):
        raw_type = find_string(obj, "type")
        tags_raw = find_block(obj, "tags")
        tags = re.findall(r"'([^']*)'", tags_raw)
        folder = find_string(obj, "folderId")
        docs.append(
            {
                "id": find_string(obj, "id"),
                "name": find_string(obj, "name"),
                "type": TYPE_MAP[raw_type],
                "size_mb": float(find_expr(obj, "size")),
                "date_added": duration(find_expr(obj, "dateAdded")),
                "last_opened": duration(find_expr(obj, "lastOpened")),
                "folder": kotlin_string(folder) if folder and folder.startswith("f-") else "null",
                "tags": tags,
                "fav": find_expr(obj, "fav") == "true",
                "fav_at": duration(find_expr(obj, "favAt")),
                "page": find_expr(find_block(obj, "progress"), "page"),
                "bookmarks": parse_bookmarks(find_block(obj, "bookmarks")),
                "pages": parse_pages(find_block(obj, "pages")),
            }
        )

    lines = []
    lines.append("package app.leaf.reader.core.data.db")
    lines.append("")
    lines.append("import app.leaf.reader.core.model.DocType")
    lines.append("")
    lines.append("/**")
    lines.append(" * The demo library, generated verbatim from `design/leaf-mockup-v2.html`")
    lines.append(" * (SEED_DOCS / SEED_FOLDERS / SEED_TAGS / SEED_SMART) by")
    lines.append(" * `tools/gen_seed_content.py`. Edit the mockup, re-run the generator.")
    lines.append(" */")
    lines.append("internal data class SeedBookmark(")
    lines.append("    val page: Int,")
    lines.append("    val label: String,")
    lines.append("    val createdAt: Long")
    lines.append(")")
    lines.append("")
    lines.append("internal data class SeedDocument(")
    lines.append("    val id: String,")
    lines.append("    val name: String,")
    lines.append("    val type: DocType,")
    lines.append("    /** Size in megabytes, exactly as the mockup states it. */")
    lines.append("    val sizeMb: Double,")
    lines.append("    val dateAdded: Long,")
    lines.append("    val lastOpened: Long,")
    lines.append("    val folderId: String?,")
    lines.append("    val tags: List<String>,")
    lines.append("    val favorite: Boolean,")
    lines.append("    val favoritedAt: Long?,")
    lines.append("    /** Last page read, 0-based, as the mockup stores it. */")
    lines.append("    val page: Int,")
    lines.append("    val bookmarks: List<SeedBookmark>,")
    lines.append("    /** One entry per page; each page is its paragraphs in reading order. */")
    lines.append("    val pages: List<List<String>>")
    lines.append(")")
    lines.append("")
    lines.append("internal const val MINUTE_MS = 60_000L")
    lines.append("internal const val HOUR_MS = 60 * MINUTE_MS")
    lines.append("internal const val DAY_MS = 24 * HOUR_MS")
    lines.append("")
    lines.append("/** Builds the demo documents relative to [now], as the mockup does. */")
    lines.append("internal fun seedDocuments(now: Long): List<SeedDocument> = listOf(")
    for d in docs:
        lines.append("    SeedDocument(")
        lines.append('        id = "%s",' % d["id"])
        lines.append("        name = %s," % kotlin_string(d["name"]))
        lines.append("        type = %s," % d["type"])
        lines.append("        sizeMb = %s," % repr(d["size_mb"]))
        lines.append("        dateAdded = %s," % d["date_added"])
        lines.append("        lastOpened = %s," % d["last_opened"])
        lines.append("        folderId = %s," % d["folder"])
        lines.append(
            "        tags = listOf(%s),"
            % ", ".join(kotlin_string(t) for t in d["tags"])
        )
        lines.append("        favorite = %s," % ("true" if d["fav"] else "false"))
        lines.append("        favoritedAt = %s," % (d["fav_at"] if d["fav"] else "null"))
        lines.append("        page = %s," % d["page"])
        lines.append(
            "        bookmarks = listOf(%s)," % ", ".join(d["bookmarks"])
        )
        lines.append("        pages = listOf(")
        for page in d["pages"]:
            lines.append("            listOf(")
            for para in page:
                lines.append("                %s," % para)
            lines.append("            ),")
        lines.append("        )")
        lines.append("    ),")
    lines.append(")")
    lines.append("")

    os.makedirs(os.path.dirname(OUT), exist_ok=True)
    with open(OUT, "w", encoding="utf-8") as f:
        f.write("\n".join(lines))
    print("wrote %s (%d documents)" % (OUT, len(docs)))




if __name__ == "__main__":
    main()
