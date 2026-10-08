#!/usr/bin/env python3
"""Generate compact, valid PDF fixtures for the seeded PDF rows.

The mockup's existing page text remains the source of truth. These fixtures are
real PDFs consumed by Android PdfRenderer, not a substitute renderer.
Run from the repository root: python3 tools/generate_demo_pdfs.py
"""
from __future__ import annotations

import os
import re
import sys
import textwrap

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
sys.path.insert(0, os.path.join(ROOT, "tools"))
from gen_seed_content import (  # noqa: E402
    find_block,
    find_string,
    load_seed_docs,
    split_pages,
    split_top_level_objects,
    unescape,
)

OUT_DIR = os.path.join(ROOT, "app", "src", "main", "assets", "demo-pdf")


def seed_pdf_documents():
    docs = []
    for obj in split_top_level_objects(load_seed_docs()):
        if find_string(obj, "type") != "pdf":
            continue
        pages = []
        for page in split_pages(find_block(obj, "pages")):
            paragraphs = [unescape(value) for value in re.findall(r'"((?:[^"\\]|\\.)*)"', page)]
            pages.append(paragraphs)
        docs.append({"id": find_string(obj, "id"), "name": find_string(obj, "name"), "pages": pages})
    return docs


def pdf_hex(text: str) -> str:
    # Built-in Helvetica with WinAnsi keeps the fixture self-contained and compact.
    encoded = text.encode("cp1252", errors="replace")
    return "<" + encoded.hex().upper() + ">"


def wrap_paragraph(text: str, width: int = 88) -> list[str]:
    return textwrap.wrap(
        " ".join(text.split()),
        width=width,
        break_long_words=True,
        break_on_hyphens=False,
    ) or [""]


def make_page_stream(paragraphs: list[str], page_number: int, total_pages: int) -> bytes:
    ops = [
        "q",
        "0.82 G 0.8 w 54 718 m 558 718 l S",
        "Q",
    ]
    y = 750
    if paragraphs:
        title_lines = wrap_paragraph(paragraphs[0], 62)
        for line in title_lines:
            ops.append(f"BT /F2 18 Tf 1 0 0 1 54 {y} Tm {pdf_hex(line)} Tj ET")
            y -= 23
        y -= 8
        for paragraph in paragraphs[1:]:
            for line in wrap_paragraph(paragraph):
                ops.append(f"BT /F1 11 Tf 1 0 0 1 54 {y} Tm {pdf_hex(line)} Tj ET")
                y -= 15
            y -= 8
    ops.extend(
        [
            "q",
            "0.72 G 0.5 w 54 42 m 558 42 l S",
            "Q",
            f"BT /F1 9 Tf 1 0 0 1 54 26 Tm {pdf_hex(f'Leaf demo · page {page_number} of {total_pages}')} Tj ET",
        ]
    )
    return ("\n".join(ops) + "\n").encode("ascii")


def make_pdf(doc: dict) -> bytes:
    pages = doc["pages"]
    objects: list[bytes] = []
    objects.append(b"<< /Type /Catalog /Pages 2 0 R >>")
    page_ids = [5 + page_index * 2 for page_index in range(len(pages))]
    kids = " ".join(f"{page_id} 0 R" for page_id in page_ids)
    objects.append(f"<< /Type /Pages /Kids [{kids}] /Count {len(pages)} >>".encode("ascii"))
    objects.append(b"<< /Type /Font /Subtype /Type1 /BaseFont /Helvetica /Encoding /WinAnsiEncoding >>")
    objects.append(b"<< /Type /Font /Subtype /Type1 /BaseFont /Helvetica-Bold /Encoding /WinAnsiEncoding >>")

    for index, paragraphs in enumerate(pages):
        page_id = 5 + index * 2
        stream_id = page_id + 1
        stream = make_page_stream(paragraphs, index + 1, len(pages))
        page_obj = (
            f"<< /Type /Page /Parent 2 0 R /MediaBox [0 0 612 792] "
            f"/Resources << /Font << /F1 3 0 R /F2 4 0 R >> >> /Contents {stream_id} 0 R >>"
        ).encode("ascii")
        stream_obj = f"<< /Length {len(stream)} >>\nstream\n".encode("ascii") + stream + b"endstream"
        objects.extend((page_obj, stream_obj))

    output = bytearray(b"%PDF-1.4\n%\xe2\xe3\xcf\xd3\n")
    offsets = [0]
    for index, obj in enumerate(objects, start=1):
        offsets.append(len(output))
        output.extend(f"{index} 0 obj\n".encode("ascii"))
        output.extend(obj)
        output.extend(b"\nendobj\n")
    xref_offset = len(output)
    output.extend(f"xref\n0 {len(offsets)}\n".encode("ascii"))
    output.extend(b"0000000000 65535 f \n")
    for offset in offsets[1:]:
        output.extend(f"{offset:010d} 00000 n \n".encode("ascii"))
    output.extend(
        f"trailer\n<< /Size {len(offsets)} /Root 1 0 R >>\nstartxref\n{xref_offset}\n%%EOF\n".encode("ascii")
    )
    return bytes(output)


def main() -> None:
    os.makedirs(OUT_DIR, exist_ok=True)
    for doc in seed_pdf_documents():
        if not doc["id"] or not doc["pages"]:
            raise ValueError(f"invalid seeded PDF document: {doc}")
        path = os.path.join(OUT_DIR, f"{doc['id']}.pdf")
        data = make_pdf(doc)
        with open(path, "wb") as output:
            output.write(data)
        print(f"wrote {path} ({len(data)} bytes, {len(doc['pages'])} pages)")


if __name__ == "__main__":
    main()
